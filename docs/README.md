# SIGMAV2 — Sistema de Inventarios y Gestión de Marbetes V2

**Versión:** 1.0.0  
**Empresa:** Tokai de México  
**Desarrollado por:** Cesar Uriel Gonzalez Saldaña  
**Estado:** Producción  

---

## Descripción

SIGMAV2 es el sistema de control de inventarios físicos de Tokai de México. Permite llevar a cabo el proceso completo de verificación física y teórica del inventario mediante la importación de archivos Excel, generación e impresión de marbetes (etiquetas de identificación), registro de conteos físicos y generación de reportes.

---

## Tecnologías

| Componente | Tecnología |
|------------|------------|
| Framework | Spring Boot 3.x |
| Seguridad | Spring Security + JWT |
| Persistencia | JPA / Hibernate + MySQL 8 |
| Migraciones de BD | Flyway |
| Generación de PDFs | JasperReports 6.21.5 |
| Lectura de Excel | Apache POI |
| Gestión de dependencias | Maven 3.8+ |
| Servidor | Tomcat embebido |
| Puerto | 8080 |
| Java | JDK 17+ |

### Patrón de arquitectura

Arquitectura Hexagonal (Ports & Adapters) con separación clara entre dominio, aplicación, infraestructura y adaptadores. Se aplican principios SOLID en toda la base de código.

---

## Requisitos previos

- Java JDK 17 o superior
- Maven 3.8 o superior
- MySQL 8.x
- Carpeta `C:\Sistemas\SIGMA\Documentos\` creada en el servidor (para archivos de importación y archivos generados)

---

## Instalación y ejecución

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd SIGMAV2-SERVICES

# 2. Configurar la base de datos en application.properties
# spring.datasource.url=jdbc:mysql://localhost:3306/sigmav2_2
# spring.datasource.username=tu_usuario
# spring.datasource.password=tu_contraseña

# 3. Compilar el proyecto
mvn clean install

# 4. Ejecutar la aplicación
mvn spring-boot:run
```

Las migraciones de base de datos se aplican automáticamente con Flyway al iniciar la aplicación.

---

## Módulos del sistema

### Seguridad y Usuarios
- Autenticación mediante JWT con expiración configurable
- Revocación inmediata de tokens al cerrar sesión (tabla `revoked_tokens`)
- Control de acceso por roles: `ADMINISTRADOR`, `AUXILIAR`, `ALMACENISTA`, `AUXILIAR_DE_CONTEO`
- Recuperación de contraseña con aprobación del administrador
- Bloqueo de cuenta por intentos fallidos

### Importación de archivos Excel
- `inventario.xlsx` — Catálogo maestro de productos
- `multialmacen.xlsx` — Existencias teóricas por almacén y periodo

### Periodos de Inventario
- Contexto temporal que agrupa todos los marbetes, conteos y reportes
- Se recomienda crear el periodo antes de cualquier operación

### Marbetes
- Solicitud de folios consecutivos por almacén
- Generación masiva de marbetes con existencias desde `inventory_stock`
- Impresión en PDF con JasperReports (actualiza estado a `IMPRESO`)
- Cancelación con motivo obligatorio (histórico en `labels_cancelled`)
- Reactivación de marbetes cancelados

### Conteos Físicos
- Primer conteo (C1) y segundo conteo (C2) por marbete
- Actualización de conteos con auditoría completa
- Solo `ADMINISTRADOR` puede actualizar C2

### Reportes
- Distribución de folios
- Listado general de marbetes
- Marbetes pendientes
- Marbetes con diferencias (C1 vs C2)
- Marbetes cancelados
- Comparativo físico vs teórico
- Inventario físico por almacén con detalle (PDF)
- Inventario físico por producto con detalle en todos los almacenes

### Generación de archivo de existencias
- Archivo TXT con inventario físico final (`Existencias_{Periodo}.txt`)
- Usa C2 cuando existe; si no, usa C1. Excluye cancelados.

---

## Flujo de trabajo

```
1. Crear periodo de inventario
2. Importar inventario.xlsx  →  POST /api/sigmav2/inventory/import
3. Importar multialmacen.xlsx  →  POST /api/sigmav2/multiwarehouse/import
4. Solicitar folios  →  POST /api/sigmav2/labels/request
5. Generar marbetes  →  POST /api/sigmav2/labels/generate
6. Imprimir marbetes (PDF)  →  POST /api/sigmav2/labels/print
7. Registrar primer conteo  →  POST /api/sigmav2/labels/counts/c1
8. Registrar segundo conteo  →  POST /api/sigmav2/labels/counts/c2
9. Revisar diferencias  →  POST /api/sigmav2/labels/reports/with-differences
10. Generar reportes finales y archivo TXT
```

---

## APIs principales

### Autenticación
```
POST  /api/sigmav2/auth/login
POST  /api/sigmav2/auth/logout
POST  /api/sigmav2/auth/request-recovery
```

### Usuarios
```
GET   /api/sigmav2/users/me
GET   /api/sigmav2/users/me/activity
POST  /api/sigmav2/users
GET   /api/sigmav2/users
PUT   /api/sigmav2/users/{id}
```

### Importación
```
POST  /api/sigmav2/inventory/import
POST  /api/sigmav2/multiwarehouse/import
```

### Marbetes
```
POST  /api/sigmav2/labels/request
POST  /api/sigmav2/labels/generate
POST  /api/sigmav2/labels/print
POST  /api/sigmav2/labels/pending-print-count
POST  /api/sigmav2/labels/cancel
POST  /api/sigmav2/labels/for-count/list
```

### Conteos
```
POST  /api/sigmav2/labels/counts/c1
POST  /api/sigmav2/labels/counts/c2
PUT   /api/sigmav2/labels/counts/c1
PUT   /api/sigmav2/labels/counts/c2
```

### Reportes
```
POST  /api/sigmav2/labels/reports/distribution
POST  /api/sigmav2/labels/reports/list
POST  /api/sigmav2/labels/reports/pending
POST  /api/sigmav2/labels/reports/with-differences
POST  /api/sigmav2/labels/reports/cancelled
POST  /api/sigmav2/labels/reports/comparative
POST  /api/sigmav2/labels/reports/warehouse-detail/pdf
POST  /api/sigmav2/labels/reports/product-detail
```

### Archivo de existencias
```
POST  /api/sigmav2/labels/generate-file
```

---

## Reglas de negocio clave

1. Un marbete representa un producto en un almacén dentro de un periodo
2. Los folios son consecutivos y únicos por periodo
3. Un marbete cancelado no se elimina; se conserva en `labels_cancelled`
4. C2 solo puede registrarse si existe C1 previo
5. Solo el ADMINISTRADOR puede actualizar C2
6. No se puede cancelar un marbete sin folios asignados
7. Los marbetes con existencias = 0 también se generan para registro completo
8. El archivo de existencias usa C2 cuando existe; de lo contrario usa C1
9. Cada usuario opera únicamente en los almacenes que tiene asignados (excepto ADMINISTRADOR)
10. El token JWT se invalida inmediatamente al hacer logout

---

## Estructura del proyecto

```
SIGMAV2-SERVICES/
├── src/
│   ├── main/
│   │   ├── java/tokai/com/mx/SIGMAV2/
│   │   │   ├── modules/
│   │   │   │   ├── labels/          # Marbetes, conteos, reportes
│   │   │   │   ├── inventory/       # Catálogo y existencias
│   │   │   │   └── multiwarehouse/  # Importación multialmacén
│   │   │   └── security/            # JWT, filtros, roles
│   │   └── resources/
│   │       ├── reports/             # Plantillas JasperReports (.jrxml)
│   │       └── db/migration/        # Scripts Flyway
│   └── test/
├── docs/                            # Documentación técnica
├── pom.xml
└── README.md
```

---

## Base de datos — Tablas principales

| Tabla | Descripción |
|-------|-------------|
| `users` | Usuarios del sistema |
| `revoked_tokens` | Tokens JWT revocados |
| `products` | Catálogo de productos |
| `warehouse` | Catálogo de almacenes |
| `periods` | Periodos de inventario |
| `label_requests` | Solicitudes de folios |
| `labels` | Marbetes activos |
| `labels_cancelled` | Marbetes cancelados (histórico) |
| `label_prints` | Registro de impresiones |
| `multiwarehouse_existences` | Existencias importadas |
| `inventory_stock` | Existencias optimizadas para consulta |
| `request_recovery_password` | Solicitudes de recuperación de contraseña |

---

## Versión

Consulta el archivo [docs/RELEASE-NOTES-v1.0.0.md](docs/RELEASE-NOTES-v1.0.0.md) para el historial completo de cambios de la versión 1.0.0.

---

*SIGMAV2 — Tokai de México — 2026*

