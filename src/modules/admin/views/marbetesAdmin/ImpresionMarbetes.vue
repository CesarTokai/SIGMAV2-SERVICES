<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import SearchBar from '@/components/SearchBar.vue';
import TooltipHelp from '@/components/TooltipHelp.vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, ToastSuccess, LoadAlert } from '@/utils/SweetAlert';
import Swal from 'sweetalert2';
import { usePeriodoStore } from '@/store/periodoStore';

// ============================================
// Interfaces
// ============================================
interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface Almacen {
  id: number;
  clave: string;
  almacenname: string;
  activo: boolean;
}

interface MarbeteGenerado {
  id: number;
  productId: number;
  folio: number;
  claveProducto: string;
  producto: string;
  claveAlmacen: string;
  almacen: string;
  existencias: number;
  impreso: boolean;
  fechaImpresion: string | null;
  foliosExistentes?: number;
  primerFolio?: number;
  ultimoFolio?: number;
  folios?: number[];
}

interface PendingPrintInfo {
  periodId: number;
  warehouseId: number;
  count: number;
  periodName?: string;
  warehouseName?: string;
}

interface PdfGenerado {
  id: string;
  nombre: string;
  url: string;
  blob: Blob | null;
  fechaGeneracion: Date;
  folioInicio: number;
  folioFin: number;
}

// Store
const periodoStore = usePeriodoStore();

// ============================================
// Estado principal
// ============================================
const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacen = ref<Almacen | null>(null);
const selectedAlmacenId = ref<number | null>(null);
const marbetesGenerados = ref<MarbeteGenerado[]>([]);
const filteredMarbetes = ref<MarbeteGenerado[]>([]);
const searchQuery = ref('');
const loading = ref(false);
const userRole = ref<string>('');

// ============================================
// FASE 2 - MEJORA: Loading states específicos
// ============================================
const loadingStates = ref({
  loading: false,
  printing: false,
  consultingPending: false
});

// Estado de impresión
const pendingPrintInfo = ref<PendingPrintInfo | null>(null);
const loadingPendingCount = ref(false);

// Estado de PDFs
const pdfsGenerados = ref<PdfGenerado[]>([]);
const pdfSeleccionado = ref<PdfGenerado | null>(null);

// Paginación
const page = ref(0);
const pageSize = ref(1000);
const totalPages = ref(1);
const totalElements = ref(0);

// Estado para hover y selección de filas
const activeRowIndex = ref<number | null>(null);
const hoverRowIndex = ref<number | null>(null);

// ============================================
// FASE 2 - MEJORA: Manejo de errores específicos
// ============================================
const handleAPIError = (error: any, contexto: string = 'operación'): string => {
  const errorMessages: Record<string, string> = {
    'PERIOD_CLOSED': 'El período está cerrado. No se pueden realizar cambios.',
    'NO_PENDING_LABELS': 'No hay marbetes pendientes de impresión.',
    'INVALID_STATE': 'El marbete no está en estado válido para impresión.',
    'PERMISSION_DENIED': 'No tiene permisos para realizar esta acción.',
    'WAREHOUSE_NOT_FOUND': 'El almacén seleccionado no existe.',
    'PERIOD_NOT_FOUND': 'El período seleccionado no existe.'
  };

  let mensaje = `Error al realizar ${contexto}`;

  if (error?.response?.data) {
    const errorData = error.response.data;
    const errorCode = errorData.code || errorData.error || errorData.type;

    if (errorCode && errorMessages[errorCode]) {
      mensaje = errorMessages[errorCode];
    } else if (errorData.message) {
      mensaje = errorData.message;
    } else if (typeof errorData === 'string') {
      mensaje = errorData;
    }
  } else if (error?.message) {
    mensaje = error.message;
  }

  console.error(`❌ ${contexto}:`, error);
  return mensaje;
};

const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

    // Cargar periodo guardado del store
    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      // Usar el periodo seleccionado del store
      const periodoGuardado = periodos.value.find(p => p.id === periodoStore.periodoSeleccionado?.id);
      if (periodoGuardado) {
        selectedPeriodo.value = periodoGuardado;
        selectedPeriodoId.value = periodoGuardado.id;
        console.log('✅ Periodo cargado desde store:', periodoGuardado);
      }
    } else if (periodos.value.length > 0 && !selectedPeriodo.value) {
      // Si no hay periodo guardado, usar el primero
      const firstPeriodo = periodos.value[0];
      if (firstPeriodo) {
        selectedPeriodo.value = firstPeriodo;
        selectedPeriodoId.value = firstPeriodo.id;
      }
    }
  } catch (error) {
    console.error('Error al cargar períodos:', error);
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};

const loadAlmacenes = async () => {
  try {
    const response = await axiosConfiguration.doGet('/warehouses', {
      page: 0,
      size: 100,
      sortBy: 'warehouseKey',
      sortDir: 'asc',
      search: false
    });

    const data = response.data.data || [];
    almacenes.value = data.map((item: any) => ({
      id: item.id,
      clave: String(item.warehouseKey || ''),
      almacenname: String(item.nameWarehouse || ''),
      activo: !item.deleted
    }));

    if (almacenes.value.length > 0 && !selectedAlmacen.value) {
      const firstAlmacen = almacenes.value[0];
      if (firstAlmacen) {
        selectedAlmacen.value = firstAlmacen;
        selectedAlmacenId.value = firstAlmacen.id;
      }
    }
  } catch (error) {
    console.error('Error al cargar almacenes:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

const loadMarbetesGenerados = async () => {
  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    marbetesGenerados.value = [];
    filteredMarbetes.value = [];
    loading.value = false;
    return;
  }

  loading.value = true;
  LoadAlert(true);

  try {
    const body = {
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id,
      search: searchQuery.value || undefined,
      page: page.value,
      pageSize: pageSize.value,
      sortBy: 'folio',
      sortDir: 'ASC'
    };

    const response = await axiosConfiguration.doPost('/labels/summary', body);
    const content = response.data.content || response.data.data || response.data || [];

    marbetesGenerados.value = Array.isArray(content) ? content.map((item: any) => ({
      id: item.id ?? 0,
      productId: item.productId ?? 0,
      folio: item.folio ?? item.foliosSolicitados ?? 0,
      claveProducto: String(item.productCode ?? item.claveProducto ?? '').trim(),
      producto: String(item.nombreProducto ?? item.producto ?? '').trim(),
      claveAlmacen: String(item.warehouseKey ?? item.claveAlmacen ?? '').trim(),
      almacen: String(item.nombreAlmacen ?? item.almacen ?? '-').trim(),
      existencias: Number(item.stock ?? item.existencias ?? 0),
      impreso: Boolean(item.printed ?? item.impreso ?? false),
      fechaImpresion: item.printedDate ?? item.fechaImpresion ?? null,
      foliosExistentes: item.foliosExistentes ?? 0,
      primerFolio: item.primerFolio ?? null,
      ultimoFolio: item.ultimoFolio ?? null,
      folios: item.folios ?? []
    })) : [];

    // Aplicar búsqueda si existe
    if (searchQuery.value) {
      const normalizedQuery = normalizeSearchText(searchQuery.value);
      filteredMarbetes.value = marbetesGenerados.value.filter(marbete =>
        matchesSearch(String(marbete.folio), normalizedQuery) ||
        matchesSearch(marbete.claveProducto, normalizedQuery) ||
        matchesSearch(marbete.producto, normalizedQuery) ||
        matchesSearch(marbete.claveAlmacen, normalizedQuery) ||
        matchesSearch(marbete.almacen, normalizedQuery)
      );
    } else {
      filteredMarbetes.value = marbetesGenerados.value;
    }

    totalPages.value = response.data.totalPages || 1;
    totalElements.value = response.data.totalElements ?? marbetesGenerados.value.length;

    await consultarMarbetesPendientes();
  } catch (error) {
    console.error('Error al cargar marbetes generados:', error);
    ToastError('Error', 'No se pudieron cargar los marbetes generados');
    marbetesGenerados.value = [];
    filteredMarbetes.value = [];
  } finally {
    loading.value = false;
    LoadAlert(false);
  }
};


// ============================================
// Funciones de impresión
// ============================================
// ============================================
// FASE 2 - MEJORA: Consultar pendientes mejorado
// ============================================
const consultarMarbetesPendientes = async () => {
  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    pendingPrintInfo.value = null;
    return;
  }

  loadingPendingCount.value = true;
  loadingStates.value.consultingPending = true;

  try {
    const response = await axiosConfiguration.doPost('/labels/pending-print-count', {
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id
    });

    pendingPrintInfo.value = {
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id,
      count: response.data.count || 0,
      periodName: selectedPeriodo.value.comments,
      warehouseName: selectedAlmacen.value.almacenname
    };

    console.log(`📊 Marbetes pendientes: ${pendingPrintInfo.value.count}`);

  } catch (error) {
    const errorMessage = handleAPIError(error, 'consultar marbetes pendientes');
    console.error('❌ Error al consultar pendientes:', error);
    ToastError('Error', errorMessage);
    pendingPrintInfo.value = null;
  } finally {
    loadingPendingCount.value = false;
    loadingStates.value.consultingPending = false;
  }
};

// ============================================
// FASE 2 - MEJORA: Impresión con confirmación y validaciones
// ============================================
const imprimirMarbetesAutomatico = async () => {
  // Validaciones previas
  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    ToastError('Error', 'Debe seleccionar un período y un almacén');
    return;
  }

  // Verificar si hay pendientes
  if (!pendingPrintInfo.value || pendingPrintInfo.value.count === 0) {
    await Swal.fire({
      icon: 'info',
      title: 'Sin marbetes pendientes',
      text: 'No hay marbetes pendientes de impresión para este período y almacén.',
      confirmButtonColor: '#3085d6'
    });
    return;
  }

  // Confirmación con detalles
  const result = await Swal.fire({
    title: '🖨️ Confirmar Impresión',
    html: `
      <div style="text-align: left;">
        <p><strong>Período:</strong> ${selectedPeriodo.value.comments}</p>
        <p><strong>Almacén:</strong> ${selectedAlmacen.value.almacenname}</p>
        <hr>
        <p style="font-size: 18px; color: #28a745;">
          <strong>📊 Marbetes pendientes:</strong> ${pendingPrintInfo.value.count}
        </p>
        <hr>
        <p style="color: #666;">¿Desea generar el PDF con estos marbetes?</p>
      </div>
    `,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, imprimir',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#28a745',
    cancelButtonColor: '#6c757d'
  });

  if (!result.isConfirmed) return;

  try {
    loadingStates.value.printing = true;
    LoadAlert(true);

    const response = await axiosConfiguration.doPost('/labels/print', {
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id
    }, { responseType: 'blob' });

    // Crear blob del PDF
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);

    // Generar nombre del archivo
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
    const filename = `marbetes_P${selectedPeriodo.value.id}_A${selectedAlmacen.value.id}_${timestamp}.pdf`;

    // Crear objeto PDF generado
    const pdfGenerado: PdfGenerado = {
      id: Date.now().toString(),
      nombre: filename,
      blob: blob,
      url: url,
      fechaGeneracion: new Date(),
      folioInicio: 1,
      folioFin: pendingPrintInfo.value.count
    };

    // Agregar a la lista de PDFs generados
    pdfsGenerados.value.push(pdfGenerado);

    // Seleccionar automáticamente el PDF generado
    pdfSeleccionado.value = pdfGenerado;

    LoadAlert(false);

    // Mostrar resultado detallado
    await Swal.fire({
      icon: 'success',
      title: '✅ PDF Generado',
      html: `
        <div style="text-align: left;">
          <p>✅ <strong>Marbetes impresos:</strong> ${pendingPrintInfo.value.count}</p>
          <p>📄 <strong>Archivo:</strong> ${filename}</p>
          <hr>
          <p style="color: #666;">El PDF se ha generado y está listo para descargar.</p>
        </div>
      `,
      confirmButtonColor: '#28a745'
    });

    // Recargar datos
    await loadMarbetesGenerados();
    await consultarMarbetesPendientes();

  } catch (error: any) {
    LoadAlert(false);

    const errorMessage = handleAPIError(error, 'imprimir marbetes');

    await Swal.fire({
      icon: 'error',
      title: 'Error al imprimir',
      html: `<div style="text-align: left; white-space: pre-line;">${errorMessage}</div>`,
      confirmButtonColor: '#dc3545'
    });
  } finally {
    loadingStates.value.printing = false;
  }
};


const seleccionarPdf = (pdf: PdfGenerado) => {
  pdfSeleccionado.value = pdf;
};

const eliminarPdf = (pdf: PdfGenerado) => {
  if (pdf.url && pdf.blob) {
    window.URL.revokeObjectURL(pdf.url);
  }

  const index = pdfsGenerados.value.findIndex(p => p.id === pdf.id);
  if (index !== -1) {
    pdfsGenerados.value.splice(index, 1);
  }

  if (pdfSeleccionado.value?.id === pdf.id) {
    pdfSeleccionado.value = pdfsGenerados.value[0] || null;
  }

  ToastSuccess('Éxito', 'PDF eliminado correctamente');
};

const imprimirPdf = (pdf: PdfGenerado) => {
  const printWindow = window.open(pdf.url, '_blank');
  if (printWindow) {
    printWindow.onload = () => printWindow.print();
  }
  ToastSuccess('Éxito', 'PDF enviado a imprimir');
};

const descargarPdf = (pdf: PdfGenerado) => {
  const link = document.createElement('a');
  link.href = pdf.url;
  link.download = pdf.nombre;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  ToastSuccess('Éxito', 'PDF descargado correctamente');
};

const abrirPdfEnNuevaTab = (pdf: PdfGenerado) => {
  window.open(pdf.url, '_blank');
};

const imprimirTodosPdfs = () => {
  pdfsGenerados.value.forEach((pdf, index) => {
    setTimeout(() => imprimirPdf(pdf), index * 500);
  });
  ToastSuccess('Éxito', `${pdfsGenerados.value.length} PDF(s) enviados a imprimir`);
};

const descargarTodosPdfs = () => {
  pdfsGenerados.value.forEach((pdf, index) => {
    setTimeout(() => descargarPdf(pdf), index * 300);
  });
  ToastSuccess('Éxito', `${pdfsGenerados.value.length} PDF(s) descargados`);
};

const eliminarTodosPdfs = async () => {
  const result = await Swal.fire({
    title: '¿Eliminar todos los PDFs?',
    text: 'Esta acción eliminará todos los PDFs generados de la lista.',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: 'Sí, eliminar todos',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d'
  });

  if (result.isConfirmed) {
    pdfsGenerados.value.forEach(pdf => {
      if (pdf.url && pdf.blob) {
        window.URL.revokeObjectURL(pdf.url);
      }
    });

    pdfsGenerados.value = [];
    pdfSeleccionado.value = null;
    ToastSuccess('Éxito', 'Todos los PDFs han sido eliminados');
  }
};


// ============================================
// Funciones auxiliares
// ============================================
const formatDate = (date: string): string => {
  if (!date) return 'N/A';
  return new Date(date).toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
};

const formatDateTime = (date: string | Date | null): string => {
  if (!date) return 'No impreso';
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  return dateObj.toLocaleString('es-ES', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// ============================================
// Función para normalizar texto en búsqueda
// ============================================
const normalizeSearchText = (text: string): string => {
  return text
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ')
    .split(' ')
    .filter(word => word.length > 0)
    .join('|');
};

// Función mejorada para búsqueda flexible
const matchesSearch = (text: string, searchPattern: string): boolean => {
  if (!searchPattern || searchPattern.length === 0) return true;

  const normalizeText = (str: string): string => {
    return str
      .toLowerCase()
      .trim()
      .replace(/\s+/g, '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^\w]/g, '');
  };

  const normalizedText = normalizeText(text);
  const patterns = searchPattern
    .split('|')
    .map(p => normalizeText(p))
    .filter(p => p.length > 0);

  return patterns.some(pattern => normalizedText.includes(pattern));
};

// ============================================
// Watchers y filtros
// ============================================
watch(searchQuery, (newQuery) => {
  if (marbetesGenerados.value.length === 0) {
    filteredMarbetes.value = [];
    return;
  }

  const normalizedQuery = normalizeSearchText(newQuery);
  filteredMarbetes.value = normalizedQuery
    ? marbetesGenerados.value.filter(marbete =>
        matchesSearch(String(marbete.folio), normalizedQuery) ||
        matchesSearch(marbete.claveProducto, normalizedQuery) ||
        matchesSearch(marbete.producto, normalizedQuery) ||
        matchesSearch(marbete.claveAlmacen, normalizedQuery) ||
        matchesSearch(marbete.almacen, normalizedQuery)
      )
    : marbetesGenerados.value;
});

watch(selectedPeriodoId, async (newId) => {
  if (newId == null) {
    selectedPeriodo.value = null;
    marbetesGenerados.value = [];
    filteredMarbetes.value = [];
    pendingPrintInfo.value = null;
    return;
  }

  selectedPeriodo.value = periodos.value.find(p => p.id === Number(newId)) || null;

  // Guardar en el store cuando cambie el periodo
  if (selectedPeriodo.value) {
    periodoStore.setPeriodo(selectedPeriodo.value);
    console.log('✅ Periodo guardado en store:', selectedPeriodo.value);
  }

  searchQuery.value = '';
  page.value = 0;
  await loadMarbetesGenerados();
  await consultarMarbetesPendientes();
});

watch(selectedAlmacenId, async (newId) => {
  if (newId == null) {
    selectedAlmacen.value = null;
    marbetesGenerados.value = [];
    filteredMarbetes.value = [];
    pendingPrintInfo.value = null;
    return;
  }

  selectedAlmacen.value = almacenes.value.find(a => a.id === Number(newId)) || null;
  searchQuery.value = '';
  page.value = 0;
  await loadMarbetesGenerados();
  await consultarMarbetesPendientes();
});


// ============================================
// Paginación
// ============================================
const goToPage = async (newPage: number) => {
  if (newPage >= 0 && newPage < totalPages.value) {
    page.value = newPage;
    await loadMarbetesGenerados();
  }
};

const changePageSize = async (event: Event) => {
  pageSize.value = Number((event.target as HTMLSelectElement).value);
  page.value = 0;
  await loadMarbetesGenerados();
};


// ============================================
// Computed
// ============================================
const validMarbetes = computed(() =>
  marbetesGenerados.value.filter(m => m.folio && m.folio > 0)
);

// ============================================
// Lifecycle
// ============================================
onMounted(async () => {
  userRole.value = localStorage.getItem('role') || '';
  await loadPeriodos();
  await loadAlmacenes();

  if (selectedPeriodo.value && selectedAlmacen.value) {
    await consultarMarbetesPendientes();
  }
});
</script>

<template>
  <div class="impresion-marbetes">
    <div class="section-card">
    <div class="title-section">
        <h1 class="section-title">
          Impresion de Marbetes
        </h1>
        <p class="subtitle">Generacion de marbetes y impresion</p>
      </div>
      <div class="filters-wrapper">
        <div class="selectors-group">
          <!-- Período -->
          <div class="filter-item">
            <label for="periodoSelect" class="form-label"><strong>Período:</strong></label>
            <select id="periodoSelect" v-model.number="selectedPeriodoId" class="form-select">
              <option :value="null" disabled>Selecciona un período</option>
              <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
                {{ formatDate(periodo.date) }} - {{ periodo.comments }}
              </option>
            </select>
          </div>

          <!-- Almacén -->
          <div class="filter-item">
            <label for="almacenSelect" class="form-label"><strong>Almacén:</strong></label>
            <select id="almacenSelect" v-model.number="selectedAlmacenId" class="form-select" :disabled="userRole === 'ALMACENISTA'">
              <option :value="null" disabled>Selecciona un almacén</option>
              <option v-for="almacen in almacenes" :key="almacen.id" :value="almacen.id">
                {{ almacen.clave }} - {{ almacen.almacenname }}
              </option>
            </select>
            <small v-if="userRole === 'ALMACENISTA'" class="form-help-text">Almacén asignado</small>
          </div>

          <div v-if="selectedPeriodo && selectedAlmacen" class="filter-info">
            <div class="info-item">
              <span class="label">
                Marbetes Pendientes
                <TooltipHelp text="Cantidad de marbetes generados que aún no han sido impresos." />
              </span>
              <span v-if="loadingPendingCount" class="spinner-small"></span>
              <span v-else-if="pendingPrintInfo" :class="['value', 'pending-count', { 'has-pending': pendingPrintInfo.count > 0, 'no-pending': pendingPrintInfo.count === 0 }]">
                {{ pendingPrintInfo.count }}
              </span>
              <span v-else class="value">0</span>
            </div>
          </div>
        </div>

        <div class="searchbar-group">
          <!-- FASE 2: Mensaje de ayuda -->
          <div v-if="pendingPrintInfo && pendingPrintInfo.count > 0" class="help-message help-message-success">
            <span class="help-icon">🖨️</span>
            <span class="help-text">
              <strong>Listo para imprimir:</strong> Hay {{ pendingPrintInfo.count }} marbete(s) pendiente(s) de impresión.
            </span>
          </div>

          <!-- BUSCADOR MEJORADO DIRECTO -->
          <div class="search-input-wrapper">
            <input
              v-model="searchQuery"
              type="text"
              class="search-input"
              placeholder="Buscar por folio, producto, almacén..."
            />
            <span class="search-icon bi bi-search"></span>
          </div>

          <button
            class="btn btn-primary btn-generate"
            @click="imprimirMarbetesAutomatico"
            :disabled="!selectedPeriodo || !selectedAlmacen || loadingStates.printing || !(pendingPrintInfo && pendingPrintInfo.count > 0)"
          >
            <span v-if="loadingStates.printing">Imprimiendo...</span>
            <span v-else-if="pendingPrintInfo && pendingPrintInfo.count > 0">
               Imprimir {{ pendingPrintInfo.count }} Marbete{{ pendingPrintInfo.count > 1 ? 's' : '' }}
            </span>
            <span v-else>Sin Marbetes</span>
          </button>
        </div>
      </div>
    </div>

    <div class="section-card">
      <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
        <p>Cargando marbetes...</p>
      </div>

      <div v-else-if="filteredMarbetes.length > 0" class="table-section">
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Folio</th>
                <th>Clave Producto</th>
                <th>Producto</th>
                <th>Clave Almacén</th>
                <th>Almacén</th>
                <th>Existencias</th>
                <th>Rango Folios</th>
                <th>Impreso</th>
                <th>Fecha Impresión</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(marbete, index) in filteredMarbetes"
                :key="marbete.id"
                :class="{
                  'active-row': activeRowIndex === index,
                  'hover-row': hoverRowIndex === index || activeRowIndex === index
                }"
                @mouseenter="hoverRowIndex = index"
                @mouseleave="hoverRowIndex = null"
                tabindex="0"
              >
                <td>{{ marbete.folio }}</td>
                <td>{{ marbete.claveProducto }}</td>
                <td>{{ marbete.producto }}</td>
                <td>{{ marbete.claveAlmacen }}</td>
                <td>{{ marbete.almacen }}</td>
                <td>{{ marbete.existencias }}</td>
                <td> {{ marbete.ultimoFolio }}</td>
                <td>{{ marbete.impreso ? 'Sí' : 'No' }}</td>
                <td>{{ marbete.fechaImpresion }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Paginador -->
        <div class="pagination-section">
          <div class="pagination-info">
            Página {{ page + 1 }} de {{ totalPages }} ({{ totalElements }} registros)
          </div>
          <div class="pagination-controls">
            <button :disabled="page === 0" @click="goToPage(0)">« Primera</button>
            <button :disabled="page === 0" @click="goToPage(page - 1)">‹ Anterior</button>
            <span class="page-indicator">{{ page + 1 }} / {{ totalPages }}</span>
            <button :disabled="page === totalPages - 1" @click="goToPage(page + 1)">Siguiente ›</button>
            <button :disabled="page === totalPages - 1" @click="goToPage(totalPages - 1)">Última »</button>
          </div>
          <div class="pagination-size">
            <label for="pageSizeSelect">Registros por página:</label>
            <select id="pageSizeSelect" :value="pageSize" @change="changePageSize">
              <option :value="20">20</option>
              <option :value="50">50</option>
              <option :value="100">100</option>
              <option :value="200">200</option>
            </select>
          </div>
        </div>
      </div>

      <div v-else class="no-data">
        <div class="no-data-icon">📦</div>
        <p v-if="!selectedPeriodo || !selectedAlmacen">
          Selecciona un período y almacén para ver los marbetes generados.
        </p>
        <p v-else-if="searchQuery">
          No se encontraron marbetes con los criterios de búsqueda.
        </p>
        <p v-else>
          No hay marbetes generados para este período y almacén.
        </p>
      </div>
    </div>
    <!-- Sección de PDFs Generados -->
    <div class="section-card pdf-section" v-if="pdfsGenerados.length > 0">
      <div class="section-header">
        <div class="title-section">
          <h2 class="section-title">
            <span class="icon">📄</span>
            PDFs Generados
          </h2>
          <span class="pdf-count-badge">{{ pdfsGenerados.length }}</span>
        </div>
        <div class="header-actions">
          <button class="btn btn-info btn-sm" @click="imprimirTodosPdfs" title="Imprimir todos">
            <span class="icon">🖨️</span>
            Imprimir Todos
          </button>
          <button class="btn btn-success btn-sm" @click="descargarTodosPdfs" title="Descargar todos">
            <span class="icon">💾</span>
            Descargar Todos
          </button>
          <button class="btn btn-danger btn-sm" @click="eliminarTodosPdfs" title="Eliminar todos">
            <span class="icon">🗑️</span>
            Eliminar Todos
          </button>
        </div>
      </div>

      <div class="pdf-viewer-container">

        <!-- Visor de PDF -->
        <div class="pdf-preview">
          <div v-if="pdfSeleccionado" class="pdf-preview-content">
            <div class="pdf-preview-header">
              <div class="pdf-preview-title-group">
                <h3 class="pdf-preview-title">{{ pdfSeleccionado.nombre }}</h3>
                <small class="pdf-preview-subtitle">{{ pdfSeleccionado.folioFin }} marbetes generados</small>
              </div>

            </div>
            <iframe
              :src="pdfSeleccionado.url"
              class="pdf-iframe"
              title="Previsualización de PDF"
            ></iframe>
          </div>
          <div v-else class="pdf-preview-empty">
            <div class="empty-icon">📄</div>
            <p>Selecciona un PDF de la lista para previsualizarlo</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title .icon {
  font-size: 24px;
}

.title-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}



.subtitle {
  font-size: 14px;
  color: #6c757d;
  margin: 0;
  text-align: center;
}

.btn-generate {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-generate:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-generate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.filters-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  flex-wrap: wrap;
  width: 100%;
}

.selectors-group {
  display: flex;
  align-items: flex-end;
  gap: 18px;
  flex-wrap: wrap;
}

.searchbar-group {
  min-width: 320px;
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.filter-item {
  display: flex;
  flex-direction: column;
  min-width: 180px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #495057;
}

.form-select {
  width: 100%;
  padding: 10px 15px;
  font-size: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background-color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.form-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-select:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.form-help-text {
  margin-top: 5px;
  font-size: 12px;
  color: #6c757d;
  font-style: italic;
}

.filter-info {
  display: flex;
  flex-direction: row;
  gap: 18px;
  margin-left: 18px;
  align-items: center;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  font-size: 13px;
  color: #6c757d;
  font-weight: 500;
}

.info-item .value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.info-item .value.has-pending {
  color: #28a745;
}

.spinner-small {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #28a745;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}


.section-card {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 5px;
}



.header-actions {
  display: flex;
  gap: 8px;
}

.btn {
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-info {
  background: #17a2b8;
  color: white;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
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

.table-section {
  overflow: hidden;
}

.table-responsive {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table thead {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.table thead th {
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 1px;
  white-space: nowrap;
}

.table tbody tr {
  border-bottom: 1px solid #e0e0e0;
  transition: all 0.2s ease;
}

.table tbody tr:hover,
.hover-row {
  background-color: #f8f9fa;
}

.table tbody tr.active-row {
  background-color: #e3f0ff;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

.table tbody td {
  padding: 16px;
  color: #495057;
  font-size: 14px;
}


.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
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

/* ============================================
   Paginación
   ============================================ */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-top: 2px solid #e0e0e0;
  flex-wrap: wrap;
  gap: 15px;
}

.pagination-info {
  font-size: 14px;
  color: #6c757d;
}

.pagination-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.pagination-controls button {
  padding: 8px 16px;
  font-size: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: white;
  color: #495057;
  cursor: pointer;
  transition: all 0.3s ease;
}

.pagination-controls button:hover:not(:disabled) {
  background-color: #667eea;
  color: white;
  border-color: #667eea;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  padding: 8px 16px;
  font-weight: 600;
  color: #667eea;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-size label {
  font-size: 14px;
  color: #6c757d;
}

.pagination-size select {
  padding: 6px 12px;
  font-size: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: white;
  cursor: pointer;
}

/* ============================================
   PDFs - Sección de visualización
   ============================================ */
.pdf-section {
  margin-top: 1.5rem;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 6px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background: white;
  border-bottom: 1px solid #e9ecef;
}




.pdf-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  background: #28a745;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn .icon {
  font-size: 16px;
}

.pdf-viewer-container {
  display: grid;
  grid-template-columns: 1500px 1fr;
  gap: 20px;
  height: 900px;
  padding: 1.5rem;
}

/* Lista de PDFs */
.pdf-list {
  border: none;
  border-radius: 0;
  overflow-y: auto;
  background: white;
  box-shadow: none;
}

.pdf-list::-webkit-scrollbar {
  width: 6px;
}

.pdf-list::-webkit-scrollbar-track {
  background: transparent;
}

.pdf-list::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.pdf-list::-webkit-scrollbar-thumb:hover {
  background: #999;
}

.pdf-list-header {
  padding: 12px 15px;
  border-bottom: 1px solid #e9ecef;
  background: white;
  position: sticky;
  top: 0;
  z-index: 10;
}

.pdf-list-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.pdf-list-header small {
  font-size: 12px;
  color: #6c757d;
}


.pdf-item-details small {
  font-size: 12px;
  color: #6c757d;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* Visor de PDF */
.pdf-preview {
  border: 1px solid #e9ecef;
  border-radius: 6px;
  overflow: hidden;
  background: white;
  box-shadow: none;
}

.pdf-preview-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
}

.pdf-preview-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
}

.pdf-preview-title-group {
  flex: 1;
  min-width: 0;
}

.pdf-preview-title {
  margin: 0 0 2px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pdf-preview-subtitle {
  font-size: 12px;
  color: #999;
  font-weight: 400;
}

.pdf-preview-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 12px;
}

.pdf-iframe {
  flex: 1;
  border: none;
  width: 100%;
  background: #f8f9fa;
}

.pdf-preview-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #6c757d;
  background: white;
}

.empty-icon {
  font-size: 80px;
  opacity: 0.2;
  margin-bottom: 12px;
}

.pdf-preview-empty p {
  font-size: 14px;
  color: #999;
  margin: 0;
}


/* ============================================
   Responsive
   ============================================ */
@media (max-width: 1024px) {
  .pdf-viewer-container {
    grid-template-columns: 280px 1fr;
    height: 650px;
  }



  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .filters-wrapper {
    flex-direction: column;
    gap: 15px;
  }

  .selectors-group {
    width: 100%;
  }

  .searchbar-group {
    width: 100%;
    min-width: auto;
    flex-direction: column;
  }

  .SearchBar {
    width: 100%;
  }

  .btn-generate {
    width: 100%;
    justify-content: center;
  }

  .pdf-viewer-container {
    grid-template-columns: 1fr;
    height: auto;
    gap: 15px;
  }

  .pdf-list {
    height: 350px;
  }

  .pdf-preview {
    height: 550px;
  }

  .pdf-preview-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .pdf-preview-actions {
    width: 100%;
    margin-left: 0;
  }

  .pdf-preview-actions .btn {
    flex: 1;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .header-actions .btn {
    flex: 1;
    min-width: 140px;
  }
}

@media (max-width: 480px) {
  .filter-item {
    min-width: 100%;
  }

  .info-item .value {
    font-size: 16px;
  }


  .pdf-list {
    height: 300px;
  }

  .pdf-item {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }

  .pdf-item-actions {
    width: 100%;
    justify-content: space-around;
    margin-left: 0;
  }

  .action-btn {
    flex: 1;
  }
}

/* ============================================ */
/* FASE 2 - Estilos para mejoras */
/* ============================================ */

/* Contador de pendientes mejorado */
.pending-count {
  font-size: 24px;
  font-weight: 700;
  padding: 8px 16px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 50px;
  transition: all 0.3s ease;
}

.pending-count.has-pending {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
  animation: pulseGlow 2s infinite;
}

.pending-count.no-pending {
  background: #e9ecef;
  color: #6c757d;
}

@keyframes pulseGlow {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
    transform: scale(1);
  }
  50% {
    box-shadow: 0 6px 20px rgba(40, 167, 69, 0.5);
    transform: scale(1.05);
  }
}

/* Mensajes de ayuda con variantes */
.help-message {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  color: white;
  font-size: 13px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  animation: slideInRight 0.5s ease;
  max-width: 600px;
}

.help-message-success {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.help-message-info {
  background: linear-gradient(135deg, #17a2b8 0%, #138496 100%);
}

.help-message-warning {
  background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
}

.help-icon {
  font-size: 18px;
  animation: pulse 2s infinite;
}

.help-text {
  line-height: 1.5;
}

.help-text strong {
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

/* Spinner pequeño en línea */
.spinner-small {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(0, 123, 255, 0.2);
  border-radius: 50%;
  border-top-color: #007bff;
  animation: spin 0.8s linear infinite;
  margin: 0 8px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Botón de impresión mejorado */
.btn-generate {
  position: relative;
  transition: all 0.3s ease;
  font-weight: 600;
  min-width: 200px;
}

.btn-generate:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 123, 255, 0.4);
}

.btn-generate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Responsive para mensajes de ayuda */
@media (max-width: 768px) {
  .help-message {
    flex-direction: column;
    text-align: center;
    max-width: 100%;
  }
}

/* BUSCADOR MEJORADO DIRECTO */
.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  max-width: 300px;
}

.search-input {
  width: 100%;
  padding: 10px 36px 10px 12px;
  font-size: 14px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background: #fafbfc;
  color: #333;
  outline: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.search-input:focus {
  border-color: #667eea;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.search-input::placeholder {
  color: #999;
}

.search-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #667eea;
  font-size: 16px;
  pointer-events: none;
  opacity: 0.7;
}
</style>



