# Validaciones de Existencias en Generación e Impresión de Marbetes

## ❓ Pregunta Original
¿Hay alguna validación que haga que al momento de generar los periodos, los periodos que no tienen existencias no dejan que se generen o impriman?

## ✅ Respuesta

**NO, actualmente NO existe ninguna validación que bloquee la generación o impresión de marbetes por falta de existencias.**

---

## 📋 Flujo Actual del Sistema

### 1️⃣ Generación de Marbetes (`generateBatch`)

**Archivo**: `LabelServiceImpl.java` - Líneas 168-227

#### Proceso:

```java
// 1. Se consultan las existencias (solo para LOG informativo)
int existencias = 0;
try {
    var stockOpt = inventoryStockRepository
        .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
            dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

    if (stockOpt.isPresent()) {
        existencias = stockOpt.get().getExistQty() != null ?
            stockOpt.get().getExistQty().intValue() : 0;
    }
    log.info("Existencias encontradas: {}", existencias);
} catch (Exception e) {
    log.warn("No se pudieron obtener existencias: {}", e.getMessage());
}

// 2. Se genera el rango de folios SIN VALIDAR existencias
long[] range = persistence.allocateFolioRange(dto.getPeriodId(), toGenerate);

// 3. Se guardan TODOS los marbetes con estado GENERADO
persistence.saveLabelsBatch(req.getIdLabelRequest(), dto.getPeriodId(),
    dto.getWarehouseId(), dto.getProductId(), primer, ultimo, userId);

log.info("Marbetes guardados exitosamente con estado GENERADO (existencias: {})", existencias);
```

#### ⚠️ Comportamiento Actual:
- ✅ Se consultan las existencias (solo informativo)
- ❌ **NO hay validación** que bloquee si existencias = 0
- ✅ Se generan TODOS los marbetes solicitados
- ✅ Estado inicial: `GENERADO`

---

### 2️⃣ Impresión de Marbetes (`printLabels`)

**Archivo**: `LabelServiceImpl.java` - Líneas 268-410

#### Validaciones Existentes:

```java
// 1. VALIDAR que se hayan importado catálogos de inventario
boolean hasInventoryData = inventoryStockRepository.existsByWarehouseIdWarehouseAndPeriodId(
    dto.getWarehouseId(), dto.getPeriodId());

if (!hasInventoryData) {
    throw new CatalogNotLoadedException(
        "No se pueden imprimir marbetes porque no se han cargado los catálogos de inventario " +
        "y multialmacén para el periodo y almacén seleccionados.");
}

// 2. VALIDAR que existan marbetes pendientes
List<Label> labelsToProcess = persistence.findPendingLabelsByPeriodAndWarehouse(
    dto.getPeriodId(), dto.getWarehouseId());

if (labelsToProcess.isEmpty()) {
    throw new InvalidLabelStateException(
        "No hay marbetes pendientes de impresión");
}

// 3. VALIDAR estado de cada marbete
if (label.getEstado() == Label.State.CANCELADO) {
    throw new InvalidLabelStateException("El folio está CANCELADO");
}
```

#### ⚠️ Comportamiento Actual:
- ✅ Valida que existan **datos de inventario** en la BD
- ✅ Valida que existan **marbetes pendientes** (estado `GENERADO`)
- ✅ Valida que los marbetes **no estén cancelados**
- ❌ **NO valida** que el producto tenga existencias > 0
- ✅ Permite imprimir marbetes de productos sin existencias

---

### 3️⃣ Consulta de Marbetes Pendientes

**Método**: `findPendingLabelsByPeriodAndWarehouse`

**Archivo**: `LabelsPersistenceAdapter.java` - Líneas 335-337

```java
public List<Label> findPendingLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
    return jpaLabelRepository.findByPeriodIdAndWarehouseIdAndEstado(
        periodId, warehouseId, Label.State.GENERADO);
}
```

#### ⚠️ Comportamiento:
- ✅ Retorna TODOS los marbetes con estado `GENERADO`
- ❌ **NO filtra** por existencias
- ✅ Incluye productos con existencias = 0

---

## 🔍 Caso de Uso: Almacén 24

Según los logs compartidos:

```
Folio  Producto    Almacén      Existencias  Cant. Folios  Estado
1      FactGlob    Almacén 24   0            0            Generado
5      X-TARIMAS   Almacén 24   0            0            Generado
```

### API: `/labels/pending-print-count`
```json
{
  "count": 0,
  "periodId": 20,
  "warehouseId": 420,
  "warehouseName": "Almacén 24"
}
```

### 🔴 Problema Identificado:

El conteo retorna **0 marbetes pendientes** porque:

1. Los marbetes se **generaron** correctamente (estado `GENERADO`)
2. Pero el método `getPendingPrintCount` está retornando **0**
3. Esto sugiere que los marbetes:
   - Ya fueron **IMPRESOS** previamente, o
   - Fueron **CANCELADOS**, o
   - No existen en la base de datos para ese periodo/almacén

### ✅ Verificación Necesaria:

Ejecuta esta consulta SQL para verificar:

```sql
-- Ver TODOS los marbetes del Almacén 24, Periodo 20
SELECT
    l.folio,
    l.estado,
    l.product_id,
    p.cve_art,
    p.descr,
    COALESCE(s.exist_qty, 0) as existencias
FROM labels l
LEFT JOIN product p ON l.product_id = p.id_product
LEFT JOIN inventory_stock s ON s.product_id = l.product_id
    AND s.warehouse_id = l.warehouse_id
    AND s.period_id = l.period_id
WHERE l.warehouse_id = 420
  AND l.period_id = 20
ORDER BY l.folio;
```

---

## 📊 Resumen de Validaciones

| Etapa | Valida Catálogos | Valida Estado | Valida Existencias | Bloquea si Exist=0 |
|-------|------------------|---------------|--------------------|--------------------|
| **Solicitud** | ❌ | ✅ (No GENERADOS) | ❌ | ❌ |
| **Generación** | ❌ | ❌ | ℹ️ (Solo log) | ❌ |
| **Impresión** | ✅ | ✅ | ❌ | ❌ |
| **Conteo** | ❌ | ✅ (IMPRESO) | ❌ | ❌ |

**Leyenda**:
- ✅ = Sí valida
- ❌ = No valida
- ℹ️ = Solo informativo (no bloquea)

---

## 🎯 Conclusiones

### ✅ Lo que SÍ funciona:
1. Se pueden **generar** marbetes para productos sin existencias
2. Se pueden **imprimir** marbetes para productos sin existencias
3. El sistema registra las existencias solo como **información complementaria**
4. No hay restricciones basadas en existencias

### ⚠️ El problema real (Almacén 24):
1. Los marbetes **SÍ se generaron** (aparecen en el summary)
2. Pero **NO aparecen** como pendientes de impresión (`count: 0`)
3. Esto indica que ya están en estado **IMPRESO** o **CANCELADO**

### 🔧 Recomendaciones:

#### 1. Verificar estado real de los marbetes:
```sql
SELECT estado, COUNT(*) as total
FROM labels
WHERE warehouse_id = 420 AND period_id = 20
GROUP BY estado;
```

#### 2. Si quieres REIMPRIMIR marbetes ya impresos:
```javascript
// Usar el endpoint de impresión con forceReprint
await api.post('/labels/print', {
  periodId: 20,
  warehouseId: 420,
  folios: [1, 5],  // Folios específicos
  forceReprint: true  // ← IMPORTANTE
}, {
  responseType: 'blob'
});
```

#### 3. Si quieres BLOQUEAR generación/impresión sin existencias:

Agregar esta validación en `generateBatch` (línea 223):

```java
// NUEVA VALIDACIÓN: No permitir generar si no hay existencias
if (existencias <= 0) {
    throw new InvalidLabelStateException(
        String.format("No se pueden generar marbetes para producto %d " +
            "porque no tiene existencias en el almacén %d periodo %d",
            dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId()));
}
```

---

**Fecha**: 2025-12-18
**Estado**: Documentado
**Acción Requerida**: Verificar estado real de marbetes en Almacén 24

