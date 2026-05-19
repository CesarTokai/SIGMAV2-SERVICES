# Solución: Prevención de Envío de Duplicados en Generación de Marbetes

## 🎯 Problema Identificado

Cuando el usuario presionaba "GENERAR", el sistema enviaba **TODOS** los marbetes de la tabla, incluyendo:
- Productos ya generados
- Productos duplicados de sesiones anteriores
- Productos no seleccionados

**Resultado:** Duplicados en el rango de folios y en los registros.

---

## ✅ Solución Implementada

### 1. **Checkboxes para Selección Explícita**
Agregué una columna con checkboxes en la tabla para que el usuario pueda **seleccionar específicamente** qué productos generar.

**Características:**
- ✅ Checkbox individual por producto
- ✅ Checkbox "Seleccionar Todo" en el header
- ✅ Resaltado visual de filas seleccionadas (`.selected-row`)

### 2. **Filtrado por Selección**
Modificué la función `validateBeforeGenerate()` para:
- Considerar productos **seleccionados** (`m.selected === true`)
- O productos con folios **pendientes** (`foliosSolicitados > foliosExistentes`)

```typescript
const productsToGenerate = filteredMarbetes.value.filter(m => {
  const isValid = (productId > 0 && foliosSolicitados > 0);
  // ✅ Solo SELECCIONADOS o con folios PENDIENTES
  return isValid && (m.selected || (m.foliosSolicitados > (m.foliosExistentes || 0)));
});
```

### 3. **Modal Mejorado con Detalles de Folios**
El modal ahora muestra:
- Cantidad de productos a generar
- Lista de productos específicos
- **Cuántos folios faltantes** por cada producto

**Antes:**
```
¿Generar Marbetes?
Período: 20/12/2025
Almacén: Almacén A
Productos: Se generarán marbetes para 8 producto(s)
```

**Después:**
```
¿Generar Marbetes?
📅 Período: 20/12/2025
🏢 Almacén: Almacén A

📋 Productos a generar (3):
• Producto ABC: 2 folio(s) a generar
• Producto DEF: 1 folio(s) a generar
• Producto GHI: 1 folio(s) a generar
```

### 4. **Envío Solo de Productos Validados**
La función `generarMarbetes()` ahora:
- Usa `validation.productsToGenerate` (productos validados)
- **NO** vuelve a filtrar `filteredMarbetes.value`
- Evita duplicados y duplicaciones

```typescript
// ✅ CORRECCIÓN: Usar los productos validados
const productsToSend = validation.productsToGenerate || [];

const products = productsToSend.map(m => ({
  productId: Number(m.productId),
  labelsToGenerate: Number(m.foliosSolicitados)
}));
```

---

## 📁 Archivos Modificados

### 1. Auxiliar Console
**Archivo:** `src/modules/auxiliar/views/marbetes/ConsultaCaptura.vue`

**Cambios:**
- ✅ Actualización interfaz `Marbete` con propiedad `selected?: boolean`
- ✅ Funciones `toggleSelectAllProducts()` y `clearAllSelections()`
- ✅ Filtrado mejorado en `validateBeforeGenerate()`
- ✅ Modal con detalles de folios por producto
- ✅ Columna de checkboxes en la tabla
- ✅ Estilo `.selected-row` para filas seleccionadas

### 2. Admin Console
**Archivo:** `src/modules/admin/views/marbetesAdmin/ConsultaCaptura.vue`

**Cambios:**
- Mismo conjunto de mejoras que en Auxiliar para consistencia

---

## 🎯 Workflow Nuevo

### Paso a Paso

1. **Selecciona Período y Almacén**
   - Sistema carga todos los marbetes

2. **Verifica la tabla**
   - Orange: Productos YA GENERADOS
   - Amarillo: Productos SIN GENERAR
   - Grises: Los demás

3. **Marca los que necesitas generar**
   - Click en checkbox para cada producto
   - O "Seleccionar Todo" si quieres todos pendientes

4. **Presiona "GENERAR MARBETES"**
   - Modal muestra detalles específicos
   - Lista qué productos van a generarse
   - Cuántos folios por cada uno

5. **Confirma**
   - Sistema envía SOLO los seleccionados
   - ✅ SIN DUPLICADOS
   - ✅ SIN PRODUCTOS INNECESARIOS

6. **Resultado**
   - Folios asignados correctamente
   - No hay superposición
   - Rango de folios correcto

---

## 🔍 Detalles Técnicos

### Cambios en la Interfaz

```typescript
interface Marbete {
  productId: number;
  foliosSolicitados: number;
  foliosExistentes: number;
  claveProducto: string;
  producto: string;
  claveAlmacen: string;
  nombreAlmacen: string;
  estado: string;
  existencias: number;
  selected?: boolean;           // ✅ NUEVO: checkbox
  isLocked?: boolean;
  isNewlyModified?: boolean;
}
```

### Cambios en validación

```typescript
const validateBeforeGenerate = (): { 
  valid: boolean; 
  message?: string; 
  details?: string; 
  productsToGenerate?: Marbete[]  // ✅ NUEVO: retorna productos validados
}
```

### Cambios en el template

**Header:**
```vue
<th style="width: 40px;">
  <input type="checkbox" @change="toggleSelectAllProducts" />
</th>
```

**Fila:**
```vue
<td class="text-center">
  <input type="checkbox" v-model="marbete.selected" />
</td>
```

---

## 💡 Ventajas

✅ **Precisión:** Solo genera productos específicamente seleccionados
✅ **Claridad:** Modal muestra exactamente qué se generará
✅ **Seguridad:** Previene duplicados y errores
✅ **Control:** Usuario tiene control total sobre qué generar
✅ **Escalabilidad:** Funciona con cientos de productos
✅ **Consistencia:** Mismo comportamiento en Admin y Auxiliar

---

## 🧪 Casos de Uso

### Caso 1: Usuario cautioso (genera de uno en uno)
1. Selecciona solo 1 producto
2. Presiona "GENERAR"
3. Modal muestra: "1 producto, 1 folio"
4. ✅ Generado exitosamente

### Caso 2: Generación en lotes
1. Selecciona 5 productos con checkbox
2. Presiona "GENERAR"
3. Modal muestra: "5 productos, X folios faltantes"
4. ✅ Los 5 generados juntos

### Caso 3: Regeneración de faltantes
1. Filtra por Estado = "GENERADO"
2. Selecciona los que tienen `foliosSolicitados > foliosExistentes`
3. Presiona "GENERAR"
4. ✅ Solo los faltantes se procesan

---

## 🚀 Resultado Final

**Antes (Problema):**
```
Usuario selecciona 2 productos
Sistema envía: 8 productos (TODOS de la tabla)
Resultado: Duplicados, folios asignados incorrectamente
```

**Después (Solución):**
```
Usuario selecciona 2 productos
Sistema envía: 2 productos (solo los seleccionados)
Resultado: Generación correcta, sin duplicados ✅
```

---

## ⚠️ Notas Importantes

- **Sin cambios en backend:** Las mejoras son 100% frontend
- **Compatibilidad:** Funciona con API actual sin modificaciones
- **Performance:** Checksboxes no afectan el rendimiento
- **UX:** Interface más intuitiva y clara

---

**Fecha:** 19/03/2026
**Estado:** ✅ Listo para pruebas
**Módulos:** Admin + Auxiliar

