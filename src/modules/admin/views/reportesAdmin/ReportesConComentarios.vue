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

interface MarbeteConComentario {
  folio: number;
  estado: string;
  claveProducto: string;
  nombreProducto: string;
  claveAlmacen: string;
  nombreAlmacen: string;
  conteo1Valor: number | null;
  conteo1Comentario: string | null;
  conteo1UsuarioNombre: string | null;
  conteo2Valor: number | null;
  conteo2Comentario: string | null;
  conteo2UsuarioNombre: string | null;
  statusConteo: string;
  diferencia: number | null;
}

const periodoStore = usePeriodoStore();

// Estado
const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacen = ref<Almacen | null>(null);
const selectedAlmacenId = ref<number | null>(null);
const reporteData = ref<MarbeteConComentario[]>([]);
const filteredData = ref<MarbeteConComentario[]>([]);

const searchQuery = ref('');
const loading = ref(false);
const exporting = ref(false);
const exportingAll = ref(false);

// Paginación
const page = ref(0);
const pageSize = ref(50);
const totalPages = ref(1);
const totalElements = ref(0);

// Cargar períodos
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

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

const handleAlmacenChange = () => {
  if (selectedAlmacenId.value !== null) {
    selectedAlmacen.value = almacenes.value.find(a => a.id === selectedAlmacenId.value) || null;
  }
  loadReporte();
};

const handlePeriodoChange = () => {
  if (selectedPeriodoId.value) {
    selectedPeriodo.value = periodos.value.find(p => p.id === selectedPeriodoId.value) || null;
    if (selectedPeriodo.value) {
      periodoStore.setPeriodo(selectedPeriodo.value);
    }
  }
  loadReporte();
};

const loadReporte = async () => {
  if (!selectedPeriodo.value) {
    reporteData.value = [];
    filteredData.value = [];
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
    const response = await axiosConfiguration.doPost('/labels/reports/with-comments', body);

    const content = Array.isArray(response.data) ? response.data : [];
    reporteData.value = content.map((item: any) => ({
      folio: Number(item.folio ?? 0),
      estado: String(item.estado ?? ''),
      claveProducto: String(item.claveProducto ?? ''),
      nombreProducto: String(item.nombreProducto ?? ''),
      claveAlmacen: String(item.claveAlmacen ?? ''),
      nombreAlmacen: String(item.nombreAlmacen ?? ''),
      conteo1Valor: item.conteo1Valor !== null && item.conteo1Valor !== undefined ? Number(item.conteo1Valor) : null,
      conteo1Comentario: String(item.conteo1Comentario ?? ''),
      conteo1UsuarioNombre: String(item.conteo1UsuarioNombre ?? ''),
      conteo2Valor: item.conteo2Valor !== null && item.conteo2Valor !== undefined ? Number(item.conteo2Valor) : null,
      conteo2Comentario: String(item.conteo2Comentario ?? ''),
      conteo2UsuarioNombre: String(item.conteo2UsuarioNombre ?? ''),
      statusConteo: String(item.statusConteo ?? ''),
      diferencia: item.diferencia !== null && item.diferencia !== undefined ? Number(item.diferencia) : null
    }));

    applySearchFilter();
    LoadAlert(false);
  } catch (error) {
    LoadAlert(false);
    ToastError('Error', 'No se pudo cargar el reporte');
    reporteData.value = [];
    filteredData.value = [];
  } finally {
    loading.value = false;
  }
};

const normalizeSearchText = (text: string): string => {
  return text
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
};

const matchesSearch = (text: string, query: string): boolean => {
  const normalizedText = normalizeSearchText(text);
  const normalizedQuery = normalizeSearchText(query);
  if (normalizedText.includes(normalizedQuery)) return true;
  const queryWords = normalizedQuery.split(' ').filter(w => w.length > 0);
  return queryWords.every(word => normalizedText.includes(word));
};

const applySearchFilter = () => {
  let filtered = [...reporteData.value];
  if (searchQuery.value) {
    filtered = filtered.filter(m =>
      matchesSearch(String(m.folio), searchQuery.value) ||
      matchesSearch(m.nombreProducto, searchQuery.value) ||
      matchesSearch(m.claveProducto, searchQuery.value) ||
      matchesSearch(m.claveAlmacen, searchQuery.value) ||
      matchesSearch(m.nombreAlmacen, searchQuery.value) ||
      matchesSearch(m.conteo1Comentario || '', searchQuery.value) ||
      matchesSearch(m.conteo2Comentario || '', searchQuery.value)
    );
  }
  totalElements.value = filtered.length;
  totalPages.value = Math.ceil(totalElements.value / pageSize.value);
  const start = page.value * pageSize.value;
  const end = start + pageSize.value;
  filteredData.value = filtered.slice(start, end);
};

const handleSearch = () => {
  page.value = 0;
  applySearchFilter();
};

const formatDate = (date: string | undefined): string => {
  if (!date) return 'N/A';
  if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    const [y, m, d] = date.split('-');
    const dateObj = new Date(Number(y), Number(m) - 1, Number(d));
    return dateObj.toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' });
  }
  if (/^\d{4}-\d{2}-\d{2}T/.test(date)) {
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

const formatNumber = (value: number | null): string => {
  if (value === null || value === undefined) return '-';
  return new Intl.NumberFormat('es-MX').format(value);
};

const getEstadoBadge = (estado: string) => {
  const styles: Record<string, { bg: string; color: string }> = {
    'IMPRESO': { bg: '#d1ecf1', color: '#0c5460' },
    'GENERADO': { bg: '#cce5ff', color: '#004085' },
    'PENDIENTE': { bg: '#fff3cd', color: '#856404' },
    'CANCELADO': { bg: '#f8d7da', color: '#721c24' }
  };
  return styles[estado] || { bg: '#e2e3e5', color: '#495057' };
};

const getConteoStatus = (status: string) => {
  const styles: Record<string, { bg: string; color: string }> = {
    'CONTEO_COMPLETO': { bg: '#d1ecf1', color: '#0c5460' },
    'CONTEO_INCOMPLETO': { bg: '#fff3cd', color: '#856404' },
    'PENDIENTE': { bg: '#cce5ff', color: '#004085' }
  };
  return styles[status] || { bg: '#e2e3e5', color: '#495057' };
};

// Exportar a PDF - Almacén específico
const exportToPDF = async () => {
  if (!selectedPeriodo.value) {
    ToastError('Error', 'Selecciona un período para exportar');
    return;
  }
  if (!selectedAlmacen.value || selectedAlmacen.value.id === 0) {
    ToastError('Error', 'Selecciona un almacén específico para exportar');
    return;
  }

  await exportReportToPDF({
    endpoint: '/labels/reports/with-comments/pdf',
    periodId: selectedPeriodo.value.id,
    warehouseId: selectedAlmacen.value.id,
    fileName: `reporte-comentarios-${selectedAlmacen.value.clave}-${selectedPeriodo.value.id}.pdf`,
    onStart: () => { exporting.value = true; },
    onComplete: () => { exporting.value = false; }
  });
};

// Exportar a PDF - Todos los almacenes
const exportAllToPDF = async () => {
  if (!selectedPeriodo.value) {
    ToastError('Error', 'Selecciona un período para exportar');
    return;
  }

  await exportReportToPDF({
    endpoint: '/labels/reports/with-comments/all/pdf',
    periodId: selectedPeriodo.value.id,
    fileName: `reporte-comentarios-todos-${selectedPeriodo.value.id}.pdf`,
    onStart: () => { exportingAll.value = true; },
    onComplete: () => { exportingAll.value = false; }
  });
};

onMounted(() => {
  loadPeriodos();
  loadAlmacenes();
  loadReporte();
});

watch(
  () => selectedPeriodo.value?.id,
  () => {
    if (selectedPeriodo.value && selectedAlmacen.value) {
      loadReporte();
    }
  }
);

watch(
  () => selectedAlmacen.value?.id,
  () => {
    if (selectedPeriodo.value && selectedAlmacen.value) {
      loadReporte();
    }
  }
);
</script>

<template>
  <div class="reporte-container">
    <div class="header-section">
      <div class="title-wrapper">
        <h1 class="page-title">Reportes con Comentarios</h1>
        <p class="subtitle">Visualiza todos los marbetes que poseen comentarios en sus conteos, permitiendo un seguimiento detallado de observaciones.</p>
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
        <input type="text" v-model="searchQuery" @input="handleSearch" class="search-input" placeholder="Buscar por folio, producto, comentario, almacén..." />
      </div>

      <div class="export-buttons">
        <button class="export-btn export-btn-primary" @click="exportToPDF" :disabled="exporting || !selectedAlmacen || selectedAlmacen.id === 0">
          <span v-if="!exporting">📄 Exportar Almacén</span>
          <span v-else>Exportando...</span>
        </button>
        <button class="export-btn export-btn-secondary" @click="exportAllToPDF" :disabled="exportingAll">
          <span v-if="!exportingAll">📊 Exportar Todos</span>
          <span v-else>Exportando...</span>
        </button>
      </div>
    </div>

    <!-- Tabla de resultados -->
    <div class="table-section" v-if="filteredData.length > 0">
      <table class="data-table">
        <thead>
          <tr>
            <th>Folio</th>
            <th>Producto</th>
            <th>Almacén</th>
            <th>Estado</th>
            <th>Conteo 1</th>
            <th>Comentario C1</th>
            <th>Conteo 2</th>
            <th>Comentario C2</th>
            <th>Diferencia</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredData" :key="item.folio">
            <td><span class="folio-badge">{{ item.folio }}</span></td>
            <td>
              <div class="product-info">
                <strong>{{ item.nombreProducto }}</strong>
                <small>{{ item.claveProducto }}</small>
              </div>
            </td>
            <td>
              <div class="warehouse-info">
                <span class="warehouse-key">{{ item.claveAlmacen }}</span>
                <small>{{ item.nombreAlmacen }}</small>
              </div>
            </td>
            <td>
              <span class="status-badge" :style="{ backgroundColor: getEstadoBadge(item.estado).bg, color: getEstadoBadge(item.estado).color }">
                {{ item.estado }}
              </span>
            </td>
            <td><span class="number-badge">{{ formatNumber(item.conteo1Valor) }}</span></td>
            <td>
              <div class="comment-cell">
                <span v-if="item.conteo1Comentario" class="comment-text">{{ item.conteo1Comentario }}</span>
                <span v-else class="comment-empty">—</span>
                <small v-if="item.conteo1UsuarioNombre">{{ item.conteo1UsuarioNombre }}</small>
              </div>
            </td>
            <td><span class="number-badge">{{ formatNumber(item.conteo2Valor) }}</span></td>
            <td>
              <div class="comment-cell">
                <span v-if="item.conteo2Comentario" class="comment-text">{{ item.conteo2Comentario }}</span>
                <span v-else class="comment-empty">—</span>
                <small v-if="item.conteo2UsuarioNombre">{{ item.conteo2UsuarioNombre }}</small>
              </div>
            </td>
            <td>
              <span class="number-badge" :class="{ 'warn-badge': item.diferencia !== 0 && item.diferencia !== null }">
                {{ formatNumber(item.diferencia) }}
              </span>
            </td>
            <td>
              <span class="status-badge" :style="{ backgroundColor: getConteoStatus(item.statusConteo).bg, color: getConteoStatus(item.statusConteo).color }">
                {{ item.statusConteo }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Paginación -->
      <div class="pagination-section" v-if="totalPages > 1">
        <button class="pagination-btn" @click="page = Math.max(0, page - 1)" :disabled="page === 0">← Anterior</button>
        <span class="pagination-info">Página {{ page + 1 }} de {{ totalPages }}</span>
        <button class="pagination-btn" @click="page = Math.min(totalPages - 1, page + 1)" :disabled="page === totalPages - 1">Siguiente →</button>
      </div>
    </div>

    <!-- Loading -->
    <div class="loading-container" v-else-if="loading">
      <svg class="spinner" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50">
        <circle cx="25" cy="25" r="20" stroke="#dc3545" stroke-width="4" fill="none"/>
      </svg>
      <p>Cargando reporte...</p>
    </div>

    <!-- Sin datos -->
    <div class="empty-state" v-else>
      <div class="empty-icon">📋</div>
      <h3>No hay datos para mostrar</h3>
      <p>Selecciona un período y almacén para ver los marbetes con comentarios</p>
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

.export-buttons {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.export-btn {
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.export-btn-primary {
  background: #dc3545;
  color: white;
}

.export-btn-primary:hover:not(:disabled) {
  background: #c82333;
  box-shadow: 0 2px 8px rgba(220, 53, 69, 0.3);
}

.export-btn-secondary {
  background: #6c757d;
  color: white;
}

.export-btn-secondary:hover:not(:disabled) {
  background: #5a6268;
  box-shadow: 0 2px 8px rgba(108, 117, 125, 0.3);
}

.export-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.table-section {
  background: white;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.record-info {
  font-size: 12px;
  color: #6c757d;
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
  transition: all 0.2s;
}

.data-table tbody tr:hover {
  background: #fff5f5;
}

.data-table tbody td {
  padding: 16px;
  color: #495057;
  font-size: 13px;
}

.folio-badge {
  display: inline-block;
  padding: 6px 14px;
  background: #f3e5f5;
  color: #7b1fa2;
  border-radius: 8px;
  font-weight: 700;
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
  background: #f3e5f5;
  color: #7b1fa2;
  border-radius: 6px;
  font-weight: 600;
  font-size: 12px;
  width: fit-content;
}

.warehouse-info small {
  color: #6c757d;
  font-size: 12px;
}

.number-badge {
  display: inline-block;
  padding: 6px 12px;
  background: #e9ecef;
  color: #495057;
  border-radius: 6px;
  font-weight: 600;
}

.number-badge.warn-badge {
  background: #fff3cd;
  color: #856404;
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 12px;
}

.comment-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 200px;
}

.comment-text {
  font-size: 12px;
  color: #dc3545;
  font-style: italic;
  word-break: break-word;
}

.comment-empty {
  color: #ccc;
}

.comment-cell small {
  color: #999;
  font-size: 11px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-top: 1px solid #e0e0e0;
}

.pagination-btn {
  padding: 8px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  background: #dc3545;
  color: white;
  border-color: #dc3545;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 12px;
  color: #6c757d;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.spinner {
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-container p {
  color: #6c757d;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 13px;
  color: #6c757d;
  margin: 0;
}

@media (max-width: 768px) {
  .controls-section {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-item {
    min-width: auto;
  }

  .search-wrapper {
    min-width: auto;
  }

  .data-table {
    font-size: 12px;
  }

  .data-table th,
  .data-table td {
    padding: 12px;
  }
}
</style>

