# AGENTS.md — Guía para Agentes IA en SIGMAV2

> Documentación esencial para que agentes de IA comprendan rápidamente la arquitectura, patrones y workflows de este codebase.

**Stack Técnico:**
- **Java:** 21 (LTS)
- **Spring Boot:** 3.5.5
- **Base de Datos:** MySQL 8.0+
- **Build:** Maven 3.8.1+
- **Migraciones:** Flyway

---

## 🏗️ Arquitectura Hexagonal (Ports & Adapters)

**SIGMAV2 implementa arquitectura hexagonal estricta.** Cada módulo tiene esta estructura:

```
modules/{nombre}/
├── domain/
│   ├── model/              # Modelos de dominio PUROS (sin anotaciones JPA)
│   │   ├── User.java       # Entity de dominio limpia
│   │   └── Role.java       # Enums y value objects
│   └── port/
│       ├── input/          # Puertos de entrada (interfaces de casos de uso)
│       │   └── UserService.java
│       └── output/         # Puertos de salida (interfaces de persistencia/externos)
│           ├── UserRepository.java
│           └── MailSender.java
├── application/
│   └── service/
│       └── UserApplicationService.java  # Implementa UserService
├── infrastructure/
│   ├── persistence/
│   │   ├── JpaUserRepository.java      # Spring Data JPA
│   │   └── UserRepositoryAdapter.java  # Implementa puerto de dominio
│   ├── mapper/
│   │   └── UserMapper.java             # Convierte BeanUser ↔ User
│   ├── mail/
│   │   └── MailSenderAdapter.java
│   └── config/
│       └── UserModuleConfig.java       # Wiring de dependencias
├── adapter/
│   └── web/
│       ├── UserController.java         # REST endpoints
│       └── dto/
│           ├── UserDomainResponse.java # DTOs desacoplados
│           └── UserRequest.java
```

**Regla crítica:** DTOs del adaptador web NUNCA lleguen al dominio. Los puertos retornan objetos de dominio; el controlador los convierte a DTO.

---

## 📦 Módulos Principales

| Módulo | Responsabilidad | Ubicación |
|--------|-----------------|-----------|
| **users** | Autenticación, gestión de usuarios, roles, rastreo de actividad | `modules/users/` |
| **inventory** | Catálogo de productos, existencias teóricas | `modules/inventory/` |
| **labels** | Marbetes (etiquetas), folios, impresión PDF, conteos C1/C2 | `modules/labels/` |
| **periods** | Contexto temporal que agrupa operaciones | `modules/periods/` |
| **warehouse** | Almacenes físicos, acceso por rol | `modules/warehouse/` |
| **MultiWarehouse** | Importación de existencias por almacén | `modules/MultiWarehouse/` |
| **mail** | Envío de notificaciones por email | `modules/mail/` |
| **security** | Seguridad JWT, filtros, autenticación, auditoría | `security/` |
| **personal_information** | Datos adicionales de usuarios (comentarios) | `modules/personal_information/` |
| **request_recovery_password** | Solicitudes de recuperación de contraseña, verificación | `modules/request_recovery_password/` |

---

## 🔐 Seguridad y Autenticación

### JWT y Revocación

- **Token expiration:** Configurable en `application.properties` bajo `security.jwt.*`
- **Token revocation:** Tabla `revoked_tokens` consultada en cada request por `JwtRevocationFilter`
- **Purga automática:** Job que ejecuta cada 1 hora (configurable: `security.revocation.purge-interval-ms`)

```java
// Modelo: security/infrastructure/entity/RevokedToken.java
// Servicio: security/infrastructure/service/TokenRevocationService.java
// Filtro: security/infrastructure/filter/JwtRevocationFilter.java
```

### Rastreo de Actividad del Usuario

- **Tabla:** `users` — Campos: `last_login_at`, `last_activity_at`, `password_changed_at`
- **Filtro:** `UserActivityFilter` se ejecuta después de `JwtAuthenticationFilter` (línea 72 en `SecurityConfig`)
- **Actualización síncrona:** Registra la actividad en cada request autenticado (maneja excepciones sin bloquear)

```java
// Filtro: security/infrastructure/filter/UserActivityFilter.java
// Actualiza lastActivityAt en cada request autenticado
```

### Auditoría (AOP)

- **Anotación:** `@Auditable(action="...", resource="...")` en métodos/clases a auditar
- **Aspect:** `AuditAspect` usa `@Around` para capturar entrada/salida/excepciones
- **Tabla:** `audit_logs` — Registra: usuario, acción, recurso, IP, timestamp, resultado
- **Resolución de usuario:** Obtiene nombre completo desde `BeanUser` + `BeanPersonalInformation`

```java
// Ubicación: shared/audit/
// AuditAspect.java — @Aspect que procesa @Auditable
// Auditable.java — Anotación para marcar métodos auditables
```

### Roles y Permisos

Roles definidos en `modules/users/domain/model/Role.java`:
- `ADMINISTRADOR` — Acceso completo, puede actualizar C2, gestionar usuarios
- `AUXILIAR` — Importación, generación de marbetes
- `ALMACENISTA` — Acceso limitado a su almacén asignado
- `AUXILIAR_DE_CONTEO` — Solo registrar conteos C1/C2

**Control de acceso por almacén:** Tabla `user_warehouse_assignments` + validación en controllers vía adaptadores.

---

## 🗄️ Migraciones de Base de Datos (Flyway)

**Ubicación:** `src/main/resources/db/migration/`

- Se ejecutan automáticamente al iniciar Spring Boot
- Nombradas como: `V1_0_X__Description.sql`
- **Importante:** Never edit applied migrations. Create new ones for changes.

Ejemplos actuales:
- `V1_0_1__Initial_schema.sql`
- `V1_0_7__Create_revoked_tokens_table.sql`
- `V1_1_1__Create_user_warehouse_assignments.sql`
- `V1_1_2__Populate_inventory_stock_from_multiwarehouse.sql`
- `V1_2_0__Add_user_activity_tracking.sql` — Campos `last_login_at`, `last_activity_at`, `password_changed_at` en users
- `V1_2_2__Add_audit_fields_to_labels.sql` — Campos de auditoría en `labels_cancelled` y `label_count_events`

---

## 📊 Flujo de Trabajo Principal (Verificación Física vs Teórica)

```
1. POST /api/sigmav2/periods           ← Crear período
2. POST /api/sigmav2/inventory/import  ← Importar inventario.xlsx (producto + clave)
3. POST /api/sigmav2/multiwarehouse/import ← Importar multialmacen.xlsx (stock teórico)
4. POST /api/sigmav2/labels/request    ← Solicitar folios por almacén
5. POST /api/sigmav2/labels/generate   ← Generar marbetes (usa inventory_stock)
6. POST /api/sigmav2/labels/print      ← Generar PDF e imprimir (JasperReports)
7. POST /api/sigmav2/labels/counts/c1  ← Registrar primer conteo físico
8. POST /api/sigmav2/labels/counts/c2  ← Registrar segundo conteo físico
9. POST /api/sigmav2/labels/reports/*  ← Generar reportes
10. POST /api/sigmav2/labels/generate-file ← Archivo TXT con existencias finales
```

**Tabla crítica:** `inventory_stock` — Caché de existencias teóricas sincronizada al importar.

---

## 🛠️ Comandos Build y Desarrollo

```bash
# Compilar (Windows)
.\mvnw.cmd clean install

# Ejecutar en desarrollo
.\mvnw.cmd spring-boot:run

# Ejecutar tests
.\mvnw.cmd test

# Build jar
.\mvnw.cmd clean package
```

**Propiedades clave** en `application.properties`:
- `spring.datasource.url` — Conexión MySQL (default: localhost:3306/SIGMAV2_2)
- `server.port` — Puerto (default: 8080)
- `app.labels.inventory-file.directory` — Ruta para generar archivos TXT (Windows: `C:/Sistemas/SIGMA/Documentos`)

---

## 📝 Patrones de Código Específicos

### 0. Dependencias Clave

```xml
<!-- Spring AOP para auditoría y aspectos (pom.xml) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- MapStruct para mapeos entre DTOs y entidades -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

### 1. Mappers con MapStruct

```java
// Usar @Mapper(componentModel = "spring") para autowiring automático
@Mapper(componentModel = "spring")
public interface UserMapper {
    User toModel(BeanUser entity);
    BeanUser toEntity(User model);
}
```

### 2. DTOs de Respuesta

Siempre desacoplados del modelo de dominio:

```java
@Data
public class UserDomainResponse {
    private Long id;
    private String email;
    private String role;  // String, no enum JPA
    // ... nunca contienen @Entity, @Column, etc.
}
```

### 3. Validación de Input

```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest request) {
    // Spring valida automáticamente via @Valid + anotaciones en DTO
    // Usa: @NotNull, @Email, @Size, @Pattern, etc.
}
```

### 4. Manejo de Errores

- Excepciones de dominio: `IllegalArgumentException`, custom exceptions en `domain/`
- Controladores convierten a HTTP: `@ExceptionHandler` o `@RestControllerAdvice`
- Logs: Inyectar `@Slf4j` (Lombok) + usar `log.info()`, `log.error()`

### 5. Transacciones

```java
@Transactional
public void updateCounts(Long labelId, Integer c1Value) {
    // Automático: Spring maneja rollback en excepciones
}
```

---

## 📚 Tabla de Entidades Principales

| Entidad | Tabla BD | Propósito |
|---------|----------|----------|
| User | users | Cuentas de usuario con roles |
| Period | periods | Contexto temporal (fecha, nombre) |
| Product | products | Catálogo de artículos |
| Warehouse | warehouses | Almacenes físicos |
| InventoryStock | inventory_stock | Existencias teóricas (cached) |
| Label | labels | Marbetes (etiquetas individuales) |
| LabelCount | label_counts | Conteos C1/C2 |
| FolioRequest | folio_requests | Solicitudes de rangos de folios |
| RevokedToken | revoked_tokens | Tokens invalidados (logout) |
| UserWarehouseAssignment | user_warehouse_assignments | Filtro de acceso por almacén |

---

## ⚠️ Vulnerabilidades de Seguridad Conocidas

**Monitoreadas en `pom.xml`:**

1. **Apache POI 5.3.0** — CVE-2025-31672 (MEDIUM)
   - Esperando release 5.4.0
   - Mitigation: Validar todos los inputs de Excel

2. **JasperReports 6.21.5** — CVE-2025-10492 (HIGH)
   - Vulnerabilidad de deserialización Java
   - Mitigation: NO procesar archivos `.jrxml` de fuentes no confiables
   - Monitorear: https://github.com/advisories/GHSA-7c3f-cg9x-f3gr

---

## 🧪 Testing

### Tests Unitarios Esperados

```java
// Ubicación: src/test/java/tokai/com/mx/SIGMAV2/modules/{modulo}/
@SpringBootTest
class UserServiceTest {
    @InjectMocks
    UserApplicationService service;
    
    @Mock
    UserRepository repository;
    
    @Test
    void testCreateUser() { }
}
```

### Pruebas Manuales (PowerShell)

Scripts en `docs/`:
- `test-labels-summary.ps1` — Prueba flujo completo de marbetes
- `test-complete-flow.ps1` — Verificación física vs teórica

---

## 📁 Rutas de Archivos Críticas

- **Importación Excel:** `C:\Sistemas\SIGMA\Documentos\` (configurable)
- **Generación de archivos:** Mismo directorio anterior
- **Reportes JasperReports:** `src/main/resources/reports/` (archivos `.jrxml`)
- **Migraciones SQL:** `src/main/resources/db/migration/`

---

## 🔍 Documentación en `docs/`

Lee estos primero cuando explores nuevas áreas:

1. **README.md** — Descripción general y APIs
2. **RELEASE-NOTES-v1.0.0.md** — Historia y cambios
3. **REFACTORIZACION-COMPLETADA-MODULO-USUARIOS.md** — Patrón hexagonal aplicado
4. **README-MARBETES-REGLAS-NEGOCIO.md** — Lógica core de marbetes
5. **FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md** — Workflow completo con ejemplos

---

## 💡 Tips para Agentes IA

✅ **Siempre:**
- Busca el puerto de entrada (`domain/port/input/`) antes de editar implementación
- Verifica mappers antes de cambiar DTOs
- Crea migraciones Flyway para cambios de BD (nunca modifiques schema directamente)
- Lee `UserModuleConfig.java` para entender cómo se wirean las dependencias
- Valida que las excepciones de dominio se conviertan a HTTP status en controladores

❌ **Nunca:**
- Mezcles lógica de negocio en controladores
- Uses entidades JPA en puertos/dominio
- Modifiques migraciones ya aplicadas
- Ignores `@Transactional` en operaciones multi-tabla
- Implementes validación solo en frontend (siempre en backend también)

---

## !!!!NOTA IMPORTANTE!!!!

- **NO HAGAS ARCHIVOS ESCRITOS ESO INCLUYE ARCHIVOS MD , TXT, JAR, etc. SOLAMENTE DA EL RESUMEN EN LA CONVERSACION.**


Para entender un módulo específico, sigue este orden:
1. Lee `domain/port/input/*.java` (interfaces de casos de uso)
2. Lee `domain/model/*.java` (qué datos maneja)
3. Lee `application/service/*.java` (cómo funciona)
4. Lee `adapter/web/*Controller.java` (endpoints públicos)
5. Lee `infrastructure/persistence/*Adapter.java` (cómo persiste)

---

**Última actualización:** 2026-03-23

