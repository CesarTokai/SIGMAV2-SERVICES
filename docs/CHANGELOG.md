# CHANGELOG - SIGMAV2-SERVICES

Proyecto: SIGMAV2 - Sistema de Inventarios y Gestión de Marbetes V2
Empresa: Tokai de México
Desarrollador: Cesar Uriel Gonzalez Saldaña
Repositorio: SIGMAV2-SERVICES

Todas las versiones siguen el formato [MAYOR.MENOR.PARCHE] y las fechas en formato AAAA-MM-DD.
El historial de cambios se organiza desde la versión más reciente a la más antigua.

---

## [1.3.4] - 2026-04-27 — Fix Crítico: PK Compuesta en Marbetes (Multi-Periodo)

### Problema
Duplicate entry al generar marbetes en un segundo periodo. La tabla `labels` tenía PK simple
(`folio`) — al iniciar un nuevo periodo, el folio 1 colisionaba con el folio 1 del periodo anterior.

### Causa Raíz
PK simple `folio` no garantizaba unicidad entre periodos. Necesario: PK compuesta `(folio, id_period)`.

### Cambios en Base de Datos
- `V1_3_4__Fix_label_primary_key_composite.sql` — Migración idempotente que cambia PK de `(folio)` a `(folio, id_period)`
- Índices auxiliares: `idx_labels_folio`, `idx_labels_period`

### Cambios en Código

| Archivo | Cambio |
|---------|--------|
| `Label.java` | `@IdClass(Label.LabelId.class)` — PK compuesta con `folio` + `periodId` |
| `JpaLabelRepository` | Tipo `JpaRepository<Label, Label.LabelId>` + método `findByFolioAndPeriodId` |
| `LabelRepository` (port) | `findByFolio(Long)` eliminado → `findByFolioAndPeriodId(Long, Long)` |
| `LabelsPersistenceAdapter` | Implementa nuevo método de búsqueda compuesta |
| `LabelQueryService` | 3 métodos actualizados con `periodId` |
| `LabelPrintService` | 4 métodos requieren `periodId` |
| `LabelCancelService` | Cancelación usa PK compuesta |
| `LabelCountService` | `findAndValidateLabelForCount` y `findLabelForUpdate` usan `findByFolioAndPeriodId` |
| `LabelService` (interface) | 3 firmas de métodos actualizadas con `periodId` |
| `LabelsController` | Endpoints PDF/reprint/fullInfo reciben `?periodId=X` como query param |
| `CountEventDTO` | `periodId` → **`@NotNull`** (antes era opcional) |
| `UpdateCountDTO` | `periodId` → `@NotNull` incluido |
| `LabelCountServiceTest` | Tests actualizados: `findByFolio` → `findByFolioAndPeriodId` |

### Impacto en el Flujo
- Generación de marbetes es **multi-periodo real** — cada periodo tiene su propia secuencia de folios
- Conteos C1/C2 no pueden cruzarse entre periodos accidentalmente
- Reportes por periodo son confiables y con trazabilidad completa
- Cancelación de marbetes afecta solo al periodo correcto

### Breaking Change — Frontend
**Todas** las llamadas que involucren un folio específico deben incluir `periodId`:
```json
// C1 / C2 registro
{ "folio": 1, "periodId": 2, "countedValue": 10 }

// Actualizar C1 / C2
{ "folio": 1, "periodId": 2, "countedValue": 12 }

// Imprimir / QR
{ "folios": [1,2,3], "periodId": 2, "warehouseId": 5 }

// Query params
GET /labels/{folio}/pdf?periodId=2
GET /labels/{folio}/full-info?periodId=2
```

### Estado
- ✅ Backend corregido y compilando
- ✅ BD migrada manualmente (V1_3_4 pendiente registrar en Flyway al reiniciar)
- ⏳ Frontend pendiente actualizar envío de `periodId` en conteos C1/C2

---

## [1.0.0] - 2026-03-10 — Primera Release Oficial

Esta versión constituye la primera release oficial de SIGMAV2 para entorno de producción.
Consolida todos los módulos desarrollados durante el ciclo 2025-2026 en un sistema estable,
con 26+ APIs REST, 8 módulos funcionales, 55+ reglas de negocio implementadas y cobertura
completa del flujo de inventario físico.

### Tecnologías de la versión

| Componente              | Version/Tecnologia             |
|-------------------------|-------------------------------|
| Java                    | JDK 21                        |
| Spring Boot             | 3.5.5                         |
| Spring Security         | 6.x con JWT                   |
| Persistencia            | JPA/Hibernate + MySQL 8.x     |
| Migraciones de BD       | Flyway                        |
| Generacion de PDF       | JasperReports 6.21.5          |
| Lectura de Excel        | Apache POI                    |
| Gestion de dependencias | Maven 3.8+                    |
| Servidor                | Apache Tomcat embebido        |
| Puerto                  | 8080                          |
| Patron de arquitectura  | Hexagonal (Ports and Adapters)|

---

## [0.9.0] - 2026-02-05 — Estabilizacion de MultiAlmacen y Correcciones de Concurrencia

### Cambios

#### Correccion: IDs saltando en tabla warehouse al importar multialmacen.xlsx

Se identifico que durante la importacion de archivos MultiAlmacen con varios almacenes nuevos,
los IDs de la tabla `warehouse` presentaban gaps (saltos) inconsistentes. La causa raiz era que
MySQL reserva el valor de AUTO_INCREMENT al momento de intentar el INSERT, y si la transaccion
se revertia por un error (violacion de constraint, fallo de red, etc.), el ID consumido no se
reutilizaba. Se corrigio el metodo `createMissingWarehouses()` en `MultiWarehouseServiceImpl.java`
para verificar la existencia del almacen por `warehouseKey` antes de intentar persistirlo,
evitando tentativas de INSERT fallidas.

- Archivo modificado: `MultiWarehouseServiceImpl.java`
- Metodo: `createMissingWarehouses()`
- Resultado: Eliminacion de gaps en AUTO_INCREMENT de la tabla `warehouse`

#### Correccion: Ordenamiento de almacenes en exportacion

Los almacenes se exportaban en el orden en que aparecian en el archivo de importacion
(por ejemplo: 3, 55, 62, 64, 40, 52...) en lugar de orden numerico ascendente por clave.
Se agrego ordenamiento numerico por `warehouse_key` al metodo `exportExistences()`.

- Archivo modificado: `MultiWarehouseServiceImpl.java`
- Metodo: `exportExistences()`
- Resultado: Almacenes siempre ordenados numericamente (1, 2, 3, 10, 15, 55, 62...)

#### Correccion: Reglas de negocio del modulo MultiAlmacen

Se corrigieron inconsistencias entre la documentacion, el formato del archivo Excel
y la logica del servicio. Los cambios principales fueron:

- El campo `warehouseKey` (CVE_ALM) se agrego al modelo `MultiWarehouseExistence` para
  garantizar consistencia entre la clave del almacen y su nombre.
- La busqueda de almacenes existentes se cambio de busqueda por nombre a busqueda por
  clave (`warehouseKey`), que es el identificador real del archivo Excel.
- La busqueda de productos existentes se actualizo para obtener la descripcion (DESCR)
  desde el catalogo de productos (`products`), no desde el archivo de importacion.
- Los almacenes nuevos detectados durante la importacion se crean automaticamente con
  una observacion que indica que fueron creados en la importacion.

---

## [0.8.0] - 2026-01-23 — Auditoria de Seguridad y Actualizacion de Dependencias

### Cambios

#### Reporte de vulnerabilidades y actualizacion de dependencias

Se realizo una auditoria completa de dependencias usando IntelliJ IDEA VulnerableLibrariesLocal
(Mend.io) y GitHub Advisory Database. Se detectaron 15 vulnerabilidades (2 criticas, 7 altas,
5 medias, 1 baja).

Acciones tomadas:

- Spring Boot actualizado de 3.5.4 a 3.5.5, corrigiendo las siguientes CVEs:
  - CVE-2025-55754 (CVSS 9.6, CRITICO) en Apache Tomcat Embed Core
  - CVE-2025-7962 (CVSS 7.5, ALTA) en Jakarta Mail
  - CVE-2025-41249 (CVSS 7.5, ALTA) en Spring Framework spring-core
  - CVE-2025-41248 (CVSS 7.5, ALTA) en Spring Security spring-security-core
  - CVE-2025-41247 (CVSS 7.5, ALTA) en Spring Security spring-security-web
  - CVE-2025-22235 (CVSS 7.5, ALTA) en Spring Framework
  - CVE-2025-22228 (CVSS 7.5, ALTA) en Spring Security BCrypt
  - CVE-2024-38820 (CVSS 7.0, ALTA) en Spring Framework

- CVE-2025-10492 (CVSS 9.8, CRITICO) en JasperReports (deserializacion Java):
  Sin parche disponible al momento del analisis. Se implementaron mitigaciones en capa
  de aplicacion: `JasperReportsSecurityAspect.java` con validacion de plantillas JRXML
  contra whitelist, validacion de parametros de reporte y auditoria de compilaciones.

- CVE-2025-48734 (CVSS 8.8, ALTA) en commons-beanutils transitivo via JasperReports:
  Mitigado con las mismas medidas aplicadas al CVE-2025-10492.

- CVE-2024-25710 (CVSS 8.1, ALTA) en Apache Commons Compress transitivo via Apache POI:
  Pendiente de parche en Apache POI. Se documenta como riesgo aceptado temporal.

---

## [0.7.0] - 2025-12-29 — Migracion de Arquitectura del Modulo de Usuarios

### Cambios

#### Migracion de interfaces deprecadas a arquitectura hexagonal

Se completaron todas las migraciones de codigo legacy que usaban la interfaz deprecada
`port.out.UserRepository` al nuevo modelo de arquitectura hexagonal con modelo de dominio puro.

Archivos eliminados:
- `BeanUserRepositoryAdapter.java` — Implementacion temporal del UserRepository deprecado.
  Reemplazado por `UserRepositoryDomainAdapter.java`.
- `UserRepository.java` (en `port/out`) — Interfaz deprecada que trabajaba directamente
  con la entidad JPA `BeanUser`. Reemplazada por la interfaz del dominio en
  `domain/port/output/UserRepository`.
- Directorio `port/out/` completo eliminado.

Archivos migrados:
- `RequestRecoveryPasswordService.java` — Actualizado para usar la nueva interfaz de
  dominio `UserRepository` y el nuevo `UserDomainMapper` para conversiones entre el
  objeto de dominio `User` y la entidad JPA `BeanUser`.

---

## [0.6.0] - 2025-12-26 — Refactorizacion del Modulo de Usuarios

### Cambios

#### Limpieza y consolidacion del modulo de usuarios

Se realizo una refactorizacion para eliminar codigo duplicado y clases no utilizadas,
sin afectar funcionalidad existente.

Clases eliminadas:
- `UserEntityHelper.java` — Clase sin referencias en ningun lugar del codigo.
- `UserResponse.java` — DTO reemplazado completamente por `UserDomainResponse.java`.
- `UserEntity.java` — Entidad JPA duplicada que replicaba exactamente a `BeanUser.java`.
  Se consolido `BeanUser` como unica entidad JPA para usuarios.
- `BeanUserRepositoryAdapter.java` — Adaptador no utilizado porque `UserRepositoryAdapter`
  tenia la anotacion `@Primary` y era siempre el inyectado por Spring.

Actualizaciones en cascade por la eliminacion de `UserEntity`:
- `BeanPersonalInformation.java` — Tipo de relacion cambiado de `UserEntity` a `BeanUser`.
- `PersonalInformationDomainMapper.java` — Referencias de campo `userId` -> `id`.
- `BeanPersonalInformationMapper.java` — Referencias de campo `userId` -> `id`.
- `JpaPersonalInformationRepository.java` — Metodos de consulta renombrados:
  `findByUser_UserId` -> `findByUser_Id`,
  `existsByUser_UserId` -> `existsByUser_Id`,
  `deleteByUser_UserId` -> `deleteByUser_Id`.
- `PersonalInformationRepositoryAdapter.java` — Actualizado para usar los nuevos metodos.
- `AuditAspect.java` — Actualizado para usar el metodo renombrado.

---

## [0.5.0] - 2025-12-16 — Mejoras en Impresion, Validacion de Cancelacion y Conteos

### Cambios

#### Cambio: Impresion automatica de marbetes (eliminacion de rangos manuales)

El endpoint `POST /api/sigmav2/labels/print` fue modificado para imprimir automaticamente
todos los marbetes pendientes del periodo y almacen indicados. Los campos `startFolio` y
`endFolio` fueron eliminados del request body. Esto elimina errores por rangos incorrectos
y simplifica el proceso de impresion.

Request anterior:
```json
{ "periodId": 16, "warehouseId": 369, "startFolio": 1, "endFolio": 50 }
```

Request actual:
```json
{ "periodId": 16, "warehouseId": 369 }
```

#### Nuevo: API para contar marbetes pendientes de impresion

Nuevo endpoint `POST /api/sigmav2/labels/pending-print-count` que devuelve en tiempo real
cuantos marbetes estan pendientes de impresion para un periodo y almacen dados. Permite
al frontend decidir si mostrar u ocultar el boton "Imprimir" sin realizar llamadas
innecesarias al endpoint de impresion.

Response:
```json
{
  "count": 25,
  "periodId": 16,
  "warehouseId": 369,
  "warehouseName": "Almacen Principal",
  "periodName": "2025-12-16"
}
```

#### Cambio: Validacion al cancelar marbetes sin folios asignados

Se agrego validacion en el proceso de cancelacion que impide cancelar un marbete cuya
solicitud tenga `requestedLabels = 0`. Esto prevenia errores de integridad al intentar
cancelar registros sin datos de folio validos.

#### Cambio: Los marbetes sin existencias ya no se cancelan automaticamente al generar

Regla de negocio corregida. En una version anterior, al generar marbetes, los productos
con `existencias = 0` eran enviados automaticamente a `labels_cancelled`. Esta logica fue
eliminada. Ahora todos los marbetes se generan en la tabla `labels` con estado `GENERADO`
independientemente de las existencias, permitiendo el conteo fisico de todos los productos.

---

## [0.4.0] - 2025-12-10 — Modulo de Generacion de Archivo TXT de Existencias

### Cambios

#### Nuevo: Endpoint para generar archivo TXT de inventario fisico final

Se implemento el endpoint `POST /api/sigmav2/labels/generate-file` que genera un archivo
de texto plano con el inventario fisico consolidado del periodo.

Caracteristicas del archivo generado:
- Nombre: `Existencias_{NombrePeriodo}.txt`
- Ubicacion: `C:\Sistemas\SIGMA\Documentos\`
- Formato: Columnas delimitadas por tabuladores (Clave, Descripcion, Existencias)
- Logica de conteo: usa C2 si existe, de lo contrario usa C1
- Excluye marbetes cancelados
- Ordena alfabeticamente por clave de producto
- Si el archivo ya existe para el periodo, lo sobreescribe

Roles con permiso: ADMINISTRADOR, AUXILIAR, ALMACENISTA.

---

## [0.3.0] - 2025-12-08 — Cancelacion de Marbetes y 8 Reportes Especializados

### Cambios

#### Nuevo: Cancelacion de marbetes con trazabilidad completa

Se implemento la cancelacion de marbetes mediante `POST /api/sigmav2/labels/cancel`.
Los marbetes cancelados no se eliminan de la base de datos; se mueven a la tabla
`labels_cancelled` manteniendo: motivo de cancelacion, usuario que cancelo, fecha y hora,
y las existencias al momento de la cancelacion. Los marbetes cancelados pueden ser
reactivados posteriormente.

Validaciones implementadas:
- El folio debe existir en el sistema.
- El marbete no puede estar ya cancelado.
- El marbete debe tener al menos un folio asignado (`requestedLabels > 0`).
- El usuario debe tener acceso al almacen del marbete.

#### Nuevo: 8 reportes especializados del modulo de marbetes

| Reporte                        | Endpoint                                               |
|-------------------------------|--------------------------------------------------------|
| Distribucion de folios         | POST /api/sigmav2/labels/reports/distribution          |
| Listado general                | POST /api/sigmav2/labels/reports/list                  |
| Marbetes pendientes            | POST /api/sigmav2/labels/reports/pending               |
| Marbetes con diferencias C1/C2 | POST /api/sigmav2/labels/reports/with-differences      |
| Marbetes cancelados            | POST /api/sigmav2/labels/reports/cancelled             |
| Comparativo fisico vs teorico  | POST /api/sigmav2/labels/reports/comparative           |
| Almacen con detalle (PDF)      | POST /api/sigmav2/labels/reports/warehouse-detail/pdf  |
| Producto con detalle           | POST /api/sigmav2/labels/reports/product-detail        |

Todos los reportes aceptan `periodId` obligatorio y `warehouseId` opcional. Si no se
indica `warehouseId`, el reporte incluye todos los almacenes del periodo.

---

## [0.2.0] - 2025-12-02 — Generacion de PDF con JasperReports

### Cambios

#### Nuevo: Impresion de marbetes en PDF usando JasperReports

Se implemento la generacion de PDF para el endpoint `POST /api/sigmav2/labels/print`.
El endpoint ahora retorna un archivo PDF con Content-Type `application/pdf` y nombre
de archivo dinamico, listo para descarga directa desde el navegador.

Implementacion:
- Dependencias `jasperreports` y `jasperreports-fonts` agregadas al `pom.xml`.
- Nuevo servicio `JasperLabelPrintService.java` que carga la plantilla JRXML, precarga
  productos y almacenes para evitar el problema N+1, y genera el PDF.
- Plantilla `Carta_Tres_Cuadros.jrxml` creada con diseno de 3 marbetes por fila.
- El controlador `LabelsController.java` actualizado para retornar `byte[]` con headers
  correctos de PDF.
- El estado del marbete se actualiza a `IMPRESO` con registro de usuario y fecha/hora.

#### Correccion: Error JRException al cargar plantilla JasperReports

El metodo `loadJasperTemplate()` intentaba retornar un `InputStream` en lugar de un
objeto `JasperReport` compilado. Se corrigio la firma del metodo para que devuelva
`JasperReport` y se agrego la logica correcta para intentar cargar primero el archivo
`.jasper` precompilado y, si no existe, compilar el `.jrxml` en tiempo de ejecucion.

---

## [0.1.0] - 2025-11-28 — Base del Sistema: Autenticacion, Usuarios e Importacion

### Cambios

#### Nuevo: Modulo de autenticacion con JWT

Se implemento el sistema de autenticacion basado en JSON Web Tokens (JWT).

Funcionalidades:
- Login con usuario y contrasena: `POST /api/sigmav2/auth/login`
- Expiracion de tokens configurable en `application.properties`
- Registro de actividad del usuario en cada peticion autenticada:
  `lastLoginAt`, `lastActivityAt`, `passwordChangedAt`, `createdAt`, `updatedAt`

#### Nuevo: Sistema de revocacion de tokens JWT en tiempo real

Se implemento un sistema para invalidar tokens JWT de forma inmediata al cerrar sesion,
sin esperar a su expiracion natural.

Componentes implementados:
- `RevokedToken.java` — Entidad JPA para almacenar tokens revocados.
- `RevokedTokenRepository.java` — Repositorio de consultas.
- `TokenRevocationService.java` — Servicio con purga automatica programada cada hora
  para eliminar tokens expirados de la tabla.
- `JwtRevocationFilter.java` — Filtro que verifica si el token esta revocado antes de
  pasar a la autenticacion normal. Si esta revocado, responde con 401 inmediatamente.
- `LogoutController.java` — Endpoint `POST /api/sigmav2/auth/logout`.
- Migracion Flyway `V1_0_7__Create_revoked_tokens_table.sql`.

Flujo de revocacion:
1. Usuario llama `POST /api/sigmav2/auth/logout`
2. El servidor guarda el JTI del token en la tabla `revoked_tokens`
3. Cualquier peticion posterior con ese token es rechazada con 401 por `JwtRevocationFilter`

#### Nuevo: Control de acceso por roles

Se definieron cuatro roles del sistema con alcances diferenciados:

| Rol                 | Descripcion                                                           |
|---------------------|-----------------------------------------------------------------------|
| ADMINISTRADOR       | Acceso completo a todas las funciones del sistema                     |
| AUXILIAR            | Acceso completo excepto actualizar segundo conteo (C2)                |
| ALMACENISTA         | Acceso restringido a sus almacenes asignados                          |
| AUXILIAR_DE_CONTEO  | Solo puede realizar conteos y consultar reportes de sus almacenes     |

#### Nuevo: Modulo de usuarios

- Alta, baja y modificacion de usuarios: `POST /api/sigmav2/users`,
  `PUT /api/sigmav2/users/{id}`
- Listado de usuarios: `GET /api/sigmav2/users`
- Perfil del usuario autenticado: `GET /api/sigmav2/users/me`
- Actividad del usuario: `GET /api/sigmav2/users/me/activity`
- Asignacion de roles y almacenes autorizados por usuario
- Bloqueo temporal de cuenta por intentos de contrasena fallidos

#### Nuevo: Sistema de recuperacion de contrasena

- Solicitud de recuperacion por parte del usuario: `POST /api/sigmav2/auth/request-recovery`
- Solo el ADMINISTRADOR puede aprobar o rechazar solicitudes pendientes
- Historial completo de solicitudes disponible para el ADMINISTRADOR
- Correcciones aplicadas posteriormente (ver version 0.3.1):
  la logica original permitia a roles no administradores consultar solicitudes ajenas;
  fue corregida para que solo ADMINISTRADOR pueda ver y gestionar todas las solicitudes.

#### Nuevo: Modulo de importacion de archivos Excel

Se implementaron dos endpoints de importacion:

`POST /api/sigmav2/inventory/import` — Importa el catalogo maestro de productos desde
`inventario.xlsx`. Crea nuevos productos y actualiza los existentes. Los productos
ausentes en la importacion se marcan como baja automaticamente.

`POST /api/sigmav2/multiwarehouse/import` — Importa existencias teoricas por almacen
y periodo desde `multialmacen.xlsx`. Crea almacenes automaticamente si no existen.
Sincroniza automaticamente con la tabla `inventory_stock`.

Ruta de archivos de entrada y salida: `C:\Sistemas\SIGMA\Documentos\`

#### Nuevo: Modulo de periodos de inventario

- Creacion y consulta de periodos de inventario
- Cada marbete, conteo y reporte queda vinculado a un periodo especifico
- Los periodos permiten comparar inventarios historicos

#### Nuevo: Modulo de marbetes — solicitud de folios y generacion

Flujo inicial de marbetes implementado:

- `POST /api/sigmav2/labels/request` — Reserva un bloque de folios consecutivos para
  un almacen en un periodo. Genera un `LabelRequest` que sirve como origen de los marbetes.

- `POST /api/sigmav2/labels/generate` — Genera marbetes de forma masiva dentro del rango
  de folios solicitados. Asigna existencias desde `inventory_stock`. Incluye marbetes
  con existencias = 0 para garantizar el registro completo del inventario.

- `POST /api/sigmav2/labels/for-count/list` — Lista todos los marbetes de un periodo y
  almacen con estado actual de conteo e indicador de cancelacion.

#### Nuevo: Modulo de conteos fisicos

- `POST /api/sigmav2/labels/counts/c1` — Registrar primer conteo. Requiere que el
  marbete este en estado IMPRESO. Previene conteos duplicados.
- `POST /api/sigmav2/labels/counts/c2` — Registrar segundo conteo. Requiere C1 previo.
- `PUT /api/sigmav2/labels/counts/c1` — Corregir primer conteo.
- `PUT /api/sigmav2/labels/counts/c2` — Corregir segundo conteo. Solo ADMINISTRADOR y
  AUXILIAR_DE_CONTEO pueden ejecutar esta accion.

#### Nuevo: Sincronizacion de inventory_stock

Se implemento la tabla `inventory_stock` como capa optimizada para consultas rapidas
de existencias por producto, almacen y periodo. La tabla se sincroniza automaticamente
al importar el archivo `multialmacen.xlsx`. Incluye migracion Flyway
`V1_1_2__Populate_inventory_stock_from_multiwarehouse.sql` para poblar la tabla en
instalaciones existentes.

#### Nuevo: Modulo de inventario y catalogo

- `GET /api/sigmav2/inventory/catalog` — Catalogo completo de productos con paginacion y filtros
- `GET /api/sigmav2/inventory/stock` — Existencias teoricas por producto y almacen

#### Correccion: Duplicados de productos en importacion

Se detecto que el metodo `findByCveArt()` lanzaba la excepcion
"Query did not return a unique result" cuando habia registros duplicados por
`cve_art` en la tabla `products`. Se aplico la migracion
`V1_0_10__Clean_duplicate_products_and_add_unique_constraint.sql` que elimina los
duplicados (manteniendo el registro mas antiguo) y agrega un constraint UNIQUE a nivel
de base de datos. La entidad `ProductEntity.java` fue actualizada con la anotacion
`@UniqueConstraint` correspondiente.

#### Correccion: Folios saltados al generar marbetes para productos sin existencias

Se identifico que cuando el frontend o un proceso automatico enviaba solicitudes con
`requestedLabels = 0` para productos con existencias = 0, se creaban huecos en la
secuencia de folios. Se agrego validacion en `LabelServiceImpl.requestLabels()` que
impide solicitar 0 folios para un producto que si existe en el catalogo de inventario,
obligando a solicitar al menos 1 folio para garantizar la trazabilidad del conteo fisico.

---

## Reglas de Negocio Fundamentales del Sistema

Las siguientes reglas aplican a todas las versiones a partir de 1.0.0:

1. Un marbete representa un producto en un almacen en un periodo especifico.
2. Los folios son consecutivos y unicos por periodo.
3. Un marbete cancelado no se elimina; se mueve a la tabla `labels_cancelled`.
4. El segundo conteo (C2) solo se puede registrar si existe C1 para ese marbete.
5. Solo ADMINISTRADOR puede actualizar C2.
6. No se puede cancelar un marbete sin folios asignados.
7. Los marbetes con existencias = 0 tambien se generan para registro completo del inventario.
8. El archivo de existencias usa C2 cuando existe; de lo contrario usa C1.
9. Cada usuario opera unicamente en los almacenes que tiene asignados,
   excepto el rol ADMINISTRADOR que tiene acceso global.
10. El token JWT se invalida inmediatamente al hacer logout, sin esperar su expiracion natural.

---

## Tablas Principales de la Base de Datos

| Tabla                         | Descripcion                                               |
|-------------------------------|-----------------------------------------------------------|
| users                         | Usuarios del sistema con roles y estados                  |
| revoked_tokens                | Tokens JWT revocados para logout inmediato                |
| products                      | Catalogo maestro de productos                             |
| warehouse                     | Catalogo de almacenes                                     |
| periods                       | Periodos de inventario                                    |
| label_requests                | Solicitudes de folios de marbetes                         |
| labels                        | Marbetes activos con conteos C1 y C2                      |
| labels_cancelled              | Marbetes cancelados (historico permanente)                |
| label_prints                  | Registro de impresiones de marbetes                       |
| multiwarehouse_existences     | Existencias importadas por almacen y periodo              |
| inventory_stock               | Tabla optimizada para consultas de existencias            |
| request_recovery_password     | Solicitudes de recuperacion de contrasena                 |

---

*SIGMAV2-SERVICES — Tokai de Mexico — Marzo 2026*

