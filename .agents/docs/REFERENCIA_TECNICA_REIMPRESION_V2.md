# 🔧 Referencia Técnica: ReimpresionMarbetes.vue v2.0

## 📁 Ubicación del Archivo

```
SIGMAV2-APP/
└── src/
    └── modules/
        └── admin/
            └── views/
                └── marbetesAdmin/
                    └── ReimpresionMarbetes.vue  ← AQUÍ
```

---

## 🏗️ Estructura del Componente

### **Imports Principales**
```typescript
import {ref, onMounted, nextTick, computed, onUnmounted, watch} from 'vue';
import axiosConfiguration from '@/config/axiosConfig';
import {ToastError, ToastSuccess, LoadAlert} from '@/utils/SweetAlert';
import Swal from 'sweetalert2';
import { usePeriodoStore } from '@/store/periodoStore';
```

### **Interfaces**

#### `Periodo`
```typescript
interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}
```

#### `Almacen`
```typescript
interface Almacen {
  id: number;
  clave: string;
  nombre: string;
  activo: boolean;
}
```

#### `MarbeteReimpresion` (UNIFICADA con ConteoMarbetes)
```typescript
interface MarbeteReimpresion {
  id: number;
  folio: number;
  claveProducto: string;
  producto: string;
  claveAlmacen: string;
  almacen: string;
  existenciasEsperadas: number;
  conteo1: number | null;              // ← Sincronizado con ConteoMarbetes
  conteo2: number | null;              // ← Sincronizado con ConteoMarbetes
  diferencia: number | null;           // ← Sincronizado con ConteoMarbetes
  estado: string;                      // GENERADO, IMPRESO, CONTADO, CANCELADO
  cancelado: boolean;                  // ← Validación de seguridad
}
```

---

## 🎯 Estado Reactivo

### **Refs Principales**
```typescript
// Datos base
const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacen = ref<Almacen | null>(null);
const selectedAlmacenId = ref<number | null>(null);

// Búsqueda y marbete
const folioInput = ref<string>('');
const marbeteActual = ref<MarbeteReimpresion | null>(null);
const resultadosBusqueda = ref<MarbeteReimpresion[]>([]);

// Referencias DOM
const folioInputRef = ref<HTMLInputElement | null>(null);

// Estados de carga
const loadingStates = ref({
  searching: false,
  reprinting: false,
  consultingPending: false
});
```

### **Computed Properties**

#### `puedeReimprimir`
```typescript
const puedeReimprimir = computed<boolean>(() => {
  return (
    !!marbeteActual.value && 
    marbeteActual.value.estado === 'IMPRESO' && 
    !marbeteActual.value.cancelado
  );
});
```

---

## 🔄 Funciones Principales

### **1. `loadPeriodos()`**
**Propósito:** Cargar períodos del backend y sincronizar con store

**Flujo:**
```
API GET /periods
  ↓
Mapear response a array Periodo[]
  ↓
Cargar periodo del store (si existe)
  ↓
Si no existe → usar primer período
  ↓
Actualizar selectedPeriodo y selectedPeriodoId
```

**Manejo de errores:**
```typescript
try {
  const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
  // ...
} catch (error) {
  ToastError('Error', 'No se pudieron cargar los períodos');
}
```

---

### **2. `loadAlmacenes()`**
**Propósito:** Cargar almacenes del backend

**Flujo:**
```
API GET /warehouses
  ↓
Mapear response a array Almacen[]
  ↓
Auto-seleccionar primer almacén
  ↓
Actualizar selectedAlmacen y selectedAlmacenId
```

---

### **3. `buscarMarbetePorFolio()` ⭐ PRINCIPAL**

**Endpoint:** `POST /labels/for-count` (compartido con ConteoMarbetes)

**Request Body:**
```typescript
{
  "folio": number,        // Integer
  "periodId": number,     // Integer
  "warehouseId": number   // Integer
}
```

**Response (Marbete encontrado):**
```typescript
{
  "id": 123,
  "folio": 195,
  "claveProducto": "PROD-001",
  "descripcionProducto": "Producto ABC",
  "claveAlmacen": "ALM-01",
  "nombreAlmacen": "Almacén Central",
  "existenciasEsperadas": 1250,
  "conteo1": 1245,
  "conteo2": 1250,
  "diferencia": 5,
  "estado": "IMPRESO",
  "cancelado": false
}
```

**Flujo:**
```
1. Validar folio ingresado
2. Validar período y almacén seleccionados
3. POST /labels/for-count
4. Validar estado = IMPRESO
5. Validar no cancelado
6. Mapear a MarbeteReimpresion
7. Mostrar información
```

**Validaciones:**
```typescript
if (!raw) → ToastError("Ingresa un folio")
if (!selectedPeriodo || !selectedAlmacen) → ToastError("Selecciona período y almacén")
if (response.data.estado !== 'IMPRESO') → ToastError("Estado inválido")
if (response.data.cancelado) → ToastError("Marbete Cancelado")
```

**Manejo de errores:**
```typescript
try {
  // Búsqueda
} catch (error: any) {
  if (error.response?.status === 404) {
    ToastError('No encontrado', `Folio no existe`);
  } else {
    ToastError('Error', error?.response?.data?.message);
  }
} finally {
  loadingStates.value.searching = false;
}
```

---

### **4. `reimprimirMarbete()` ⭐ ACCIÓN PRINCIPAL**

**Endpoint:** `POST /labels/print`

**Request Body:**
```typescript
{
  "periodId": number,
  "warehouseId": number,
  "folios": [number],     // Array con un folio
  "forceReprint": true    // ← CLAVE PARA REIMPRESIÓN EXTRAORDINARIA
}
```

**Response (PDF Blob):**
```
Binary PDF file
Content-Type: application/pdf
```

**Flujo:**
```
1. Validar marbeteActual existe
2. Validar período y almacén
3. Mostrar modal de confirmación
4. Si confirmado:
   a. POST /labels/print
   b. Crear Blob del PDF
   c. Generar nombre: reimpresion_folio_{folio}_{timestamp}.pdf
   d. Trigger descarga
   e. Limpiar formulario
   f. Mostrar éxito
```

**Generación del nombre:**
```typescript
const timestamp = new Date().toISOString()
  .replace(/[:.]/g, '-')      // Reemplazar : . con -
  .slice(0, -5);              // Remover últimos 5 caracteres

const filename = `reimpresion_folio_${marbeteActual.value.folio}_${timestamp}.pdf`;
// Ejemplo: reimpresion_folio_195_2026-02-23T15-30-45.pdf
```

**Manejo de errores:**
```typescript
try {
  // Reimpresión
} catch (error: any) {
  ToastError('Error en reimpresión', error?.response?.data?.message);
} finally {
  loadingStates.value.reprinting = false;
}
```

---

### **5. `limpiarFormulario()`**

**Acción:**
```typescript
folioInput.value = '';
marbeteActual.value = null;
resultadosBusqueda.value = [];
```

---

## 📡 Watchers

### **Watch: selectedPeriodoId**
```typescript
watch(selectedPeriodoId, (newId) => {
  if (newId !== null) {
    selectedPeriodo.value = periodos.value.find(p => p.id === newId) || null;
    if (selectedPeriodo.value) {
      periodoStore.setPeriodo(selectedPeriodo.value);  // Guardar en store
      console.log('Periodo seleccionado:', selectedPeriodo.value);
    }
  }
});
```

### **Watch: selectedAlmacenId**
```typescript
watch(selectedAlmacenId, (newId) => {
  if (newId !== null) {
    selectedAlmacen.value = almacenes.value.find(a => a.id === newId) || null;
    console.log('Almacén seleccionado:', selectedAlmacen.value);
  }
});
```

---

## ⌨️ Event Handlers

### **`handleFolioKeyPress(event: KeyboardEvent)`**
```typescript
if (event.key === 'Enter') {
  event.preventDefault();
  buscarMarbetePorFolio();  // Buscar al presionar Enter
}
```

### **`handleGlobalKeyPress(event: KeyboardEvent)`**
```typescript
// Alt + F: Focus en folio
if (event.altKey && event.key === 'f') {
  event.preventDefault();
  limpiarFormulario();
  folioInputRef.value?.focus();
}

// Alt + L: Limpiar
if (event.altKey && event.key === 'l') {
  event.preventDefault();
  limpiarFormulario();
}

// Escape: Limpiar
if (event.key === 'Escape') {
  limpiarFormulario();
  folioInputRef.value?.focus();
}
```

---

## 🎨 Template Structure

```vue
<template>
  <div class="reimpresion-marbetes">
    <!-- 1. Título y Selector de Período -->
    <div class="section-card">
      <div class="title-section">
        <h1>📄 Reimpresión Extraordinaria de Marbetes</h1>
      </div>
      <div class="periodo-selector">
        <select v-model.number="selectedPeriodoId">
          <!-- Opciones de período -->
        </select>
      </div>
    </div>

    <!-- 2. Información del Marbete -->
    <div class="marbete-info">
      <!-- Folio, Producto, Clave, Almacén, Estado, Existencias -->
    </div>

    <!-- 3. Búsqueda de Folio -->
    <div class="search-section">
      <input
        v-model="folioInput"
        @keydown="handleFolioKeyPress"
        placeholder="Ingresa folio"
      />
      <button @click="buscarMarbetePorFolio">Buscar</button>
    </div>

    <!-- 4. Botones de Acción -->
    <div class="actions">
      <button @click="reimprimirMarbete" :disabled="!puedeReimprimir">
        📄 Reimprimir Marbete
      </button>
      <button @click="limpiarFormulario">Limpiar</button>
    </div>

    <!-- 5. Info Box -->
    <div class="info-box">
      <!-- Instrucciones de uso -->
    </div>
  </div>
</template>
```

---

## 🔌 Lifecycle Hooks

### **`onMounted()`**
```typescript
onMounted(() => {
  const init = async () => {
    await loadPeriodos();
    await loadAlmacenes();
    nextTick(() => {
      folioInputRef.value?.focus();  // Enfocar input
    });
  };

  init();
  
  // Agregar listener de teclado global
  window.addEventListener('keydown', handleGlobalKeyPress);
});
```

### **`onUnmounted()`**
```typescript
onUnmounted(() => {
  // Limpiar listener
  window.removeEventListener('keydown', handleGlobalKeyPress);
});
```

---

## 📊 Comparativa: ConteoMarbetes vs ReimpresionMarbetes

| Propiedad | ConteoMarbetes | ReimpresionMarbetes |
|-----------|-----------------|---------------------|
| `endpoint búsqueda` | `/labels/for-count` | `/labels/for-count` ✅ |
| `interfaz datos` | `MarbeteConteo` | `MarbeteReimpresion` |
| `watchers` | watch(selectedPeriodoId) | watch(selectedPeriodoId) ✅ |
| `validación estado` | Cualquiera | Solo IMPRESO ✅ |
| `acción principal` | POST `/labels/counts/c1` | POST `/labels/print` |
| `confirmación` | No | Sí (Swal.fire) ✅ |
| `descarga PDF` | No | Sí ✅ |
| `patrón búsqueda` | Idéntico | Idéntico ✅ |

---

## 🧪 Testing

### **Test 1: Búsqueda Exitosa**
```typescript
// Given
selectedPeriodoId.value = 7;
selectedAlmacenId.value = 218;
folioInput.value = "195";

// When
await buscarMarbetePorFolio();

// Then
expect(marbeteActual.value?.folio).toBe(195);
expect(marbeteActual.value?.estado).toBe('IMPRESO');
expect(puedeReimprimir.value).toBe(true);
```

### **Test 2: Validación de Estado**
```typescript
// Given
API retorna folio GENERADO

// When
await buscarMarbetePorFolio();

// Then
expect(marbeteActual.value).toBeNull();
expect(ToastError).toHaveBeenCalled();
```

### **Test 3: Reimpresión Exitosa**
```typescript
// Given
marbeteActual.value = { folio: 195, estado: 'IMPRESO', cancelado: false };

// When
await reimprimirMarbete();
// Usuario confirma en modal

// Then
expect(PDF blob).toBeDefined();
expect(filename).toContain('reimpresion_folio_195');
expect(folioInput.value).toBe('');
```

---

## 🐛 Debugging Tips

### **1. Verificar Estado del Marbete**
```typescript
console.log('Marbete actual:', marbeteActual.value);
console.log('¿Puede reimprimir?:', puedeReimprimir.value);
```

### **2. Verificar Período/Almacén**
```typescript
console.log('Período seleccionado:', selectedPeriodo.value);
console.log('Almacén seleccionado:', selectedAlmacen.value);
```

### **3. Monitorear Requests**
```typescript
// Abrir DevTools → Network
// Buscar request a /labels/for-count
// Revisar payload y response
```

### **4. Ver Errores de API**
```typescript
// En el catch de buscarMarbetePorFolio
console.error('Error API:', error.response?.data);
```

---

## 📝 Notas Importantes

1. **Patrón Compartido**: Usa el mismo endpoint que ConteoMarbetes (`/labels/for-count`)
2. **Validación de Seguridad**: Frontend valida estado IMPRESO antes de permitir reimpresión
3. **Store Persistence**: El período se guarda en periodoStore automáticamente
4. **Sin cambios Backend**: El endpoint `/labels/print` ya soporta `forceReprint=true`
5. **Interfaz Unificada**: MarbeteReimpresion tiene los mismos campos que MarbeteConteo

---

## 🔗 Archivos Relacionados

- `ConteoMarbetes.vue` - Componente similar (patrón base)
- `ImpresionMarbetes.vue` - Impresión inicial
- `periodoStore.ts` - Store para sincronizar período
- `/labels/for-count` - Endpoint compartido
- `/labels/print` - Endpoint de impresión/reimpresión

---

**Versión:** 2.0  
**Actualizado:** 2026-02-23  
**Compatible con:** Vue 3.x, TypeScript 5.x

