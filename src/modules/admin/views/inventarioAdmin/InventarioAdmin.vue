<script setup lang="ts">
import {ref, onMounted, watch, computed} from 'vue';
import { usePeriodoStore } from '@/store/periodoStore';
import SearchBar from '@/components/SearchBar.vue';
import PeriodoSelector from '@/components/PeriodoSelector.vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, LoadAlert } from '@/utils/SweetAlert';
import Swal from 'sweetalert2';

interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface Producto {
  id?: number;
  cveArt: string;
  descr: string;
  uniMed: string;
  existQty: number;
  status: string;
}

interface Warehouse {
  id: number;
  warehouseKey: string;
  nameWarehouse: string;
  observations: string;
  assignedUsersCount: number;
}

interface PaginationInfo {
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
}

const periodoStore = usePeriodoStore();


const visiblePages = computed(() => {
  const total = pagination.value.totalPages;
  const current = pagination.value.currentPage;
  const delta = 2;

  const pages: number[] = [];

  const start = Math.max(0, current - delta);
  const end = Math.min(total - 1, current + delta);

  // Siempre incluir primera
  if (start > 0) pages.push(0);

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  // Siempre incluir última
  if (end < total - 1) pages.push(total - 1);

  return [...new Set(pages)];
});

const getWarehouseKeyNumber = (key: string): number => {
  const match = key.match(/\d+/g);
  if (!match) return Number.MAX_SAFE_INTEGER;
  const value = Number(match[match.length - 1]);
  return Number.isFinite(value) ? value : Number.MAX_SAFE_INTEGER;
};

// Estado
const periodos = ref<Periodo[]>([]);
const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);
const productos = ref<Producto[]>([]);
const searchQuery = ref('');
const loading = ref(false);
const showImportModal = ref(false);
const importFile = ref<File | null>(null);
const warehouses = ref<Warehouse[]>([]);
const selectedWarehouse = ref<number | null>(null);
const importing = ref(false);
const showImportResultModal = ref(false);
const importResult = ref({
  totalRows: 0,
  inserted: 0,
  updated: 0,
  deactivated: 0,
  errors: [],
  logFileUrl: ''
});

// Estado para modal de consulta de stock
const showStockModal = ref(false);
const selectedProduct = ref<Producto | null>(null);
const selectedWarehouseForStock = ref<number | null>(null);
const stockInfo = ref<any>(null);
const loadingStock = ref(false);

const allProductos = ref<Producto[]>([]);

// Paginación: por defecto mostrar todos los productos
const pagination = ref<PaginationInfo>({
  currentPage: 0,
  totalPages: 0,
  totalElements: 0,
  pageSize: 50
});

// Computed para los contadores de productos
const startIndex = computed(() => {
  if (pagination.value.totalElements === 0) return 0;
  return (pagination.value.currentPage * pagination.value.pageSize) + 1;
});
const endIndex = computed(() => {
  if (pagination.value.totalElements === 0) return 0;
  return (pagination.value.currentPage * pagination.value.pageSize) + productos.value.length;
});

// Cargar períodos disponibles
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=50');
    periodos.value = response.data.content || [];

    // Cargar periodo guardado del store
    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      selectedPeriodo.value = periodoStore.periodoSeleccionado;
      selectedPeriodoId.value = periodoStore.periodoSeleccionado.id;
      await loadInventario();
    } else if (periodos.value.length > 0 && !selectedPeriodo.value) {
      selectedPeriodo.value = periodos.value[0] || null;
      selectedPeriodoId.value = selectedPeriodo.value ? selectedPeriodo.value.id : null;
      if (selectedPeriodo.value) {
        periodoStore.setPeriodo(selectedPeriodo.value);
      }
      await loadInventario();
    }
  } catch (error) {
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};

// Cargar almacenes disponibles
const loadWarehouses = async () => {
  try {
    const response = await axiosConfiguration.doGet('/warehouses?page=0&size=100&sortBy=warehouseKey&sortDir=asc&search=false');
    if (response.data.success) {
      warehouses.value = Array.isArray(response.data.data)
        ? response.data.data
        : [response.data.data];
      warehouses.value = warehouses.value
        .slice()
        .sort((a, b) => {
          const aNum = getWarehouseKeyNumber(a.warehouseKey || '');
          const bNum = getWarehouseKeyNumber(b.warehouseKey || '');
          if (aNum !== bNum) return aNum - bNum;
          return String(a.warehouseKey || '').localeCompare(String(b.warehouseKey || ''));
        });
    }
  } catch (error) {
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};


const loadInventario = async (page: number = 0) => {
  if (!selectedPeriodo.value) return;
  loading.value = true;
  try {
    LoadAlert(true);
    const searchParam = searchQuery.value
        ? `&search=${encodeURIComponent(searchQuery.value)}`
        : '';

    const response = await axiosConfiguration.doGet(
        `/inventory/period-report?periodId=${selectedPeriodo.value.id}${searchParam}`
    );

    // La API devuelve array directo, no objeto paginado
    const data: Producto[] = Array.isArray(response.data)
        ? response.data
        : response.data.content || [];

    allProductos.value = data;

    // Actualizar paginación
    pagination.value.totalElements = data.length;
    pagination.value.totalPages = Math.ceil(data.length / pagination.value.pageSize);
    pagination.value.currentPage = page;

    // Slice para la página actual
    const start = page * pagination.value.pageSize;
    productos.value = data.slice(start, start + pagination.value.pageSize);

    LoadAlert(false);
  } catch (error) {
    LoadAlert(false);
    ToastError('Error', 'No se pudo cargar el inventario');
  } finally {
    loading.value = false;
  }
};

// Manejar cambio de período desde el componente
const handlePeriodoChange = async (periodo: Periodo | null) => {
  selectedPeriodo.value = periodo;
  selectedPeriodoId.value = periodo ? periodo.id : null;
  if (periodo) {
    periodoStore.setPeriodo(periodo);
  }
  pagination.value.currentPage = 0;
  searchQuery.value = '';
  await loadInventario(0);
};


const changePageSize = async (event: Event) => {
  const value = Number((event.target as HTMLSelectElement).value);
  if (!Number.isFinite(value) || value <= 0) return;
  pagination.value.pageSize = value;
  pagination.value.totalPages = Math.ceil(allProductos.value.length / value);
  pagination.value.currentPage = 0;

  // No vuelve a llamar API, solo re-slice
  const start = 0;
  productos.value = allProductos.value.slice(start, value);
};

const changePage = async (page: number) => {
  if (
      page === pagination.value.currentPage ||
      page < 0 ||
      page >= pagination.value.totalPages ||
      loading.value
  ) return;

  pagination.value.currentPage = page;
  const start = page * pagination.value.pageSize;
  productos.value = allProductos.value.slice(start, start + pagination.value.pageSize);
};

// Abrir modal de importación
const openImportModal = () => {
  if (!selectedPeriodo.value) {
    ToastError('Error', 'Por favor selecciona un período antes de importar');
    return;
  }
  showImportModal.value = true;
  importFile.value = null;
};

// Cerrar modal de importación
const closeImportModal = () => {
  showImportModal.value = false;
  importFile.value = null;
};

// Manejar selección de archivo
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
      importFile.value = target.files[0] || null;
  }
};

// Estado para progress bar realista
const importProgress = ref(0);
const importStepText = ref('');

// Simulador de progreso mejorado
const simulateProgress = () => {
  importProgress.value = 0;
  const steps = [
    {progress: 10, text: 'Validando archivo...', duration: 400},
    {progress: 25, text: 'Leyendo datos...', duration: 400},
    {progress: 40, text: 'Procesando registros...', duration: 400},
    {progress: 60, text: 'Importando al sistema...', duration: 400},
    {progress: 80, text: 'Finalizando...', duration: 400},
    {progress: 95, text: 'Completando...', duration: 400}
  ];

  let stepIndex = 0;

  const progressInterval = setInterval(() => {
    if (stepIndex < steps.length && importing.value) {
      const currentStep = steps[stepIndex];
      if (currentStep) {
        importProgress.value = currentStep.progress;
        importStepText.value = currentStep.text;
      }
      stepIndex++;
    } else if (!importing.value) {
      clearInterval(progressInterval);
    }
  }, 400);

  return progressInterval;
};

// Importar inventario
const importarInventario = async () => {
   // Validación 1: Verificar que haya un período seleccionado
   if (!selectedPeriodo.value || !selectedPeriodo.value.id) {
     ToastError('Error', 'Debe seleccionar un período válido antes de importar inventario');
     console.error('Intento de importar sin período:', selectedPeriodo.value);
     return;
   }

   // Validación 2: Verificar que haya un archivo seleccionado
   if (!importFile.value) {
     ToastError('Error', 'Por favor selecciona un archivo');
     return;
   }

   // Validación extra: el periodo debe ser un número válido
   if (isNaN(Number(selectedPeriodo.value.id))) {
     ToastError('Error', 'El período seleccionado no es válido');
     return;
   }

   const formData = new FormData();
   formData.append('file', importFile.value);
   formData.append('periodId', selectedPeriodo.value.id.toString());

   // Agregar warehouseId solo si está seleccionado
   if (selectedWarehouse.value) {
     formData.append('warehouseId', selectedWarehouse.value.toString());
   }

   // DEBUG: Imprimir lo que se envía al backend
   const debugData = {} as Record<string, any>;
   formData.forEach((value, key) => {
     debugData[key] = value;
   });
   console.log('📦 Inventario a importar (FormData):', debugData);

   importing.value = true;
   const progressInterval = simulateProgress();

   try {
     LoadAlert(false); // Oculta el SweetAlert si lo usas
     const response = await axiosConfiguration.doPostFile(`/inventory/import`, formData);
     LoadAlert(false);

     // Validar respuesta del backend
     if (!response || response.status !== 200 || (response.data && response.data.success === false)) {
       clearInterval(progressInterval);
       ToastError('Error', 'El servidor no confirmó la importación. Verifica el período.');
       return;
     }

     // Completar al 100%
     importProgress.value = 100;
     importStepText.value = 'Importación completada';
     clearInterval(progressInterval);

     // Esperar un poco antes de cerrar
     await new Promise(resolve => setTimeout(resolve, 800));

     // Modal de resultado de importación
     importResult.value = response.data;
     showImportResultModal.value = true;

     await Swal.fire({
       icon: 'success',
       title: '¡Importación exitosa!',
       text: 'El inventario se importó correctamente.',
       timer: 2000,
       showConfirmButton: false
     });

     closeImportModal();
     await loadInventario(pagination.value.currentPage);
   } catch (error) {
     LoadAlert(false);
     clearInterval(progressInterval);
     ToastError('Error', 'No se pudo importar el inventario');
     console.error('Error en importación:', error);
   } finally {
     importing.value = false;
     importProgress.value = 0;
   }
};



// Cerrar modal de consulta de stock
const closeStockModal = () => {
  showStockModal.value = false;
  selectedProduct.value = null;
  selectedWarehouseForStock.value = null;
  stockInfo.value = null;
};

// Consultar stock de un producto en un almacén
const consultarStock = async () => {
  if (!selectedProduct.value || !selectedWarehouseForStock.value) {
    ToastError('Error', 'Por favor selecciona un almacén');
    return;
  }

  loadingStock.value = true;
  try {
    // Usar id si existe, sino usar cveArt como identificador
    const productIdentifier = selectedProduct.value.id || selectedProduct.value.cveArt;

    const response = await axiosConfiguration.doGet(
      `/inventory/stock?productId=${productIdentifier}&warehouseId=${selectedWarehouseForStock.value}`
    );
    stockInfo.value = response.data;
  } catch (error) {
    ToastError('Error', 'No se pudo consultar el stock del producto');
    stockInfo.value = null;
  } finally {
    loadingStock.value = false;
  }
};

// Formatear fecha
const formatDate = (date: string): string => {
  if (!date) return 'N/A';
  return new Date(date).toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
};

// Formatear estado
const getEstadoClass = (estado: string): string => {
  switch (estado?.toLowerCase()) {
    case 'activo':
    case 'disponible':
      return 'badge-success';
    case 'inactivo':
    case 'agotado':
      return 'badge-danger';
    case 'pendiente':
      return 'badge-warning';
    default:
      return 'badge-secondary';
  }
};

// Función debounce manual (reemplazo de lodash)
function debounce<T extends (...args: any[]) => void>(func: T, wait: number) {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  return function(this: any, ...args: Parameters<T>) {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
  };
}

// Debounce para búsqueda del lado servidor
const debouncedSearch = debounce(() => {
  pagination.value.currentPage = 0;
  loadInventario(0);
}, 500);

watch(searchQuery, () => {
  debouncedSearch()});

// Montar componente
onMounted(() => {
  loadPeriodos();
  loadWarehouses();
});
</script>

<template>
  <div class="inventario-admin">
    <div class="container-fluid">
      <div class="page-subtitle">
        <h1 class="title">
          Gestión de Inventario
        </h1>
        <p>Administra el inventario por período</p>
      </div>

      <div class="periodo-section">
        <PeriodoSelector
          v-model="selectedPeriodoId"
          :periodos="periodos"
          :show-info="false"
          :show-total-count="false"
          label="Período:"
          @change="handlePeriodoChange"
        />
        <div v-if="selectedPeriodo" class="periodo-info">
          <div class="info-item">
            <span class="label">Estado:</span>
            <span :class="['badge', getEstadoClass(selectedPeriodo.state)]">
              {{ selectedPeriodo.state }}
            </span>
          </div>

          <div class="info-item">
            <span class="label">Total Productos:</span>
            <span class="value">{{ pagination.totalElements }}</span>
          </div>


        </div>
        <SearchBar
            placeholder="Buscar por clave, producto, unidad o estado..."
            v-model="searchQuery"
        />
         <div class="inventory-actions">
           <button class="btn btn-primary btn-import btn-import-inline" :disabled="!selectedPeriodo" @click="openImportModal" :title="!selectedPeriodo ? 'Selecciona un período antes' : 'Abrir modal de importación'">
             Importar Inventario
           </button>
          <div class="page-size-control">
            <label for="pageSizeSelect" class="page-size-label">Productos:</label>
            <select
              id="pageSizeSelect"
              class="page-size-select"
              :value="pagination.pageSize"
              @change="changePageSize"
            >
                <option :value="20">20</option>
                <option :value="50">50</option>
                <option :value="100">100</option>
                <option :value="200">200</option>
                <option :value="500">500</option>
            </select>
          </div>
        </div>
      </div>

    <!-- Tabla de Inventario -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>Cargando inventario...</p>
    </div>

      <div v-else-if="productos.length > 0" class="table-section">
      <div class="table-responsive">
        <table class="table">
          <thead>
            <tr>
              <th>Clave de Producto</th>
              <th>Descripción</th>
              <th>Unidad</th>
              <th>Existencias</th>
              <th>Estado</th>
            </tr>
          </thead>
            <tbody>
              <tr v-for="(producto, index) in productos" :key="producto.cveArt + '-' + index">
              <td>
                <span class="product-key">{{ producto.cveArt }}</span>
              </td>
              <td>
                <strong>{{ producto.descr }}</strong>
              </td>
              <td>{{ producto.uniMed }}</td>
              <td class="text-center">
                <span :class="['existencias-badge', producto.existQty <= 10 ? 'low-stock' : '']">
                  {{ producto.existQty }}
                </span>
              </td>
              <td>
                <span :class="['badge', getEstadoClass(producto.status)]">
                  {{ producto.status }}
                </span>
              </td>

            </tr>
          </tbody>
        </table>
      </div>


        <div class="pagination-section">
          <div class="pagination-info">
            Mostrando {{ startIndex }}–{{ endIndex }} de {{ pagination.totalElements }} productos
          </div>

          <div class="pagination-controls">
            <button
                class="btn-pagination"
                :disabled="pagination.currentPage === 0"
                @click="changePage(pagination.currentPage - 1)"
            >
              ← Anterior
            </button>

            <template v-for="page in visiblePages" :key="page">
              <button
                  :class="['btn-pagination', { active: page === pagination.currentPage }]"
                  @click="changePage(page)"
              >
                {{ page + 1 }}
              </button>
            </template>

            <button
                class="btn-pagination"
                :disabled="pagination.currentPage >= pagination.totalPages - 1"
                @click="changePage(pagination.currentPage + 1)"
            >
              Siguiente →
            </button>
          </div>
        </div>
    </div>

    <!-- Sin datos -->
      <div v-else class="no-data">
        <div class="no-data-icon">📭</div>
        <h3>{{ searchQuery ? 'No se encontraron productos' : 'No hay inventario disponible' }}</h3>
        <p>{{ searchQuery ? 'Intenta con otro término de búsqueda' : 'Selecciona un período o importa un inventario' }}</p>
      </div>
  </div>

  <!-- Modal de Importación -->
  <div v-if="showImportModal" class="modal-overlay" @click.self="closeImportModal">
    <div class="modal-content">
      <div class="modal-header">
        <h2>Importar Inventario</h2>
        <button class="btn-close" @click="closeImportModal" :disabled="importing">&times;</button>
      </div>
      <div class="modal-body">
        <!-- Mostrar spinner durante la importación -->
        <!-- Reemplaza la sección del spinner en tu modal de importación -->
        <div v-if="importing" class="import-progress-container">
          <div class="import-spinner-wrapper">
            <svg class="import-spinner" viewBox="0 0 50 50">
              <circle
                  class="spinner-circle"
                  cx="25"
                  cy="25"
                  r="20"
                  fill="none"
                  stroke="#28a745"
                  stroke-width="4"
                  stroke-linecap="round"
              />
            </svg>
          </div>
          <div class="import-status-text">{{ importStepText }}</div>
          <div class="progress-bar-container">
            <div class="progress-bar-fill" :style="{ width: importProgress + '%' }"></div>
          </div>
        </div>

        <!-- Contenido normal del modal (oculto durante importación) -->
        <div v-if="!importing">
          <!-- Mostrar información del período seleccionado -->
          <div v-if="selectedPeriodo" class="selected-period-info" >
            <strong>Período seleccionado:</strong>
            {{ formatDate(selectedPeriodo.date) }} - {{ selectedPeriodo.comments }}
            <span :class="['badge', getEstadoClass(selectedPeriodo.state)]" >
              {{ selectedPeriodo.state }}
            </span>
          </div>
          <div class="import-info">
            <p><strong>Instrucciones:</strong></p>
            <ul>
              <li>Completa la información del inventario en la plantilla</li>
              <li>Asegúrate de incluir: Clave de Producto = CVE_ART, \N Descripcion del producto = DESCR, Unidad = UNI_MED, Existencias = Existencias y Estado = Status</li>
            </ul>
          </div>
          <div class="form-group">
            <label for="importFile" class="form-label">
              Seleccionar archivo <span class="required">*</span>
            </label>
            <input
              id="importFile"
              type="file"
              accept=".xlsx,.xls,.csv"
              @change="handleFileSelect"
              class="form-control"
              :disabled="importing"
            />
            <small class="form-help-text">
              Formatos aceptados: Excel (.xlsx, .xls) o CSV (.csv)
            </small>
            <div v-if="importFile" class="file-selected">
              <span class="icon">📎</span>
              {{ importFile.name }}
            </div>
          </div>

          <div v-if="!selectedPeriodo" class="alert alert-warning">
            ⚠️ Por favor, selecciona un período antes de importar
          </div>
        </div>
       <div v-if="!importing" class="modal-footer">
         <button class="btn btn-secondary" @click="closeImportModal" :disabled="importing">
           Cancelar
         </button>
         <button
           class="btn btn-primary"
           @click="importarInventario"
           :disabled="!importFile || !selectedPeriodo || importing"
           :title="!selectedPeriodo ? 'Debe seleccionar un período primero' : !importFile ? 'Debe seleccionar un archivo' : 'Importar archivo'"
          >
            <span class="icon">📥</span>
            Importar
          </button>
        </div>
       </div>
     </div>
   </div>

   <!-- Modal de Consulta de Stock -->
  <div v-if="showStockModal" class="modal-overlay" @click.self="closeStockModal">
    <div class="modal-content">
      <div class="modal-header">
        <h2>Consultar Stock del Producto</h2>
        <button class="btn-close" @click="closeStockModal">&times;</button>
      </div>
      <div class="modal-body">
        <div v-if="selectedProduct" class="product-info">
          <div class="info-row">
            <span class="label">Producto:</span>
            <span class="value"><strong>{{ selectedProduct.descr }}</strong></span>
          </div>
          <div class="info-row">
            <span class="label">Clave:</span>
            <span class="value">{{ selectedProduct.cveArt }}</span>
          </div>
          <div class="info-row">
            <span class="label">Unidad:</span>
            <span class="value">{{ selectedProduct.uniMed }}</span>
          </div>
        </div>

        <div class="form-group">
          <label for="warehouseSelectStock" class="form-label">
            Seleccionar Almacén <span class="required">*</span>
          </label>
          <select
            id="warehouseSelectStock"
            v-model="selectedWarehouseForStock"
            class="form-select"
            @change="stockInfo = null"
          >
            <option :value="null" disabled>Selecciona un almacén</option>
            <option
              v-for="warehouse in warehouses"
              :key="warehouse.id"
              :value="warehouse.id"
            >
              {{ warehouse.warehouseKey }} - {{ warehouse.nameWarehouse }}
            </option>
          </select>
        </div>

        <button
          class="btn btn-primary btn-block"
          @click="consultarStock"
          :disabled="!selectedWarehouseForStock || loadingStock"
        >
          <span v-if="loadingStock" class="spinner-small"></span>
          <span v-else class="icon">🔍</span>
          {{ loadingStock ? 'Consultando...' : 'Consultar Stock' }}
        </button>

        <div v-if="stockInfo" class="stock-result">
          <h3>Resultado de Stock</h3>
          <div class="stock-details">
            <div class="stock-item">
              <span class="label">Existencias:</span>
              <span class="value stock-value">{{ stockInfo.existQty || 0 }}</span>
            </div>
            <div class="stock-item">
              <span class="label">Estado:</span>
              <span :class="['badge', getEstadoClass(stockInfo?.status || '')]">
                {{ stockInfo?.status || 'N/A' }}
              </span>
            </div>
            <div v-if="stockInfo?.lastUpdated" class="stock-item">
              <span class="label">Última Actualización:</span>
              <span class="value">{{ formatDate(stockInfo?.lastUpdated) }}</span>
            </div>
          </div>
        </div>

        <div v-else-if="!loadingStock && selectedWarehouseForStock" class="no-stock-info">
          <p>Presiona "Consultar Stock" para ver la información</p>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="closeStockModal">
          Cerrar
        </button>
      </div>
    </div>
  </div>

  <!-- Modal de Resumen de Importación -->
  <div v-if="showImportResultModal" class="modal-overlay" @click.self="showImportResultModal = false">
    <div class="modal-content modal-result">
      <div class="modal-header">
        <div class="header-status">
          <span class="status-icon">✓</span>
          <h2>Importación Completada</h2>
        </div>
        <button class="btn-close" @click="showImportResultModal = false">&times;</button>
      </div>

      <div class="modal-body">
        <!-- Información general de la importación -->
        <div class="import-summary">
          <p class="summary-title">Se ha completado la importación de inventario exitosamente</p>
          <div class="summary-details">
            <div class="detail-item">
              <span class="label">Total procesado:</span>
              <span class="value">{{ importResult.totalRows }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Insertados:</span>
              <span class="value" style="color: #4CAF50; font-weight: 600">{{ importResult.inserted }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Actualizados:</span>
              <span class="value">{{ importResult.updated }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Desactivados:</span>
              <span class="value">{{ importResult.deactivated }}</span>
            </div>
          </div>
        </div>

        <!-- Errores si existen -->
        <div v-if="importResult.errors && importResult.errors.length" class="alert alert-warning">
          <strong>Errores detectados:</strong>
          <ul style="margin: 8px 0 0 20px; padding: 0;">
            <li v-for="(err, idx) in importResult.errors" :key="idx" style="margin: 4px 0; font-size: 13px;">{{ err }}</li>
          </ul>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-primary" @click="showImportResultModal = false">
          Aceptar
        </button>
      </div>
    </div>
  </div>
</div>

</template>

<style scoped>

.icon {
  font-size: 24px;
}

.btn-import {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
}

.btn-import:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(40, 167, 69, 0.5);
}

/* Selector de Período */
.periodo-section {
  display: flex;
  gap: 20px;
  padding: 10px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  flex-wrap: wrap;
  align-items: center;
}
.periodo-info {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}


.btn-import-inline {
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 6px;
  box-shadow: none;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  gap: 6px;
}

.inventory-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-size-control {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #6c757d;
}

.page-size-select {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #ced4da;
  font-size: 14px;
  background: white;
}
.periodo-selector {
  margin-bottom: 20px;
  padding: 15px 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}


.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #495057;
}

.form-select {
  width: 100%;
  padding: 10px 15px;
  font-size: 15px;
  border: 2px solid #ced4da;
  border-radius: 8px;
  transition: all 0.3s ease;
  background-color: white;
  cursor: pointer;
}

.form-select:focus {
  outline: none;
  border-color: #20c997;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  font-weight: 600;
  color: #6c757d;
}

.info-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #007bff;
}



/* Loading */
.loading-container {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.spinner {
  border: 4px solid #f3f4f6;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Tabla */
.table-section {
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.table-responsive {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin: 0;
}

.table thead {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.table thead th {
  padding: 15px;
  text-align: left;
  font-weight: 600;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table tbody tr {
  border-bottom: 1px solid #dee2e6;
  transition: background-color 0.2s ease;
}

.table tbody tr:hover {
  background-color: #f8f9fa;
}

.table tbody td {
  padding: 15px;
  color: #495057;
  font-size: 14px;
}

.text-center {
  text-align: center;
}

.product-key {
  background: #e9ecef;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
  color: #495057;
}

.existencias-badge {
  background: #28a745;
  color: white;
  padding: 6px 12px;
  border-radius: 8px;
  font-weight: bold;
  display: inline-block;
  min-width: 50px;
}

.existencias-badge.low-stock {
  background: #dc3545;
}

.badge {
  padding: 6px 12px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  display: inline-block;
}


/* Paginación */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-top: 2px solid #e9ecef;
  flex-wrap: wrap;
  gap: 15px;
}

.pagination-info {
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}

.pagination-controls {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.btn-pagination {
  padding: 8px 16px;
  border: 1px solid #dee2e6;
  background: white;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}


.periodo-selector {
  margin-bottom: 20px;
  padding: 15px 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}



.btn-pagination:hover:not(:disabled) {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.btn-pagination:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  padding: 8px 16px;
  font-weight: 600;
  color: #495057;
}

/* Sin datos */
.no-data {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.no-data-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.no-data h3 {
  font-size: 24px;
  color: #333;
  margin-bottom: 10px;
}

.no-data p {
  color: #6c757d;
  font-size: 16px;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    transform: translateY(50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 25px;
  border-bottom: 2px solid #dee2e6;
  background: linear-gradient(135deg, #28a745 0%, #577e85 100%);
  color: white;
}

.modal-header h2 {
  margin: 0;
  font-size: 24px;
}

.btn-close {
  background: none;
  border: none;
  font-size: 32px;
  color: white;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.3s ease;
}

.btn-close:hover {
  opacity: 0.7;
}

.modal-body {
  padding: 25px;
}

.import-info {
  background: #e7f3ff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  border-left: 4px solid #007bff;
}

.import-info ul {
  margin: 10px 0 0 20px;
  padding: 0;
}

.import-info li {
  margin-bottom: 8px;
  color: #495057;
}

.btn-block {
  width: 100%;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-control {
  width: 100%;
  padding: 10px 15px;
  border: 2px solid #ced4da;
  border-radius: 8px;
  font-size: 15px;
  transition: all 0.3s ease;
}

.form-control:focus {
  outline: none;
  border-color: #28a745;
  box-shadow: 0 0 0 3px rgba(40, 167, 69, 0.1);
}

.form-help-text {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #6c757d;
  font-style: italic;
}

.required {
  color: #dc3545;
}

.file-selected {
  margin-top: 10px;
  padding: 10px;
  background: #d4edda;
  border-radius: 6px;
  color: #155724;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.alert {
  padding: 15px;
  border-radius: 8px;
  margin-top: 15px;
}

.alert-warning {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeeba;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 25px;
  border-top: 2px solid #dee2e6;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-primary {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(40, 167, 69, 0.5);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

/* Modal de Resultado de Importación - Patrón MultiAlmacen */
.modal-result {
  max-width: 600px;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
}

.header-status .status-icon {
  font-size: 28px;
  color: #4CAF50;
}

.import-summary {
  padding: 16px;
  background: #f8f9fa;
  border-left: 4px solid #4CAF50;
  border-radius: 4px;
  margin-bottom: 20px;
}

.summary-title {
  font-weight: 600;
  margin: 0 0 12px 0;
  color: #333;
  font-size: 15px;
}

.summary-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #e0e0e0;
  font-size: 14px;
}

.detail-item .label {
  font-weight: 500;
  color: #666;
}

.detail-item .value {
  font-weight: 700;
  color: #333;
}

.alert-warning {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeeba;
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.alert-warning strong {
  display: block;
  margin-bottom: 8px;
}

/* Estilos para modal de stock */
.product-info {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  border-left: 4px solid #007bff;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #dee2e6;
}
.btn-pagination.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
  font-weight: bold;
}
.info-row:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.info-row .label {
  font-weight: 600;
  color: #6c757d;
  min-width: 120px;
}

.info-row .value {
  color: #495057;
  flex: 1;
  text-align: right;
}

.stock-result {
  margin-top: 25px;
  padding: 20px;
  background: #e7f3ff;
  border-radius: 8px;
  border: 2px solid #007bff;
}

.stock-result h3 {
  margin: 0 0 15px 0;
  font-size: 18px;
  color: #007bff;
  font-weight: bold;
}

.stock-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stock-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: white;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stock-item .label {
  font-weight: 600;
  color: #6c757d;
}

.stock-item .value {
  color: #495057;
  font-weight: 500;
}

.stock-value {
  font-size: 24px;
  font-weight: bold;
  color: #28a745;
}

.no-stock-info {
  margin-top: 20px;
  padding: 20px;
  background: #fff3cd;
  border-radius: 8px;
  text-align: center;
  color: #856404;
  font-style: italic;
}

.spinner-small {
  border: 2px solid #f3f4f6;
  border-top: 2px solid #ffffff;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  animation: spin 1s linear infinite;
  display: inline-block;
}




@keyframes global-progressbar-indeterminate {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}



/* Responsive */
@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
    align-items: stretch;
  }

  .periodo-section {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-section {
    flex-direction: column;
    text-align: center;
  }

  .pagination-controls {
    justify-content: center;
  }

  .modal-content {
    width: 95%;
    margin: 10px;
  }
}



/* Estilos para importación con spinner elegante */
.import-progress-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 30px;
  text-align: center;
  min-height: 300px;
  background: transparent;
}

.import-spinner-wrapper {
  position: relative;
  margin-bottom: 30px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.import-spinner {
  width: 80px;
  height: 80px;
  animation: spinner-rotate 1.5s linear infinite;
}

.spinner-circle {
  stroke-dasharray: 125.6;
  stroke-dashoffset: 0;
  animation: spinner-dash 1.5s ease-in-out infinite;
}

@keyframes spinner-stroke {
  0% {
    stroke-dasharray: 0, 125.6;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 125.6, 125.6;
    stroke-dashoffset: 0;
  }
  100% {
    stroke-dasharray: 0, 125.6;
    stroke-dashoffset: -125.6;
  }
}

@keyframes spinner-dash {
  0% {
    stroke-dasharray: 1, 125.6;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 100, 125.6;
    stroke-dashoffset: -25;
  }
  100% {
    stroke-dasharray: 1, 125.6;
    stroke-dashoffset: -125.6;
  }
}

@keyframes spinner-rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.import-status-text {
  font-size: 16px;
  font-weight: 600;
  color: #28a745;
  margin-bottom: 25px;
  letter-spacing: 0.5px;
  min-height: 24px;
}

.progress-bar-container {
  width: 100%;
  max-width: 400px;
  height: 8px;
  background: #e9ecef;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #28a745 0%, #20c997 100%);
  border-radius: 10px;
  transition: width 0.4s ease;
  position: relative;
  overflow: hidden;
}

.progress-percentage {
  font-size: 18px;
  font-weight: 700;
  color: #28a745;
  margin-top: 10px;
}

.progress-bar-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.3),
      transparent
  );
  animation: shimmer 1.5s infinite;
}


/* Estilos para botones deshabilitados */
.btn-import:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: linear-gradient(135deg, #6c757d 0%, #5a6268 100%);
  box-shadow: 0 2px 4px rgba(108, 117, 125, 0.2);
  transform: none;
}

.btn-import:disabled:hover {
  transform: none;
  box-shadow: 0 2px 4px rgba(108, 117, 125, 0.2);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

