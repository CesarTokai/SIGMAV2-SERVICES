<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import SearchBar from '@/components/SearchBar.vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, ToastSuccess, LoadAlert } from '@/utils/SweetAlert';

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
  conteo1: number;
  conteo2: number;
  motivoCancelacion: string;
  canceladoAt: string;
  canceladoPor: string;
}

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

// Paginación
const page = ref(0);
const pageSize = ref(50);
const totalPages = ref(1);
const totalElements = ref(0);

// Estadísticas
const estadisticas = ref({
  totalCancelados: 0,
  canceladosHoy: 0,
  canceladosUltimaSemana: 0
});

// Cargar períodos
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

    if (periodos.value.length > 0 && !selectedPeriodo.value) {
      selectedPeriodo.value = periodos.value[0] || null;
      selectedPeriodoId.value = selectedPeriodo.value ? selectedPeriodo.value.id : null;
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
      selectedAlmacen.value = almacenes.value[0];
      selectedAlmacenId.value = selectedAlmacen.value.id;
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

    console.log('📤 Consultando marbetes cancelados:', body);

    const response = await axiosConfiguration.doPost('/labels/reports/cancelled', body);
    const content = Array.isArray(response.data) ? response.data : [];

    console.log('📥 Marbetes cancelados recibidos:', content.length);

    marbetesCancelados.value = content.map((item: any) => ({
      numeroMarbete: item.numeroMarbete ?? 0,
      claveProducto: String(item.claveProducto ?? '').trim(),
      descripcionProducto: String(item.descripcionProducto ?? '').trim(),
      unidad: String(item.unidad ?? '').trim(),
      claveAlmacen: String(item.claveAlmacen ?? '').trim(),
      nombreAlmacen: String(item.nombreAlmacen ?? '').trim(),
      conteo1: Number(item.conteo1 ?? 0),
      conteo2: Number(item.conteo2 ?? 0),
      motivoCancelacion: String(item.motivoCancelacion ?? '').trim(),
      canceladoAt: item.canceladoAt ?? '',
      canceladoPor: String(item.canceladoPor ?? '').trim()
    }));

    // Aplicar filtro de búsqueda si existe
    applySearchFilter();
    calcularEstadisticas();

    LoadAlert(false);
  } catch (error) {
    LoadAlert(false);
    console.error('Error al cargar marbetes cancelados:', error);
    ToastError('Error', 'No se pudieron cargar los marbetes cancelados');
    marbetesCancelados.value = [];
    filteredMarbetes.value = [];
  } finally {
    loading.value = false;
  }
};

// Aplicar filtro de búsqueda
const applySearchFilter = () => {
  let filtered = [...marbetesCancelados.value];

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    filtered = filtered.filter(m =>
      m.numeroMarbete.toString().includes(query) ||
      m.claveProducto.toLowerCase().includes(query) ||
      m.descripcionProducto.toLowerCase().includes(query) ||
      m.claveAlmacen.toLowerCase().includes(query) ||
      m.nombreAlmacen.toLowerCase().includes(query) ||
      m.canceladoPor.toLowerCase().includes(query) ||
      m.motivoCancelacion.toLowerCase().includes(query)
    );
  }

  // Calcular paginación
  totalElements.value = filtered.length;
  totalPages.value = Math.ceil(totalElements.value / pageSize.value);

  // Aplicar paginación
  const start = page.value * pageSize.value;
  const end = start + pageSize.value;
  filteredMarbetes.value = filtered.slice(start, end);
};

// Calcular estadísticas
const calcularEstadisticas = () => {
  estadisticas.value.totalCancelados = marbetesCancelados.value.length;

  const hoy = new Date();
  const inicioDia = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  const inicioSemana = new Date(hoy.getTime() - 7 * 24 * 60 * 60 * 1000);

  estadisticas.value.canceladosHoy = marbetesCancelados.value.filter(m => {
    if (!m.canceladoAt) return false;
    const fechaCancelacion = new Date(m.canceladoAt);
    return fechaCancelacion >= inicioDia;
  }).length;

  estadisticas.value.canceladosUltimaSemana = marbetesCancelados.value.filter(m => {
    if (!m.canceladoAt) return false;
    const fechaCancelacion = new Date(m.canceladoAt);
    return fechaCancelacion >= inicioSemana;
  }).length;
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

// Calcular diferencia entre conteos
const calcularDiferencia = (conteo1: number, conteo2: number): number => {
  return conteo2 - conteo1;
};

// Paginación
const goToPage = (newPage: number) => {
  if (newPage < 0 || newPage >= totalPages.value) return;
  page.value = newPage;
  applySearchFilter();
};

const changePageSize = (event: Event) => {
  const value = Number((event.target as HTMLSelectElement).value);
  pageSize.value = value;
  page.value = 0;
  applySearchFilter();
};

// Manejar búsqueda
const handleSearch = () => {
  page.value = 0;
  applySearchFilter();
};

onMounted(() => {
  loadPeriodos();
  loadAlmacenes();
});
</script>

<template>
  <div class="cancelacion-marbetes">
    <!-- Selección de periodo y almacén -->
    <div class="section-card">
      <div class="section-header">
        <h2 class="section-title">
          <span class="icon">❌</span>
          Marbetes Cancelados
        </h2>
        <p class="section-description">
          Consulta y revisa todos los marbetes que han sido cancelados durante el periodo seleccionado.
        </p>
      </div>

      <div class="filters-grid">
        <div class="filter-item">
          <label for="periodoSelect" class="form-label">
            <strong>Período:</strong>
          </label>
          <select
            id="periodoSelect"
            v-model.number="selectedPeriodoId"
            class="form-select"
            @change="handlePeriodoChange"
          >
            <option :value="null" disabled>Selecciona un período</option>
            <option
              v-for="periodo in periodos"
              :key="periodo.id"
              :value="periodo.id"
            >
              {{ formatDate(periodo.date) }} - {{ periodo.comments }}
            </option>
          </select>
        </div>

        <div class="filter-item">
          <label for="almacenSelect" class="form-label">
            <strong>Almacén:</strong>
          </label>
          <select
            id="almacenSelect"
            v-model.number="selectedAlmacenId"
            class="form-select"
            @change="handleAlmacenChange"
          >
            <option
              v-for="almacen in almacenes"
              :key="almacen.id"
              :value="almacen.id"
            >
              {{ almacen.clave === 'TODOS' ? almacen.nombre : `${almacen.clave} - ${almacen.nombre}` }}
            </option>
          </select>
        </div>
      </div>

      <!-- Estadísticas -->
      <div class="statistics-grid">
        <div class="stat-card danger">
          <div class="stat-icon">📊</div>
          <div class="stat-content">
            <div class="stat-value">{{ estadisticas.totalCancelados }}</div>
            <div class="stat-label">Total Cancelados</div>
          </div>
        </div>
        <div class="stat-card warning">
          <div class="stat-icon">📅</div>
          <div class="stat-content">
            <div class="stat-value">{{ estadisticas.canceladosHoy }}</div>
            <div class="stat-label">Cancelados Hoy</div>
          </div>
        </div>
        <div class="stat-card info">
          <div class="stat-icon">📈</div>
          <div class="stat-content">
            <div class="stat-value">{{ estadisticas.canceladosUltimaSemana }}</div>
            <div class="stat-label">Últimos 7 días</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Listado de Marbetes Cancelados -->
    <div class="section-card">
      <div class="section-header">
        <h2 class="section-title">
          <span class="icon">📋</span>
          Listado de Marbetes Cancelados
        </h2>
      </div>

      <!-- Buscador -->
      <div class="search-section">
        <SearchBar
          placeholder="Buscar por folio, producto, almacén, usuario o motivo..."
          v-model="searchQuery"
          @input="handleSearch"
        />
      </div>

      <!-- Tabla -->
      <div v-if="loading" class="loading-container">
        <div class="spinner"></div>
        <p>Cargando marbetes cancelados...</p>
      </div>

      <div v-else-if="filteredMarbetes.length > 0" class="table-section">
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>Folio</th>
                <th>Producto</th>
                <th>Almacén</th>
                <th>Conteo 1</th>
                <th>Conteo 2</th>
                <th>Diferencia</th>
                <th>Motivo</th>
                <th>Cancelado Por</th>
                <th>Fecha Cancelación</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="marbete in filteredMarbetes" :key="marbete.numeroMarbete" class="row-cancelled">
                <td>
                  <span class="folio-badge cancelled">{{ marbete.numeroMarbete }}</span>
                </td>
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
                <td class="text-center">
                  <span class="number-badge">{{ formatNumber(marbete.conteo1) }}</span>
                </td>
                <td class="text-center">
                  <span class="number-badge">{{ formatNumber(marbete.conteo2) }}</span>
                </td>
                <td class="text-center">
                  <span :class="['diferencia-badge', calcularDiferencia(marbete.conteo1, marbete.conteo2) > 0 ? 'positivo' : calcularDiferencia(marbete.conteo1, marbete.conteo2) < 0 ? 'negativo' : 'igual']">
                    {{ calcularDiferencia(marbete.conteo1, marbete.conteo2) > 0 ? '+' : calcularDiferencia(marbete.conteo1, marbete.conteo2) < 0 ? '-' : '' }}{{ formatNumber(Math.abs(calcularDiferencia(marbete.conteo1, marbete.conteo2))) }}
                  </span>
                </td>
                <td>
                  <div class="motivo-cancelacion">
                    <span class="motivo-text">{{ marbete.motivoCancelacion || 'No especificado' }}</span>
                  </div>
                </td>
                <td>
                  <div class="user-info">
                    <span class="user-email">{{ marbete.canceladoPor }}</span>
                  </div>
                </td>
                <td class="text-center">
                  <span class="date-badge">{{ formatDateTime(marbete.canceladoAt) }}</span>
                </td>
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
        <div class="no-data-icon">✅</div>
        <p v-if="!selectedPeriodo">
          Selecciona un período para ver los marbetes cancelados.
        </p>
        <p v-else>
          No hay marbetes cancelados en este período y almacén.
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cancelacion-marbetes {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 25px;
}

.section-header {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 5px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title .icon {
  font-size: 24px;
}

.section-description {
  margin: 0;
  font-size: 14px;
  color: #6c757d;
}

/* Filtros */
.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 25px;
}

.filter-item {
  display: flex;
  flex-direction: column;
}

.form-label {
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

/* Estadísticas */
.statistics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 10px;
  border-left: 4px solid #6c757d;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-card.danger {
  background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%);
  border-left-color: #dc3545;
}

.stat-card.warning {
  background: linear-gradient(135deg, #fff3cd 0%, #ffeeba 100%);
  border-left-color: #ffc107;
}

.stat-card.info {
  background: linear-gradient(135deg, #d1ecf1 0%, #bee5eb 100%);
  border-left-color: #17a2b8;
}

.stat-icon {
  font-size: 36px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  line-height: 1;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 13px;
  color: #6c757d;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Búsqueda y tabla */
.search-section {
  margin-bottom: 20px;
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

.table-responsive {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table thead {
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
}

.table thead th {
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.table tbody tr {
  border-bottom: 1px solid #e0e0e0;
  transition: all 0.2s ease;
}

.table tbody tr:hover {
  background-color: #fff5f5;
}

.table tbody tr.row-cancelled {
  background-color: #fdf2f2;
}

.table tbody td {
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
  .filters-grid,
  .statistics-grid {
    grid-template-columns: 1fr;
  }

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
