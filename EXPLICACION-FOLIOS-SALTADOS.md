# ✅ ACTUALIZACIÓN: Los Folios YA NO se Saltan

**Fecha actualización:** 2025-12-16
**Estado:** ✅ **CORREGIDO**

---

## 🎯 Cambio Implementado

Se **eliminó la funcionalidad** que cancelaba automáticamente los marbetes sin existencias.

### ❌ Comportamiento ANTERIOR (Eliminado)

```
Producto SIN existencias → Marbetes con estado CANCELADO (auto)
→ Guardados en tabla labels_cancelled
→ No aparecían en la tabla labels
→ CAUSABA "SALTOS" en los folios visibles
```

### ✅ Comportamiento ACTUAL (Nuevo)

```
TODOS los productos → Marbetes con estado GENERADO
→ Guardados en tabla labels
→ Todos visibles
→ SIN SALTOS en la secuencia de folios
```

---

## 📊 Ahora los Folios son Continuos y Visibles

### Antes del Cambio:
```
Folio  Producto      Estado        Visible
18     GM17MEXB8     GENERADO      ✓ Sí
19     GM17MEXB8     GENERADO      ✓ Sí
20     GM17MEXB8     GENERADO      ✓ Sí
21     ProductoX     CANCELADO     ✗ No (en otra tabla)
...
30     ProductoX     CANCELADO     ✗ No (en otra tabla)
31     GM17WLMB8     GENERADO      ✓ Sí
```

### Después del Cambio:
```
Folio  Producto      Estado        Visible
18     GM17MEXB8     GENERADO      ✓ Sí
19     GM17MEXB8     GENERADO      ✓ Sí
20     GM17MEXB8     GENERADO      ✓ Sí
21     ProductoX     GENERADO      ✓ Sí (ahora visible)
...
30     ProductoX     GENERADO      ✓ Sí (ahora visible)
31     GM17WLMB8     GENERADO      ✓ Sí
```

---

## 🔧 Código Modificado

### En `LabelServiceImpl.generateBatch()`:

**ANTES:**
```java
if (existencias > 0) {
    persistence.saveLabelsBatch(...);
    generadosConExistencias = toGenerate;
} else {
    persistence.saveLabelsBatchAsCancelled(...);  // ← ELIMINADO
    generadosSinExistencias = toGenerate;
}
```

**AHORA:**
```java
// TODOS los marbetes se generan normalmente
persistence.saveLabelsBatch(req.getIdLabelRequest(), dto.getPeriodId(),
    dto.getWarehouseId(), dto.getProductId(), primer, ultimo, userId);

log.info("Marbetes guardados exitosamente con estado GENERADO (existencias: {})", existencias);
```

---

## ✅ Validaciones Eliminadas

### 1. En Generación
- ❌ Ya NO se valida si hay existencias > 0
- ✅ Todos los marbetes se generan con estado GENERADO

### 2. En Cancelación Manual
- ❌ Ya NO se valida que tenga existencias físicas
- ✅ Se puede cancelar cualquier marbete (con o sin existencias)

---

## 📝 Validaciones que SÍ Permanecen en `cancelLabel()`

1. ✅ Acceso al almacén
2. ✅ Marbete existe
3. ✅ Pertenece al periodo/almacén correcto
4. ✅ NO está ya cancelado
5. ✅ Tiene folios asignados (requestedLabels > 0)
6. ❌ ~~Tiene existencias físicas~~ **← ELIMINADA**

---

## 🎯 Beneficios del Cambio

### Para el Usuario
- ✅ **Todos los folios son visibles** en la interfaz
- ✅ **Secuencia continua** sin saltos
- ✅ **Menos confusión** al ver los marbetes
- ✅ El usuario decide **manualmente** qué cancelar

### Para el Sistema
- ✅ **Una sola tabla** para gestionar (`labels`)
- ✅ **Menos complejidad** en consultas
- ✅ **Lógica más simple** de entender
- ✅ **Más flexibilidad** operativa

---

## ⚠️ Consideraciones Importantes

### Responsabilidad del Usuario

Ahora es **responsabilidad del usuario** cancelar marbetes de productos sin existencias si lo considera necesario.

**Antes:**
- Sistema: "Este producto no tiene existencias → LO CANCELO AUTOMÁTICAMENTE"

**Ahora:**
- Sistema: "Genero el marbete, el usuario decide si lo cancela o no"

### Flujo Recomendado

```
1. Usuario genera marbetes (todos con estado GENERADO)
2. Usuario revisa cuáles tienen/no tienen existencias
3. Usuario cancela MANUALMENTE los que considere necesarios
```

---

## 🔄 Migración de Datos Existentes

### Datos Anteriores

Si ya tienes marbetes en `labels_cancelled` por "falta de existencias":

**Opción A:** Dejarlos como están (histórico)
**Opción B:** Migrarlos a `labels` con estado CANCELADO
**Opción C:** Eliminarlos y regenerar

---

## 📊 Impacto en Reportes

### Reportes que Ahora Incluyen TODO

Los reportes que antes solo mostraban marbetes con existencias, ahora mostrarán TODOS:

- ✅ Distribución de marbetes
- ✅ Listado completo
- ✅ Marbetes pendientes
- ✅ Estadísticas

---

## ✅ Problema RESUELTO

**Ya NO hay "saltos" en los folios** porque:

1. ✅ Todos los marbetes se guardan en la misma tabla (`labels`)
2. ✅ Todos tienen estado GENERADO inicialmente
3. ✅ La secuencia es completamente visible
4. ✅ No hay folios "escondidos" en otra tabla

---

## 🎉 Resultado Final

```
Folio  Producto           Existencias  Estado      Visible
1      ProductoA          100          GENERADO    ✓
2      ProductoA          100          GENERADO    ✓
3      ProductoA          100          GENERADO    ✓
4      ProductoB          0            GENERADO    ✓ (Ahora visible!)
5      ProductoB          0            GENERADO    ✓ (Ahora visible!)
6      ProductoC          500          GENERADO    ✓
7      ProductoC          500          GENERADO    ✓
```

**Secuencia continua: 1, 2, 3, 4, 5, 6, 7... sin saltos** ✅

---

**Fecha de cambio:** 2025-12-16
**Compilación:** ✅ Exitosa
**Estado:** ✅ Implementado y Funcionando


---

## 🎯 El Problema Observado

```
Tu tabla muestra:
- GM17MEXB8: Folios 18-20 ✓ Impreso
- GM17WLMB8: Folios 31-33 ✓ Impreso

¿Dónde están los folios 21-30? 🤔
```

---

## ✅ Explicación: NO es un Error

El sistema funciona **correctamente**. Los folios 21-30 **SÍ existen**, pero están en la tabla `labels_cancelled` porque:

### 📊 Flujo Real del Sistema

```
1. Usuario genera marbetes para varios productos:
   - Producto A (existencias: 516) → Folios 18-20 → Estado: GENERADO
   - Producto B (existencias: 0)   → Folios 21-30 → Estado: CANCELADO (auto)
   - Producto C (existencias: 29,274) → Folios 31-33 → Estado: GENERADO

2. Sistema asigna folios CONTINUOS (18, 19, 20, 21, 22...33)

3. Pero guarda en diferentes tablas:
   - labels: Solo productos CON existencias (GENERADO)
   - labels_cancelled: Productos SIN existencias (CANCELADO)
```

---

## 🔍 Código Responsable

### En `LabelServiceImpl.generateBatch()`:

```java
if (existencias > 0) {
    // Producto CON existencias
    persistence.saveLabelsBatch(...);  // → Tabla 'labels'
    generadosConExistencias = toGenerate;
} else {
    // Producto SIN existencias
    persistence.saveLabelsBatchAsCancelled(...);  // → Tabla 'labels_cancelled'
    generadosSinExistencias = toGenerate;
}
```

### Asignación de Folios (SIEMPRE continua):

```java
public synchronized long[] allocateFolioRange(Long periodId, int quantity) {
    long primer = seq.getUltimoFolio() + 1;  // Siempre siguiente
    long ultimo = seq.getUltimoFolio() + quantity;
    seq.setUltimoFolio(ultimo);
    return new long[]{primer, ultimo};
}
```

---

## 📊 Donde Están los Folios "Perdidos"

### Query para Verificar:

```sql
-- Folios en estado GENERADO/IMPRESO (tabla labels)
SELECT folio, estado
FROM labels
WHERE period_id = 16 AND warehouse_id = 1
ORDER BY folio;

-- Folios CANCELADOS automáticamente (tabla labels_cancelled)
SELECT folio, motivo_cancelacion
FROM labels_cancelled
WHERE period_id = 16 AND warehouse_id = 1
ORDER BY folio;
```

**Resultado esperado:**
```
labels:
18, 19, 20 (GENERADO/IMPRESO)
31, 32, 33 (GENERADO/IMPRESO)

labels_cancelled:
21, 22, 23, 24, 25, 26, 27, 28, 29, 30
Motivo: "Sin existencias al momento de generación"
```

---

## 🎯 Solución para el Frontend

Tu tabla frontend solo muestra la tabla `label_requests`, que tiene esta estructura:

```
label_requests:
- id_label_request
- id_product
- requested_labels  ← Cantidad solicitada
- folios_generados  ← Cantidad generada
```

### Problema:
`requested_labels` puede ser 10, pero si el producto no tiene existencias:
- Los 10 folios se asignan (ej: 21-30)
- Se guardan en `labels_cancelled`
- **NO aparecen en la tabla `labels`**
- Frontend solo consulta `labels` → No los ve

---

## ✅ Soluciones

### Opción 1: Mostrar También los Cancelados

**Modificar el query del frontend para incluir ambas tablas:**

```sql
-- Query mejorado
SELECT
    COALESCE(l.folio, lc.folio) as folio,
    lr.id_product,
    COALESCE(l.estado, 'CANCELADO') as estado,
    lc.motivo_cancelacion,
    ...
FROM label_requests lr
LEFT JOIN labels l ON l.label_request_id = lr.id_label_request
LEFT JOIN labels_cancelled lc ON lc.label_request_id = lr.id_label_request
WHERE lr.period_id = ? AND lr.warehouse_id = ?
ORDER BY folio;
```

**Resultado en tabla:**
```
Folio  Producto      Estado        Rango
18     GM17MEXB8     IMPRESO       18-20
19     GM17MEXB8     IMPRESO       18-20
20     GM17MEXB8     IMPRESO       18-20
21     ProductoX     CANCELADO     21-30  ← Ahora visible
22     ProductoX     CANCELADO     21-30
...
31     GM17WLMB8     GENERADO      31-33
```

---

### Opción 2: Indicador Visual de Folios Cancelados

**En la fila del producto sin existencias, mostrar:**

```
Producto: ProductoX
Existencias: 0
Cant. Folios: 10
Rango Folios: 21-30 (CANCELADOS automáticamente)
Estado: ⚠️ Sin existencias
```

---

### Opción 3: API para Verificar Todos los Folios

Crear endpoint que devuelva TODOS los folios (cancelados y activos):

```
GET /api/sigmav2/labels/all-folios?periodId=16&warehouseId=1

Response:
[
  { folio: 18, estado: "IMPRESO", producto: "GM17MEXB8" },
  { folio: 19, estado: "IMPRESO", producto: "GM17MEXB8" },
  { folio: 20, estado: "IMPRESO", producto: "GM17MEXB8" },
  { folio: 21, estado: "CANCELADO", producto: "ProductoX", motivo: "Sin existencias" },
  ...
  { folio: 31, estado: "GENERADO", producto: "GM17WLMB8" },
]
```

---

## 🎨 Ejemplo de UI Mejorada

### Antes (Confuso):
```
GM17MEXB8  | 18-20  | Impreso
GM17WLMB8  | 31-33  | Impreso  ← ¿Dónde está 21-30?
```

### Después (Claro):
```
GM17MEXB8  | 18-20  | ✓ Impreso
ProductoX  | 21-30  | ⚠️ Cancelado (Sin existencias)
GM17WLMB8  | 31-33  | ✓ Impreso
```

---

## 📝 Modificación Recomendada

### En el Endpoint Actual

Modificar `/api/sigmav2/labels/for-count/list` para incluir marbetes cancelados:

```java
public List<LabelForCountDTO> getLabelsForCountList(...) {
    // Obtener activos
    List<Label> activeLabels = jpaLabelRepository
        .findByPeriodIdAndWarehouseId(periodId, warehouseId);

    // Obtener cancelados
    List<LabelCancelled> cancelledLabels = jpaLabelCancelledRepository
        .findByPeriodIdAndWarehouseId(periodId, warehouseId);

    // Combinar y ordenar por folio
    List<LabelForCountDTO> result = new ArrayList<>();

    // Agregar activos
    for (Label l : activeLabels) {
        result.add(convertToDTO(l));
    }

    // Agregar cancelados
    for (LabelCancelled lc : cancelledLabels) {
        result.add(convertToDTO(lc));
    }

    // Ordenar por folio
    result.sort(Comparator.comparing(LabelForCountDTO::getFolio));

    return result;
}
```

---

## ✅ Conclusión

**NO hay error en el sistema.** Los folios son continuos, pero:

1. ✅ **Folios 18-20:** Productos CON existencias → Tabla `labels`
2. ✅ **Folios 21-30:** Productos SIN existencias → Tabla `labels_cancelled`
3. ✅ **Folios 31-33:** Productos CON existencias → Tabla `labels`

**El frontend solo muestra la tabla `labels`**, por eso parece que "se saltan" números.

### Solución Inmediata:
Modificar el frontend para que también consulte y muestre los folios de `labels_cancelled`.

---

## 🔧 ¿Necesitas que Implemente la Solución?

Puedo implementar:

**A)** Modificar el endpoint para incluir cancelados
**B)** Crear nuevo endpoint que muestre todos los folios
**C)** Documentar cómo debe modificarse el frontend

¿Cuál prefieres?

---

**Fecha:** 2025-12-16
**Estado:** Explicación completa - Sistema funcionando correctamente

