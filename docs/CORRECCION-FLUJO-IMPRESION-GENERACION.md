# Corrección: Flujo de Impresión y Generación de Marbetes

**Fecha:** 2026-03-17

---

## 🔴 PROBLEMA 1 — Error al imprimir "Folios Faltantes"

### Causa raíz

La secuencia de folios es **global por período**, compartida entre todos los almacenes. Si un almacén genera marbetes en dos momentos distintos, sus folios quedan en bloques no consecutivos porque otros almacenes generaron en medio.

```
Tiempo 1: Almacén 280 genera 38  → folios 390-427  [GENERADO]
Tiempo 2: Almacén   5 genera 104 → folios 428-531  [IMPRESO]
Tiempo 3: Almacén 280 genera 38  → folios 532-569  [GENERADO]

Almacén 280 tiene: [390-427] + [532-569] = 76 marbetes (no consecutivos)
```

La validación anterior asumía que **todos los folios del rango** debían pertenecer al mismo almacén:

```java
// INCORRECTO — asumía rango consecutivo por almacén
long count = endFolio - startFolio + 1; // 569 - 390 + 1 = 180
if (filteredLabels.size() != count)     // 76 != 180 → ERROR
    throw "Folios faltantes: 428,429...531"
```

Los folios 428-531 **SÍ existen** — pertenecen al almacén 5, no "faltan".

### Solución aplicada

**Archivo:** `LabelsPersistenceAdapter.java` → método `printLabelsRange()`

Se eliminó la validación incorrecta de rango consecutivo. Ahora el método:

1. Busca los marbetes del rango **filtrados por** `warehouseId + periodoId + estadoEsperado`
2. Si no encuentra ninguno → error
3. Si alguno está cancelado → error
4. Si todo es válido → imprime y registra auditoría con cantidad real

Además se agregó el parámetro `boolean isReprint` para distinguir entre:

| `isReprint` | Flujo | Estado esperado |
|-------------|-------|-----------------|
| `false` | Impresión normal | Solo `GENERADO` → `IMPRESO` |
| `true` | Reimpresión extraordinaria | Solo `IMPRESO` → `IMPRESO` |

Esto evita que una impresión normal re-actualice marbetes ya impresos que caigan dentro del mismo rango de folios.

### Reglas de negocio que cumple

| Regla | Estado |
|-------|--------|
| Folios únicos por período | ✅ Secuencia global no modificada |
| Impresión normal solo actualiza `GENERADO` | ✅ `isReprint=false` filtra por `GENERADO` |
| Reimpresión solo procesa `IMPRESO`s | ✅ `isReprint=true` filtra por `IMPRESO` |
| No imprimir `CANCELADO`s | ✅ `validateLabelsForPrinting()` los rechaza |
| No mezclar almacenes | ✅ Filtra por `warehouseId` |
| Auditoría exacta | ✅ `cantidadImpresa = filteredLabels.size()` real |

---

## 🔴 PROBLEMA 2 — Re-generación después del flujo completo

### Causa raíz

Después de completar el flujo completo (generar → imprimir → contar), el sistema **permitía volver al paso de generación** y crear más marbetes para el mismo período y almacén. Esto comprometía la integridad de los conteos en curso.

```
1️⃣ generateBatchList(periodo=7, almacén=280) → 76 marbetes [GENERADO]
2️⃣ print()                                   → 76 marbetes [IMPRESO]
3️⃣ Conteos C1, C2 registrados

Usuario regresa al paso 1:
4️⃣ generateBatchList(periodo=7, almacén=280) → ✅ Sistema lo permitía ← ERROR
   └─ Genera 10 marbetes más [GENERADO]
   └─ Rompe integridad del conteo en curso
```

### Solución aplicada

**Archivo:** `LabelGenerationService.java` → método `generateBatchList()`

Se agregó una validación al inicio del método que verifica si ya existen marbetes en estado `IMPRESO` para ese período y almacén antes de generar:

```java
if (persistence.existsImpresosForPeriodAndWarehouse(periodId, warehouseId)) {
    throw new InvalidLabelStateException(
        "No se pueden generar más marbetes. Ya existen marbetes impresos...");
}
```

**Archivo:** `LabelsPersistenceAdapter.java` — método auxiliar agregado:

```java
public boolean existsImpresosForPeriodAndWarehouse(Long periodId, Long warehouseId) {
    return !jpaLabelRepository
        .findByPeriodIdAndWarehouseIdAndEstado(periodId, warehouseId, Label.State.IMPRESO)
        .isEmpty();
}
```

### Comportamiento por escenario

| Escenario | Resultado |
|-----------|-----------|
| Mismo período, mismo almacén, ya tiene `IMPRESO`s | ❌ Bloqueado |
| Mismo período, diferente almacén | ✅ Permitido |
| Mismo almacén, diferente período | ✅ Permitido |
| Mismo período, mismo almacén, solo `GENERADO`s (sin imprimir) | ✅ Permitido |

### Cambio en frontend requerido

Ninguno obligatorio. El backend retorna `400 BAD REQUEST` con mensaje claro. Opcionalmente el frontend puede deshabilitar el botón "Generar" consultando si ya existen marbetes impresos para el período/almacén seleccionado.

---

## 📁 Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `LabelsPersistenceAdapter.java` | Eliminada validación incorrecta de rango consecutivo, agregado parámetro `isReprint`, agregado método `existsImpresosForPeriodAndWarehouse()` |
| `LabelGenerationService.java` | Validación al inicio de `generateBatchList()` para bloquear si ya hay `IMPRESO`s |
| `LabelServiceImpl.java` | Llamadas a `printLabelsRange()` actualizadas con `isReprint=false`/`true` |

---

## ⚠️ NOTA IMPORTANTE SOBRE INTEGRIDAD

La validación bloquea **todo tipo de generación nueva** una vez que hay marbetes `IMPRESO`s. Esto es **intencional** para garantizar que:

1. Los conteos en curso no se mezclen con nuevos lotes
2. La auditoría sea clara (una generación = un lote cohesivo)
3. No haya ambigüedad en qué marbetes pertenecen a qué lote

Si necesita agregar marbetes al mismo período/almacén, debe:
- Crear un **NUEVO período**, O
- Usar un **ALMACÉN diferente**

Esto respeta la política: **"Un ciclo completo por período/almacén"**
