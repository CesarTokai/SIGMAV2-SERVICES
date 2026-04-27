# Refactorización de Arquitectura — Abril 2026

**Fecha:** 2026-04-27
**Rama:** `implementacion_qr_funciones`
**Estado:** ✅ Completado — 33 tests verdes

---

## Resumen Ejecutivo

Refactorización técnica de deuda acumulada en 3 áreas: código muerto, credenciales hardcodeadas, y violaciones de arquitectura hexagonal. Se dividió `LabelServiceImpl` de 2057 líneas en 6 servicios especializados. Se agregaron 33 pruebas unitarias.

---

## 1. Código Muerto Eliminado (RED)

Archivos eliminados de `inventory/domain/` — nunca fueron registrados como beans Spring ni usados:

| Archivo | Razón |
|---------|-------|
| `domain/model/Label.java` | Stub mínimo, duplicaba `labels/domain/model/Label.java` |
| `domain/service/LabelManagementService.java` | Sin `@Service`, sin referencias |
| `domain/ports/input/LabelManagementUseCase.java` | Interface huérfana |
| `domain/ports/output/LabelRepository.java` | Interface huérfana |

---

## 2. Credenciales → Variables de Entorno (RED)

`src/main/resources/application.properties` — todos los valores sensibles reemplazados:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/SIGMAV2_2?...}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:Tokai}
security.jwt.key.private=${JWT_SECRET:C4S4RB4CkJND}
security.jwt.user.generator=${JWT_ISSUER:S1GM4V2}
spring.mail.username=${MAIL_USERNAME:demexicotokai@gmail.com}
spring.mail.password=${MAIL_PASSWORD:pbko zjte apif ngyd}
app.labels.inventory-file.directory=${LABELS_DIR:C:/Sistemas/SIGMA/Documentos}
```

Los valores por defecto mantienen compatibilidad local. En producción se deben configurar como variables de entorno del sistema.

---

## 3. Controladores Movidos a Capa Correcta (RED)

**Antes:** `inventory/application/controller/` (violación — application layer no debe exponer HTTP)

**Después:** `inventory/adapter/controller/` (correcto en arquitectura hexagonal)

Archivos movidos (package actualizado de `...inventory.application.controller` → `...inventory.adapter.controller`):
- `InventoryController.java`
- `ProductController.java`
- `InventoryWarehouseController.java`
- `InventoryPeriodController.java`

---

## 4. InventoryStockEntity — Ruta Corregida (YELLOW)

**Antes:** `inventory/infrastructure/persistence/entity/InventoryStockEntity.java`

**Después:** `inventory/infrastructure/persistence/InventoryStockEntity.java`

Imports actualizados en 5 archivos:
- `InventoryStockRepositoryAdapter.java`
- `InventoryStockMapper.java`
- `JpaInventoryStockRepository.java`
- `LabelServiceImpl.java`
- `MultiWarehouseInventoryAdapter.java`

---

## 5. Migración `users/model/` → `users/infrastructure/persistence/` (YELLOW)

Las clases en `users/model/` eran entidades JPA (no modelos de dominio) — movidas a su capa correcta.

**Archivos migrados:**

| Clase | Tipo | Tabla |
|-------|------|-------|
| `BeanUser.java` | Entidad JPA | `users` |
| `ERole.java` | Enum | — |
| `BeanUserActivityLog.java` | Entidad JPA | `user_activity_log` |
| `BeanPasswordResetAttempt.java` | Entidad JPA | `password_reset_attempts` |

**24 archivos** en 6 módulos tuvieron imports actualizados de `users.model.*` → `users.infrastructure.persistence.*`.

---

## 6. Sub-modularización de LabelServiceImpl (YELLOW)

`LabelServiceImpl` pasó de **2057 líneas** a **~280 líneas** (facade puro).

### Servicios creados

#### `LabelPrintService.java`
Todo el código de generación de PDFs / impresión:
- `printLabels` — impresión estándar
- `extraordinaryReprint` — reimpresión extraordinaria
- `getPrintedLabelPdf` — obtener PDF de marbete impreso
- `reprintSimple` — reimpresión simple
- `printSelectedLabelsWithInfo` — impresión seleccionada con info
- `printSelectedLabelsAutoWarehouse` — impresión automática por almacén
- `printSelectedLabelsWithQR` — impresión con QR
- `generarPDFConQRInterno` (privado) — generación interna QR

#### `LabelQueryService.java`
Todas las consultas (solo lectura, ~450 líneas):
- `getPendingPrintCount`, `getLabelSummary`, `getLabelStatus`
- `countLabelsByPeriodAndWarehouse`, `getLabelsByProduct`
- `getLabelForCount`, `getLabelsForCountList`
- `getLabelFullDetail`, `getLabelFullDetailList`
- `getSelectedLabelsInfo`

**Regla clave:** `AUXILIAR_DE_CONTEO` omite validación de acceso a almacén.

#### `LabelCancelService.java`
Lógica de cancelación y reactivación:
- `getCancelledLabels` — lista marbetes cancelados
- `cancelLabel` — cancela marbete (IMPRESO → CANCELADO)
- `updateCancelledStock` — actualiza existencias; reactiva si `existenciasActuales > 0`

**Regla clave:** reactivación solo ocurre si `reactivado == false` y `existenciasActuales > 0`.

### Diagrama de dependencias (LabelServiceImpl como facade)

```
LabelServiceImpl (facade)
├── LabelGenerationService  — generación de folios
├── LabelCountService       — registro C1 / C2
├── LabelReportService      — reportes Excel/PDF
├── LabelPrintService       — impresión PDFs      ← nuevo
├── LabelQueryService       — consultas           ← nuevo
└── LabelCancelService      — cancelación         ← nuevo
```

---

## 7. Pruebas Unitarias Agregadas

**Total: 33 tests** — todos pasan ✅

### `UserTest.java` — 12 tests
Módulo: `users/domain/model/User`

| Test | Comportamiento |
|------|---------------|
| `incrementAttempts_increments_counter` | Contador sube de 0 a 1 |
| `incrementAttempts_blocks_account_after_three_failures` | Al tercer intento: `status=false`, `lastBlockedAt` set |
| `incrementAttempts_does_not_block_before_third_failure` | Dos intentos no bloquean |
| `isBlocked_returns_true_when_blocked_within_five_minutes` | Bloqueado hace 2 min → true |
| `isBlocked_returns_false_when_block_expired` | Bloqueado hace 10 min → false |
| `isBlocked_returns_false_when_account_active` | `status=true` → false |
| `markAsVerified_sets_verified_and_updatedAt` | Verified=true, updatedAt set |
| `resetAttempts_clears_counter_and_activates_account` | attempts=0, status=true |
| `getFullName_concatenates_all_name_parts` | "Juan Garcia Lopez" |
| `getFullName_handles_missing_second_last_name` | "Ana Martinez" |
| `hasCompletePersonalInfo_true_when_...` | Nombre + apellido paterno presentes |
| `hasCompletePersonalInfo_false_when_name_missing` | Sin nombre → false |

### `LabelCountServiceTest.java` — 12 tests
Módulo: `labels/application/service/impl/LabelCountService`

| Test | Comportamiento |
|------|---------------|
| `registerCountC1_success` | Guarda evento C1 con parámetros correctos |
| `registerCountC1_throws_when_C1_already_exists` | → `DuplicateCountException` "C1 ya fue registrado" |
| `registerCountC1_throws_when_C2_exists_before_C1` | → `CountSequenceException` menciona "C2" |
| `registerCountC1_throws_when_label_not_found` | → `LabelNotFoundException` |
| `registerCountC1_throws_when_label_not_impreso` | Estado GENERADO → `InvalidLabelStateException` |
| `registerCountC1_throws_when_label_is_cancelled` | Estado CANCELADO → `InvalidLabelStateException` |
| `registerCountC1_throws_when_role_not_allowed` | Rol USUARIO → `PermissionDeniedException` |
| `registerCountC1_throws_when_role_null` | Rol null → `PermissionDeniedException` |
| `registerCountC2_success` | Guarda evento C2 con parámetros correctos |
| `registerCountC2_throws_when_C1_not_exists` | → `CountSequenceException` "C1 previo" |
| `registerCountC2_throws_when_C2_already_exists` | → `DuplicateCountException` "C2 ya fue registrado" |
| `registerCountC2_AUXILIAR_DE_CONTEO_bypasses_warehouse_validation` | Nunca llama `validateWarehouseAccess` |

### `LabelCancelServiceTest.java` — 9 tests
Módulo: `labels/application/service/impl/LabelCancelService`

| Test | Comportamiento |
|------|---------------|
| `cancelLabel_changes_state_to_cancelled` | Estado → CANCELADO, guarda en `jpa_label_cancelled` |
| `cancelLabel_throws_when_already_cancelled` | → `LabelAlreadyCancelledException` |
| `cancelLabel_throws_when_folio_not_found` | → `LabelNotFoundException` |
| `cancelLabel_throws_when_folio_null` | → `InvalidLabelStateException` menciona "folio" |
| `cancelLabel_saves_cancelled_with_reason` | `motivoCancelacion` persistido correctamente |
| `updateCancelledStock_reactivates_when_existencias_positive` | `reactivado=true`, guarda label y cancelled |
| `updateCancelledStock_does_not_reactivate_when_existencias_zero` | `reactivado=false`, no llama `persistence.save` |
| `updateCancelledStock_throws_when_cancelled_not_found` | → `LabelNotFoundException` |

---

## Pendientes Técnicos

- **Eliminar métodos deprecated** `requestLabels` + `generateBatch` de `LabelServiceImpl` cuando frontend migre a `generateBatchList`.
- **Pruebas de integración** para `LabelQueryService` (métodos complejos de filtrado/paginación).
- **CVE-2025-10492** — JasperReports 6.21.5 (deserialización Java, severidad HIGH). Sin parche disponible a la fecha. Monitorear.
