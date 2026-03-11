# RELEASE NOTES — SIGMAV2 v1.0.0

**Versión:** 1.0.0  
**Fecha de Release:** 10 de Marzo de 2026  
**Estado:** PRODUCCIÓN  
**Tipo:** Primera Release Oficial  

---

## DESCRIPCIÓN GENERAL

**SIGMAV2** (Sistema de Inventarios y Gestión de Marbetes V2) es la segunda generación del sistema de control de inventarios físicos de **Tokai de México**. Esta primera release oficial consolida todas las funcionalidades implementadas durante el ciclo de desarrollo 2025–2026.

El sistema permite realizar un proceso completo de **verificación física y teórica del inventario** a través de:

- Importación de catálogos y existencias desde archivos Excel
- Generación e impresión de marbetes (etiquetas de identificación)
- Registro de conteos físicos (primer y segundo conteo)
- Generación de reportes de diferencias
- Correcciones iterativas hasta alcanzar inventario en cero diferencias

---

## FUNCIONALIDADES INCLUIDAS EN ESTA VERSIÓN

### 1. Módulo de Seguridad y Usuarios

#### Autenticación y Sesiones
- Login con usuario y contraseña mediante **JWT (JSON Web Token)**
- Expiración de tokens configurable
- Sistema de **revocación de tokens** en tiempo real:
  - Al hacer logout, el token queda invalidado inmediatamente en el servidor
  - No es necesario esperar a que el token expire de manera natural
  - Los tokens revocados se almacenan en tabla `revoked_tokens`
  - Purga automática de tokens expirados cada hora
- Registro de actividad del usuario en cada petición autenticada:
  - `lastLoginAt` — Último inicio de sesión
  - `lastActivityAt` — Última actividad registrada
  - `passwordChangedAt` — Última vez que se cambió la contraseña
  - `createdAt` / `updatedAt` — Fechas de creación y modificación

#### Control de Acceso por Roles
| Rol | Descripción |
|-----|-------------|
| `ADMINISTRADOR` | Acceso completo a todas las funciones del sistema |
| `AUXILIAR` | Acceso completo excepto actualizar segundo conteo (C2) |
| `ALMACENISTA` | Acceso restringido solo a sus almacenes asignados |
| `AUXILIAR_DE_CONTEO` | Solo puede realizar conteos y consultar reportes de sus almacenes |

#### Módulo de Usuarios
- Alta, baja y modificación de usuarios
- Asignación de roles y almacenes autorizados
- Sistema de **recuperación de contraseña**:
  - El usuario solicita la recuperación
  - Solo el ADMINISTRADOR puede aprobar o rechazar la solicitud
  - Historial completo de solicitudes
- Bloqueo temporal de cuenta por intentos de contraseña fallidos
- API de actividad del usuario: `GET /api/sigmav2/users/me/activity`
- API de información personal: `GET /api/sigmav2/users/me`

---

### 2. Módulo de Importación de Archivos Excel

#### Catálogo de Productos — `inventario.xlsx`
- **Endpoint:** `POST /api/sigmav2/inventory/import`
- Crea y actualiza el catálogo maestro de productos
- Campos procesados: Clave de artículo, Descripción, Unidad de medida, Estatus
- Productos no presentes en la importación se marcan como baja automáticamente

**Ruta del archivo:** `C:\Sistemas\SIGMA\Documentos\inventario.xlsx`

#### Existencias Multialmacén — `multialmacen.xlsx`
- **Endpoint:** `POST /api/sigmav2/multiwarehouse/import`
- Registra existencias teóricas por almacén y periodo
- Crea automáticamente almacenes nuevos si no existen
- Sincronización automática con tabla `inventory_stock`
- Establece el **punto de referencia teórico** del inventario

**Ruta del archivo:** `C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx`

---

### 3. Módulo de Periodos de Inventario

- Creación de periodos de inventario por fecha o nombre
- Cada marbete, conteo y reporte está vinculado a un periodo específico
- Los periodos permiten comparar múltiples inventarios históricos
- **Proceso recomendado:** Crear el periodo antes de iniciar cualquier operación de inventario

---

### 4. Módulo de Marbetes (Gestión Completa)

Los marbetes son etiquetas de identificación asignadas a cada producto en cada almacén durante un periodo de inventario.

#### 4.1 Solicitar Folios
- **Endpoint:** `POST /api/sigmav2/labels/request`
- Reserva un bloque de folios consecutivos para un almacén
- Registrado con usuario, fecha y hora de solicitud
- Genera un `LabelRequest` que sirve como origen de los marbetes

```json
{
  "periodId": 7,
  "warehouseId": 254,
  "requestedLabels": 100,
  "observations": "Primera solicitud almacén principal"
}
```

#### 4.2 Generar Marbetes
- **Endpoint:** `POST /api/sigmav2/labels/generate`
- Genera los marbetes de forma masiva dentro del rango de folios solicitados
- Asigna existencias desde `inventory_stock`
- Incluye marbetes con y sin existencias (existencias = 0)
- Previene duplicados en el mismo periodo/almacén

#### 4.3 Imprimir Marbetes — PDF con JasperReports
- **Endpoint:** `POST /api/sigmav2/labels/print`
- Genera un archivo **PDF listo para imprimir** usando JasperReports
- Impresión **automática** de todos los marbetes pendientes (sin necesidad de indicar rango)
- Actualiza el estado del marbete a `IMPRESO`
- Registra usuario y fecha/hora de impresión

```json
{
  "periodId": 7,
  "warehouseId": 254
}
```

#### 4.4 Contar Marbetes Pendientes de Impresión
- **Endpoint:** `POST /api/sigmav2/labels/pending-print-count`
- Devuelve cuántos marbetes están pendientes antes de imprimir
- Útil para el frontend: mostrar/ocultar el botón "Imprimir"

#### 4.5 Cancelar Marbete
- **Endpoint:** `POST /api/sigmav2/labels/cancel`
- Cancela un marbete con motivo obligatorio
- Los marbetes cancelados se mueven a la tabla `labels_cancelled` (no se eliminan)
- Posibilidad de reactivación posterior
- Validaciones previas a la cancelación:
  - El marbete debe tener folios asignados
  - El marbete no puede estar ya cancelado
  - El usuario debe tener acceso al almacén correspondiente

---

### 5. Módulo de Conteos Físicos

Una vez impresos los marbetes, el personal de almacén realiza el conteo físico. El sistema soporta dos rondas de conteo para mayor precisión.

#### Primer Conteo (C1)
- **Registro:** `POST /api/sigmav2/labels/counts/c1`
- **Actualización:** `PUT /api/sigmav2/labels/counts/c1`
- Requiere que el marbete esté en estado `IMPRESO`
- Previene conteos duplicados por marbete
- Auditoría completa (usuario, fecha, hora)

#### Segundo Conteo (C2)
- **Registro:** `POST /api/sigmav2/labels/counts/c2`
- **Actualización:** `PUT /api/sigmav2/labels/counts/c2`
- Requiere que exista un C1 registrado previamente
- Solo `ADMINISTRADOR` y `AUXILIAR_DE_CONTEO` pueden actualizar C2

#### Listado para Conteo
- **Endpoint:** `POST /api/sigmav2/labels/for-count/list`
- Muestra todos los marbetes de un periodo/almacén con:
  - Estado actual del conteo (sin conteo, C1 registrado, C2 registrado)
  - Indicador de marbetes cancelados
  - Existencias registradas

---

### 6. Módulo de Reportes

Todos los reportes se obtienen mediante peticiones `POST` con `periodId` y opcionalmente `warehouseId`. Si no se envía `warehouseId`, el reporte incluye todos los almacenes.

| # | Reporte | Endpoint | Descripción |
|---|---------|----------|-------------|
| 1 | Distribución de Marbetes | `POST /api/sigmav2/labels/reports/distribution` | Muestra quién solicitó folios, en qué almacén, rango de folios |
| 2 | Listado de Marbetes | `POST /api/sigmav2/labels/reports/list` | Lista completa con folio, producto, conteos y estado |
| 3 | Marbetes Pendientes | `POST /api/sigmav2/labels/reports/pending` | Marbetes sin C1 o C2 (excluye cancelados) |
| 4 | Marbetes con Diferencias | `POST /api/sigmav2/labels/reports/with-differences` | Marbetes donde C1 != C2 (ambos deben existir) |
| 5 | Marbetes Cancelados | `POST /api/sigmav2/labels/reports/cancelled` | Muestra motivo, usuario y fecha de cancelación |
| 6 | Comparativo (Físico vs Teórico) | `POST /api/sigmav2/labels/reports/comparative` | Compara existencias físicas vs existencias teóricas importadas |
| 7 | Almacén con Detalle (PDF) | `POST /api/sigmav2/labels/reports/warehouse-detail/pdf` | PDF ordenado: Almacén -> Producto -> No. Marbete |
| 8 | Producto con Detalle | `POST /api/sigmav2/labels/reports/product-detail` | Vista: Producto -> Almacén -> No. Marbete con estado y existencias |

#### Reporte Especial: Inventario Físico por Almacén (PDF)
- Genera un PDF en formato tabular con columnas: Almacén, Producto, Descripción, Unidad, No. Marbete, Cantidad, Estado
- Los marbetes cancelados se muestran marcados como `CANCELADO`
- Los marbetes activos no muestran etiqueta de estado (celda vacía)
- Generado con JasperReports

---

### 7. Módulo de Generación de Archivo de Existencias

- **Endpoint:** `POST /api/sigmav2/labels/generate-file`
- Genera un archivo de texto plano con el inventario físico final
- **Nombre del archivo:** `Existencias_{NombrePeriodo}.txt`
- **Ubicación:** `C:\Sistemas\SIGMA\Documentos\`
- **Formato:** Columnas delimitadas por tabuladores: Clave, Descripción, Existencias
- **Lógica de cálculo:**
  - Usa C2 si existe, de lo contrario usa C1
  - Excluye marbetes cancelados
  - Ordena alfabéticamente por clave de producto
- Si ya existe el archivo de ese periodo, lo sobrescribe

---

### 8. Módulo de Inventario y Catálogo

- `GET /api/sigmav2/inventory/catalog` — Consulta el catálogo completo de productos con paginación y filtros
- `GET /api/sigmav2/inventory/stock` — Consulta las existencias teóricas por producto y almacén
- Integración completa con `inventory_stock` para consultas rápidas y eficientes

---

## ARQUITECTURA TÉCNICA

| Componente | Tecnología |
|------------|-----------|
| **Framework** | Spring Boot 3.x |
| **Seguridad** | Spring Security + JWT |
| **Persistencia** | JPA/Hibernate + MySQL |
| **Migraciones de BD** | Flyway |
| **Generación de PDFs** | JasperReports 6.21.5 |
| **Lectura de Excel** | Apache POI |
| **Gestión de dependencias** | Maven |
| **Servidor** | Apache Tomcat (embebido) |
| **Puerto** | 8080 |

### Patrón de Arquitectura
- **Arquitectura Hexagonal (Ports & Adapters)**
- Separación clara de capas: dominio, aplicación, infraestructura, adaptadores
- Principios SOLID aplicados

---

## RESUMEN DE APIs PRINCIPALES

### Autenticación
```
POST   /api/sigmav2/auth/login                          → Iniciar sesión
POST   /api/sigmav2/auth/logout                         → Cerrar sesión (revoca token)
POST   /api/sigmav2/auth/request-recovery               → Solicitar recuperación de contraseña
```

### Usuarios
```
GET    /api/sigmav2/users/me                            → Perfil del usuario actual
GET    /api/sigmav2/users/me/activity                   → Actividad del usuario
POST   /api/sigmav2/users                               → Crear usuario (ADMIN)
GET    /api/sigmav2/users                               → Listar usuarios (ADMIN)
PUT    /api/sigmav2/users/{id}                          → Actualizar usuario
```

### Importación
```
POST   /api/sigmav2/inventory/import                    → Importar inventario.xlsx
POST   /api/sigmav2/multiwarehouse/import               → Importar multialmacen.xlsx
```

### Marbetes
```
POST   /api/sigmav2/labels/request                      → Solicitar folios
POST   /api/sigmav2/labels/generate                     → Generar marbetes
POST   /api/sigmav2/labels/print                        → Imprimir marbetes (PDF)
POST   /api/sigmav2/labels/pending-print-count          → Marbetes pendientes de impresión
POST   /api/sigmav2/labels/cancel                       → Cancelar marbete
POST   /api/sigmav2/labels/for-count/list               → Listar marbetes para conteo
```

### Conteos
```
POST   /api/sigmav2/labels/counts/c1                    → Registrar primer conteo
POST   /api/sigmav2/labels/counts/c2                    → Registrar segundo conteo
PUT    /api/sigmav2/labels/counts/c1                    → Actualizar primer conteo
PUT    /api/sigmav2/labels/counts/c2                    → Actualizar segundo conteo (ADMIN/AUX_CONTEO)
```

### Reportes
```
POST   /api/sigmav2/labels/reports/distribution         → Distribución de folios
POST   /api/sigmav2/labels/reports/list                 → Listado general de marbetes
POST   /api/sigmav2/labels/reports/pending              → Marbetes pendientes
POST   /api/sigmav2/labels/reports/with-differences     → Marbetes con diferencias C1 vs C2
POST   /api/sigmav2/labels/reports/cancelled            → Marbetes cancelados
POST   /api/sigmav2/labels/reports/comparative          → Comparativo físico vs teórico
POST   /api/sigmav2/labels/reports/warehouse-detail/pdf → PDF: Almacén con detalle
POST   /api/sigmav2/labels/reports/product-detail       → Vista: Producto con detalle en todos los almacenes
```

### Archivo de Existencias
```
POST   /api/sigmav2/labels/generate-file                → Generar Existencias_{Periodo}.txt
```

---

## FLUJO DE TRABAJO RECOMENDADO

```
1. CREAR PERIODO DE INVENTARIO
   └─ Define el contexto temporal del inventario

2. IMPORTAR ARCHIVOS EXCEL
   ├─ POST /inventory/import        → Catálogo de productos (inventario.xlsx)
   └─ POST /multiwarehouse/import   → Existencias por almacén (multialmacen.xlsx)

3. SOLICITAR FOLIOS
   └─ POST /labels/request          → Reservar rango de folios para el almacén

4. GENERAR MARBETES
   └─ POST /labels/generate         → Crear marbetes para los productos del almacén

5. IMPRIMIR MARBETES
   ├─ POST /labels/pending-print-count  → Verificar cuántos hay pendientes
   └─ POST /labels/print             → Generar PDF e imprimir

6. CONTEO FÍSICO EN ALMACÉN
   ├─ POST /labels/counts/c1         → Primer conteo (C1)
   └─ POST /labels/counts/c2         → Segundo conteo (C2) para validación

7. REVISAR DIFERENCIAS
   └─ POST /labels/reports/with-differences  → Identificar discrepancias C1 vs C2

8. CORREGIR Y ACTUALIZAR
   ├─ PUT /labels/counts/c1          → Corregir C1 si necesario
   └─ PUT /labels/counts/c2          → Corregir C2 si necesario

9. GENERAR REPORTES FINALES
   ├─ POST /labels/reports/comparative      → Físico vs Teórico
   ├─ POST /labels/reports/warehouse-detail/pdf  → PDF por almacén
   └─ POST /labels/generate-file            → Archivo TXT final
```

---

## BASE DE DATOS — TABLAS PRINCIPALES

| Tabla | Descripción |
|-------|-------------|
| `users` | Usuarios del sistema con roles y estados |
| `revoked_tokens` | Tokens JWT revocados (para logout inmediato) |
| `products` | Catálogo maestro de productos |
| `warehouse` | Catálogo de almacenes |
| `periods` | Periodos de inventario |
| `label_requests` | Solicitudes de folios de marbetes |
| `labels` | Marbetes activos con conteos C1 y C2 |
| `labels_cancelled` | Marbetes cancelados (histórico) |
| `label_prints` | Registro de impresiones de marbetes |
| `multiwarehouse_existences` | Existencias importadas por almacén y periodo |
| `inventory_stock` | Tabla optimizada para consultas de existencias |
| `request_recovery_password` | Solicitudes de recuperación de contraseña |

---

## REGLAS DE NEGOCIO CLAVE

1. **Un marbete = Un producto en un almacén en un periodo**
2. **Los folios son consecutivos y únicos por periodo**
3. **Un marbete cancelado NO se elimina**, se mueve a `labels_cancelled`
4. **C2 solo se puede registrar si existe C1**
5. **Solo ADMINISTRADOR puede actualizar C2**
6. **No se puede cancelar un marbete sin folios asignados**
7. **Los marbetes sin existencias (cantidad = 0) también se generan** para registro completo del inventario
8. **El archivo de existencias usa C2 cuando existe; de lo contrario usa C1**
9. **Cada usuario solo puede operar en los almacenes que tiene asignados** (excepto ADMINISTRADOR)
10. **El token JWT se invalida inmediatamente al hacer logout**, sin esperar expiración

---

## REQUISITOS DE INSTALACIÓN

### Requisitos del Servidor
- **Java:** JDK 17 o superior
- **Base de datos:** MySQL 8.x
- **Maven:** 3.8+

### Configuración Inicial
1. Clonar el repositorio
2. Configurar `application.properties` con los datos de conexión a MySQL
3. Ejecutar `mvn clean install` para compilar
4. Las migraciones de base de datos se aplican automáticamente con **Flyway** al iniciar la aplicación
5. Iniciar con `mvn spring-boot:run` o ejecutando el JAR generado

### Rutas de Archivos Requeridas
Crear las siguientes carpetas en el servidor:
```
C:\Sistemas\SIGMA\Documentos\
```
Aquí se leerán los archivos Excel de importación y se generarán los archivos TXT de existencias.

---

## PROBLEMAS CONOCIDOS Y SOLUCIONES EN ESTA VERSIÓN

| Problema | Solución Aplicada |
|----------|------------------|
| Marbetes sin existencias no aparecían en reportes | Corrección de lógica: se incluyen marbetes con cantidad = 0 |
| Folios saltados al generar marbetes para múltiples almacenes | Corrección de manejo de concurrencia en generación de folios |
| Error al cancelar marbetes sin folios asignados | Validación agregada: `requestedLabels > 0` antes de cancelar |
| Impresión manual de rangos era propensa a errores | Eliminados campos `startFolio` / `endFolio`: ahora es automático |
| Duplicación de productos en la importación | Script de limpieza de duplicados incluido |
| Tokens de sesión no se invalidaban al cerrar sesión | Sistema de revocación de tokens implementado |
| El logout no desconectaba al usuario del servidor | Tabla `revoked_tokens` + `JwtRevocationFilter` implementados |

---

## MÉTRICAS DE DESARROLLO

| Métrica | Valor |
|---------|-------|
| APIs REST implementadas | **26+** |
| DTOs creados | **32+** |
| Reglas de negocio implementadas | **55+** |
| Validaciones de seguridad | **26** |
| Reportes disponibles | **8** |
| Módulos del sistema | **7** |
| Documentos técnicos generados | **150+** |
| Estado de implementación | **100%** |

---

## EQUIPO

**Desarrollado por:** Cesar Uriel Gonzalez Saldaña

**Empresa:** Tokai de México  
**Contacto técnico:** Área de Sistemas, Tokai de México

---



*SIGMAV2 v1.0.0 — Tokai de México — Marzo 2026*
