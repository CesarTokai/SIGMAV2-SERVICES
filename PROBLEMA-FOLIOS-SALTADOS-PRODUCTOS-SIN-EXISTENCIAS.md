# Problema: Folios Saltados para Productos sin Existencias

## Problema Identificado

Al revisar los marbetes generados, se observa que **los folios se saltan** cuando hay productos sin existencias:

```
367 - 371 → M3L8TG5D2 (15,345 existencias) ✓
372 - 376 → ❌ FALTANTE (M3L8TNTD4 con 0 existencias)
377 - 381 → M3LS-01H5 (11,302 existencias) ✓
```

### Evidencia

```
M3L8TG5D2   | Existencias: 15345 | Folios: 5 | Rango: 367-371 | ✓
M3L8TNTD4   | Existencias: 0     | Folios: 0 | Rango: -       | ❌ NO SE GENERARON
M3LS-01H5   | Existencias: 11302 | Folios: 5 | Rango: 377-381 | ✓
```

**Resultado**: Faltan los folios 372-376 (5 folios para M3L8TNTD4)

## Causa Raíz

### 1. Solicitud de Folios con `requestedLabels = 0`

En la tabla `label_requests` existe un registro para el producto `M3L8TNTD4` con:
- `requested_labels = 0` ← **AQUÍ ESTÁ EL PROBLEMA**

### 2. Lógica en `requestLabels()`

```java
// LabelServiceImpl.java - línea 73
if (dto.getRequestedLabels() == 0) {
    // Si la cantidad es 0, se cancela la solicitud o no se crea
    // NO SE GENERAN FOLIOS
    return;
}
```

**Conclusión**: Si `requestedLabels = 0`, el método termina sin generar folios.

### 3. ¿Quién establece `requestedLabels = 0`?

Hay 3 posibilidades:

#### Opción A: Frontend automático
El frontend podría tener lógica que establece la cantidad en 0 cuando detecta existencias = 0:

```javascript
// Ejemplo de lógica problemática en frontend
const cantidadFolios = producto.existencias > 0 ? calcularFolios() : 0;
```

#### Opción B: Proceso de importación
Al importar el inventario, algún proceso automático crea solicitudes con cantidad 0 para productos sin existencias.

#### Opción C: Usuario manual
El usuario ingresó manualmente 0 en la cantidad de folios solicitados.

## Según los Requerimientos

### Regla de Negocio Correcta

> "Se podrán imprimir marbetes siempre y cuando se hayan importado datos de los catálogos de inventario y multialmacén, así como también, **se haya efectuado la captura de marbetes por producto**."

**NO dice**: "solo para productos con existencias"

### Propósito del Conteo Físico

El objetivo del inventario físico es **verificar las existencias reales** vs. las registradas en el sistema:

- Producto con 0 existencias en sistema pero 50 en físico → **DIFERENCIA A DETECTAR**
- Producto con 100 existencias en sistema pero 0 en físico → **DIFERENCIA A DETECTAR**

**Por lo tanto**: Se deben generar marbetes para TODOS los productos, incluso los que tienen 0 existencias en el sistema.

## Soluciones Propuestas

### Solución 1: Validar en Backend (RECOMENDADA)

Modificar `requestLabels()` para prevenir que se acepten solicitudes con `requestedLabels = 0` **SOLO si hay existencias diferentes de cero**:

```java
@Override
@Transactional
public void requestLabels(LabelRequestDTO dto, Long userId, String userRole) {
    // ... código existente ...

    // NUEVA VALIDACIÓN: No permitir requestedLabels = 0
    // EXCEPTO cuando el producto realmente no existe en el inventario
    if (dto.getRequestedLabels() == 0) {
        // Verificar si el producto existe en el inventario del almacén
        Optional<InventoryStockEntity> stockOpt = inventoryStockRepository
            .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

        if (stockOpt.isPresent()) {
            // El producto EXISTE en el inventario
            // Lanzar error si intenta solicitar 0 folios
            throw new InvalidLabelStateException(
                "No se puede solicitar 0 folios para un producto que existe en el inventario. " +
                "Debe solicitar al menos 1 folio para permitir el conteo físico, " +
                "incluso si las existencias actuales son 0."
            );
        } else {
            // El producto NO existe en el inventario - permitir cancelar
            if (existingRequest.isPresent()) {
                persistence.delete(existingRequest.get());
            }
            return;
        }
    }

    // ... resto del código ...
}
```

### Solución 2: Establecer Cantidad Mínima Automática

Modificar la lógica para que si un producto existe en el inventario, siempre tenga al menos 1 folio:

```java
// Al crear/actualizar la solicitud
int cantidadFolios = dto.getRequestedLabels();

// Si el producto existe en inventario, mínimo 1 folio
if (cantidadFolios == 0) {
    Optional<InventoryStockEntity> stockOpt = inventoryStockRepository
        .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
            dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

    if (stockOpt.isPresent()) {
        cantidadFolios = 1;  // Forzar mínimo 1 folio
        log.warn("Producto {} tiene 0 folios solicitados pero existe en inventario. " +
                 "Se establece cantidad mínima de 1 folio.", dto.getProductId());
    }
}

req.setRequestedLabels(cantidadFolios);
```

### Solución 3: Corregir Frontend (COMPLEMENTARIA)

Si el frontend está estableciendo automáticamente 0 para productos sin existencias, modificarlo:

```javascript
// ANTES (incorrecto)
const cantidadFolios = producto.existencias > 0 ? calcularFolios() : 0;

// DESPUÉS (correcto)
const cantidadFolios = calcularFolios(); // Sin importar existencias
```

## Verificación del Problema

### Script SQL para Detectar Productos con 0 Folios Solicitados

```sql
-- Productos en inventario con 0 folios solicitados
SELECT
    p.product_key,
    p.description,
    COALESCE(inv.exist_qty, 0) AS existencias,
    COALESCE(lr.requested_labels, 0) AS folios_solicitados,
    lr.folios_generados
FROM products p
INNER JOIN inventory_stock inv ON inv.product_id = p.id_product
LEFT JOIN label_requests lr ON lr.product_id = p.id_product
    AND lr.period_id = inv.period_id
    AND lr.warehouse_id = inv.warehouse_id
WHERE inv.period_id = (SELECT MAX(id_period) FROM periods)
  AND inv.warehouse_id = 1
  AND (lr.requested_labels = 0 OR lr.requested_labels IS NULL)
ORDER BY p.product_key;
```

### Script SQL para Corregir Temporalmente

```sql
-- TEMPORAL: Establecer mínimo 1 folio para productos con 0
UPDATE label_requests lr
SET requested_labels = 1
WHERE requested_labels = 0
  AND period_id = (SELECT MAX(id_period) FROM periods)
  AND warehouse_id = 1
  AND EXISTS (
      SELECT 1 FROM inventory_stock inv
      WHERE inv.product_id = lr.product_id
        AND inv.warehouse_id = lr.warehouse_id
        AND inv.period_id = lr.period_id
  );
```

**⚠️ ADVERTENCIA**: Esta es una solución temporal. Después de ejecutarla, debes ejecutar `/generate` para generar los folios faltantes.

## Recomendación Final

### Plan de Acción:

1. **Inmediato (Solución Temporal)**:
   - Ejecutar el script SQL de corrección temporal
   - Generar los folios faltantes con `/api/sigmav2/labels/generate`

2. **Corto Plazo (Solución Permanente)**:
   - Implementar Solución 1 o 2 en el backend
   - Revisar el frontend para detectar si está estableciendo automáticamente 0

3. **Validación**:
   - Ejecutar script de verificación
   - Confirmar que no hay productos con `requested_labels = 0` en inventario
   - Verificar que los folios sean continuos

## Impacto

### Problema Actual:
- ❌ Productos sin existencias NO generan folios
- ❌ Se crean "huecos" en la secuencia de folios
- ❌ No se pueden contar físicamente esos productos
- ❌ No se pueden detectar discrepancias en el inventario

### Después de la Solución:
- ✅ TODOS los productos generan folios
- ✅ Secuencia de folios continua sin huecos
- ✅ Se pueden contar físicamente todos los productos
- ✅ Se pueden detectar discrepancias en el inventario

## Conclusión

El problema NO está en la generación de folios (que es continua y correcta), sino en la **solicitud previa** donde se establece `requestedLabels = 0` para productos sin existencias.

La solución requiere modificar la lógica de solicitud para **prevenir o corregir automáticamente** las solicitudes con 0 folios cuando el producto existe en el inventario.

