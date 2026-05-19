📋 MEJORA: Validación Requerida de warehouseId en buscarMarbetePorFolio

==============================================================================
🎯 PROBLEMA IDENTIFICADO
==============================================================================

El warehouseId era **OPCIONAL** en la búsqueda de marbetes:

ANTES:
```typescript
const body: any = { folio: parseInt(raw) };

// Agregar período y almacén SOLO si están seleccionados
if (selectedPeriodo.value) body.periodId = selectedPeriodo.value.id;
if (selectedAlmacen.value) body.warehouseId = selectedAlmacen.value.id;
```

CONFUSIÓN GENERADA:
- Usuario selecciona almacén en la UI
- Pero no sabe si se está usando en la búsqueda
- El backend puede devolver datos de **otro almacén** si el folio existe allí
- No hay validación clara


==============================================================================
✅ SOLUCIÓN: Requerir warehouseId
==============================================================================

AHORA (Validación clara):
```typescript
// ✅ VALIDAR que período y almacén sean seleccionados REQUERIDOS
if (!selectedPeriodo.value) {
  ToastError('Falta período', 'Selecciona un período antes de buscar');
  return;
}

if (!selectedAlmacen.value) {
  ToastError('Falta almacén', 'Selecciona un almacén antes de buscar');
  return;
}

// ✅ REQUERIR warehouseId - No es opcional
const body: any = {
  folio: parseInt(raw),
  periodId: selectedPeriodo.value.id,
  warehouseId: selectedAlmacen.value.id  // ✅ SIEMPRE incluido
};
```

BENEFICIOS:
- ✅ Usuario ve errores claros si falta algo
- ✅ warehouseId SIEMPRE se envía (no hay ambigüedad)
- ✅ Búsqueda filtrada por período + almacén específico
- ✅ UX mejorado y predecible


==============================================================================
📊 CASO DE USO MEJORADO
==============================================================================

SCENARIO 1: Usuario abre la página
- Período: NO SELECCIONADO
- Almacén: NO SELECCIONADO
- Intenta buscar folio 35
→ ❌ "Falta período" (claro y específico)

SCENARIO 2: Usuario selecciona período pero no almacén
- Período: Seleccionado ✅ (Periodo 7)
- Almacén: NO SELECCIONADO
- Intenta buscar folio 35
→ ❌ "Falta almacén" (claro y específico)

SCENARIO 3: Usuario selecciona período Y almacén
- Período: Seleccionado ✅ (Periodo 7)
- Almacén: Seleccionado ✅ (Almacen 1)
- Intenta buscar folio 35
→ ✅ API recibe:
   {
     folio: 35,
     periodId: 7,
     warehouseId: 8  // ← REQUERIDO, NO OPCIONAL
   }
→ Retorna marbete del Almacen 1, Periodo 7 (específico)


==============================================================================
🔧 ARCHIVOS MODIFICADOS
==============================================================================

1. src/modules/auxiliar_de_conteo/views/marbetes/ConteoMarbetes.vue
   - Línea ~161: Requerida validación de periodo
   - Línea ~167: Requerida validación de almacén
   - Línea ~176-186: warehouseId REQUERIDO en body

2. src/modules/almacenista/views/marbetes/ConteoMarbetes.vue
   - Línea ~161: Requerida validación de periodo
   - Línea ~167: Requerida validación de almacén
   - Línea ~176-186: warehouseId REQUERIDO en body

3. src/modules/auxiliar/views/marbetes/ConteoMarbetes.vue
   - Línea ~161: Requerida validación de periodo
   - Línea ~167: Requerida validación de almacén
   - Línea ~176-186: warehouseId REQUERIDO en body

4. src/modules/admin/views/marbetesAdmin/ConteoMarbetes.vue
   - Línea ~175: Mejorada validación de periodo
   - Línea ~181: Mejorada validación de almacén
   - Línea ~191-200: warehouseId REQUERIDO en body


==============================================================================
🎓 RAZONES TÉCNICAS
==============================================================================

1. **ESPECIFICIDAD**: Un folio en un período específico puede existir en múltiples
   almacenes. Necesitamos el warehouseId para saber CUÁL exactamente devolver.

2. **PREDICTIBILIDAD**: El usuario selecciona un almacén en la UI. Es lógico
   que ese almacén se use en la búsqueda.

3. **ERROR HANDLING**: Si falta warehouseId, es mejor fallar EN EL FRONTEND
   (validación clara) que en el backend (error genérico).

4. **BUSINESS LOGIC**: En un sistema de inventario por almacén, la búsqueda
   DEBE estar filtrada por almacén específico.


==============================================================================
✨ MEJORA FUTURA POSIBLE
==============================================================================

Si el backend permitiera "buscar folic sin especificar almacén", se podría
agregar lógica para auto-seleccionar el almacén si hay un único resultado:

```typescript
// SOLO si backend devuelve warehouseId automáticamente
if (response.data.warehouseId && response.data.warehouseId !== selectedAlmacen.value?.id) {
  const almacenDelFolio = almacenes.value.find(a => a.id === response.data.warehouseId);
  if (almacenDelFolio) {
    // Auto-actualizar selección
    selectedAlmacen.value = almacenDelFolio;
    selectedAlmacenId.value = almacenDelFolio.id;
  }
}
```

Pero por ahora: **warehouseId es REQUERIDO** para mayor claridad.


==============================================================================
🧪 TESTING RECOMENDADO
==============================================================================

1. Abre auxiliar_de_conteo
2. NO selecciones ningún almacén
3. Intenta buscar un folio
   → Debes ver: "Falta almacén"

4. Selecciona un almacén
5. NO selecciones periodo
6. Intenta buscar un folio
   → Debes ver: "Falta período"

7. Selecciona período Y almacén
8. Busca un folio que EXISTS
   → Debes ver datos del folio

9. Busca un folio que NO EXISTS
   → Debes ver: "Marbete con folio XXX no encontrado" (del backend)

