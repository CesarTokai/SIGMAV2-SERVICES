# SOLUCIÓN IMPLEMENTADA: Prevenir Folios Saltados por Productos sin Existencias

## Resumen Ejecutivo

✅ **Solución implementada** para prevenir que se generen "huecos" en la secuencia de folios cuando hay productos sin existencias.

## Problema Original

```
Folios 367-371 → Producto con existencias ✓
Folios 372-376 → ❌ SALTADOS (Producto con 0 existencias, 0 folios solicitados)
Folios 377-381 → Producto con existencias ✓
```

## Causa Raíz Identificada

El frontend o algún proceso automático estaba creando solicitudes con `requestedLabels = 0` para productos sin existencias, lo que causaba que:

1. No se generaran folios para esos productos
2. Se crearan "huecos" en la secuencia de folios
3. No fuera posible contar físicamente esos productos

## Solución Implementada

### Modificación en `LabelServiceImpl.requestLabels()`

**Archivo**: `LabelServiceImpl.java` (líneas 67-105 aprox.)

**Cambio**: Agregada validación que **previene** solicitar 0 folios cuando el producto existe en el inventario.

```java
if (dto.getRequestedLabels() == 0) {
    // Verificar si el producto existe en el inventario
    Optional<InventoryStockEntity> stockOpt = inventoryStockRepository
        .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
            dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

    if (stockOpt.isPresent()) {
        // El producto EXISTE en el inventario
        // No permitir solicitar 0 folios
        throw new InvalidLabelStateException(
            "No se puede solicitar 0 folios para un producto que existe en el inventario. " +
            "Debe solicitar al menos 1 folio para permitir el conteo físico, " +
            "incluso si las existencias actuales son 0. " +
            "Esto permite detectar discrepancias entre el inventario del sistema y el físico.");
    }

    // Si el producto NO existe en inventario, sí permitir cancelar
    // ... lógica existente ...
}
```

### Lógica

| Situación | `requestedLabels` | Acción |
|-----------|-------------------|--------|
| Producto **EXISTE** en inventario | 0 | ❌ **ERROR** - Debe solicitar al menos 1 |
| Producto **EXISTE** en inventario | ≥ 1 | ✅ Permite la solicitud |
| Producto **NO EXISTE** en inventario | 0 | ✅ Permite cancelar solicitud |
| Producto **NO EXISTE** en inventario | ≥ 1 | ✅ Permite la solicitud |

## Impacto

### Antes de la Solución ❌
- Productos sin existencias → 0 folios solicitados → NO se generan folios
- Secuencia de folios con huecos: 1, 2, 3... 20, 25, 26... (faltan 21-24)
- Imposible contar físicamente productos sin existencias
- No se detectan discrepancias en inventario

### Después de la Solución ✅
- Productos sin existencias → Mínimo 1 folio solicitado → SÍ se generan folios
- Secuencia de folios continua: 1, 2, 3, 4, 5... sin huecos
- Posible contar físicamente TODOS los productos
- Se detectan discrepancias en inventario (ej: producto con 0 en sistema pero 50 en físico)

## Migración de Datos Existentes

Si ya tienes productos con `requestedLabels = 0` en la base de datos, debes ejecutar el script de corrección:

### Paso 1: Diagnosticar

```sql
-- Ver productos afectados
SELECT
    p.product_key,
    p.description,
    COALESCE(inv.exist_qty, 0) AS existencias,
    lr.requested_labels
FROM products p
INNER JOIN inventory_stock inv ON inv.product_id = p.id_product
LEFT JOIN label_requests lr ON lr.product_id = p.id_product
    AND lr.period_id = inv.period_id
    AND lr.warehouse_id = inv.warehouse_id
WHERE inv.period_id = (SELECT MAX(id_period) FROM periods)
  AND inv.warehouse_id = 1
  AND lr.requested_labels = 0;
```

### Paso 2: Corregir

```sql
-- Establecer mínimo 1 folio para productos en inventario
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

### Paso 3: Generar Folios Faltantes

Después de corregir las solicitudes, usa la API para generar los folios:

```bash
POST /api/sigmav2/labels/generate
{
  "productId": <ID_PRODUCTO>,
  "warehouseId": 1,
  "periodId": <ID_PERIODO>,
  "labelsToGenerate": 1
}
```

O usa el script PowerShell proporcionado: `corregir-folios-saltados.ps1`

## Validación

### 1. Compilación ✅
- Sin errores de compilación
- Solo warnings menores (no afectan funcionalidad)

### 2. Comportamiento Esperado

#### Escenario A: Producto en inventario con existencias = 0
```
POST /api/sigmav2/labels/request
{
  "productId": 123,
  "warehouseId": 1,
  "periodId": 1,
  "requestedLabels": 0  ← Intentar solicitar 0
}

Respuesta esperada:
❌ 400 Bad Request
"No se puede solicitar 0 folios para un producto que existe en el inventario..."
```

#### Escenario B: Producto en inventario con existencias = 0, solicitando 1+
```
POST /api/sigmav2/labels/request
{
  "productId": 123,
  "warehouseId": 1,
  "periodId": 1,
  "requestedLabels": 1  ← Solicitar al menos 1
}

Respuesta esperada:
✅ 201 Created
```

#### Escenario C: Producto NO en inventario
```
POST /api/sigmav2/labels/request
{
  "productId": 999,  ← No existe en inventory_stock
  "warehouseId": 1,
  "periodId": 1,
  "requestedLabels": 0
}

Respuesta esperada:
✅ 200 OK (Cancela la solicitud si existe)
```

## Próximos Pasos

### Para Solucionar el Problema Actual:

1. **Ejecutar diagnóstico**: `verificar-y-corregir-folios-saltados.sql` (PASO 1)
2. **Revisar resultados**: Ver cuántos productos tienen `requestedLabels = 0`
3. **Aplicar corrección SQL**: Ejecutar UPDATE (PASO 2 del script SQL)
4. **Generar folios faltantes**: Usar API `/generate` para cada producto corregido
5. **Verificar**: Confirmar que no hay huecos en la secuencia de folios

### Para Prevenir el Problema en el Futuro:

1. **Backend**: ✅ Ya implementado (validación en `requestLabels()`)
2. **Frontend**: Revisar si hay lógica que establece automáticamente 0 para productos sin existencias
3. **Pruebas**: Ejecutar `test-validacion-cero-folios.ps1` para validar el comportamiento

## Archivos Relacionados

### Documentación
- `PROBLEMA-FOLIOS-SALTADOS-PRODUCTOS-SIN-EXISTENCIAS.md` - Análisis detallado del problema
- `EXPLICACION-FOLIOS-Y-EXISTENCIAS.md` - Explicación general de folios y existencias

### Scripts SQL
- `verificar-y-corregir-folios-saltados.sql` - Diagnóstico y corrección de datos existentes
- `diagnostico-folios-completo.sql` - Diagnóstico general de folios

### Scripts PowerShell
- `corregir-folios-saltados.ps1` - Script automático para corregir productos afectados
- `test-validacion-cero-folios.ps1` - Pruebas de la nueva validación
- `verificar-folios-existencias.ps1` - Verificación general

## Conclusión

✅ **Solución permanente implementada** en el backend que previene la creación de solicitudes con 0 folios para productos que existen en el inventario.

✅ **Scripts de migración** disponibles para corregir datos existentes.

✅ **Alineado con requerimientos**: Permite generar marbetes para TODOS los productos en inventario, incluso los que tienen 0 existencias, para facilitar el conteo físico y detección de discrepancias.

## Beneficios

1. **Secuencia de folios continua** - Sin huecos
2. **Conteo físico completo** - Todos los productos pueden contarse
3. **Detección de discrepancias** - Se pueden encontrar productos "perdidos" o no registrados
4. **Cumplimiento de requerimientos** - Alineado con las reglas de negocio del módulo de inventario

