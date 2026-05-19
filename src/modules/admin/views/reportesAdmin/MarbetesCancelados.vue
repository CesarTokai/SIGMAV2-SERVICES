<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { usePeriodoStore } from '@/store/periodoStore';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, LoadAlert } from '@/utils/SweetAlert';
import { exportReportToPDF } from '@/utils/pdfExportHelper';

interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface Almacen {
  id: number;
  clave: string;
  nombre: string;
  activo: boolean;
}

interface MarbeteCancelado {
  numeroMarbete: number;
  claveProducto: string;
  descripcionProducto: string;
  unidad: string;
  claveAlmacen: string;
  nombreAlmacen: string;
  conteo1: number | null;
  conteo2: number | null;
  motivoCancelacion: string;
  canceladoPor: string;
  canceladoAt: string;
}

const periodoStore = usePeriodoStore();

// Estado
const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacen = ref<Almacen | null>(null);
const selectedAlmacenId = ref<number | null>(null);
const marbetesCancelados = ref<MarbeteCancelado[]>([]);
const filteredMarbetes = ref<MarbeteCancelado[]>([]);


const searchQuery = ref('');
const loading = ref(false);
const exporting = ref(false);

// Paginación
const page = ref(0);
const pageSize = ref(50);
const totalPages = ref(1);
const totalElements = ref(0);

// Estadísticas
const estadisticas = ref({
  totalConDiferencia: 0,
  totalDiferenciaHoy: 0,
});

// Cargar períodos
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

    // Cargar periodo guardado del store
    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      selectedPeriodo.value = periodoStore.periodoSeleccionado;
      selectedPeriodoId.value = periodoStore.periodoSeleccionado.id;
    } else if (periodos.value.length > 0 && !selectedPeriodo.value) {
      selectedPeriodo.value = periodos.value[0] || null;
      selectedPeriodoId.value = selectedPeriodo.value ? selectedPeriodo.value.id : null;
      if (selectedPeriodo.value) {
        periodoStore.setPeriodo(selectedPeriodo.value);
      }
    }
  } catch (error) {
    console.error('Error al cargar períodos:', error);
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};

// Cargar almacenes
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
      nombre: String(item.nameWarehouse || ''),
      activo: !item.deleted
    }));

    // Agregar opción "Todos los almacenes"
    almacenes.value.unshift({
      id: 0,
      clave: 'TODOS',
      nombre: 'Todos los almacenes',
      activo: true
    });

    if (almacenes.value.length > 0 && !selectedAlmacen.value) {
      selectedAlmacen.value = almacenes.value[0] || null;
      selectedAlmacenId.value = selectedAlmacen.value ? selectedAlmacen.value.id : null;
    }
  } catch (error) {
    console.error('Error al cargar almacenes:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

// Manejar cambio de almacén
const handleAlmacenChange = () => {
  if (selectedAlmacenId.value !== null) {
    selectedAlmacen.value = almacenes.value.find(a => a.id === selectedAlmacenId.value) || null;
  }
  loadMarbetesCancelados();
};

// Manejar cambio de periodo
const handlePeriodoChange = () => {
  if (selectedPeriodoId.value) {
    selectedPeriodo.value = periodos.value.find(p => p.id === selectedPeriodoId.value) || null;
    if (selectedPeriodo.value) {
      periodoStore.setPeriodo(selectedPeriodo.value);
    }
  }
  loadMarbetesCancelados();
};

// Cargar marbetes cancelados
const loadMarbetesCancelados = async () => {
  if (!selectedPeriodo.value) {
    marbetesCancelados.value = [];
    filteredMarbetes.value = [];
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    LoadAlert(true);
    const body = {
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value?.id === 0 ? null : selectedAlmacen.value?.id
    };
    const response = await axiosConfiguration.doPost('/labels/reports/cancelled', body);
    // Mostrar los resultados crudos en consola
    console.log('Resultados crudos de la API:', response.data);
    const content = Array.isArray(response.data) ? response.data : [];
    marbetesCancelados.value = content.map((item: any) => ({
      numeroMarbete: Number(item.numeroMarbete ?? 0),
      claveProducto: String(item.claveProducto ?? ''),
      descripcionProducto: String(item.descripcionProducto ?? ''),
      unidad: String(item.unidad ?? ''),
      claveAlmacen: String(item.claveAlmacen ?? ''),
      nombreAlmacen: String(item.nombreAlmacen ?? ''),
      conteo1: item.conteo1 !== null && item.conteo1 !== undefined ? Number(item.conteo1) : null,
      conteo2: item.conteo2 !== null && item.conteo2 !== undefined ? Number(item.conteo2) : null,
      motivoCancelacion: String(item.motivoCancelacion ?? ''),
      canceladoPor: String(item.canceladoPor ?? ''),
      canceladoAt: String(item.canceladoAt ?? '')
    }));
    applySearchFilter();
    LoadAlert(false);
  } catch (error) {
    LoadAlert(false);
    ToastError('Error', 'No se pudieron cargar los marbetes cancelados');
    marbetesCancelados.value = [];
    filteredMarbetes.value = [];
  } finally {
    loading.value = false;
  }
};

// Normalizar texto para búsqueda flexible
const normalizeSearchText = (text: string): string => {
  return text
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ')  // Remover espacios múltiples
    .normalize('NFD')       // Remover tildes/acentos
    .replace(/[\u0300-\u036f]/g, '');
};

// Función mejorada de búsqueda flexible
const matchesSearch = (text: string, query: string): boolean => {
  const normalizedText = normalizeSearchText(text);
  const normalizedQuery = normalizeSearchText(query);

  // Búsqueda exacta normalizada
  if (normalizedText.includes(normalizedQuery)) return true;

  // Búsqueda por palabras individuales
  const queryWords = normalizedQuery.split(' ').filter(w => w.length > 0);
  return queryWords.every(word => normalizedText.includes(word));
};

// Aplicar filtro de búsqueda mejorado
const applySearchFilter = () => {
  let filtered = [...marbetesCancelados.value];
  if (searchQuery.value) {
    filtered = filtered.filter(m =>
      matchesSearch(m.numeroMarbete.toString(), searchQuery.value) ||
      matchesSearch(m.descripcionProducto, searchQuery.value) ||
      matchesSearch(m.claveProducto, searchQuery.value) ||
      matchesSearch(m.unidad, searchQuery.value) ||
      matchesSearch(m.nombreAlmacen, searchQuery.value) ||
      matchesSearch(m.claveAlmacen, searchQuery.value) ||
      matchesSearch(m.motivoCancelacion, searchQuery.value) ||
      matchesSearch(m.canceladoPor, searchQuery.value)
    );
  }
  totalElements.value = filtered.length;
  totalPages.value = Math.ceil(totalElements.value / pageSize.value);
  const start = page.value * pageSize.value;
  const end = start + pageSize.value;
  filteredMarbetes.value = filtered.slice(start, end);
};

// Calcular estadísticas
const calcularEstadisticas = () => {
  estadisticas.value.totalConDiferencia = marbetesCancelados.value.length;
};

// Formatear fecha de manera robusta para evitar desfase de día
const formatDate = (date: string | undefined): string => {
  if (!date) return 'N/A';
  // Caso: 'YYYY-MM-DD' puro
  if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    const [y, m, d] = date.split('-');
    const dateObj = new Date(Number(y), Number(m) - 1, Number(d));
    return dateObj.toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' });
  }
  // Caso: ISO o con zona horaria, extraer solo la parte de la fecha
  if (/^\d{4}-\d{2}-\d{2}T/.test(date)) {
    const match = date.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (match) {
      const [_, y, m, d] = match;
      const dateObj = new Date(Number(y), Number(m) - 1, Number(d));
      return dateObj.toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' });
    }
    // fallback
    const dateObj = new Date(date);
    return dateObj.toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' });
  }
  try {
    const dateObj = new Date(date);
    return dateObj.toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' });
  } catch {
    return date;
  }
};

const formatDateTime = (datetime: string): string => {
  if (!datetime) return 'N/A';
  return new Date(datetime).toLocaleDateString('es-ES', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const formatNumber = (value: number | null): string => {
  if (value === null || value === undefined) return '-';
  return new Intl.NumberFormat('es-MX', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
};

// Paginación
const goToPage = (newPage: number) => {
  if (newPage < 0 || newPage >= totalPages.value) return;
  page.value = newPage;
  applySearchFilter();
};

const changePageSize = (event: Event) => {
  pageSize.value = Number((event.target as HTMLSelectElement).value);
  page.value = 0;
  applySearchFilter();
};

// Manejar búsqueda
const handleSearch = () => {
  page.value = 0;
  applySearchFilter();
};

// Exportar a PDF
const exportToPDF = async () => {
  if (!selectedPeriodo.value) {
    ToastError('Error', 'Selecciona un período para exportar');
    return;
  }

  await exportReportToPDF({
    endpoint: '/labels/reports/cancelled/pdf',
    periodId: selectedPeriodo.value.id,
    warehouseId: selectedAlmacen.value?.id === 0 ? null : selectedAlmacen.value?.id,
    fileName: `marbetes-cancelados-${selectedPeriodo.value.id}.pdf`,
    onStart: () => { exporting.value = true; },
    onComplete: () => { exporting.value = false; }
  });
};

onMounted(() => {
  loadPeriodos();
  loadAlmacenes();
  loadMarbetesCancelados();
});

// Watchers para recargar datos automáticamente
watch(
  () => selectedPeriodo.value?.id,
  () => {
    if (selectedPeriodo.value && selectedAlmacen.value) {
      loadMarbetesCancelados();
    }
  }
);

watch(
  () => selectedAlmacen.value?.id,
  () => {
    if (selectedPeriodo.value && selectedAlmacen.value) {
      loadMarbetesCancelados();
    }
  }
);
</script>

<template>
  <div class="reporte-container">

    <!-- Header -->
    <div class="header-section">
      <div class="title-wrapper">
        <h1 class="page-title">Marbetes Cancelados</h1>
        <p class="subtitle">Lista los marbetes que han sido cancelados, permitiendo el seguimiento y control de anulaciones.</p>
      </div>
    </div>

    <!-- Filtros -->
    <div class="controls-section">
      <div class="filter-item">
        <label class="filter-label">Período</label>
        <select v-model.number="selectedPeriodoId" class="filter-select" @change="handlePeriodoChange">
          <option :value="null" disabled>Selecciona un período</option>
          <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
            {{ formatDate(periodo.date) }} - {{ periodo.comments }}
          </option>
        </select>
      </div>
      <div class="filter-item">
        <label class="filter-label">Almacén</label>
        <select v-model.number="selectedAlmacenId" class="filter-select" @change="handleAlmacenChange">
          <option v-for="almacen in almacenes" :key="almacen.id" :value="almacen.id">
            {{ almacen.clave === 'TODOS' ? almacen.nombre : `${almacen.clave} - ${almacen.nombre}` }}
          </option>
        </select>
      </div>
      <div class="search-wrapper">
        <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
        </svg>
        <input type="text" v-model="searchQuery" @input="handleSearch" class="search-input" placeholder="Buscar por folio, producto, almacén..." />
      </div>
      <div class="stats-mini">
        <div class="stat-mini danger">
          <span class="stat-mini-value">{{ estadisticas.totalConDiferencia }}</span>
          <span class="stat-mini-label">Total cancelados</span>
        </div>
        <button
          @click="exportToPDF"
          :disabled="exporting || !selectedPeriodo"
          class="btn-export-pdf"
          title="Exportar reporte a PDF"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2m0 0v-8m0 8H3m0 0h18"/>
          </svg>
          {{ exporting ? 'Exportando...' : 'PDF' }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>Cargando marbetes cancelados...</p>
    </div>

    <!-- Tabla -->
    <div v-else-if="filteredMarbetes.length > 0" class="table-section">
      <table class="data-table">
        <thead>
          <tr>
            <th>Folio</th>
            <th>Producto</th>
            <th>Almacén</th>
            <th class="text-center">Conteo 1</th>
            <th class="text-center">Conteo 2</th>
            <th>Motivo Cancelación</th>
            <th>Cancelado Por</th>
            <th>Fecha Cancelación</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="marbete in filteredMarbetes" :key="marbete.numeroMarbete">
            <td><span class="folio-badge">{{ marbete.numeroMarbete }}</span></td>
            <td>
              <div class="product-info">
                <strong>{{ marbete.descripcionProducto }}</strong>
                <small>{{ marbete.claveProducto }} - {{ marbete.unidad }}</small>
              </div>
            </td>
            <td>
              <div class="warehouse-info">
                <span class="warehouse-key">{{ marbete.claveAlmacen }}</span>
                <small>{{ marbete.nombreAlmacen }}</small>
              </div>
            </td>
            <td class="text-center"><span class="number-badge">{{ marbete.conteo1 !== null ? formatNumber(marbete.conteo1) : '-' }}</span></td>
            <td class="text-center"><span class="number-badge">{{ marbete.conteo2 !== null ? formatNumber(marbete.conteo2) : '-' }}</span></td>
            <td><span class="motivo-text">{{ marbete.motivoCancelacion || '-' }}</span></td>
            <td><span class="user-email">{{ marbete.canceladoPor || '-' }}</span></td>
            <td><span class="date-text">{{ formatDateTime(marbete.canceladoAt) }}</span></td>
          </tr>
        </tbody>
      </table>

      <!-- Paginador -->
      <div class="pagination-section">
        <div class="pagination-info">Página {{ page + 1 }} de {{ totalPages }} ({{ totalElements }} registros)</div>
        <div class="pagination-controls">
          <button :disabled="page === 0" @click="goToPage(0)">« Primera</button>
          <button :disabled="page === 0" @click="goToPage(page - 1)">‹ Anterior</button>
          <span class="page-indicator">{{ page + 1 }} / {{ totalPages }}</span>
          <button :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">Siguiente ›</button>
          <button :disabled="page >= totalPages - 1" @click="goToPage(totalPages - 1)">Última »</button>
        </div>
        <div class="pagination-size">
          <label>Registros por página:</label>
          <select :value="pageSize" @change="changePageSize">
            <option :value="20">20</option><option :value="50">50</option>
            <option :value="100">100</option><option :value="200">200</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Sin datos -->
    <div v-else class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
      </svg>
      <p v-if="!selectedPeriodo">Selecciona un período para ver los marbetes cancelados.</p>
      <p v-else>No hay marbetes cancelados en este período y almacén.</p>
    </div>

  </div>
</template>

<style scoped>
.reporte-container {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.title-wrapper {
  text-align: center;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.subtitle {
  font-size: 14px;
  color: #6c757d;
  margin: 0;
}

.controls-section {
  display: flex;
  align-items: flex-end;
  gap: 18px;
  background: #ffffff;
  border-radius: 10px;
  padding: 10px 10px 10px 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  min-width: 160px;
}

.filter-label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #495057;
}

.filter-select {
  width: 100%;
  padding: 8px 12px;
  font-size: 14px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background-color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.search-wrapper {
  position: relative;
  flex: 1 1 220px;
  display: flex;
  align-items: flex-end;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  color: #495057;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 36px;
  font-size: 14px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background-color: white;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.stats-mini {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.stat-mini {
  padding: 10px 12px;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.stat-mini:hover {
  border-color: #dee2e6;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.stat-mini.danger {
  border-left: 3px solid #dc3545;
}

.stat-mini.warning {
  border-left: 3px solid #ffc107;
}

.stat-mini.info {
  border-left: 3px solid #17a2b8;
}


.stat-mini-value {
  font-size: 18px;
  font-weight: 600;
  color: #212529;
  line-height: 1;
}

.stat-mini-label {
  font-size: 12px;
  color: #868e96;
  font-weight: 400;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.btn-export-pdf {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.btn-export-pdf svg {
  width: 18px;
  height: 18px;
  stroke-width: 2;
}

.btn-export-pdf:hover:not(:disabled) {
  background: linear-gradient(135deg, #c82333 0%, #bd2130 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(220, 53, 69, 0.3);
}

.btn-export-pdf:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-export-pdf.btn-export-all {
  background: linear-gradient(135deg, #17a2b8 0%, #138496 100%);
}

.btn-export-pdf.btn-export-all:hover:not(:disabled) {
  background: linear-gradient(135deg, #138496 0%, #0d6674 100%);
  box-shadow: 0 4px 12px rgba(23, 162, 184, 0.3);
}

.table-section {
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
}

.data-table thead th {
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.data-table tbody tr {
  border-bottom: 1px solid #e0e0e0;
  transition: all 0.2s ease;
}

.data-table tbody tr:hover {
  background-color: #fff5f5;
}

.data-table tbody tr.row-cancelled {
  background-color: #fdf2f2;
}

.data-table tbody td {
  padding: 16px;
  color: #495057;
  font-size: 14px;
}

.folio-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 15px;
}

.folio-badge.cancelled {
  background-color: #f8d7da;
  color: #721c24;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-info small {
  color: #6c757d;
  font-size: 12px;
}

.warehouse-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.warehouse-info small {
  color: #6c757d;
  font-size: 12px;
}

.number-badge {
  display: inline-block;
  padding: 6px 12px;
  background-color: #e9ecef;
  color: #495057;
  border-radius: 6px;
  font-weight: 600;
  font-size: 14px;
}

.diferencia-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 14px;
}

.diferencia-badge.positivo {
  background-color: #fff3cd;
  color: #856404;
}

.diferencia-badge.negativo {
  background-color: #f8d7da;
  color: #721c24;
}

.diferencia-badge.igual {
  background-color: #d4edda;
  color: #155724;
}

.motivo-cancelacion {
  max-width: 200px;
}

.motivo-text {
  font-style: italic;
  color: #dc3545;
  font-weight: 500;
  word-wrap: break-word;
}

.user-info {
  max-width: 180px;
}

.user-email {
  font-size: 13px;
  color: #6c757d;
  word-wrap: break-word;
}

.date-badge {
  display: inline-block;
  padding: 6px 12px;
  background-color: #e2e3e5;
  color: #495057;
  border-radius: 6px;
  font-weight: 500;
  font-size: 12px;
  white-space: nowrap;
}

.text-center {
  text-align: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-state svg {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 18px;
  color: #6c757d;
  margin: 0;
}

/* Paginación */
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
  background-color: #dc3545;
  color: white;
  border-color: #dc3545;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  padding: 8px 16px;
  font-weight: 600;
  color: #dc3545;
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

/* Responsive */
@media (max-width: 768px) {
  .pagination-section {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-controls {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
