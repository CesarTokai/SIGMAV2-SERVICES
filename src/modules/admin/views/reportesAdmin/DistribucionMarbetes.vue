<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, LoadAlert } from '@/utils/SweetAlert';
import { usePeriodoStore } from '@/store/periodoStore';

interface Almacen {
  id: number;
  clave: string;
  nombre: string;
  activo: boolean;
}

interface DistribucionMarbete {
  claveAlmacen: string;
  nombreAlmacen: string;
  primerFolio: number;
  ultimoFolio: number;
  totalMarbetes: number;
  usuario: string;
}


interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

const periodos = ref<Periodo[]>([]);

const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);

const almacenes = ref<Almacen[]>([]);
const selectedAlmacen = ref<Almacen | null>(null);
const selectedAlmacenId = ref<number | null>(null);
const distribucionMarbetes = ref<DistribucionMarbete[]>([]);
const filteredDistribucion = ref<DistribucionMarbete[]>([]);


const searchQuery = ref('');
const loading = ref(false);
const exporting = ref(false);
const exportingAll = ref(false);

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

// Store
const periodoStore = usePeriodoStore();


// Función robusta para formatear fechas sin desfase de día
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
  page.value = 0;
  loadDistribucionMarbetes();
};

// Cargar distribución de marbetes desde API de usuarios con almacenes
const loadDistribucionMarbetes = async () => {
  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    distribucionMarbetes.value = [];
    filteredDistribucion.value = [];
    loading.value = false;
    return;
  }

  loading.value = true;
  try {
    LoadAlert(true);
    // Construir parámetros de consulta
    const params = new URLSearchParams();
    params.append('page', page.value.toString());
    params.append('size', pageSize.value.toString());
    params.append('periodId', selectedPeriodo.value.id.toString());

    // Agregar warehouseId SOLO si no es "TODOS" (id !== 0)
    if (selectedAlmacen.value.id !== 0) {
      params.append('warehouseId', selectedAlmacen.value.id.toString());
    } else {
      // Para TODOS los almacenes, usar un endpoint diferente o no pasar warehouseId
      console.log('Cargando TODOS los almacenes');
    }

    // Llamar a la API con GET
    const response = await axiosConfiguration.doGet(`/admin/users/with-warehouses?${params.toString()}`);

    console.log('Resultados crudos de la API:', response.data);

    // Obtener los datos de la respuesta
    const content = response.data?.data || response.data?.content || [];

    // Mapear los datos al formato esperado
    distribucionMarbetes.value = Array.isArray(content) ? content.map((item: any) => ({
      claveAlmacen: String(item.claveAlmacen ?? item.warehouseKey ?? ''),
      nombreAlmacen: String(item.nombreAlmacen ?? item.nameWarehouse ?? ''),
      primerFolio: Number(item.primerFolio ?? 0),
      ultimoFolio: Number(item.ultimoFolio ?? 0),
      totalMarbetes: (Number(item.ultimoFolio ?? 0) - Number(item.primerFolio ?? 0)) + 1 || 0,
      usuario: String(item.usuario ?? item.email ?? '')
    })) : [];

    // Actualizar información de paginación desde la API
    totalElements.value = response.data?.totalElements || distribucionMarbetes.value.length;
    totalPages.value = response.data?.totalPages || 1;

    // Actualizar estadísticas
    estadisticas.value.totalConDiferencia = distribucionMarbetes.value.length;

    applySearchFilterDistribucion();
    LoadAlert(false);
  } catch (error) {
    LoadAlert(false);
    console.error('Error al cargar la distribución de marbetes:', error);
    ToastError('Error', 'No se pudo cargar la distribución de marbetes');
    distribucionMarbetes.value = [];
    filteredDistribucion.value = [];
  } finally {
    loading.value = false;
  }
};

const handlePeriodoChange = () => {
  if (selectedPeriodoId.value) {
    selectedPeriodo.value = periodos.value.find(p => p.id === selectedPeriodoId.value) || null;
    if (selectedPeriodo.value) {
      periodoStore.setPeriodo(selectedPeriodo.value);
    }
  }
  page.value = 0;
  loadDistribucionMarbetes();
};

// Normalizar texto para búsqueda flexible
const normalizeSearchText = (text: string): string => {
  return text
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
};

// Función mejorada de búsqueda flexible
const matchesSearch = (text: string, query: string): boolean => {
  const normalizedText = normalizeSearchText(text);
  const normalizedQuery = normalizeSearchText(query);
  if (normalizedText.includes(normalizedQuery)) return true;
  const queryWords = normalizedQuery.split(' ').filter(w => w.length > 0);
  return queryWords.every(word => normalizedText.includes(word));
};

// Filtro de búsqueda mejorado para la tabla de distribución
const applySearchFilterDistribucion = () => {
  let filtered = [...distribucionMarbetes.value];
  if (searchQuery.value) {
    filtered = filtered.filter(m =>
      matchesSearch(m.claveAlmacen, searchQuery.value) ||
      matchesSearch(m.nombreAlmacen, searchQuery.value) ||
      matchesSearch(m.usuario, searchQuery.value)
    );
  }
  filteredDistribucion.value = filtered;
};

// Paginación
const goToPage = (newPage: number) => {
  if (newPage < 0 || newPage >= totalPages.value) return;
  page.value = newPage;
  loadDistribucionMarbetes();
};

const changePageSize = (event: Event) => {
  pageSize.value = Number((event.target as HTMLSelectElement).value);
  page.value = 0;
  loadDistribucionMarbetes();
};

// Manejar búsqueda
const handleSearch = () => {
  page.value = 0;
  applySearchFilterDistribucion();
};


// Exportar a PDF
const exportToPDF = async () => {
  if (!selectedPeriodo.value) {
    ToastError('Error', 'Selecciona un período');
    return;
  }

  if (!selectedAlmacen.value || selectedAlmacen.value.id === 0) {
    ToastError('Error', 'Selecciona un almacén específico para exportar');
    return;
  }

  exporting.value = true;
  try {
    const response = await axiosConfiguration.doPost('/labels/reports/distribution/pdf', {
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id
    }, {
      responseType: 'blob'
    });

    // Crear URL de descarga
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `distribucion-marbetes-${selectedAlmacen.value.clave}-${new Date().toISOString().split('T')[0]}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.parentNode?.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error('Error al exportar PDF:', error);
    ToastError('Error', 'No se pudo exportar el PDF');
  } finally {
    exporting.value = false;
  }
};

// Exportar todos los almacenes a PDF
const exportAllToPDF = async () => {
  if (!selectedPeriodo.value) {
    ToastError('Error', 'Selecciona un período');
    return;
  }

  exportingAll.value = true;
  try {
    const response = await axiosConfiguration.doPost('/labels/reports/distribution/pdf/all', {
      periodId: selectedPeriodo.value.id
    }, {
      responseType: 'blob'
    });

    // Crear URL de descarga
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `distribucion-marbetes-todos-${new Date().toISOString().split('T')[0]}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.parentNode?.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error('Error al exportar PDF de todos los almacenes:', error);
    ToastError('Error', 'No se pudo exportar el PDF');
  } finally {
    exportingAll.value = false;
  }
};

onMounted(async () => {
  await loadPeriodos();
  await loadAlmacenes();

  // Asegurar que siempre cargue con los valores inicializados
  if (selectedPeriodo.value && selectedAlmacen.value) {
    await loadDistribucionMarbetes();
  }
});
</script>

<template>
  <div class="reporte-container">
    <div class="header-section">
      <div class="title-wrapper">
        <h1 class="page-title">
          Distribución de Marbetes
        </h1>
        <p class="subtitle">
          Muestra la distribución de folios impresos por almacén y usuario, permitiendo identificar cuántos marbetes se asignaron a cada responsable.
        </p>
      </div>
    </div>
    <div class="controls-section">
      <div class="filter-item">
        <label class="filter-label">Período</label>
        <select v-model.number="selectedPeriodoId" class="filter-select" @change="handlePeriodoChange">
          <option :value="null" disabled>Selecciona un período</option>
          <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">{{ formatDate(periodo.date) }} - {{ periodo.comments }}</option>
        </select>
      </div>
      <div class="filter-item">
        <label class="filter-label">Almacén</label>
        <select v-model.number="selectedAlmacenId" class="filter-select" @change="handleAlmacenChange">
          <option v-for="almacen in almacenes" :key="almacen.id" :value="almacen.id">{{ almacen.clave === 'TODOS' ? almacen.nombre : `${almacen.clave} - ${almacen.nombre}` }}</option>
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
          <span class="stat-mini-label">Total registros</span>
        </div>
        <button
            @click="exportToPDF"
            :disabled="exporting || !selectedPeriodo || !selectedAlmacen || selectedAlmacen.id === 0"
            class="btn-export-pdf"
            title="Exportar reporte de almacén actual a PDF"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2m0 0v-8m0 8H3m0 0h18"/>
          </svg>
          {{ exporting ? 'Exportando...' : 'PDF' }}
        </button>
        <button
            @click="exportAllToPDF"
            :disabled="exportingAll || !selectedPeriodo"
            class="btn-export-pdf btn-export-all"
            title="Exportar reporte de todos los almacenes a PDF"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2m0 0v-8m0 8H3m0 0h18"/>
          </svg>
          {{ exportingAll ? 'Exportando...' : 'PDF Todo' }}
        </button>
      </div>
    </div>
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>Cargando distribución de marbetes...</p>
    </div>
    <div v-else-if="filteredDistribucion.length > 0" class="table-section">
      <table class="data-table">
        <thead>
        <tr>
          <th>Usuario</th>
          <th>Clave Almacen</th>
          <th class="text-center">Nombre almacen</th>
          <th class="text-center">Primer Folio</th>
          <th class="text-center">Último Folio</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in filteredDistribucion" :key="item.claveAlmacen + '-' + item.primerFolio">
          <td><span class="user-email">{{ item.usuario }}</span></td>
          <td><span class="warehouse-key">{{ item.claveAlmacen }}</span></td>
          <td>{{ item.nombreAlmacen }}</td>
          <td class="text-center"><span class="number-badge">{{ item.primerFolio }}</span></td>
          <td class="text-center"><span class="number-badge">{{ item.ultimoFolio }}</span></td>

        </tr>
        </tbody>
      </table>
      <div class="pagination-section">
        <div class="pagination-info">
          Página {{ page + 1 }} de {{ totalPages }} ({{ totalElements }} registros)
        </div>
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
    <div v-else class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
      </svg>
      <p>No hay distribución de marbetes registrada.</p>
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
  border-top: 4px solid #dc3545;
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

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
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


.data-table tbody td {
  padding: 16px;
  color: #495057;
  font-size: 14px;
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

.user-email {
  font-size: 13px;
  color: #6c757d;
  word-wrap: break-word;
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
  color: #6c757d;
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
