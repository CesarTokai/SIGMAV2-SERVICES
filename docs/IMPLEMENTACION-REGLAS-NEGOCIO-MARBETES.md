# Implementación de Reglas de Negocio - Módulo de Marbetes

## Fecha: 27 de Noviembre de 2025

---

## RESUMEN DE CAMBIOS IMPLEMENTADOS

### ✅ 1. Control de Acceso a Nivel de Endpoint (PROBLEMA #1 - CORREGIDO)

**Archivo modificado:** `LabelsController.java`

**Cambios realizados:**
- Se agregaron anotaciones `@PreAuthorize` a todos los endpoints
- Se importó `org.springframework.security.access.prepost.PreAuthorize`

**Permisos implementados:**

| Endpoint | Método | Roles Permitidos | Regla de Negocio |
|----------|--------|------------------|------------------|
| `/request` | POST | ADMINISTRADOR, AUXILIAR, ALMACENISTA | Captura de marbetes |
| `/generate` | POST | ADMINISTRADOR, AUXILIAR, ALMACENISTA | Generación de marbetes |
| `/print` | POST | ADMINISTRADOR, AUXILIAR, ALMACENISTA | Impresión de marbetes |
| `/counts/c1` | POST | ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO | Conteo C1 |
| `/counts/c2` | POST | AUXILIAR_DE_CONTEO | Conteo C2 (exclusivo) |

**Código añadido:**
```java
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
@PreAuthorize("hasRole('AUXILIAR_DE_CONTEO')")
```

---

### ✅ 2. Sistema de Validación de Almacenes Asignados (PROBLEMA #3 - IMPLEMENTADO)

#### 2.1 Migración de Base de Datos
**Archivo creado:** `V1_1_1__Create_user_warehouse_assignments.sql`

**Tabla creada:**
```sql
CREATE TABLE user_warehouse_assignments (
    id_user BIGINT NOT NULL,
    id_warehouse BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (id_user, id_warehouse),
    FOREIGN KEY (id_user) REFERENCES users(id_user),
    FOREIGN KEY (id_warehouse) REFERENCES main_warehouse(id_warehouse)
);
```

**Características:**
- Clave primaria compuesta (usuario + almacén)
- Soft delete con campo `is_active`
- Auditoría con `assigned_at` y `assigned_by`
- Índices para optimizar consultas

#### 2.2 Entidad JPA
**Archivo creado:** `UserWarehouseAssignment.java`

**Ubicación:** `modules/warehouse/domain/model/`

**Características:**
- Anotación `@IdClass` para clave compuesta
- Clase interna `UserWarehouseId` para la clave primaria
- Lombok para reducir boilerplate

#### 2.3 Repositorio
**Archivo creado:** `UserWarehouseAssignmentRepository.java`

**Ubicación:** `modules/warehouse/infrastructure/repository/`

**Métodos implementados:**
- `existsByUserIdAndWarehouseIdAndIsActiveTrue()` - Verifica acceso
- `findWarehouseIdsByUserId()` - Lista almacenes del usuario
- `findUserIdsByWarehouseId()` - Lista usuarios del almacén
- `findByUserIdAndIsActiveTrue()` - Todas las asignaciones del usuario
- `findByWarehouseIdAndIsActiveTrue()` - Todas las asignaciones del almacén

#### 2.4 Servicio de Validación de Acceso
**Archivo creado:** `WarehouseAccessService.java`

**Ubicación:** `modules/warehouse/application/service/`

**Métodos implementados:**

1. **`validateWarehouseAccess(userId, warehouseId, userRole)`**
   - Valida que un usuario tenga acceso a un almacén
   - ADMINISTRADOR y AUXILIAR: Acceso total (sin validación)
   - ALMACENISTA y AUXILIAR_DE_CONTEO: Solo almacenes asignados
   - Lanza `PermissionDeniedException` si no tiene acceso

2. **`getAccessibleWarehouses(userId, userRole)`**
   - Retorna lista de IDs de almacenes accesibles
   - Retorna `null` si tiene acceso total (ADMINISTRADOR/AUXILIAR)
   - Útil para filtrar listados

3. **`hasFullAccess(userRole)`**
   - Verifica si un rol tiene acceso total
   - Retorna `true` para ADMINISTRADOR y AUXILIAR

#### 2.5 Integración en LabelServiceImpl
**Archivo modificado:** `LabelServiceImpl.java`

**Cambios realizados:**

1. **Inyección de dependencia:**
```java
private final WarehouseAccessService warehouseAccessService;
```

2. **Validación en `requestLabels()`:**
```java
warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
```

3. **Validación en `generateBatch()`:**
```java
warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
```

4. **Validación en `printLabels()`:**
```java
warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
```

5. **Validación en `registerCountC1()` y `registerCountC2()`:**
```java
// Primero obtiene el marbete para conocer su almacén
Label label = persistence.findByFolio(dto.getFolio()).orElseThrow(...);
// Luego valida acceso
warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
```

---

### ✅ 3. Actualización de Interfaces y Firmas de Métodos

#### 3.1 Interface LabelService
**Archivo modificado:** `LabelService.java`

**Cambios:**
- Todos los métodos ahora reciben `String userRole` como parámetro
- Permite validar acceso a almacenes basado en rol

**Nuevas firmas:**
```java
void requestLabels(LabelRequestDTO dto, Long userId, String userRole);
void generateBatch(GenerateBatchDTO dto, Long userId, String userRole);
LabelPrint printLabels(PrintRequestDTO dto, Long userId, String userRole);
LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole);
LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole);
```

#### 3.2 Controlador
**Archivo modificado:** `LabelsController.java`

**Cambios:**
- Todos los endpoints ahora reciben header `X-User-Role`
- Se pasa el rol al servicio para validaciones

**Ejemplo:**
```java
@RequestHeader(value = "X-User-Role", required = false) String userRole
```

---

## FLUJO DE VALIDACIÓN IMPLEMENTADO

### Ejemplo: Solicitar Folios

```
Cliente → LabelsController.requestLabels()
   ↓
   @PreAuthorize verifica rol (ADMINISTRADOR/AUXILIAR/ALMACENISTA)
   ↓
   LabelServiceImpl.requestLabels(dto, userId, userRole)
   ↓
   WarehouseAccessService.validateWarehouseAccess()
   ↓
   Si rol = ADMINISTRADOR o AUXILIAR → ✅ Permitir
   Si rol = ALMACENISTA o AUXILIAR_DE_CONTEO → Consultar BD
      ↓
      UserWarehouseAssignmentRepository.existsByUserIdAndWarehouseId()
      ↓
      Si existe asignación activa → ✅ Permitir
      Si no existe → ❌ PermissionDeniedException
```

---

## VENTAJAS DE LA IMPLEMENTACIÓN

### 1. Seguridad en Capas
- **Capa 1:** Spring Security con `@PreAuthorize` (nivel de endpoint)
- **Capa 2:** Validación de almacenes en servicio (nivel de lógica de negocio)

### 2. Flexibilidad
- Administradores y Auxiliares: Acceso total sin restricciones
- Almacenistas y Auxiliares de Conteo: Control fino por almacén
- Fácil de extender para nuevos roles

### 3. Auditoría
- Tabla `user_warehouse_assignments` registra quién asignó y cuándo
- Soft delete permite histórico de asignaciones

### 4. Performance
- Índices en campos más consultados
- Consultas optimizadas con `@Query`
- Solo roles restringidos consultan la BD

### 5. Mantenibilidad
- Separación de responsabilidades clara
- Servicio dedicado `WarehouseAccessService`
- Fácil de testear unitariamente

---

## PENDIENTES Y RECOMENDACIONES

### 🟡 Problema #2: Validación de Roles en C2
**Estado:** PENDIENTE DE ACLARACIÓN

**Situación:**
- Código actual: Solo AUXILIAR_DE_CONTEO puede hacer C2
- Documentación: Ambigua ("todos los roles pueden operar conteo")

**Recomendación:**
Clarificar con stakeholder si C2 debe ser:
- **Opción A:** Exclusivo de AUXILIAR_DE_CONTEO (implementación actual)
- **Opción B:** Todos los roles (requiere modificar `registerCountC2()`)

### 🟡 Problema #4: Validación de Catálogos Cargados
**Estado:** MARCADO COMO TODO EN CÓDIGO

**Ubicación:** `LabelServiceImpl.printLabels()`

**Código actual:**
```java
// TODO: Agregar validación de catálogos cargados (inventario y multialmacén)
```

**Recomendación:**
Implementar validación que verifique:
1. Existe inventario cargado para el periodo/almacén
2. Existe multialmacén cargado para el periodo/almacén

**Ejemplo de implementación:**
```java
if (!inventoryService.existsForPeriodWarehouse(periodId, warehouseId)) {
    throw new InvalidStateException("No existe inventario cargado");
}
if (!multiWarehouseService.existsForPeriodWarehouse(periodId, warehouseId)) {
    throw new InvalidStateException("No existe multialmacén cargado");
}
```

---

## PASOS PARA COMPLETAR LA IMPLEMENTACIÓN

### 1. Ejecutar Migración de Base de Datos
```bash
# La migración se ejecutará automáticamente al iniciar la aplicación
# Flyway detectará V1_1_1__Create_user_warehouse_assignments.sql
```

### 2. Asignar Almacenes a Usuarios
**Opción A - SQL directo:**
```sql
INSERT INTO user_warehouse_assignments (id_user, id_warehouse, assigned_by)
VALUES (1, 1, NULL);
```

**Opción B - Crear endpoint de administración (recomendado):**
```java
@PostMapping("/admin/users/{userId}/warehouses/{warehouseId}")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public ResponseEntity<Void> assignWarehouse(@PathVariable Long userId,
                                              @PathVariable Long warehouseId);
```

### 3. Actualizar Frontend
**Headers requeridos en todas las peticiones:**
```javascript
headers: {
    'X-User-Id': userId,
    'X-User-Role': userRole  // Nuevo header requerido
}
```

### 4. Testing
**Casos de prueba recomendados:**
1. ADMINISTRADOR puede acceder a cualquier almacén ✓
2. AUXILIAR puede acceder a cualquier almacén ✓
3. ALMACENISTA solo accede a almacenes asignados ✓
4. AUXILIAR_DE_CONTEO solo accede a almacenes asignados ✓
5. Intento de acceso a almacén no asignado → 403 Forbidden ✓
6. Conteo C1 con todos los roles permitidos ✓
7. Conteo C2 solo con AUXILIAR_DE_CONTEO ✓

---

## DOCUMENTOS GENERADOS

1. **`ANALISIS-CUMPLIMIENTO-MARBETES.md`** - Análisis detallado de cumplimiento
2. **Este documento** - Resumen de implementación

---

## CONCLUSIÓN

Se han implementado exitosamente las correcciones para los problemas **#1 (Control de acceso)** y **#3 (Almacenes asignados)**, que eran de prioridad **ALTA** 🔴.

El módulo de Marbetes ahora cumple con las reglas de negocio especificadas en cuanto a:
- ✅ Control de acceso por rol a nivel de endpoint
- ✅ Contexto informativo (almacenes asignados por usuario)
- ✅ Validaciones de secuencia de conteos
- ✅ Validaciones de estado de marbetes
- ✅ Restricciones de captura y generación

**Próximos pasos:**
1. Ejecutar migraciones de BD
2. Asignar almacenes a usuarios
3. Actualizar frontend para enviar header `X-User-Role`
4. Implementar validación de catálogos cargados (Problema #4)
5. Clarificar y ajustar roles permitidos en C2 (Problema #2)

---

**Elaborado por:** GitHub Copilot
**Fecha:** 27 de Noviembre de 2025
**Versión:** 1.0

