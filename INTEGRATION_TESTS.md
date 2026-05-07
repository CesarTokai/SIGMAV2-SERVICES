# Integration Tests - SIGMAV2

Documentación de pruebas de integración para módulos críticos.

## Resumen Ejecutivo

- **Total Tests**: 84
- **Cobertura**: Labels (Counting, Query, Print, Reprint, Reports, File Generation) + Users (Admin Management)
- **Enfoque**: Validaciones críticas, casos de error, control de acceso por roles
- **Estado**: ✓ Todos los 84 tests pasando

---

## Label Module Tests (58 tests)

### 1. LabelCountIntegrationTest (11 tests)
**Archivo**: `src/test/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelCountIntegrationTest.java`

**Propósito**: Validar flujo de conteo (C1/C2) con control de errores críticos

**Tests**:
- `registerCountC1_throws_when_folio_not_exists` → Validar folio existe
- `registerCountC2_throws_when_C1_not_registered` → C1 es prerequisito
- `registerCountC1_throws_when_label_cancelled` → Validar estado IMPRESO
- `registerCountC1_throws_when_label_not_impreso` → Estado debe ser IMPRESO
- `registerCountC1_throws_when_period_closed` → Periodo válido
- `registerCountC1_throws_when_warehouse_no_access` → Permisos almacén
- `registerCountC1_throws_when_duplicate` → Prevenir doble-click
- `registerCountC2_duplicate_prevention` → C2 no duplicable
- `registerCountC1_throws_when_invalid_role` → Solo roles válidos
- `registerCountC1_success_with_valid_roles` → ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO
- `registerCountC1_handles_concurrent_access` → Prueba threads concurrentes

---

### 2. LabelQueryAndCaptureIntegrationTest (5 tests)
**Archivo**: `src/test/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelQueryAndCaptureIntegrationTest.java`

**Propósito**: Validar consultas y generación de marbetes

**Tests**:
- `getLabelSummary_throws_when_warehouse_access_denied` → Control acceso
- `getLabelSummary_throws_when_period_not_exists` → Período válido
- `generateBatchList_throws_when_warehouse_access_denied` → Permisos
- `generateBatchList_throws_when_product_not_exists` → Validar producto
- `updateCancelledStock_throws_when_cancelled_not_exists` → Marbete existe

---

### 3. LabelPrintIntegrationTest (3 tests)
**Archivo**: `src/test/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelPrintIntegrationTest.java`

**Propósito**: Validar operaciones de impresión

**Tests**:
- `printLabels_throws_when_warehouse_access_denied` → Control permisos
- `getPendingPrintCount_throws_when_warehouse_access_denied` → Permisos
- `getPendingPrintCount_success_with_zero_count` → Respuesta vacía

---

### 4. LabelReprintIntegrationTest (3 tests)
**Archivo**: `src/test/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelReprintIntegrationTest.java`

**Propósito**: Validar reimpresión de marbetes

**Tests**:
- `extraordinaryReprint_throws_when_warehouse_access_denied` → Control acceso
- `getPrintedLabelPdf_throws_when_label_not_found` → Marbete existe
- `reprintSimple_throws_when_label_not_found` → Marbete existe

---

### 5. LabelReportIntegrationTest (32 tests)
**Archivo**: `src/test/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelReportIntegrationTest.java`

**Propósito**: Validar 8 tipos de reportes con cobertura completa de roles y errores

**Reportes cubiertos** (4 tests cada uno):
1. **Distribution Report** (getDistributionReport)
   - success_empty
   - throws_when_warehouse_access_denied
   - success_with_invalid_period
   - success_with_valid_roles

2. **Label List Report** (getLabelListReport)
   - success_empty
   - throws_when_warehouse_access_denied
   - success_with_invalid_period
   - success_with_valid_roles

3. **Pending Labels Report** (getPendingLabelsReport)
   - success_empty
   - throws_when_warehouse_access_denied
   - success_with_invalid_period
   - success_with_valid_roles

4. **Differences Report** (getDifferencesReport)
   - success_empty
   - throws_when_warehouse_access_denied
   - success_with_invalid_period
   - success_with_valid_roles

5. **Cancelled Labels Report** (getCancelledLabelsReport)
   - success_empty
   - throws_when_warehouse_access_denied
   - success_with_invalid_period
   - success_with_valid_roles

6. **Comparative Report** (getComparativeReport)
   - success_empty
   - throws_when_warehouse_access_denied
   - success_with_invalid_period
   - success_with_valid_roles

7. **Warehouse Detail Report** (getWarehouseDetailReport)
   - success_empty
   - throws_when_warehouse_access_denied
   - success_with_invalid_period
   - success_with_valid_roles

8. **Product Detail Report** (getProductDetailReport)
   - success_empty
   - throws_when_warehouse_access_denied
   - success_with_invalid_period
   - success_with_valid_roles

**Cobertura de roles** en cada reporte:
- ADMINISTRADOR
- ALMACENISTA
- AUXILIAR
- AUXILIAR_DE_CONTEO

---

### 6. GenerateFileIntegrationTest (10 tests)
**Archivo**: `src/test/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/GenerateFileIntegrationTest.java`

**Propósito**: Validar generación de archivos CSV y conteos

**Tests**:

**Generar archivo (2)**
- `generateInventoryFile_throws_when_period_not_exists` → Periodo requerido
- `generateInventoryFile_throws_when_period_zero` → Periodo válido

**Contar marbetes (3)**
- `countLabelsByPeriodAndWarehouse_with_invalid_period` → Retorna 0 si no existe
- `countLabelsByPeriodAndWarehouse_with_invalid_warehouse` → Retorna 0 si no existe
- `countLabelsByPeriodAndWarehouse_consistent_across_calls` → Consistencia datos

**Reportes (3)**
- `getDistributionReport_success_with_invalid_period` → Maneja periodo inválido
- `getDistributionReport_throws_with_invalid_warehouse` → Maneja almacén inválido
- `getDistributionReport_success_with_all_roles` → Soporta 4 roles + errores graceful

**Detalles etiquetas (2)**
- `getLabelsForCountList_throws_with_invalid_period` → Validar periodo
- `getLabelsForCountList_throws_with_invalid_warehouse` → Validar almacén

---

## User Module Tests (20 tests)

### AdminUserManagementIntegrationTest (20 tests)
**Archivo**: `src/test/java/tokai/com/mx/SIGMAV2/modules/users/application/service/impl/AdminUserManagementIntegrationTest.java`

**Propósito**: Validar operaciones administrativas de usuarios

**Tests por funcionalidad**:

**List Users (2)**
- `listUsers_success_with_valid_pagination` → Paginación funciona
- `listUsers_success_empty_page` → Maneja páginas vacías

**Create User (5)**
- `createUser_success_with_valid_data` → Crea usuario válido
- `createUser_throws_when_email_already_exists` → Email único
- `createUser_throws_when_password_weak` → Validar contraseña fuerte
- `createUser_throws_when_invalid_email` → Email formato válido
- `createUser_success_with_all_valid_roles` → 4 roles soportados

**Toggle Status (3)**
- `toggleUserStatus_success_deactivate` → Desactiva usuario
- `toggleUserStatus_success_activate` → Activa usuario
- `toggleUserStatus_throws_when_user_not_found` → Usuario existe

**Change Role (4)**
- `changeUserRole_success` → Cambio rol simple
- `changeUserRole_throws_when_user_not_found` → Usuario existe
- `changeUserRole_throws_when_invalid_role` → Rol válido
- `changeUserRole_success_with_all_roles` → Todos 4 roles válidos

**Delete User (2)**
- `deleteUser_success` → Elimina usuario
- `deleteUser_throws_when_user_not_found` → Usuario existe

**Reset Attempts (2)**
- `resetUserAttempts_success` → Reset intentos fallidos
- `resetUserAttempts_throws_when_user_not_found` → Usuario existe

**Force Verify (2)**
- `forceVerifyUser_success` → Verifica usuario forzado
- `forceVerifyUser_throws_when_user_not_found` → Usuario existe

---

## Infraestructura de Tests

### Base de datos
- **Motor**: H2 (en memoria)
- **Perfil**: `test`
- **Config**: `application-test.properties`
- **Estrategia DDL**: create-drop (limpia tras cada ejecución)

### Anotaciones base
```java
@SpringBootTest              // Carga contexto completo
@ActiveProfiles("test")      // Usa profile test
@Transactional               // Rollback automático tras test
```

### Seguridad en tests
- Mock de `Authentication` y `SecurityContext`
- Simula usuario logueado con email
- Permite probar @PreAuthorize

---

## Ejecución de Tests

### Todos los tests
```bash
./mvnw test
```

### Tests específicos por módulo
```bash
# Labels
./mvnw test -Dtest=LabelCountIntegrationTest
./mvnw test -Dtest=LabelQueryAndCaptureIntegrationTest
./mvnw test -Dtest=LabelPrintIntegrationTest
./mvnw test -Dtest=LabelReprintIntegrationTest
./mvnw test -Dtest=LabelReportIntegrationTest

# Users
./mvnw test -Dtest=AdminUserManagementIntegrationTest
```

### Tests silenciosos
```bash
./mvnw test -q
```

---

## Patrones de validación

### 1. Validación de acceso
```java
doThrow(new PermissionDeniedException("Sin acceso"))
    .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());
```

### 2. Validación de cobertura de roles
```java
String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};
for (String role : validRoles) {
    // Test con cada rol
}
```

### 3. Validación de excepciones
```java
assertThatThrownBy(() -> service.method(param))
    .isInstanceOf(SpecificException.class)
    .hasMessageContaining("expected text");
```

### 4. Aislamiento de datos
```java
@Transactional  // Rollback automático
User testData = createTestData();  // Limpio después
```

---

## Próximos pasos

- Agregar tests para endpoints de warehouse si se requiere
- Expandir tests de labels con validaciones adicionales
- Considerar tests de concurrencia más complejos
- Integrar con CI/CD pipeline

---

## Resumen Final

**Tests por módulo**:
- Label Module: 64 tests (Counting, Query, Print, Reprint, Reports, File Generation)
  - LabelCountIntegrationTest: 11
  - LabelQueryAndCaptureIntegrationTest: 5
  - LabelPrintIntegrationTest: 3
  - LabelReprintIntegrationTest: 3
  - LabelReportIntegrationTest: 32
  - GenerateFileIntegrationTest: 10

- User Module: 20 tests (Admin Management)
  - AdminUserManagementIntegrationTest: 20

**Total de tests**: 84
**Estado**: ✓ Todos pasando
**Última actualización**: 2026-05-06
