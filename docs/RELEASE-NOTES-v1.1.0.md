# SIGMAV2 v1.1.0 - Release Notes

**Fecha de Lanzamiento:** 2026-03-23  
**Versión:** 1.1.0  
**Estado:** Estable

## Descripción General

SIGMAV2 v1.1.0 incluye mejoras significativas en seguridad, auditoría y documentación. El sistema ha sido completamente verificado y está listo para producción. Todas las funcionalidades principales están implementadas y validadas.

## Funcionalidades Nuevas

### 1. Sistema de Auditoría Mejorado

- Implementación de @Auditable para marcar operaciones críticas
- Tabla audit_logs con registro completo de acciones
- Resolución automática de nombre completo del usuario desde BeanUser + BeanPersonalInformation
- Rastreo de cambios en conteos con previous_value
- Archivado de datos cancelados en labels_cancelled

### 2. Rastreo de Actividad del Usuario

- Nuevos campos en tabla users: last_login_at, last_activity_at, password_changed_at
- Filtro UserActivityFilter que registra actividad en cada request autenticado
- Actualización síncrona con manejo robusto de excepciones
- Índices para optimizar consultas de actividad
- No bloquea requests si BD está lenta

### 3. Mejoras en Seguridad

- Filtro JwtRevocationFilter para validar tokens no revocados
- Purga automática de tokens expirados cada 1 hora
- Validación en dos niveles para revocación de tokens
- Control de acceso por almacén mejorado
- Bloqueo automático por intentos fallidos

### 4. Filtro de Acceso por Almacén

- Tabla user_warehouse_assignments para asignación flexible
- Validación de almacén en cada operación crítica
- Usuarios pueden acceder solo a sus almacenes asignados
- Control granular de permisos

### 5. Sistema de Recuperación de Contraseña

- Módulo request_recovery_password completo
- Códigos únicos y expiración
- Verificación de usuario requerida
- Reset seguro de contraseña
- Rastreo de solicitudes (PENDING, VERIFIED, COMPLETED, REJECTED)

### 6. Documentación Extensiva

Nuevos documentos para facilitar el desarrollo y deployment:

- FLUJO-COMPLETO-SISTEMA-SIGMAV2.md: Arquitectura completa con diagramas ASCII
- VERIFICACION-FINAL-PROYECTO-COMPLETO.md: Verificación exhaustiva de componentes
- ANALISIS-FILTRO-ACTIVIDAD-USUARIO.md: Análisis técnico del filtro de seguridad
- CHECKLIST-VERIFICAR-FILTRO-ACTIVIDAD.md: 12 pasos de verificación
- CAUSAS-ROMPER-LOGICA-FILTRO.md: 9 causas de problemas y soluciones
- FAQ-FILTRO-ACTIVIDAD-USUARIO.md: 12 preguntas frecuentes
- INDICE-MAESTRO-DOCUMENTACION.md: Navegación completa
- CHECKLIST-ENTREGA-FINAL.md: Checklist de entrega a producción
- AGENTS.md: Actualizado con stack técnico

## Cambios Principales

### Base de Datos

- V1_2_0__Add_user_activity_tracking.sql: Campos de seguimiento de actividad
- V1_2_1__Make_id_label_request_nullable_in_labels_cancelled.sql: Flexibilidad en cancelaciones
- V1_2_2__Add_audit_fields_to_labels.sql: Campos de auditoría en conteos

Total de migraciones: 19
Total de tablas: 19

### Arquitectura

- Implementación completa de arquitectura hexagonal
- 7 capas bien definidas: Domain, Ports, Application, Infrastructure, Adapter, Shared, Security
- Separación clara de responsabilidades
- Puertos de entrada (Input) y salida (Output) correctamente definidos

### Módulos

Verificación de 10 módulos completados:

- users: Autenticación, usuarios, roles, rastreo
- labels: Marbetes completo (11 operaciones)
- periods: Períodos contables
- inventory: Catálogo de productos
- warehouse: Almacenes
- MultiWarehouse: Stock teórico
- mail: Notificaciones
- personal_information: Datos personales
- request_recovery_password: Recuperación de contraseña
- security: JWT, auditoría, filtros

### APIs

Implementación de 65+ endpoints:

- Autenticación: 11 endpoints
- Usuarios: 8 endpoints
- Períodos: 5 endpoints
- Inventario: 4 endpoints
- MultiAlmacén: 3 endpoints
- Almacenes: 4 endpoints
- Marbetes: 23 endpoints
- Mail: 1 endpoint
- Otros: 6+ endpoints

## Mejoras de Seguridad

- Validación de entrada en todos los endpoints
- Manejo centralizado de excepciones
- Transaccionalidad garantizada con @Transactional
- JWT con firma segura
- BCrypt para hashing de contraseñas
- 4 roles implementados: ADMINISTRADOR, AUXILIAR, ALMACENISTA, AUXILIAR_DE_CONTEO
- Auditoría con @Auditable
- Activity tracking en cada request
- Control de acceso por almacén

## Reglas de Negocio Validadas

### Flujo Completo de Marbetes

1. Crear Período Contable
2. Importar Catálogo (inventario.xlsx)
3. Importar Stock Teórico (multialmacen.xlsx)
4. Solicitar Folios
5. Generar Marbetes
6. Imprimir Marbetes (JasperReports → PDF)
7. Registrar Conteo C1 (Primer contador)
8. Registrar Conteo C2 (Segundo contador)
9. Generar Reportes
10. Generar Archivo Final (TXT con existencias)

### Validaciones Implementadas

- Folio único por período y almacén
- Cantidad teórica desde inventory_stock
- Cálculo de qty_final:
  - Si C1 = C2: qty_final = C1
  - Si diferencia < 5%: qty_final = promedio(C1, C2)
  - Si diferencia >= 5%: qty_final = C2
- Cancelación permitida antes de C2_DONE
- Archivado de datos cancelados
- Rastreo de auditoría en todas las operaciones

## Stack Técnico

- Java 21 LTS
- Spring Boot 3.5.5
- MySQL 8.0+
- Maven 3.8.1+
- Spring Security
- Spring Data JPA
- Spring AOP
- Flyway (Migraciones)
- Apache POI 5.3.0 (Excel)
- JasperReports 6.21.5 (PDF)
- MapStruct 1.5.5 (Mappers)
- Lombok
- SLF4J (Logging)

## Vulnerabilidades Monitoreadas

1. Apache POI 5.3.0 - CVE-2025-31672 (MEDIUM)
   - Esperando release 5.4.0
   - Mitigation: Validar todos los inputs de Excel

2. JasperReports 6.21.5 - CVE-2025-10492 (HIGH)
   - Vulnerabilidad de deserialización Java
   - Mitigation: NO procesar archivos .jrxml de fuentes no confiables

## Guía de Instalación

### Requisitos

- Java 21 JDK
- Maven 3.8.1+
- MySQL 8.0+

### Compilación

```bash
.\mvnw.cmd clean install
```

### Ejecución

```bash
.\mvnw.cmd spring-boot:run
```

### Configuración

Editar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/SIGMAV2_2
spring.datasource.username=root
spring.datasource.password=Tokai
server.port=8080
security.jwt.key.private=C4S4RB4CkJND
```

Para producción, cambiar credenciales y JWT key privada.

## Documentación

Documentación completa disponible en `docs/`:

- README.md: Descripción general del proyecto
- AGENTS.md: Guía para agentes IA y patrones de código
- FLUJO-COMPLETO-SISTEMA-SIGMAV2.md: Arquitectura y flujos completos
- VERIFICACION-FINAL-PROYECTO-COMPLETO.md: Verificación exhaustiva
- INDICE-MAESTRO-DOCUMENTACION.md: Índice de navegación
- 150+ documentos adicionales con ejemplos, guías y análisis

## Cambios Desde v1.0.0

- Implementación completa del módulo request_recovery_password
- Nuevo filtro UserActivityFilter para rastreo de actividad
- Sistema de auditoría mejorado con @Auditable
- Nuevas migraciones para campos de auditoría
- Documentación extensiva agregada
- Verificación exhaustiva de todo el sistema
- Arquitectura hexagonal validada en todos los módulos

## Testing

Estructura de testing preparada en `src/test/java/`.

Pruebas manuales disponibles en PowerShell scripts en `docs/`.

## Soporte y Reporte de Bugs

Para reportar bugs o problemas, consultar la documentación en `docs/`:

- CHECKLIST-VERIFICAR-FILTRO-ACTIVIDAD.md: Para diagnóstico
- CAUSAS-ROMPER-LOGICA-FILTRO.md: Para troubleshooting
- FAQ-FILTRO-ACTIVIDAD-USUARIO.md: Para preguntas frecuentes

## Contribuciones

El proyecto está arquitectado siguiendo hexagonal patterns. Para nuevas funcionalidades:

1. Crear puerto en domain/port/
2. Implementar en application/service/
3. Crear adaptadores en infrastructure/
4. Agregar controller en adapter/web/
5. Escribir migraciones Flyway si es necesario

Consultar AGENTS.md para detalles de arquitectura.

## Licencia

Propietario - Tokai de México

## Autores

Cesar Uriel Gonzalez Saldaña

---

Para más información, consultar la documentación completa en el directorio `docs/` o revisar los archivos README específicos de cada módulo.

Última actualización: 2026-03-23

