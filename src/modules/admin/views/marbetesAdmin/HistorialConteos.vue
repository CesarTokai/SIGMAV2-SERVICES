<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, ToastSuccess, LoadAlert } from '@/utils/SweetAlert';

interface HistorialConteo {
  id: number;
  email: string;
  fullName: string;
  role: string;
  folio: number;
  countType: 'C1' | 'C2';
  countValue: number;
  status: string;
  periodId: number | null;
  warehouseId: number | null;
  createdAt: string;
  updatedAt: string;
  description: string | null;
}

// Estado
const historial = ref<HistorialConteo[]>([]);
const filteredHistorial = ref<HistorialConteo[]>([]);
const loading = ref(false);

// Filtros
const searchQuery = ref<string>('');
const filterCountType = ref<'todos' | 'C1' | 'C2'>('todos');
const filterRole = ref<string>('todos');
const filterStatus = ref<string>('todos');
const sortBy = ref<'createdAt' | 'folio' | 'countValue'>('createdAt');
const sortOrder = ref<'asc' | 'desc'>('desc');

// Paginación
const currentPage = ref<number>(0);
const pageSize = ref<number>(20);
const totalElements = ref<number>(0);
const totalPages = ref<number>(0);

// Obtener roles únicos
const roles = computed(() => {
  const rolesSet = new Set(historial.value.map(h => h.role));
  return Array.from(rolesSet).sort();
});

// Obtener estados únicos
const estados = computed(() => {
  const estSet = new Set(historial.value.map(h => h.status));
  return Array.from(estSet).sort();
});

// Cargar historial
const loadHistorial = async () => {
  try {
    loading.value = true;
    LoadAlert(true);

    const response = await axiosConfiguration.doGet('/labels/history/all');

    // Extraer el array de datos de la respuesta
    let data: HistorialConteo[] = [];

    if (response?.data?.data && Array.isArray(response.data.data)) {
      // Si viene en response.data.data (objeto con propiedades)
      data = response.data.data;
    } else if (Array.isArray(response?.data)) {
      // Si viene directamente en response.data
      data = response.data;
    } else if (Array.isArray(response)) {
      // Si la respuesta es directamente un array
      data = response;
    }


    historial.value = data;
    totalElements.value = data.length;
    totalPages.value = Math.ceil(data.length / pageSize.value);

    aplicarFiltros();
    LoadAlert(false);
    ToastSuccess('Cargado', `${data.length} registros de conteo`);
  } catch (error: any) {
    LoadAlert(false);
    console.error('Error al cargar historial:', error);
    ToastError('Error', error?.response?.data?.message || 'No se pudo cargar el historial');
    historial.value = [];  // Asegurar que es un array vacío en caso de error
  } finally {
    loading.value = false;
  }
};

// Aplicar filtros
const aplicarFiltros = () => {
  let filtered = [...historial.value];

  // Filtro de búsqueda
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase();
    filtered = filtered.filter(item =>
      item.folio.toString().includes(query) ||
      item.email.toLowerCase().includes(query) ||
      item.fullName.toLowerCase().includes(query)
    );
  }

  // Filtro por tipo de conteo
  if (filterCountType.value !== 'todos') {
    filtered = filtered.filter(item => item.countType === filterCountType.value);
  }

  // Filtro por rol
  if (filterRole.value !== 'todos') {
    filtered = filtered.filter(item => item.role === filterRole.value);
  }

  // Filtro por estado
  if (filterStatus.value !== 'todos') {
    filtered = filtered.filter(item => item.status === filterStatus.value);
  }

  // Ordenamiento
  filtered.sort((a, b) => {
    let aVal: any = a[sortBy.value as keyof HistorialConteo];
    let bVal: any = b[sortBy.value as keyof HistorialConteo];

    if (sortBy.value === 'createdAt') {
      aVal = new Date(aVal).getTime();
      bVal = new Date(bVal).getTime();
    }

    if (aVal < bVal) return sortOrder.value === 'asc' ? -1 : 1;
    if (aVal > bVal) return sortOrder.value === 'asc' ? 1 : -1;
    return 0;
  });

  filteredHistorial.value = filtered;
  totalElements.value = filtered.length;
  totalPages.value = Math.ceil(filtered.length / pageSize.value);
  currentPage.value = 0;
};

// Paginación
const paginatedHistorial = computed(() => {
  const start = currentPage.value * pageSize.value;
  const end = start + pageSize.value;
  return filteredHistorial.value.slice(start, end);
});

// Watchers para filtros
watch([searchQuery, filterCountType, filterRole, filterStatus, sortBy, sortOrder], () => {
  aplicarFiltros();
});

// Formatear fecha
const formatDate = (dateString: string): string => {
  try {
    return new Date(dateString).toLocaleDateString('es-MX', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  } catch {
    return dateString;
  }
};

// Formatear números
const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('es-MX').format(num);
};

// Clases para badges
const getCountTypeBadgeClass = (type: string): string => {
  return type === 'C1' ? 'badge-c1' : 'badge-c2';
};

const getStatusBadgeClass = (status: string): string => {
  const statusLower = status.toLowerCase();
  if (statusLower.includes('registrado')) return 'badge-success';
  if (statusLower.includes('actualizado')) return 'badge-info';
  if (statusLower.includes('cancelado')) return 'badge-danger';
  return 'badge-secondary';
};

const getRoleBadgeClass = (role: string): string => {
  const roleUpper = role.toUpperCase();
  if (roleUpper === 'ADMINISTRADOR') return 'badge-admin';
  if (roleUpper === 'ALMACENISTA') return 'badge-almacenista';
  if (roleUpper === 'AUXILIAR') return 'badge-auxiliar';
  if (roleUpper === 'AUXILIAR_DE_CONTEO') return 'badge-auxiliar-conteo';
  return 'badge-secondary';
};

const displayRole = (role: string): string => {
  const roleMap: Record<string, string> = {
    'ADMINISTRADOR': 'Admin',
    'ALMACENISTA': 'Almacenista',
    'AUXILIAR': 'Auxiliar',
    'AUXILIAR_DE_CONTEO': 'Aux. Conteo'
  };
  return roleMap[role] || role;
};

onMounted(() => {
  loadHistorial();
});
</script>

<template>
  <div class="historial-conteos">
    <!-- Header -->
    <div class="header-section">
      <div class="header-title">
        <h1>Historial de Conteos</h1>
        <p class="subtitle">Consulta todos los conteos registrados en la organización</p>
      </div>
      <button @click="loadHistorial" :disabled="loading" class="btn-reload">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M23 4v6h-6"></path>
          <path d="M1 20v-6h6"></path>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36M20.49 15a9 9 0 0 1-14.85 3.36"></path>
        </svg>
        {{ loading ? 'Cargando...' : 'Recargar' }}
      </button>
    </div>

    <!-- Filtros -->
    <div class="filters-section">
      <div class="filter-group">
        <label for="searchInput">Buscar:</label>
        <input
          id="searchInput"
          v-model="searchQuery"
          type="text"
          placeholder="Folio, Email o Nombre..."
          class="filter-input"
        />
      </div>

      <div class="filter-group">
        <label for="countTypeFilter">Tipo de Conteo:</label>
        <select id="countTypeFilter" v-model="filterCountType" class="filter-select">
          <option value="todos">Todos</option>
          <option value="C1">Primer Conteo (C1)</option>
          <option value="C2">Segundo Conteo (C2)</option>
        </select>
      </div>

      <div class="filter-group">
        <label for="roleFilter">Rol:</label>
        <select id="roleFilter" v-model="filterRole" class="filter-select">
          <option value="todos">Todos</option>
          <option v-for="role in roles" :key="role" :value="role">
            {{ displayRole(role) }}
          </option>
        </select>
      </div>

      <div class="filter-group">
        <label for="statusFilter">Estado:</label>
        <select id="statusFilter" v-model="filterStatus" class="filter-select">
          <option value="todos">Todos</option>
          <option v-for="status in estados" :key="status" :value="status">
            {{ status }}
          </option>
        </select>
      </div>

      <div class="filter-group">
        <label for="sortByFilter">Ordenar por:</label>
        <select id="sortByFilter" v-model="sortBy" class="filter-select">
          <option value="createdAt">Fecha</option>
          <option value="folio">Folio</option>
          <option value="countValue">Valor</option>
        </select>
      </div>

      <div class="filter-group">
        <label for="sortOrderFilter">Orden:</label>
        <select id="sortOrderFilter" v-model="sortOrder" class="filter-select">
          <option value="desc">Descendente</option>
          <option value="asc">Ascendente</option>
        </select>
      </div>
    </div>

    <!-- Tabla -->
    <div class="table-container">
      <table class="historial-table">
        <thead>
          <tr>
            <th>Folio</th>
            <th>Usuario</th>
            <th>Rol</th>
            <th>Tipo</th>
            <th>Valor</th>
            <th>Estado</th>
            <th>Fecha</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="paginatedHistorial.length === 0">
            <td colspan="7" class="text-center text-muted">
              {{ loading ? 'Cargando...' : 'No hay registros' }}
            </td>
          </tr>
          <tr v-for="item in paginatedHistorial" :key="item.id">
            <td class="folio-cell">
              <strong>#{{ item.folio }}</strong>
            </td>
            <td class="email-cell">
              <div class="email-info">
                <div class="email">{{ item.email }}</div>
                <div class="fullname">{{ item.fullName }}</div>
              </div>
            </td>
            <td class="role-cell">
              <span :class="['badge', getRoleBadgeClass(item.role)]">
                {{ displayRole(item.role) }}
              </span>
            </td>
            <td class="type-cell">
              <span :class="['badge', getCountTypeBadgeClass(item.countType)]">
                {{ item.countType }}
              </span>
            </td>
            <td class="value-cell">
              <strong>{{ formatNumber(item.countValue) }}</strong>
            </td>
            <td class="status-cell">
              <span :class="['badge', getStatusBadgeClass(item.status)]">
                {{ item.status }}
              </span>
            </td>
            <td class="date-cell">
              {{ formatDate(item.createdAt) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Paginación -->
    <div class="pagination-section">
      <div class="pagination-info">
        Mostrando {{ paginatedHistorial.length === 0 ? 0 : currentPage * pageSize + 1 }}
        a {{ Math.min((currentPage + 1) * pageSize, totalElements) }}
        de {{ totalElements }} registros
      </div>
      <div class="pagination-controls">
        <button @click="currentPage--" :disabled="currentPage === 0">Anterior</button>
        <div class="page-indicator">
          Página {{ currentPage + 1 }} de {{ Math.max(1, totalPages) }}
        </div>
        <button @click="currentPage++" :disabled="currentPage >= totalPages - 1">Siguiente</button>
      </div>
      <div class="pagesize-selector">
        <label for="pageSizeSelect">Por página:</label>
        <select id="pageSizeSelect" v-model.number="pageSize">
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
          <option value="100">100</option>
        </select>
      </div>
    </div>
  </div>
</template>

<style scoped>
.historial-conteos {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
}

/* Header */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 20px;
}

.header-title h1 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: #1a202c;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  color: #718096;
}

.btn-reload {
  padding: 10px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.btn-reload:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-reload:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Filtros */
.filters-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f7fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-group label {
  font-size: 12px;
  font-weight: 600;
  color: #4a5568;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.filter-input,
.filter-select {
  padding: 8px 12px;
  border: 1px solid #cbd5e0;
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  background: white;
  transition: border-color 0.2s ease;
}

.filter-input:focus,
.filter-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* Tabla */
.table-container {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-bottom: 24px;
}

.historial-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.historial-table thead {
  background: #f7fafc;
  border-bottom: 2px solid #e2e8f0;
}

.historial-table th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  color: #2d3748;
  white-space: nowrap;
}

.historial-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  color: #2d3748;
}

.historial-table tbody tr:hover {
  background: #f7fafc;
}

.folio-cell {
  font-weight: 600;
  color: #667eea;
}

.email-cell {
  font-size: 12px;
}

.email-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.email {
  font-weight: 500;
  color: #2d3748;
}

.fullname {
  color: #718096;
  font-size: 11px;
}

.type-cell,
.role-cell,
.status-cell {
  text-align: center;
}

.value-cell {
  text-align: right;
  font-weight: 600;
  color: #2e7d32;
}

.date-cell {
  font-size: 12px;
  color: #718096;
  white-space: nowrap;
}

.text-center {
  text-align: center !important;
}

.text-muted {
  color: #a0aec0;
}

/* Badges */
.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.badge-success {
  background: #c6f6d5;
  color: #22543d;
}

.badge-info {
  background: #bee3f8;
  color: #2c5282;
}

.badge-danger {
  background: #fed7d7;
  color: #742a2a;
}

.badge-secondary {
  background: #e2e8f0;
  color: #2d3748;
}

.badge-c1 {
  background: #e0e7ff;
  color: #3730a3;
  border-left: 3px solid #6366f1;
}

.badge-c2 {
  background: #fef3c7;
  color: #78350f;
  border-left: 3px solid #f59e0b;
}

.badge-admin {
  background: #fecaca;
  color: #7f1d1d;
}

.badge-almacenista {
  background: #bfdbfe;
  color: #1e3a8a;
}

.badge-auxiliar {
  background: #ddd6fe;
  color: #4c1d95;
}

.badge-auxiliar-conteo {
  background: #d1d5db;
  color: #374151;
}

/* Paginación */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f7fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  gap: 20px;
  flex-wrap: wrap;
}

.pagination-info {
  font-size: 13px;
  color: #4a5568;
  font-weight: 500;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pagination-controls button {
  padding: 8px 14px;
  border: 1px solid #cbd5e0;
  background: white;
  color: #2d3748;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-controls button:hover:not(:disabled) {
  border-color: #667eea;
  background: #f7fafc;
  color: #667eea;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  font-size: 12px;
  color: #4a5568;
  font-weight: 600;
  min-width: 140px;
  text-align: center;
}

.pagesize-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.pagesize-selector label {
  font-weight: 600;
  color: #4a5568;
}

.pagesize-selector select {
  padding: 6px 10px;
  border: 1px solid #cbd5e0;
  border-radius: 6px;
  font-size: 12px;
  background: white;
}

/* Responsive */
@media (max-width: 1024px) {
  .filters-section {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }

  .historial-table {
    font-size: 12px;
  }

  .historial-table th,
  .historial-table td {
    padding: 10px 12px;
  }
}

@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
  }

  .filters-section {
    grid-template-columns: 1fr;
  }

  .pagination-section {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-controls {
    justify-content: center;
  }

  .table-container {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
</style>


