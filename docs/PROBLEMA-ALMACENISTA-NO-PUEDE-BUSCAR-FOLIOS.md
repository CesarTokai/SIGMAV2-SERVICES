# 🔴 PROBLEMA: Almacenista no puede buscar folios

## 📋 Descripción del Problema

El rol **ALMACENISTA** recibe un error **403 (Forbidden)** al intentar buscar folios usando los endpoints:
- `GET /api/sigmav2/labels/for-count?folio=xxx&periodId=xxx&warehouseId=xxx`
- `POST /api/sigmav2/labels/for-count`

**Error esperado:**
```
403 Forbidden: "No tiene acceso al almacén especificado"
```

---

## 🔍 Causa Raíz

### El flujo de validación es:

```
┌─────────────────────────────────────────────────┐
│ 1. LabelsController                             │
│    @PreAuthorize("hasAnyRole(...ALMACENISTA)") │ ✅ Pasa
└────────────┬────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────┐
│ 2. LabelServiceImpl.getLabelForCount()            │
│    warehouseAccessService.validateWarehouse     │
│    Access(userId, warehouseId, userRole)        │
└────────────┬────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────┐
│ 3. WarehouseAccessService.validateWarehouse     │
│    Access()                                      │
│                                                  │
│    if (ROLES_WITH_FULL_ACCESS.contains(role))  │
│        return;  // ADMINISTRADOR, AUXILIAR: ✅ │
│    else                                          │
│        // ALMACENISTA, AUXILIAR_DE_CONTEO:      │
│        // Consultar BD                          │
└────────────┬────────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────┐
│ 4. UserWarehouseRepository.existsByUserIdAnd    │
│    WarehouseIdAndWarehouseDeletedAtIsNull(...)  │
│                                                   │
│    SELECT COUNT(*) > 0 FROM                      │
│    user_warehouse_assignments                    │
│    WHERE user_id = ? AND warehouse_id = ?        │
└────────────┬───────────────────────────────────┘
             │
             ▼
     ┌────────────────┐
     │ ¿Existe en BD? │
     └────┬───────┬──┘
          │       │
         NO     SÍ
         │       │
         ▼       ▼
        403     200 ✅
```

### El problema es:

**El ALMACENISTA NO está asignado a los almacenes en la tabla `user_warehouse_assignments`**

---

## ✅ Solución

### Paso 1: Verificar asignaciones actuales

```sql
SELECT 
    u.id,
    u.username,
    u.rol,
    COUNT(uw.id) as almacenes_asignados
FROM users u
LEFT JOIN user_warehouse_assignments uw ON u.id = uw.user_id
WHERE u.rol = 'ALMACENISTA'
    AND u.deleted_at IS NULL
GROUP BY u.id, u.username, u.rol;
```

**Resultado esperado:** `almacenes_asignados = 0` o números bajos

### Paso 2: Asignar almacenes al almacenista

**Opción A: Asignar almacenes específicos**
```sql
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, created_at)
VALUES 
    (2, 420, NOW()),  -- Usuario ID 2, Almacén ID 420
    (2, 421, NOW());  -- Usuario ID 2, Almacén ID 421
```

**Opción B: Asignar TODOS los almacenes (recomendado si es almacenista general)**
```sql
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, created_at)
SELECT DISTINCT
    u.id as user_id,
    w.id as warehouse_id,
    NOW() as created_at
FROM users u
CROSS JOIN warehouses w
WHERE u.id = 2  -- Reemplazar con ID del almacenista
    AND u.deleted_at IS NULL
    AND w.deleted_at IS NULL
    AND NOT EXISTS (
        SELECT 1 FROM user_warehouse_assignments uw2
        WHERE uw2.user_id = u.id AND uw2.warehouse_id = w.id
    );
```

### Paso 3: Verificar que funciona

```sql
SELECT 
    u.username,
    w.warehouse_key,
    w.name_warehouse
FROM users u
JOIN user_warehouse_assignments uw ON u.id = uw.user_id
JOIN warehouses w ON uw.warehouse_id = w.id
WHERE u.id = 2;  -- Reemplazar con ID del almacenista
```

---

## 🔧 Alternativa: Modificar el código

Si se desea que ALMACENISTA tenga acceso SIN asignación (como `AUXILIAR_DE_CONTEO`), se puede modificar `LabelCountService.java`:

### En `findAndValidateLabelForCount()` (línea 169):
```java
// Antes:
if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
    warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), roleUpper);
}

// Después:
if (!roleUpper.equals("AUXILIAR_DE_CONTEO") && !roleUpper.equals("ALMACENISTA")) {
    warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), roleUpper);
}
```

### En `findLabelForUpdate()` (línea 192):
```java
// Antes:
if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
    warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), roleUpper);
}

// Después:
if (!roleUpper.equals("AUXILIAR_DE_CONTEO") && !roleUpper.equals("ALMACENISTA")) {
    warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), roleUpper);
}
```

**⚠️ NOTA:** Esta alternativa cambia las reglas de negocio. Se recomienda la **Solución 1** (asignar almacenes en BD).

---

## 📊 Resumen de Roles y Acceso

| Rol | Acceso | Almacenes | Método |
|-----|--------|-----------|--------|
| ADMINISTRADOR | Total | Todos | Automático |
| AUXILIAR | Total | Todos | Automático |
| ALMACENISTA | Restringido | Asignados en BD | Validación en `user_warehouse_assignments` |
| AUXILIAR_DE_CONTEO | Restringido | Sin validación | Excepción especial en código |

---

## ✅ Pasos para Resolver

1. ✅ Ejecutar script SQL de asignación
2. ✅ Verificar que el almacenista aparece en `user_warehouse_assignments`
3. ✅ Intentar buscar folios nuevamente
4. ✅ Debería recibir **200 OK** en lugar de **403 Forbidden**

---

## 📝 Archivo de Script

**Ubicación:** `scripts/asignar-almacenes-almacenista.sql`

Contiene queries para:
- Verificar almacenistas sin asignaciones
- Listar almacenes activos
- Asignar todos los almacenes a todos los almacenistas
- Verificar asignaciones

