# Re-importación de Inventario: Comportamiento y Sincronización

## Escenario 1: Importar dos veces el MISMO período (inventario.xlsx)

### Primera Importación
```
Entrada: inventario.xlsx (Nov 2024)
- PROD-001: 100 existencias, Status A
- PROD-002: 50 existencias, Status A

Resultado BD:
✓ products: 2 registros nuevos
✓ inventory_snapshot: 2 registros (sin warehouse_id)
```

### Segunda Importación (re-importación)
```
Entrada: inventario.xlsx (Nov 2024, actualizado)
- PROD-001: 150 existencias, Status A  ← CAMBIÓ
- PROD-002: [AUSENTE EN EXCEL]
- PROD-003: 75 existencias, Status A    ← NUEVO

Lógica (InventoryImportApplicationService):
1. PROD-001 existe → ACTUALIZA exist_qty: 150
2. PROD-002 NO está en Excel → Cambia status a "B" (BAJA)
3. PROD-003 es nuevo → INSERTA con exist_qty: 75

Resultado BD:
✓ PROD-001: exist_qty = 150 (actualizado)
✓ PROD-002: status = B (marcado baja)
✓ PROD-003: exist_qty = 75 (nuevo)

⚠️ PROBLEMA IDENTIFICADO:
- Status se cambia en tabla products
- Pero NO se sincroniza a inventory_snapshot si tiene warehouse_id
```

---

## Escenario 2: Interacción con MultiAlmacén

### Flujo Completo
```
1. Importas inventario.xlsx
   → Crea: products, inventory_snapshot (sin warehouse)

2. Importas multialmacen.xlsx (productos por almacén)
   → Crea: multiwarehouse_existences + inventory_stock
   → Sincroniza automáticamente ambas tablas
   → Crea almacenes si faltan
   → Crea productos si faltan

EJEMPLO:
multialmacen.xlsx:
- PROD-001, ALM-01: 200 existencias, Status A
- PROD-001, ALM-02: 80 existencias, Status A
- PROD-002, ALM-01: 100 existencias, Status A

Resultado:
✓ multiwarehouse_existences: 3 registros
✓ inventory_stock: 3 registros (tablaoptimizada para Labels)
  - PROD-001 + ALM-01: 200
  - PROD-001 + ALM-02: 80
  - PROD-002 + ALM-01: 100

⚠️ CONFLICTO POTENCIAL:
- inventory_snapshot (sin warehouse): PROD-001 = 100
- inventory_stock (con warehouse): PROD-001 = 200 + 80 = 280
- ¿CUÁL es la verdad? Depende de cuál se importó último
```

---

## Matriz de Riesgos: Cómo Altera Datos

| Acción | Tabla Afectada | Efecto | Riesgo |
|--------|---|---|---|
| **1ª Importación Inventario** | products, inventory_snapshot | Crea registros | ✓ Seguro |
| **2ª Importación Inventario** | products, inventory_snapshot | Actualiza exist_qty, marca bajas | ⚠️ Status desincronizado |
| **Importar MultiAlmacén** | multiwarehouse_existences, inventory_stock | Crea desglose por almacén | ⚠️ Dos fuentes de verdad |
| **2ª Importación MultiAlmacén** | multiwarehouse_existences, inventory_stock | Upsert (actualiza) | ✓ OK |
| **Generar Marbetes** | labels | Lee de inventory_stock | ❌ Puede leer datos desactualizados |
| **Registrar Conteo C1/C2** | label_counts | Solo registra, NO altera inventario | ✓ OK |

---

## Sincronización Entre Tablas

### Tabla `inventory_snapshot` (Catálogo Simple)
- Sin warehouse_id → válido solo para períodos
- Se actualiza en: `InventoryImportApplicationService.processSnapshot()`
- Usado por: Consulta de inventario, pre-generación de marbetes

### Tabla `inventory_stock` (Multialmacén Optimizado)
- Con warehouse_id → desglose por almacén
- Se actualiza en: `MultiWarehouseServiceImpl.importFile()` → `inventoryPort.upsertStock()`
- Usado por: Labels (generación de marbetes), reportes

### El Problema
```
Si importas en este orden:
1. inventario.xlsx → inventory_snapshot tiene PROD-001 = 100
2. multialmacen.xlsx → inventory_stock tiene PROD-001 = 200 + 80 = 280
3. Labels genera marbetes usando inventory_stock (280) ← CORRECTO
4. Importas de nuevo inventario.xlsx → inventory_snapshot se actualiza a 150
5. Pero inventory_stock sigue con 280 ← INCONSISTENCIA

Solución: MultiAlmacén es la "fuente de verdad" cuando existe
          Inventario simple es fallback si no hay multialmacén
```

---

## Operaciones Costosas (N+1 Queries)

### `deactivateMissingProducts()` - CRÍTICO
```java
// Línea 355-379 en InventoryImportApplicationService
for (cada producto ausente en Excel) {
    productRepository.save(product);      // UPDATE query 1
    snapshotRepository.save(snapshot);     // UPDATE query 2
}
// Con 10,000 productos ausentes → 20,000 queries en transacción
// Resultado: DEADLOCK, TIMEOUT
```

**Solución Recomendada:** Usar `markAsInactiveNotInImport()` bulk UPDATE (1 query)

---

## Checklist de Sincronización

- [ ] ¿Importaste inventario.xlsx? → Verifica `inventory_snapshot`
- [ ] ¿Importaste multialmacen.xlsx? → Verifica `inventory_stock`
- [ ] ¿Los datos coinciden? → Ejecuta verificación SQL (ver abajo)
- [ ] ¿Generaste marbetes? → Compara con `labels.stock`

### Verificación SQL
```sql
-- Ver si hay inconsistencias
SELECT
    p.cve_art,
    w.name_warehouse,
    COALESCE(ist.exist_qty, 0) AS inventory_stock_qty,
    COALESCE(mwh.stock, 0) AS multiwarehouse_qty,
    CASE WHEN ist.exist_qty != mwh.stock THEN '⚠️ INCONSISTENCIA' ELSE '✓ OK' END AS estado
FROM products p
LEFT JOIN inventory_stock ist ON p.id_product = ist.id_product
LEFT JOIN multiwarehouse_existences mwh ON p.id_product = mwh.product_id
LEFT JOIN warehouse w ON ist.id_warehouse = w.id_warehouse
WHERE ist.id_period = 7  -- Tu período
ORDER BY p.cve_art, w.name_warehouse;
```

---

## Resumen

✅ **Funciona correctamente si:**
- Importas inventario UNA sola vez
- Luego importas multialmacén UNA sola vez
- Generas marbetes después

❌ **Rompe si:**
- Re-importas inventario después de multialmacén → Status desincronizado
- Re-importas con muchos productos ausentes → DEADLOCK (N+1 queries)
- Comparas inventory_snapshot vs inventory_stock sin saber cuál es la verdad

⚠️ **Mitigation:**
- Multialmacén es fuente de verdad cuando existe
- Usar bulk operations para desactivaciones (P1.1)
- Sincronizar status explícitamente en re-importaciones

---

## Preguntas y Escenarios Adicionales

### 1. ¿Qué pasa si cambias warehouse_id en una re-importación?

**Escenario:**
```
1ª Importación:
- PROD-001, ALM-01: 100 existencias

2ª Importación (warehouse cambió):
- PROD-001, ALM-02: 150 existencias

Problema:
- inventory_stock tiene AMBOS registros (ALM-01 y ALM-02)
- Marbetes generados para ALM-01 siguen validando contra ALM-01
- Pero import_log solo registra ALM-02
- ¿Cuál es la verdadera fuente?

Impacto: Conteos C1/C2 pueden estar en almacén incorrecto
```

**Recomendación:** Validar que warehouse_id NO cambie en re-importaciones del mismo período.

---

### 2. ¿Qué pasa si eliminas columna STATUS en re-importación?

**Escenario:**
```
1ª Importación: inventario.xlsx CON columna STATUS
- PROD-001: Status A
- PROD-002: Status B

2ª Importación: inventario.xlsx SIN columna STATUS
- PROD-001: Status = "A" (default)
- PROD-002: Status = "A" (default, se cambia de B a A)

Problema:
- PROD-002 que estaba BAJA ahora se REACTIVA
- Usuario no se da cuenta
- Marbetes se generan para producto que debería estar baja

Impacto: ⚠️ ALTO - Producto inactivo se vuelve activo sin validación
```

**Recomendación:** Lanzar WARNING si columna STATUS está ausente en re-importación.

---

### 3. ¿Qué pasa si importas productos con CVE_ART duplicado (mayúsculas/minúsculas)?

**Escenario:**
```
1ª Importación:
- prod-001 (minúsculas)

2ª Importación:
- PROD-001 (mayúsculas)

Flujo:
productRepository.findByCveArt("PROD-001")
  └─ Si BD es CASE SENSITIVE → NO encuentra prod-001
  └─ Crea nuevo producto PROD-001 (duplicate)

Resultado:
- products tiene TWO registros: prod-001 y PROD-001
- Marbetes pueden usar indistintamente
- Labels duplicados para mismo producto

Impacto: ⚠️ ALTO - Datos corruptos, marbetes duplicados
```

**Recomendación:** Normalizar CVE_ART a UPPER/LOWER en validación (línea 207-214).

---

### 4. ¿Qué pasa en re-importación si un producto tiene Marbetes ya generados?

**Escenario:**
```
Timeline:
1. Importas inventario.xlsx → PROD-001: 100
2. Generas marbetes para PROD-001
   → labels tabla tiene 50 folios (folio 1-50) generados
3. Re-importas con PROD-001: 150 (cambió de 100 a 150)
4. Sistema marca PROD-001 como ausente (si no viene en Excel)
   → status = B

Problema:
- Marbetes ya generados quedan "huérfanos" (product.status = B)
- ¿Qué pasa si haces Conteo C1 en folio ya usado?
- ¿Se revalida cantidad vs status?

Impacto: ⚠️ MEDIO - Marbetes válidos pero producto inactivo
```

**Recomendación:** Antes de marcar producto como BAJA, verificar si tiene marbetes pendientes.

---

### 5. ¿Qué pasa si re-importas el MISMO Excel dos veces seguidas (byte-a-byte)?

**Escenario:**
```
1ª Importación: inventario.xlsx (checksum: ABC123)
2ª Importación: inventario.xlsx (checksum: ABC123, idéntico)

Problema:
- Sistema NO detecta que es el mismo archivo
- Ejecuta lógica de desactivación (deactivateMissingProducts)
- Si productos cambiaron estado entre importaciones → estado inconsistente

Impacto: ⚠️ BAJO pero molesto - Importación innecesaria gasta queries
```

**Recomendación:** Usar checksum para detectar archivos idénticos (ya se calcula en línea 636).

---

### 6. ¿Qué pasa si existe Conteo C2 y re-importas el inventario?

**Escenario:**
```
Timeline:
1. Importas inventario → PROD-001: 100 teórico
2. Generas marbetes
3. Haces Conteo C1 (físico): 95
4. Haces Conteo C2 (verificación): 95
5. Sistema calcula diferencia: 100 - 95 = 5 faltantes
6. Re-importas inventario → PROD-001: 120 (cambió teórico)

Problema:
- ¿La diferencia se recalcula? (120 - 95 = 25)
- ¿Se cierra el conteo C2 antes?
- ¿Qué pasa con audit_logs de labels?

Impacto: ⚠️ ALTO - Reportes de diferencias pueden ser incorrectos
```

**Recomendación:** No permitir re-importación si hay conteos pendientes en ese período.

---

### 7. ¿Qué pasa si importas 50,000 productos y uno falla a mitad?

**Escenario:**
```
Import con 50,000 productos:
- Fila 1-25,000: OK
- Fila 25,001: ERROR (formato inválido)
- Fila 25,002-50,000: SE ABORTA la transacción

@Transactional rollback:
- Todos los 25,000 cambios se revierten
- BD queda sin cambios
- Pero usuario espera al menos los 25,000 válidos

Problema:
- Todo o nada: no hay importación parcial
- Con 50,000 productos y N+1 queries → timeout
- Usuario no sabe qué salió mal

Impacto: ⚠️ CRÍTICO - Timeout total, sin resultado
```

**Recomendación:** 
- Implementar batch processing (procesar en chunks de 5,000)
- Hacer rollback solo del chunk fallido, no de toda la importación
- Reportar progreso (25,000 de 50,000 OK, error en fila XXX)

---

### 8. ¿Qué pasa si dos usuarios importan al MISMO TIEMPO el mismo período?

**Escenario:**
```
Usuario A:     Usuario B:
  import         import
    ↓              ↓
 READ (snap)   READ (snap)
    ↓              ↓
 PROCESS      PROCESS
    ↓              ↓
 WRITE (snap) WRITE (snap) ← RACE CONDITION
    ↓
 deactivate   deactivate
    ↓              ↓

Problema:
- Ambos leen el MISMO estado inicial
- Ambos hacen cambios concurrentes
- Uno sobrescribe cambios del otro
- N+1 queries + transacciones largas = DEADLOCK

Impacto: ⚠️ CRÍTICO - Datos corrompidos, DEADLOCK
```

**Recomendación:** 
- Agregar LOCK de optimistic concurrency (version field)
- O implementar importación serializada por período

---

### 9. ¿Qué pasa si status en Excel viene como "Active" en lugar de "A"?

**Escenario:**
```
Excel con formato diferente:
- STATUS: "Active" en lugar de "A"
- STATUS: "Inactive" en lugar de "B"
- STATUS: "1" en lugar de "A"
- STATUS: vacío

Lógica actual (línea 495-512):
Product.Status.valueOf(rawStatus)  ← Espera "A" o "B" exactamente

Problema:
- Lanza IllegalArgumentException
- Status se defaultea a "A"
- Usuario no sabe por qué su "Active" se convirtió a "A"

Impacto: ⚠️ MEDIO - Status se ignoran silenciosamente
```

**Recomendación:** Normalizar status: "Active"→"A", "Inactive"→"B", "1"→"A", etc.

---

### 10. ¿Qué pasa si re-importas sin pasar warehouse_id?

**Escenario:**
```
1ª Importación:
POST /api/sigmav2/inventory/import?periodId=7&warehouseId=null
→ Crea inventory_snapshot SIN warehouse_id

2ª Importación (re-import con warehouse):
POST /api/sigmav2/inventory/import?periodId=7&warehouseId=250
→ Crea inventory_snapshot CON warehouse_id=250

Problema:
- Ahora tienes AMBOS registros (uno sin warehouse, otro con)
- Consulta suma ambos → cantidad duplicada
- Snapshots se desincroniza entre warehouse=null y warehouse=250

Impacto: ⚠️ ALTO - Datos duplicados en snapshot
```

**Recomendación:** Validar que warehouse_id sea CONSISTENTE en re-importaciones del mismo período.

---

### Resumen de Riesgos Identificados

| # | Riesgo | Criticidad | Área Afectada | Estado |
|---|--------|-----------|---|---|
| 1 | Cambio de warehouse_id en re-import | ⚠️ ALTA | Sincronización | Revisar |
| 2 | Eliminar columna STATUS | ⚠️ ALTA | Validación | Revisar |
| 3 | Duplicados por case-sensitivity | ⚠️ ALTA | Integridad | Revisar |
| 4 | Marbetes huérfanos (producto pasa a BAJA) | ⚠️ MEDIA | Integridad | Revisar |
| 5 | Detección de archivos idénticos | ⚠️ BAJA | Performance | Implementado (checksum) |
| 6 | Re-import con Conteo C2 activo | ⚠️ ALTA | Reportes | Revisar |
| 7 | Fallo en fila 25k de 50k | ⚠️ CRÍTICA | Performance | **BLOCKER P1** |
| 8 | Race condition (2 usuarios simultáneos) | ⚠️ CRÍTICA | Concurrencia | **BLOCKER P1** |
| 9 | Status con formato diferente | ⚠️ MEDIA | Validación | Revisar |
| 10 | Inconsistencia warehouse_id en snapshot | ⚠️ ALTA | Sincronización | Revisar |



