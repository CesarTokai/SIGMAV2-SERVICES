<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { usePeriodoStore } from '@/store/periodoStore';
import SearchBar from '@/components/SearchBar.vue';
import PeriodoSelector from '@/components/PeriodoSelector.vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, LoadAlert } from '@/utils/SweetAlert';
import Swal from 'sweetalert2';
import axios from "axios";

interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface ProductoMultiAlmacen {
  claveProducto: string;
  producto: string;
  claveAlmacen: string;
  almacen: string;
  estado: string;
  existencias: number;
}

interface ProductoDadoDeBaja {
  claveProducto: string;
  nombreProducto: string;
  claveAlmacen: string;
  nombreAlmacen: string;
  existenciasAnteriores: number;
}

interface ImportLog {
  id: number;
  fileName: string;
  period: string;
  importDate: string;
  status: 'SUCCESS' | 'SUCCESS_WITH_WARNINGS' | 'NO_CHANGES' | 'ERROR';
  message: string;
  fileHash: string;
  stage: string;
}

interface ImportResponse {
  importLog: ImportLog;
  tieneWarnings: boolean;
  mensajeWarning: string | null;
  productosDadosDeBaja: ProductoDadoDeBaja[];
  totalProcesados: number;
  totalActualizados: number;
  totalCreados: number;
  totalAlmacenesCreados: number;
  totalProductosCreados: number;
  totalDadosDeBaja: number;
}

const periodoStore = usePeriodoStore();

// Estado
const periodos = ref<Periodo[]>([]);
const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);
const productos = ref<ProductoMultiAlmacen[]>([]);


const searchQuery = ref('');
const loading = ref(false);
const showImportModal = ref(false);
const importFile = ref<File | null>(null);
const importing = ref(false);
const importProgress = ref(0);
const importStepText = ref('');


const page = ref(0);
const pageSize = ref(100);
const totalPages = ref(1);
const totalElements = ref(0);
const currentFilteredProductos = ref<ProductoMultiAlmacen[]>([]);


const allProductos = ref<ProductoMultiAlmacen[]>([]);

const importResponse = ref<ImportResponse | null>(null);
const activeTab = ref<'productos' | 'bajas'>('productos');
const productosDadosDeBaja = ref<ProductoDadoDeBaja[]>([]);
const showImportResultModal = ref(false);
const importStatusClass = ref<'success' | 'warning' | 'no-changes' | 'error'>('success');

// Cargar períodos disponibles
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods');
    periodos.value = response.data.content || [];

    // Cargar periodo guardado del store
    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      selectedPeriodo.value = periodoStore.periodoSeleccionado;
      selectedPeriodoId.value = periodoStore.periodoSeleccionado.id;
      await loadProductos();
    } else if (periodos.value.length > 0 && !selectedPeriodo.value) {
      selectedPeriodo.value = periodos.value[0] || null;
      selectedPeriodoId.value = selectedPeriodo.value ? selectedPeriodo.value.id : null;
      if (selectedPeriodo.value) {
        periodoStore.setPeriodo(selectedPeriodo.value);
      }
      await loadProductos();
    }
  } catch (error) {
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};


interface PaginationInfo {
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
}



const loadProductos = async () => {
  if (!selectedPeriodo.value) {
    productos.value = [];
    totalPages.value = 1;
    totalElements.value = 0;
    loading.value = false;
    return;
  }

  loading.value = true;

  try {
    LoadAlert(true);

    const body: any = {
      periodId: selectedPeriodo.value.id,
      search: searchQuery.value || undefined,
    };

    const response = await axios.post('/multi-warehouse/existences', body);

    // API devuelve array directo
    const data: ProductoMultiAlmacen[] = Array.isArray(response.data)
        ? response.data.map(mapItem)
        : (response.data.content ?? []).map(mapItem);

    allProductos.value = data;
    totalElements.value = data.length;
    totalPages.value = Math.ceil(data.length / pageSize.value);
    page.value = 0;

    // Slice para página actual
    productos.value = data.slice(0, pageSize.value);

    LoadAlert(false);
  } catch (error: any) {
    LoadAlert(false);
    ToastError('Error', 'No se pudieron cargar los productos');
    productos.value = [];
    totalElements.value = 0;
    totalPages.value = 1;
  } finally {
    loading.value = false;
  }
};


const mapItem = (item: any): ProductoMultiAlmacen => {
  return {
    claveProducto: String(item.productCode ?? item.cveArt ?? item.claveProducto ?? '').trim(),
    producto: String(item.productName ?? item.descr ?? item.producto ?? '').trim(),
    claveAlmacen: String(item.warehouseKey ?? item.claveAlmacen ?? '').trim(),
    almacen: String(item.warehouseName ?? item.nameWarehouse ?? item.almacen ?? '-').trim(),
    estado: String(item.status ?? item.estado ?? '').trim(),
    existencias: Number(item.stock ?? item.existQty ?? item.existencias ?? 0),
  };
};



// Abrir modal de importación
const openImportModal = () => {
  showImportModal.value = true;
  importFile.value = null;
};

// Cerrar modal de importación
const closeImportModal = () => {
  showImportModal.value = false;
  importFile.value = null;
};

// Cerrar modal de resultado de importación
const closeImportResultModal = () => {
  showImportResultModal.value = false;
  importResponse.value = null;
  productosDadosDeBaja.value = [];
  activeTab.value = 'productos';
};

// Cambiar tab en modal de resultado
const switchTab = (tab: 'productos' | 'bajas') => {
  activeTab.value = tab;
};

// Obtener icono según status
const getImportStatusIcon = (status: string): string => {
  switch (status) {
    case 'SUCCESS': return '✅';
    case 'SUCCESS_WITH_WARNINGS': return '⚠️';
    case 'NO_CHANGES': return 'ℹ️';
    default: return '❌';
  }
};

// Obtener color de badge según status
const getImportStatusColor = (status: string): string => {
  switch (status) {
    case 'SUCCESS': return '#4CAF50';
    case 'SUCCESS_WITH_WARNINGS': return '#FFC107';
    case 'NO_CHANGES': return '#2196F3';
    default: return '#F44336';
  }
};

// Manejar selección de archivo
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    importFile.value = target.files[0] || null;
  }
};

// Simulador de progreso mejorado con duraciones personalizadas
const simulateProgress = () => {
  importProgress.value = 0;
  const steps = [
    { progress: 10, text: 'Validando archivo...', duration: 400 },
    { progress: 25, text: 'Leyendo datos...', duration: 400 },
    { progress: 40, text: 'Procesando registros...', duration: 400 },
    { progress: 60, text: 'Importando al sistema...', duration: 400 },
    { progress: 80, text: 'Finalizando...', duration: 400 },
    { progress: 95, text: 'Completando...', duration: 400 }
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

// Importar productos multi-almacén
const importarMultiAlmacen = async () => {
  if (!importFile.value) {
    ToastError('Error', 'Por favor selecciona un archivo');
    return;
  }

  if (!selectedPeriodo.value) {
    ToastError('Error', 'Por favor selecciona un período');
    return;
  }

  // Usar la fecha completa del periodo tal como viene de la base de datos
  const periodString = selectedPeriodo.value.date;

  const formData = new FormData();
  formData.append('file', importFile.value);
  formData.append('period', periodString);

  importing.value = true;
  const progressInterval = simulateProgress();

  try {
    LoadAlert(false);
    const response = await axiosConfiguration.doPostFile(`/multi-warehouse/import`, formData);
    LoadAlert(false);

    // Procesar respuesta según el status
    const data: ImportResponse = response.data;
    importResponse.value = data;

    // Determinar clase según status
    if (data.importLog.status === 'SUCCESS') {
      importStatusClass.value = 'success';
    } else if (data.importLog.status === 'SUCCESS_WITH_WARNINGS') {
      importStatusClass.value = 'warning';
      productosDadosDeBaja.value = data.productosDadosDeBaja || [];
      activeTab.value = 'bajas'; // Abrir tab de bajas automáticamente
    } else if (data.importLog.status === 'NO_CHANGES') {
      importStatusClass.value = 'no-changes';
    } else {
      importStatusClass.value = 'error';
    }

    // Completar al 100%
    importProgress.value = 100;
    importStepText.value = 'Importación completada';
    clearInterval(progressInterval);

    // Esperar un poco antes de mostrar resultado
    await new Promise(resolve => setTimeout(resolve, 800));

    // Mostrar modal de resultado en lugar de alert
    showImportResultModal.value = true;

    closeImportModal();

    // Recargar productos después de un tiempo
    setTimeout(() => {
      loadProductos();
    }, 1500);

  } catch (error: any) {
    LoadAlert(false);
    clearInterval(progressInterval);
    importStatusClass.value = 'error';

    let mensaje = 'No se pudo importar el archivo';
    if (error?.response?.data?.message) {
      mensaje = error.response.data.message;
    }

    // Crear respuesta de error
    importResponse.value = {
      importLog: {
        id: 0,
        fileName: importFile.value?.name || 'desconocido.xlsx',
        period: selectedPeriodo.value.date,
        importDate: new Date().toISOString(),
        status: 'ERROR',
        message: mensaje,
        fileHash: '',
        stage: 'default'
      },
      tieneWarnings: false,
      mensajeWarning: null,
      productosDadosDeBaja: [],
      totalProcesados: 0,
      totalActualizados: 0,
      totalCreados: 0,
      totalAlmacenesCreados: 0,
      totalProductosCreados: 0,
      totalDadosDeBaja: 0
    };

    showImportResultModal.value = true;
  } finally {
    importing.value = false;
    importProgress.value = 0;
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

// Parsear el mensaje del backend para extraer estadísticas reales
// Ej: "Registros procesados: 1301, Almacenes creados: 2, Productos creados: 0, Existentes actualizados: 0, Marcados como baja: 0"
const parseImportMessage = (message: string): Record<string, number> => {
  const result: Record<string, number> = {};
  const pairs = message.split(',');
  for (const pair of pairs) {
    const match = pair.match(/([^:]+):\s*(\d+)/);
    if (match && match[1] && match[2]) {
      result[match[1].trim().toLowerCase()] = parseInt(match[2], 10);
    }
  }
  return result;
};

// Estadísticas consolidadas: usa campos de la respuesta y complementa con el mensaje parseado
const importStats = computed(() => {
  if (!importResponse.value) return null;
  const r = importResponse.value;
  const parsed = parseImportMessage(r.importLog?.message || '');

  return {
    totalProcesados:       r.totalProcesados       ?? parsed['registros procesados'] ?? 0,
    totalActualizados:     r.totalActualizados      ?? parsed['existentes actualizados'] ?? 0,
    totalCreados:          r.totalCreados           ?? parsed['registros procesados'] ?? 0,
    totalAlmacenesCreados: r.totalAlmacenesCreados  ?? parsed['almacenes creados'] ?? 0,
    totalProductosCreados: r.totalProductosCreados  ?? parsed['productos creados'] ?? 0,
    totalDadosDeBaja:      r.totalDadosDeBaja       ?? parsed['marcados como baja'] ?? 0,
    // Fallback desde mensaje cuando el campo del response sea 0 pero el mensaje diga otro valor
    almacenesCreados_real: (r.totalAlmacenesCreados > 0)
      ? r.totalAlmacenesCreados
      : (parsed['almacenes creados'] ?? 0),
  };
});

// Formatear número
const formatNumber = (value: number): string => {
  return new Intl.NumberFormat('es-MX').format(value);
};

// Formatear estado
const getEstadoClass = (estado: string): string => {
  const estadoLower = estado?.toLowerCase();
  if (estadoLower?.includes('activo') || estadoLower?.includes('disponible')) {
    return 'badge-success';
  } else if (estadoLower?.includes('inactivo') || estadoLower?.includes('no disponible')) {
    return 'badge-danger';
  } else if (estadoLower?.includes('pendiente')) {
    return 'badge-warning';
  }
  return 'badge-secondary';
};

// Actualiza el watch de searchQuery
watch(searchQuery, (query) => {
  const q = query.toLowerCase().trim();
  const filtered = q
      ? allProductos.value.filter(p =>
          p.claveProducto.toLowerCase().includes(q) ||
          p.producto.toLowerCase().includes(q) ||
          p.claveAlmacen.toLowerCase().includes(q) ||
          p.almacen.toLowerCase().includes(q) ||
          p.estado.toLowerCase().includes(q)
      )
      : allProductos.value;

  currentFilteredProductos.value = filtered; // <-- guardar filtrado
  totalElements.value = filtered.length;
  totalPages.value = Math.ceil(filtered.length / pageSize.value);
  page.value = 0;
  productos.value = filtered.slice(0, pageSize.value);
});

watch(selectedPeriodoId, async (newIdRaw, oldIdRaw) => {
  const newId = newIdRaw == null ? null : Number(newIdRaw);
  const oldId = oldIdRaw == null ? null : Number(oldIdRaw);


  if (newId == null || isNaN(newId)) {
    selectedPeriodo.value = null;
    searchQuery.value = '';
    page.value = 0;
    productos.value = [];
    loading.value = false;
    totalPages.value = 1;
    totalElements.value = 0;
    return;
  }
  if (newId === oldId) return;
  const found = periodos.value.find(p => p.id === newId) || null;
  productos.value = [];
  searchQuery.value = '';
  page.value = 0;
  totalPages.value = 1;
  totalElements.value = 0;
  loading.value = true;
  selectedPeriodo.value = found;
  await new Promise(resolve => setTimeout(resolve, 0));
  await loadProductos();
});

const goToPage = (newPage: number) => {
  if (newPage < 0 || newPage >= totalPages.value) return;
  page.value = newPage;
  const source = currentFilteredProductos.value.length > 0 || searchQuery.value
      ? currentFilteredProductos.value
      : allProductos.value;
  const start = newPage * pageSize.value;
  productos.value = source.slice(start, start + pageSize.value);
};

const visiblePages = computed(() => {
  const total = totalPages.value;
  const current = page.value;
  const delta = 2;

  const pages: number[] = [];
  const start = Math.max(0, current - delta);
  const end = Math.min(total - 1, current + delta);

  if (start > 0) pages.push(0);
  for (let i = start; i <= end; i++) pages.push(i);
  if (end < total - 1) pages.push(total - 1);

  return [...new Set(pages)];
});


const changePageSize = (event: Event) => {
  const value = Number((event.target as HTMLSelectElement).value);
  if (!value || value <= 0) return;
  pageSize.value = value;
  page.value = 0;
  const source = currentFilteredProductos.value.length > 0 || searchQuery.value
      ? currentFilteredProductos.value
      : allProductos.value;
  totalPages.value = Math.ceil(source.length / value);
  productos.value = source.slice(0, value);
};

// Montar componente
onMounted(() => {
  loadPeriodos();
});

// FUNCIONES A NIVEL SUPERIOR: consultar stock y mostrar detalle
const fetchStock = async (productCode: string, warehouseKey: string | number, periodId: number) => {
  const body = {
    productCode: productCode,
    warehouseKey: String(warehouseKey),
    periodId: periodId
  };

  try {
    LoadAlert(true);
    const url = '/multi-warehouse/stock';
    const resp = await axios.post(url, body);
    LoadAlert(false);
    return resp.data;
  } catch (err: any) {
    LoadAlert(false);
    const isCanceled =
        err?.name === 'CanceledError' ||
        err?.code === 'ERR_CANCELED' ||
        (axios && typeof (axios as any).isCancel === 'function' && (axios as any).isCancel(err)) ||
        err?.message === 'canceled';
    if (isCanceled) {
      return null;
    }
    ToastError('Error', 'No se pudo consultar el stock');
    return null;
  }
};

const viewStock = async (producto: ProductoMultiAlmacen) => {
  if (!selectedPeriodo.value) {
    ToastError('Error', 'Selecciona un período primero');
    return;
  }
  const productCode = producto.claveProducto;
  const warehouseKey = producto.claveAlmacen;
  const periodId = selectedPeriodo.value.id;

  const data = await fetchStock(productCode, warehouseKey, periodId);
  if (!data) return;

  await Swal.fire({
    title: `Stock: ${productCode} - Almacén ${warehouseKey}`,
    html: `<pre style="text-align:left;max-height:300px;overflow:auto">${JSON.stringify(data, null, 2)}</pre>`,
    width: '680px'
  });
};

// Manejar cambio de período desde PeriodoSelector
const handlePeriodoChange = async (periodo: Periodo | null) => {
  // Si el período es nulo, significa que se deseleccionó
  if (!periodo) {
    selectedPeriodo.value = null;
    selectedPeriodoId.value = null;
    searchQuery.value = '';
    page.value = 0;
    productos.value = [];
    loading.value = false;
    totalPages.value = 1;
    totalElements.value = 0;
    return;
  }

  // PRIMERO limpiar TODO inmediatamente antes de actualizar el período
  productos.value = [];
  searchQuery.value = '';
  page.value = 0;
  totalPages.value = 1;
  totalElements.value = 0;
  loading.value = true;

  // LUEGO actualizar el período seleccionado
  selectedPeriodo.value = periodo;
  selectedPeriodoId.value = periodo.id;

  // Guardar en el store
  periodoStore.setPeriodo(periodo);

  // Esperar un tick para asegurar que Vue actualice el DOM
  await new Promise(resolve => setTimeout(resolve, 0));

  // Finalmente cargar los datos del nuevo período
  await loadProductos();
};
</script>

<template>
  <div class="multi-almacen-admin">
    <div class="container-fluid">
      <div class="page-subtitle">
        <h1 class="title">
          Multi-Almacéna
        </h1>
        <p>Gestión de productos en múltiples almacenes</p>
      </div>

      <!-- Periodo Section -->
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
            <span class="value">{{ totalElements }}</span>
          </div>
        </div>
        <SearchBar
            placeholder="Buscar por clave, producto, unidad o estado..."
            v-model="searchQuery"
        />
        <button class="btn btn-primary btn-import btn-import-inline"  @click="openImportModal">
          Importar Multi-Almacén
        </button>
      </div>


      <div :key="`content-${selectedPeriodoId ?? 'none'}-${page}-$`">
        <!-- Vista de Productos -->
        <div>
          <div v-if="loading" class="loading-container">
            <div class="spinner"></div>
            <p>Cargando productos...</p>
          </div>
          <div v-else-if="productos.length > 0" class="table-section">
            <div class="table-responsive">
              <table class="table">
                <thead>
                <tr>
                  <th>Clave de Producto</th>
                  <th>Producto</th>
                  <th>Clave Almacén</th>
                  <th>Almacén</th>
                  <th>Estado</th>
                  <th>Existencias</th>
                </tr>
                </thead>
                <tbody>
                <tr v-for="(producto, index) in productos" :key="`${producto.claveProducto}-${producto.claveAlmacen}-${index}`">
                  <td>
                    <span class="product-key">{{ producto.claveProducto }}</span>
                  </td>
                  <td>
                    <strong>{{ producto.producto }}</strong>
                  </td>
                  <td>
                    <span class="warehouse-key">{{ producto.claveAlmacen }}</span>
                  </td>
                  <td>{{ producto.almacen }}</td>
                  <td>
                     <span :class="['badge', 'badge-minimal', getEstadoClass(producto.estado)]">
                       {{ producto.estado }}
                     </span>
                  </td>
                  <td class="text-center">
                     <span :class="['existencias-badge', producto.existencias <= 10 ? 'low-stock' : '']">
                       {{ formatNumber(producto.existencias) }}
                     </span>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>

            <div class="pagination-section">
              <div class="pagination-info">
                Mostrando {{ page * pageSize + 1 }}–{{ Math.min((page + 1) * pageSize, totalElements) }}
                de {{ totalElements }} registros
              </div>

              <div class="pagination-controls">
                <button
                    class="btn-pagination"
                    :disabled="page === 0"
                    @click="goToPage(page - 1)"
                >
                  ← Anterior
                </button>

                <template v-for="p in visiblePages" :key="p">
                  <button
                      :class="['btn-pagination', { active: p === page }]"
                      @click="goToPage(p)"
                  >
                    {{ p + 1 }}
                  </button>
                </template>

                <button
                    class="btn-pagination"
                    :disabled="page >= totalPages - 1"
                    @click="goToPage(page + 1)"
                >
                  Siguiente →
                </button>
              </div>

              <div class="pagination-size">
                <label for="pageSizeSelect">Registros por página:</label>
                <select id="pageSizeSelect" :value="pageSize" @change="changePageSize">
                  <option :value="20">20</option>
                  <option :value="50">50</option>
                  <option :value="100">100</option>
                  <option :value="200">200</option>
                  <option :value="500">500</option>
                </select>
              </div>
            </div>
          </div>

          <div v-else class="no-data">
            <div class="no-data-icon">📦</div>
            <p v-if="!selectedPeriodo">Por favor, selecciona un período para ver los productos.</p>
            <p v-else-if="searchQuery">No se encontraron productos con los criterios de búsqueda.</p>
            <p v-else>No hay productos registrados para este período.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal de Importación -->
    <div v-if="showImportModal" class="modal-overlay" @click.self="closeImportModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Importar Productos Multi-Almacén</h2>
          <button class="btn-close" @click="closeImportModal">&times;</button>
        </div>
        <div class="modal-body">
          <!-- Mostrar spinner durante la importación -->
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
            <p class="import-status-text">{{ importStepText || 'Importando productos...' }}</p>
            <div class="progress-bar-container">
              <div class="progress-bar-fill" :style="{ width: importProgress + '%' }"></div>
            </div>
            <div class="progress-percentage">{{ importProgress }}%</div>
          </div>

          <!-- Contenido normal del modal (oculto durante importación) -->
          <div v-if="!importing">
            <div v-if="selectedPeriodo" class="periodo-info-modal">
              <div class="info-row">
                <span class="label">Período seleccionado:</span>
                <span class="value">
                  <strong>{{ formatDate(selectedPeriodo.date) }}</strong> - {{ selectedPeriodo.comments }}
                </span>
              </div>
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
        </div>
        <div v-if="!importing" class="modal-footer">
          <button class="btn btn-secondary" @click="closeImportModal" :disabled="importing">
            Cancelar
          </button>
          <button
              class="btn btn-primary"
              @click="importarMultiAlmacen"
              :disabled="!importFile || !selectedPeriodo || importing"
          >
            <span class="icon">📥</span>
            Importar
          </button>
        </div>
      </div>
    </div>

    <!-- Modal de Resultado de Importación -->
    <div v-if="showImportResultModal && importResponse" class="modal-overlay" @click.self="closeImportResultModal">
      <div class="modal-content modal-result" :class="`status-${importStatusClass}`">
        <div class="modal-header">
          <div class="header-status">
            <span class="status-icon">{{ getImportStatusIcon(importResponse.importLog.status) }}</span>
            <h2>Resultado de Importación</h2>
          </div>
          <button class="btn-close" @click="closeImportResultModal">&times;</button>
        </div>
        <div class="modal-body">
          <!-- Información general de la importación -->
          <div class="import-summary" :style="{ borderLeftColor: getImportStatusColor(importResponse.importLog.status) }">
            <p class="summary-title">{{ importResponse.importLog.message }}</p>
            <div class="summary-details">
              <div class="detail-item">
                <span class="label">Registros procesados:</span>
                <span class="value">{{ formatNumber(importStats?.totalProcesados ?? 0) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Nuevos registros:</span>
                <span class="value" style="color:#4CAF50;font-weight:600">{{ formatNumber(importStats?.totalCreados ?? 0) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Registros actualizados:</span>
                <span class="value">{{ formatNumber(importStats?.totalActualizados ?? 0) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Almacenes nuevos:</span>
                <span class="value">{{ formatNumber(importResponse.totalAlmacenesCreados ?? 0) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Productos nuevos:</span>
                <span class="value">{{ formatNumber(importResponse.totalProductosCreados ?? 0) }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Dados de baja:</span>
                <span class="value" :style="{ color: (importStats?.totalDadosDeBaja ?? 0) > 0 ? '#FF9800' : '#4CAF50' }">
                  {{ formatNumber(importStats?.totalDadosDeBaja ?? 0) }}
                </span>
              </div>
            </div>
          </div>

          <!-- Banner de warning si hay bajas -->
          <div v-if="importResponse.tieneWarnings" class="alert alert-warning">
            ⚠️ {{ importResponse.mensajeWarning }}
          </div>

          <!-- Tabs si hay productos dados de baja -->
          <div v-if="importResponse.totalDadosDeBaja > 0" class="tabs-container">
            <div class="tabs-header">
              <button
                  class="tab-button"
                  :class="{ active: activeTab === 'productos' }"
                  @click="switchTab('productos')"
              >
                📦 Resumen
              </button>
              <button
                  class="tab-button"
                  :class="{ active: activeTab === 'bajas' }"
                  @click="switchTab('bajas')"
              >
                🗑️ Productos dados de baja ({{ importResponse.totalDadosDeBaja }})
              </button>
            </div>

            <!-- Tab: Resumen -->
            <div v-if="activeTab === 'productos'" class="tab-content">
              <p>✅ Importación completada exitosamente.</p>
            </div>

            <!-- Tab: Productos dados de baja -->
            <div v-if="activeTab === 'bajas'" class="tab-content">
              <div class="table-responsive">
                <table class="table table-bajas">
                  <thead>
                  <tr>
                    <th>Clave Producto</th>
                    <th>Nombre Producto</th>
                    <th>Clave Almacén</th>
                    <th>Nombre Almacén</th>
                    <th>Existencias Anteriores</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-for="(producto, index) in importResponse.productosDadosDeBaja" :key="index">
                    <td>
                      <span class="product-key">{{ producto.claveProducto }}</span>
                    </td>
                    <td>{{ producto.nombreProducto }}</td>
                    <td>
                      <span class="warehouse-key">{{ producto.claveAlmacen }}</span>
                    </td>
                    <td>{{ producto.nombreAlmacen }}</td>
                    <td class="text-right">
                      <span class="existencias-value">{{ formatNumber(producto.existenciasAnteriores) }}</span>
                    </td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-primary" @click="closeImportResultModal">
            Aceptar
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>


.title .icon {
  font-size: 32px;
}

/* Breadcrumb */
.breadcrumb-container {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 10px;
  background: white;
  border-bottom: 2px solid #f0f0f0;
  margin-bottom: 15px;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: none;
  border: none;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.breadcrumb-item:hover {
  background: #f5f5f5;
  color: #333;
}

.breadcrumb-item.active {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  font-weight: 600;
}

.breadcrumb-separator {
  color: #ccc;
  font-weight: 300;
}

.badge-count {
  display: inline-block;
  background: #FF9800;
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 4px;
}

.btn-bajas-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #fff3cd;
  border: 1px solid #ffc107;
  color: #856404;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-bajas-link:hover {
  background: #ffc107;
  color: white;
  box-shadow: 0 2px 8px rgba(255, 193, 7, 0.3);
}

.periodo-label-import-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.periodo-label-minimal {
  min-width: unset;
  width: auto;
  margin-bottom: 0;
  font-size: 15px;
  padding-right: 6px;
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
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.4);
}

.btn-import-inline {
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 6px;
  margin-left: 100px;
  box-shadow: none;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  gap: 6px;
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
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.periodo-info {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-container p {
  color: #6c757d;
  font-size: 16px;
}

/* Tabla */
.table-section {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table tbody tr {
  border-bottom: 1px solid #e0e0e0;
  transition: all 0.2s ease;
}

.table tbody tr:hover {
  background-color: #f8f9fa;
  transform: scale(1.001);
}

.table tbody td {
  padding: 16px;
  color: #495057;
  font-size: 14px;
}

.product-key {
  display: inline-block;
  padding: 4px 10px;
  background-color: #e3f2fd;
  color: #1976d2;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
}

.warehouse-key {
  display: inline-block;
  padding: 4px 10px;
  background-color: #f3e5f5;
  color: #7b1fa2;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
}

.text-center {
  text-align: center;
}

.existencias-badge {
  display: inline-block;
  padding: 6px 14px;
  background-color: #d4edda;
  color: #155724;
  border-radius: 8px;
  font-weight: 700;
  font-size: 15px;
}

.existencias-badge.low-stock {
  background-color: #f8d7da;
  color: #721c24;
}

.badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  text-transform: capitalize;
}


/* No Data */
.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.no-data-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.no-data p {
  font-size: 18px;
  color: #6c757d;
  margin: 0;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
  backdrop-filter: blur(3px);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
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
  padding: 24px 28px;
  border-bottom: 2px solid #e0e0e0;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  border-radius: 16px 16px 0 0;
}

.modal-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.btn-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  font-size: 28px;
  color: white;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.btn-close:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.modal-body {
  padding: 28px;
}

.periodo-info-modal {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #e3f2fd;
  border-radius: 8px;
  border-left: 4px solid #2196f3;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.info-row .label {
  font-weight: 600;
  color: #6c757d;
}

.info-row .value {
  font-size: 15px;
  color: #333;
}

.form-group {
  margin-bottom: 24px;
}

.form-control {
  width: 100%;
  padding: 12px 15px;
  font-size: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.form-control:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.form-help-text {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  color: #6c757d;
}

.required {
  color: #dc3545;
}

.file-selected {
  margin-top: 12px;
  padding: 12px 15px;
  background-color: #e8f5e9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2e7d32;
  font-weight: 500;
}

.file-selected .icon {
  font-size: 18px;
}



.alert {
  padding: 14px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-top: 15px;
}

.alert-warning {
  background-color: #fff3cd;
  color: #856404;
  border-left: 4px solid #ffc107;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 28px;
  border-top: 2px solid #e0e0e0;
  background-color: #f8f9fa;
  border-radius: 0 0 16px 16px;
}

.btn {
  padding: 10px 20px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
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
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.4);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}


.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

/* Responsive */
@media (max-width: 992px) {
  .periodo-section {
    flex-direction: column;
  }

  .periodo-info {
    width: 100%;
  }
}

@media (max-width: 768px) {


  .table {
    font-size: 13px;
  }

  .table thead th,
  .table tbody td {
    padding: 12px 8px;
  }

  .modal-content {
    width: 95%;
    margin: 10px;
  }
}

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

.page-btn {
  padding: 6px 12px;
  border-radius: 6px;
  background: #f3f3f3;
  color: #32705d;
  font-weight: 600;
  cursor: pointer;
  border: none;
  margin: 0 2px;
  transition: background 0.18s, color 0.18s;
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

.btn-pagination.active {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  border-color: #28a745;
  font-weight: bold;
}

.btn-pagination:hover:not(:disabled) {
  background: #28a745;
  color: white;
  border-color: #28a745;
}

.page-btn.active {
  background: linear-gradient(90deg, #32705d 60%, #20c997 100%);
  color: #fff;
}
.pagination-controls button {
  padding: 6px 12px;
  border-radius: 6px;
  background: #e0e0e0;
  color: #32705d;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: background 0.18s, color 0.18s;
}
.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #6c757d;
}

.pagination-size select {
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #ced4da;
  font-size: 14px;
  background: white;
}
.periodo-select-small {
  width: 180px !important;
  min-width: 120px;
  max-width: 220px;
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

/* Modal de Resultado de Importación */
.modal-result {
  max-width: 700px;
}

.modal-result.status-success .header-status {
  color: #4CAF50;
}

.modal-result.status-warning .header-status {
  color: #FF9800;
}

.modal-result.status-no-changes .header-status {
  color: #2196F3;
}

.modal-result.status-error .header-status {
  color: #F44336;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
}

.header-status .status-icon {
  font-size: 28px;
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

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.progress-percentage {
  font-size: 18px;
  font-weight: 700;
  color: #28a745;
  margin-top: 10px;
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
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
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

.tabs-container {
  margin-top: 20px;
}

.tabs-header {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid #e0e0e0;
  margin-bottom: 16px;
}

.tab-button {
  padding: 10px 16px;
  background: none;
  border: none;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
  position: relative;
  bottom: -2px;
}

.tab-button:hover {
  color: #333;
}

.tab-button.active {
  color: #28a745;
  border-bottom-color: #28a745;
}

.tab-content {
  padding: 12px 0;
}

.table-bajas {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table-bajas thead {
  background: #f5f5f5;
}

.table-bajas th {
  padding: 12px;
  text-align: left;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #ddd;
}

.table-bajas td {
  padding: 12px;
  border-bottom: 1px solid #e0e0e0;
}

.table-bajas tbody tr:hover {
  background: #f9f9f9;
}

.table-bajas .product-key {
  background: #e3f2fd;
  color: #1976d2;
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  font-size: 12px;
}

.table-bajas .warehouse-key {
  background: #f3e5f5;
  color: #7b1fa2;
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  font-size: 12px;
}

.table-bajas .existencias-value {
  color: #FF9800;
  font-weight: 700;
}

.text-right {
  text-align: right;
}
</style>
