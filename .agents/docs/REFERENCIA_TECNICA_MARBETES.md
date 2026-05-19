# 🔧 REFERENCIA TÉCNICA RÁPIDA - Pantallas de Marbetes

## 📍 Ubicación de Archivos

```
src/modules/
├── almacenista/
│   └── views/marbetes/
│       ├── ConteoMarbetes.vue           (1020 líneas)
│       ├── ConsultaCaptura.vue          (2048 líneas)
│       ├── ImpresionMarbetes.vue        (1880 líneas)
│       ├── MarbetesLayout.vue           (166 líneas)
│       └── CancelacionMarbetes.vue      (No replicada)
│
├── auxiliar/
│   └── views/marbetes/
│       ├── ConteoMarbetes.vue           ✅ (1020 líneas)
│       ├── ConsultaCaptura.vue          ✅ (2048 líneas)
│       ├── ImpresionMarbetes.vue        ✅ (1880 líneas)
│       └── MarbetesLayout.vue           ✅ (166 líneas)
│
└── auxiliar_de_conteo/
    └── views/marbetes/
        ├── ConteoMarbetes.vue           ✅ (1020 líneas)
        ├── ConsultaCaptura.vue          ✅ (2048 líneas)
        ├── ImpresionMarbetes.vue        ✅ (1880 líneas)
        └── MarbetesLayout.vue           ✅ (166 líneas)
```

---

## 🔗 Rutas Configuradas

```typescript
// ALMACENISTA
/almacenista               → AlmacenistaDashboard
/almacenista/marbetes    → MarbetesLayout (children)

// AUXILIAR
/auxiliar                 → AuxiliarDashboard
/auxiliar/marbetes       → MarbetesLayout (children)

// AUXILIAR DE CONTEO
/auxiliar-de-conteo      → AuxiliarConteoDashboard
/auxiliar-de-conteo/marbetes → MarbetesLayout (children)
```

---

## 📦 Componentes Utilizados

### Imports Principales
```typescript
import { ref, onMounted, watch, computed } from 'vue'
import axiosConfiguration from '@/config/axiosConfig'
import { ToastError, ToastSuccess, LoadAlert } from '@/utils/SweetAlert'
import Swal from 'sweetalert2'
import { usePeriodoStore } from '@/store/periodoStore'
import SearchBar from '@/components/SearchBar.vue'
import TooltipHelp from '@/components/TooltipHelp.vue'
```

### Store Utilizado
```typescript
const periodoStore = usePeriodoStore()
periodoStore.cargarPeriodoGuardado()
periodoStore.setPeriodo(periodo)
```

---

## 🔌 API Endpoints Utilizados

### Períodos
```typescript
GET  /periods?page=0&size=100
```

### Almacenes
```typescript
GET  /warehouses
```

### Marbetes
```typescript
POST /labels/for-count                    // Buscar por folio
POST /labels/summary                      // Listar marbetes
POST /labels/generate/batch               // Generar marbetes
POST /labels/request                      // Solicitar folios
```

### Conteos
```typescript
POST /labels/counts/c1                    // Guardar 1er conteo
POST /labels/counts/c2                    // Guardar 2do conteo
PUT  /labels/counts/c2                    // Actualizar 2do conteo
```

### Impresión
```typescript
POST /labels/pending-print-count          // Contar pendientes
POST /labels/print                        // Generar PDF (blob)
```

### Cancelación
```typescript
GET  /labels/cancelled                    // Listar cancelados
POST /labels/cancel                       // Cancelar marbete
PUT  /labels/cancelled/update-stock       // Actualizar existencias
```

---

## 🎯 Interfaces/Tipos

```typescript
interface Periodo {
  id: number
  date: string
  comments: string
  state: string
}

interface Almacen {
  id: number
  clave: string
  nombre: string
  almacenname: string
  activo: boolean
}

interface MarbeteConteo {
  id: number
  folio: number
  claveProducto: string
  producto: string
  claveAlmacen: string
  almacen: string
  existenciasEsperadas: number
  conteo1: number | null
  conteo2: number | null
  diferencia: number | null
  estado: string
  cancelado: boolean
}

interface Marbete {
  productId: number
  foliosSolicitados: number
  foliosExistentes: number
  claveProducto: string
  producto: string
  claveAlmacen: string
  nombreAlmacen: string
  estado: string
  existencias: number
}

interface MarbeteGenerado {
  id: number
  productId: number
  folio: number
  claveProducto: string
  producto: string
  claveAlmacen: string
  almacen: string
  existencias: number
  impreso: boolean
  fechaImpresion: string | null
}
```

---

## 🌐 Métodos de Utilidad

### Formateo
```typescript
formatDate(date: string): string
  // "2026-02-09" → "9 de febrero de 2026"

formatNumber(value: number): string
  // 1500 → "1,500"

formatNumberWithCommas(value: number | string): string
  // "1500" → "1,500"

formatDateTime(date: string | Date | null): string
  // Fecha con hora en formato es-ES
```

### Validación
```typescript
handleConteoInput(event: Event, inputRef: 'conteo1' | 'conteo2')
  // Solo permite números enteros

preventNonNumeric(event: KeyboardEvent)
  // Valida entrada de teclado

handleFolioKeyPress(event: KeyboardEvent)
  // ENTER = buscar folio
```

### Atajos
```typescript
handleGlobalKeyPress(event: KeyboardEvent)
  Alt + F → Enfoca búsqueda
  Alt + L → Limpia formulario
  ESC    → Limpia y enfoca búsqueda
```

---

## 📊 Estados y Computed

### Refs Principales
```typescript
const periodos = ref<Periodo[]>([])
const almacenes = ref<Almacen[]>([])
const selectedPeriodo = ref<Periodo | null>(null)
const selectedAlmacen = ref<Almacen | null>(null)
const marbetes = ref<Marbete[]>([])
const filteredMarbetes = ref<Marbete[]>([])
const loading = ref(false)
const searchQuery = ref('')
```

### Computed
```typescript
activeSubmodule  // Pantalla activa basada en query param
activeComponent  // Componente a renderizar
diferenciaConteos // C2 - C1
validMarbetes    // Marbetes con folio > 0
```

### Watchers
```typescript
watch(searchQuery)        // Debounce 500ms para búsqueda
watch(selectedPeriodoId)  // Recargar marbetes
watch(selectedAlmacenId)  // Recargar marbetes
```

---

## 🎨 Clases CSS Principales

```css
.marbetes-layout          /* Container principal */
.header-section           /* Header con botones */
.submodule-btn            /* Botones de navegación */
.section-card             /* Tarjeta de sección */
.table-section            /* Tabla de marbetes */
.conteos-section          /* Sección de conteos */
.search-section           /* Buscador */
.pagination-section       /* Paginación */
.loading-container        /* Loader */
.modal-overlay            /* Modal background */
.badge                    /* Badges de estado */
.spinner                  /* Spinner animation */
```

---

## ⚡ Ciclo de Vida

### OnMounted
```typescript
1. Cargar períodos
2. Cargar almacenes
3. Seleccionar primero disponible
4. Enfocar input primario
5. Agregar listeners de teclado
```

### OnUnmounted
```typescript
1. Remover listeners de teclado
2. Limpiar timers
3. Cancelar requests pendientes
```

---

## 🔐 Guard del Router

```typescript
meta: {
  role: "ALMACENISTA"  // Validado en beforeEach
}

// beforeEach Validaciones:
1. Verificar token JWT
2. Validar no esté expirado
3. Verificar rol requerido
4. Si no coincide → redirigir a login
```

---

## 💾 LocalStorage

```typescript
// Guardado automáticamente por periodoStore:
localStorage.setItem('periodoSeleccionado', JSON.stringify(periodo))
localStorage.setItem('token', token)
localStorage.setItem('role', role)
localStorage.setItem('username', username)

// Lectura automática en onMounted:
periodoStore.cargarPeriodoGuardado()
```

---

## 📈 Paginación

```typescript
page: ref(0)           // Página actual (0-based)
pageSize: ref(100)     // Registros por página
totalPages: ref(1)     // Total de páginas
totalElements: ref(0)  // Total de registros

// Métodos:
goToPage(newPage: number)
changePageSize(event: Event)

// Body POST:
{
  page: page.value,
  size: pageSize.value,
  searchText: debouncedSearch.value,
  sortBy: sortBy.value,
  sortDirection: sortDirection.value
}
```

---

## 🎯 Búsqueda y Ordenamiento

```typescript
sortBy: ref<string>('claveProducto')
sortDirection: ref<'ASC' | 'DESC'>('ASC')
debouncedSearch: ref<string>('')
searchDebounceTimeout: number | null

// Al buscar:
1. Esperar 500ms sin escribir
2. Actualizar debouncedSearch
3. Reset a página 0
4. Llamar loadMarbetes()

// Al hacer click en header:
1. Si es mismo campo → cambiar dirección
2. Si es diferente → ASC automáticamente
3. Reset a página 0
4. Llamar loadMarbetes()
```

---

## 🚀 Optimizaciones

### Debounce
- Búsqueda: 500ms
- Evita múltiples requests

### Validaciones
- Cliente (antes de enviar)
- Servidor (segundo nivel de seguridad)

### Caché
- Períodos guardados en store
- Almacenes cargados una sola vez

### Lazy Loading
- Componentes cargados en demanda
- MarbetesLayout importa dinámicamente

---

## 🧪 Testing Sugerido

### Unitarias
```typescript
// Formatters
formatDate('2026-02-09')
formatNumber(1500)
handleConteoInput(event, 'conteo1')

// Validaciones
preventNonNumeric(event)
validateBeforeGenerate()
```

### Integración
```typescript
// Flujo de búsqueda
1. Ingresar folio
2. Presionar ENTER/Buscar
3. Verificar marbete cargado
4. Verificar inputs enfocados

// Flujo de conteo
1. Búsqueda exitosa
2. Ingresar C1
3. Ingresar C2
4. Verificar diferencia
5. Guardar
6. Verificar guardado
```

### E2E
```typescript
// Flujo completo AUXILIAR
1. Login como AUXILIAR
2. Navegar a /auxiliar/marbetes
3. Completar 3 pantallas
4. Generar PDF
5. Descargar

// Flujo completo AUXILIAR DE CONTEO
1. Login como AUXILIAR DE CONTEO
2. Navegar a /auxiliar-de-conteo/marbetes
3. Completar 3 pantallas
4. Generar PDF
5. Descargar
```

---

## 📝 Notas de Mantenimiento

### Si Cambia API
1. Actualizar endpoints en las 3 ubicaciones
2. Actualizar interfaces si cambian campos
3. Actualizar mapeos de respuesta

### Si Cambia Store
1. Actualizar import en todos los 3 módulos
2. Verificar persistencia de período

### Si Cambia Estilo
1. Editar en MarbetesLayout.vue (cualquier módulo como referencia)
2. No hace falta replicar (CSS compartido)

### Si Agregar Nueva Funcionalidad
1. Implementar en almacenista primero
2. Copiar a auxiliar y auxiliar_de_conteo
3. Actualizar router si es necesario

---

## 🎓 Flujo de Datos

```
Usuario Login
     ↓
Router Valida Rol
     ↓
Dashboard del Rol
     ↓
Link a /[rol]/marbetes
     ↓
MarbetesLayout (elige pantalla)
     ↓
Conteo/Consulta/Impresión
     ↓
API REST
     ↓
Base de Datos
```

---

## ✅ Checklist de Configuración

- [x] Archivos creados en /auxiliar/views/marbetes/
- [x] Archivos creados en /auxiliar_de_conteo/views/marbetes/
- [x] Imports agregados en router/index.ts
- [x] Rutas configuradas con children
- [x] Meta roles asignados correctamente
- [x] Guard del router valida roles
- [x] Store periodoStore disponible
- [x] Componentes importados correctamente
- [x] CSS scope no genera conflictos
- [x] APIs endpoints accesibles

---

## 📞 Contacto Técnico

Para cambios o mantenimiento:
1. Revisar esta documentación
2. Consultar código en almacenista (referencia original)
3. Aplicar cambios en los 3 módulos
4. Testar en cada rol
5. Verificar en router si es necesario

---

*Última Actualización: 2026-02-09*  
*Estado: ✅ Listo para Producción*

