# Explicación: Folios, Existencias y Visualización de Marbetes

## Resumen Ejecutivo

**NO HAY NINGUNA VALIDACIÓN que impida imprimir marbetes sin existencias.** El sistema está diseñado correctamente para generar e imprimir TODOS los marbetes solicitados, independientemente de si el producto tiene existencias o no.

## ¿Por qué parece que los folios "se saltan" (del 50 al 70)?

### La Clave: La Tabla Muestra PRODUCTOS, No FOLIOS

La tabla que estás viendo muestra un **RESUMEN POR PRODUCTO**, no una lista de folios individuales. Por eso ves "saltos" en los números:

```
Folio 3  → GM17CRTC1  (Rango: -)
Folio 5  → GM17CRTCJ  (Rango: -)
Folio 0  → GM17CWMB2  (Rango: -)
Folio 3  → GM17MEXB8  (Rango: 18-20) ← Aquí hay 3 marbetes: folio 18, 19, 20
Folio 3  → GM17WLMB8  (Rango: 31-33) ← Aquí hay 3 marbetes: folio 31, 32, 33
```

### Lo que realmente está pasando:

1. **Los folios SÍ se generan de forma continua**: 1, 2, 3, 4, 5, 6, 7, 8, 9... sin saltos
2. **Pero cada producto puede tener múltiples folios**
3. **La tabla agrupa por producto y muestra el rango de folios de ese producto**

### Ejemplo Real:

Si generaste 50 marbetes para diferentes productos:
- **Producto A**: Folios 1-10 (10 marbetes)
- **Producto B**: Folios 11-25 (15 marbetes)
- **Producto C**: Folios 26-30 (5 marbetes)
- **Producto D**: Folios 31-50 (20 marbetes)

En la tabla verás:
```
Producto A → Rango: 1-10
Producto B → Rango: 11-25
Producto C → Rango: 26-30
Producto D → Rango: 31-50
```

**No hay saltos en los folios**, solo agrupación por producto.

## Reglas de Negocio Confirmadas

### 1. Generación de Marbetes (generateBatch)

**Ubicación**: `LabelServiceImpl.java` líneas 145-244

```java
// TODOS los marbetes se generan sin importar existencias
persistence.saveLabelsBatch(req.getIdLabelRequest(), dto.getPeriodId(),
    dto.getWarehouseId(), dto.getProductId(), primer, ultimo, userId);
```

**✅ Confirmado**: Se generan TODOS los folios solicitados, tengan o no existencias.

### 2. Impresión de Marbetes (printLabels)

**Ubicación**: `LabelServiceImpl.java` líneas 246-370

```java
// Buscar marbetes pendientes SIN filtrar por existencias
labelsToProcess = persistence.findPendingLabelsByPeriodAndWarehouse(
    dto.getPeriodId(), dto.getWarehouseId());
```

**✅ Confirmado**: Se imprimen TODOS los marbetes con estado GENERADO, sin validar existencias.

### 3. Consulta de Marbetes Pendientes

**Ubicación**: `LabelsPersistenceAdapter.java` líneas 335-349

```java
public List<Label> findPendingLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
    return jpaLabelRepository.findByPeriodIdAndWarehouseIdAndEstado(
        periodId, warehouseId, Label.State.GENERADO);
}
```

**✅ Confirmado**: Solo filtra por estado GENERADO, NO por existencias.

### 4. Generación del PDF

**Ubicación**: `JasperLabelPrintService.java` líneas 150-200

```java
for (Label label : labels) {
    // Procesa TODOS los labels recibidos sin filtrar
    record.put("NomMarbete", String.valueOf(label.getFolio()));
    // ...
    dataSource.add(record);
}
```

**✅ Confirmado**: Se procesan TODOS los marbetes, sin validar existencias.

## Cambios Históricos Importantes

### Antes (Cancelación Automática) ❌

```java
// CÓDIGO ANTIGUO - Ya no se usa
if (existencias == 0) {
    // Se cancelaban automáticamente los marbetes sin existencias
    label.setEstado(Label.State.CANCELADO);
}
```

### Ahora (Sin Cancelación Automática) ✅

```java
// CÓDIGO ACTUAL
// Generar TODOS los marbetes con estado GENERADO (sin validar existencias)
persistence.saveLabelsBatch(req.getIdLabelRequest(), dto.getPeriodId(),
    dto.getWarehouseId(), dto.getProductId(), primer, ultimo, userId);
```

**Razón del cambio**: Según los requerimientos, se deben generar e imprimir TODOS los marbetes solicitados, para que el personal pueda realizar el conteo físico y determinar si realmente hay o no existencias.

## Verificación de la Base de Datos

### Query para Ver TODOS los Folios Individuales

```sql
-- Ver TODOS los marbetes ordenados por folio (sin agrupar)
SELECT
    l.folio,
    p.product_key AS clave_producto,
    p.description AS producto,
    COALESCE(inv.exist_qty, 0) AS existencias,
    l.estado,
    CASE WHEN lp.id_label_print IS NOT NULL THEN 'Sí' ELSE 'No' END AS impreso
FROM labels l
INNER JOIN products p ON l.product_id = p.id_product
LEFT JOIN inventory_stock inv ON inv.product_id = p.id_product
    AND inv.warehouse_id = l.warehouse_id
    AND inv.period_id = l.period_id
LEFT JOIN labels_print lp ON lp.folio = l.folio
    AND lp.period_id = l.period_id
    AND lp.warehouse_id = l.warehouse_id
WHERE l.period_id = (SELECT MAX(id_period) FROM periods)
  AND l.warehouse_id = 1
ORDER BY l.folio;  -- Orden por folio para ver la secuencia continua
```

### Query para Detectar Huecos en la Secuencia

```sql
-- Verificar si hay folios faltantes (huecos en la secuencia)
WITH RECURSIVE folio_sequence AS (
    SELECT MIN(folio) AS expected_folio, MAX(folio) AS max_folio
    FROM labels
    WHERE period_id = (SELECT MAX(id_period) FROM periods)

    UNION ALL

    SELECT expected_folio + 1, max_folio
    FROM folio_sequence
    WHERE expected_folio < max_folio
)
SELECT fs.expected_folio AS folio_faltante
FROM folio_sequence fs
LEFT JOIN labels l ON l.folio = fs.expected_folio
    AND l.period_id = (SELECT MAX(id_period) FROM periods)
WHERE l.folio IS NULL
AND fs.expected_folio IS NOT NULL
ORDER BY fs.expected_folio
LIMIT 50;
```

Si esta query devuelve resultados, entonces HAY huecos. Si no devuelve nada, los folios son continuos.

## Conclusión

### ¿Hay validación que impida imprimir marbetes sin existencias?

**NO.** El sistema está diseñado para:

1. ✅ **Generar** todos los marbetes solicitados (con o sin existencias)
2. ✅ **Imprimir** todos los marbetes generados (con o sin existencias)
3. ✅ **Permitir conteo** de todos los marbetes (con o sin existencias)

### ¿Por qué se ven "saltos" en los números?

**La tabla muestra productos agrupados, no folios individuales.** Los folios SÍ son continuos en la base de datos, pero la visualización agrupa por producto mostrando rangos.

### ¿Cómo verificar que los folios son continuos?

1. Ejecuta el script `diagnostico-folios-completo.sql`
2. Revisa la primera query para ver TODOS los folios ordenados
3. Si ves: 1, 2, 3, 4, 5... entonces están continuos
4. Si la segunda query no devuelve resultados, NO hay huecos

### Recomendaciones

1. **No modificar** la lógica de generación - está correcta
2. **Verificar en la base de datos** con las queries proporcionadas
3. **Revisar el frontend** si la visualización causa confusión
4. **Considerar agregar** una vista que muestre folios individuales además del resumen por producto

