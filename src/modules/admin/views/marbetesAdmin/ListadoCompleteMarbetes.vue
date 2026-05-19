<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, ToastSuccess, LoadAlert } from '@/utils/SweetAlert';
import { usePeriodoStore } from '@/store/periodoStore';

interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface Marbete {
  folio: number;
  estado: string;
  createdAt: string;
  impresoAt: string;
  createdByEmail: string;
  createdByFullName: string;
  claveProducto: string;
  nombreProducto: string;
  warehouseId: number;
  claveAlmacen: string;
  nombreAlmacen: string;
  periodId: number;
  existenciasTeoricas: number;
  conteo1Valor: number | null;
  conteo1Fecha: string | null;
  conteo1UsuarioNombre: string | null;
  conteo2Valor: number | null;
  conteo2Fecha: string | null;
  conteo2UsuarioNombre: string | null;
  diferencia: number | null;
  diferenciaPorcentaje: string;
  statusConteo: string;
  primeraImpresionAt: string | null;
  ultimaReimpresionAt: string | null;
  printHistory: PrintHistory[] | undefined;
  countHistory: CountHistory[] | undefined;
  cancelado: boolean;
  comment: string | null;
}

interface PrintHistory {
  printedAt: string;
  printedByNombre: string;
  isExtraordinary: boolean;
}

interface CountHistory {
  countNumber: number;
  value: number;
  recordedAt: string;
  recordedByNombre: string;
  action: string;
}

interface Almacen {
  id: number;
  clave: string;
  nombre: string;
}

// Store
const periodoStore = usePeriodoStore();

// Estado
const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const marbetes = ref<Marbete[]>([]);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacenId = ref<number | null | 'todos'>(null);
const searchQuery = ref<string>('');

// Paginación (servidor-side)
const currentPage = ref<number>(0);  // 0-indexed para backend
const pageSize = ref<number>(20);
const totalElements = ref<number>(0);
const totalPages = ref<number>(0);

// Estados de carga y UI
const isLoading = ref<boolean>(false);
const selectedMarbeteDetail = ref<Marbete | null>(null);
const showDetailModal = ref<boolean>(false);
const filterEstado = ref<string>('');

// Filtros de ordenamiento
const sortBy = ref<string>('createdAt');  // folio, createdAt, impresoAt, ultimaReimpresionAt
const sortDirection = ref<string>('DESC'); // ASC, DESC

let searchDebounceTimeout: ReturnType<typeof setTimeout>;

// Cargar períodos
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      const periodoGuardado = periodos.value.find(p => p.id === periodoStore.periodoSeleccionado?.id);
      if (periodoGuardado) {
        selectedPeriodoId.value = periodoGuardado.id;
      }
    } else if (periodos.value.length > 0) {
      selectedPeriodoId.value = periodos.value[0]?.id || null;
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
      nombre: String(item.nameWarehouse || '')
    }));
    if (almacenes.value.length > 0 && !selectedAlmacenId.value) {
      selectedAlmacenId.value = almacenes.value[0]?.id || null;
    }
  } catch (error) {
    console.error('Error al cargar almacenes:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

// Cargar listado con paginación servidor-side
const loadMarbetes = async () => {
  if (!selectedPeriodoId.value || !selectedAlmacenId.value) {
    return;
  }

  try {
    isLoading.value = true;
    LoadAlert(true);

    const params: any = {
      periodId: selectedPeriodoId.value,
      page: currentPage.value,              // 0-indexed
      size: pageSize.value,                 // 20
      searchText: searchQuery.value.trim() || '',
      sortBy: sortBy.value,
      sortDirection: sortDirection.value
    };

    // Solo incluir warehouseId si NO es "todos"
    if (selectedAlmacenId.value && selectedAlmacenId.value !== 'todos') {
      params.warehouseId = selectedAlmacenId.value;
    }

    const response = await axiosConfiguration.doGet('/labels/full-list', params);

    LoadAlert(false);

    // Destructurar respuesta
    const data = response.data.data || [];
    const pagination = response.data.pagination || {};

    // Actualizar paginación desde backend
    totalElements.value = pagination.totalElements || 0;
    totalPages.value = pagination.totalPages || 0;

    // Aplicar filtro por estado (cliente-side)
    let marbe: Marbete[] = data;
    if (filterEstado.value) {
      marbe = marbe.filter((m: Marbete) => m.estado === filterEstado.value);
    }

    marbetes.value = marbe;


    ToastSuccess('Éxito', `Página ${currentPage.value + 1} de ${totalPages.value} (Total: ${totalElements.value})`);
  } catch (error) {
    LoadAlert(false);
    console.error('Error al cargar marbetes:', error);
    ToastError('Error', 'No se pudieron cargar los marbetes');
    marbetes.value = [];
  } finally {
    isLoading.value = false;
  }
};

// Aplicar filtros
// Watchers
watch(selectedPeriodoId, () => {
  if (selectedPeriodoId.value) {
    const periodo = periodos.value.find(p => p.id === selectedPeriodoId.value);
    if (periodo) {
      periodoStore.setPeriodo(periodo);
    }
  }
  currentPage.value = 0;  // Reset a página 0
  loadMarbetes();
});

watch(selectedAlmacenId, () => {
  currentPage.value = 0;  // Reset a página 0
  loadMarbetes();
});

watch(searchQuery, () => {
  if (searchDebounceTimeout) clearTimeout(searchDebounceTimeout);
  searchDebounceTimeout = setTimeout(() => {
    currentPage.value = 0;  // Reset a página 0
    loadMarbetes();
  }, 500);
});

watch(filterEstado, () => {
  currentPage.value = 0;  // Reset a página 0
  loadMarbetes();
});

watch(sortBy, () => {
  currentPage.value = 0;  // Reset a página 0
  loadMarbetes();
});

watch(sortDirection, () => {
  currentPage.value = 0;  // Reset a página 0
  loadMarbetes();
});

// Watcher para cambios de página
watch(currentPage, () => {
  loadMarbetes();
});

// Ir a una página específica (siempre recarga aunque sea la misma)
const goToPage = (page: number) => {
  currentPage.value = page;
  // Forzar carga incluso si es la misma página
  loadMarbetes();
};

// Mostrar detalle de marbete
const mostrarDetalle = (marbete: Marbete) => {
  // Normalizar arrays para evitar undefined
  const marbeteNormalizado: Marbete = {
    ...marbete,
    countHistory: marbete.countHistory || [],
    printHistory: marbete.printHistory || []
  };
  selectedMarbeteDetail.value = marbeteNormalizado;
  showDetailModal.value = true;
};

// Formatear fecha
const formatDate = (date: string | null): string => {
  if (!date) return 'N/A';
  return new Date(date).toLocaleDateString('es-ES', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const formatNumber = (value: number | null): string => {
  if (value === null) return '-';
  return new Intl.NumberFormat('es-MX').format(value);
};

// Obtener badge de estado
const getEstadoBadge = (estado: string) => {
  const styles: Record<string, { bg: string; color: string }> = {
    'IMPRESO': { bg: '#4CAF50', color: 'white' },
    'GENERADO': { bg: '#2196F3', color: 'white' },
    'PENDIENTE': { bg: '#FF9800', color: 'white' },
    'CANCELADO': { bg: '#f44336', color: 'white' }
  };
  return styles[estado] || { bg: '#9E9E9E', color: 'white' };
};

// Obtener badge de conteo
const getConteoStatus = (status: string) => {
  const styles: Record<string, { bg: string; color: string }> = {
    'COMPLETO': { bg: '#4CAF50', color: 'white' },
    'INCOMPLETO': { bg: '#FF9800', color: 'white' },
    'PENDIENTE': { bg: '#2196F3', color: 'white' }
  };
  return styles[status] || { bg: '#9E9E9E', color: 'white' };
};

// Cerrar modal
const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedMarbeteDetail.value = null;
};

// Obtener páginas adyacentes para paginador
const getAdjacentPages = (): number[] => {
  const pages: number[] = [];
  const startPage = Math.max(3, currentPage.value - 1);
  const endPage = Math.min(totalPages.value - 2, currentPage.value + 1);
  for (let i = startPage; i <= endPage; i++) {
    pages.push(i);
  }
  return pages;
};

onMounted(() => {
  const init = async () => {
    await loadPeriodos();
    await loadAlmacenes();
  };
  init();
});
</script>

<template>
  <div class="listado-completo-marbetes">
    <!-- Filtros -->
    <div class="section-card">
      <div class="title-section">
        <h1 class="section-title">📊 Listado Completo de Marbetes</h1>
        <p class="subtitle">Consulta detallada con historial de conteos e impresiones</p>
      </div>

      <div class="filters-row">
        <div class="filter-group">
          <label>Período:</label>
          <select v-model.number="selectedPeriodoId">
            <option :value="null" disabled>Selecciona un período</option>
            <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
              {{ new Date(periodo.date).toLocaleDateString('es-ES') }} - {{ periodo.comments }}
            </option>
          </select>
        </div>

        <div class="filter-group">
          <label>Almacén:</label>
          <select v-model="selectedAlmacenId">
            <option :value="null" disabled>Selecciona un almacén</option>
            <option value="todos"> TODOS LOS ALMACENES</option>
            <option v-for="almacen in almacenes" :key="almacen.id" :value="almacen.id">
              {{ almacen.clave }} - {{ almacen.nombre }}
            </option>
          </select>
        </div>

        <div class="filter-group">
          <label>Estado:</label>
          <select v-model="filterEstado">
            <option value="">Todos los estados</option>
            <option value="IMPRESO">IMPRESO</option>
            <option value="GENERADO">GENERADO</option>
            <option value="PENDIENTE">PENDIENTE</option>
            <option value="CANCELADO">CANCELADO</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Ordenar por:</label>
          <select v-model="sortBy">
            <option value="createdAt">Más Recientes</option>
            <option value="folio">Folio</option>
            <option value="impresoAt">Impresión</option>
            <option value="ultimaReimpresionAt">Última Reimpresión</option>
          </select>
        </div>

        <div class="filter-group">
          <label>Orden:</label>
          <select v-model="sortDirection">
            <option value="DESC">Descendente (↓)</option>
            <option value="ASC">Ascendente (↑)</option>
          </select>
        </div>

        <div class="filter-group search">
          <label>Buscar:</label>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Folio, clave, producto, usuario..."
          />
        </div>
      </div>
    </div>

    <!-- Tabla de marbetes -->
    <div class="table-section">
      <div class="table-info">
        <span class="record-count">{{ totalElements }} registros encontrados</span>
        <span v-if="marbetes.length > 0" class="page-info">
          Mostrando {{ currentPage * pageSize + 1 }} a {{ Math.min((currentPage + 1) * pageSize, totalElements) }} de {{ totalElements }}
        </span>
      </div>

      <div v-if="isLoading" class="loading-spinner">
        <p>Cargando marbetes...</p>
      </div>

      <table v-else-if="marbetes.length > 0" class="marbetes-table">
        <thead>
          <tr>
            <th>Folio</th>
            <th>Producto</th>
            <th>Clave</th>
            <th>Almacén</th>
            <th>Estado</th>
            <th>C1</th>
            <th>C2</th>
            <th>Diferencia</th>
            <th>Conteo</th>
            <th>Impresión</th>
            <th>Comentario</th>
            <th>Acción</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="marbete in marbetes" :key="marbete.folio">
            <td class="folio-cell">
              <span class="folio-badge">{{ marbete.folio }}</span>
            </td>
            <td class="producto-cell" :title="marbete.nombreProducto">
              {{ marbete.nombreProducto }}
            </td>
            <td class="clave-cell">{{ marbete.claveProducto }}</td>
            <td class="almacen-cell">{{ marbete.claveAlmacen }} - {{ marbete.nombreAlmacen }}</td>
            <td class="estado-cell">
              <span
                class="badge"
                :style="{
                  backgroundColor: getEstadoBadge(marbete.estado).bg,
                  color: getEstadoBadge(marbete.estado).color
                }"
              >
                {{ marbete.estado }}
              </span>
            </td>
            <td class="conteo-cell">
              {{ marbete.conteo1Valor !== null ? formatNumber(marbete.conteo1Valor) : '-' }}
            </td>
            <td class="conteo-cell">
              {{ marbete.conteo2Valor !== null ? formatNumber(marbete.conteo2Valor) : '-' }}
            </td>
            <td class="diferencia-cell">
              <span
                v-if="marbete.diferencia !== null"
                :class="[
                  'diferencia-badge',
                  marbete.diferencia === 0 ? 'igual' : marbete.diferencia > 0 ? 'positiva' : 'negativa'
                ]"
              >
                {{ marbete.diferencia > 0 ? '+' : '' }}{{ formatNumber(marbete.diferencia) }}
              </span>
              <span v-else class="diferencia-badge neutral">-</span>
            </td>
            <td class="status-cell">
              <span
                class="badge"
                :style="{
                  backgroundColor: getConteoStatus(marbete.statusConteo).bg,
                  color: getConteoStatus(marbete.statusConteo).color
                }"
              >
                {{ marbete.statusConteo }}
              </span>
            </td>
            <td class="impresion-cell">
              <span v-if="marbete.primeraImpresionAt" class="impreso-check">✓ Impreso</span>
              <span v-else class="no-impreso">Sin imprimir</span>
            </td>
            <td class="comentario-cell">
              <span v-if="marbete.comment" :title="marbete.comment" class="comentario-text">{{ marbete.comment }}</span>
              <span v-else class="comentario-vacio">-</span>
            </td>
            <td class="accion-cell">
              <button @click="mostrarDetalle(marbete)" class="btn-detalles">
                Ver Detalles
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-else class="no-data">
        <p>No se encontraron marbetes con los filtros seleccionados</p>
      </div>

      <!-- Paginación - SIEMPRE VISIBLE -->
      <div class="pagination-section">
        <div class="pagination-info-detailed">
          <span class="total-info">
            Total: <strong>{{ totalElements }}</strong> registros
          </span>
          <span class="page-indicator">
            Página <strong>{{ currentPage + 1 }}</strong> de <strong>{{ totalPages }}</strong>
          </span>
          <span class="items-info">
            Mostrando <strong>{{ Math.min(pageSize, marbetes.length) }}</strong> de <strong>{{ totalElements }}</strong> registros
          </span>
        </div>

        <div class="pagination-controls">
          <button
            @click="goToPage(currentPage - 1)"
            :disabled="currentPage === 0"
            class="btn-paginate"
          >
            ← Anterior
          </button>

          <div class="page-numbers">
            <button
              v-if="totalPages <= 7"
              v-for="page in totalPages"
              :key="page"
              @click="goToPage(page - 1)"
              :class="['page-btn', { active: currentPage === page - 1 }]"
            >
              {{ page }}
            </button>

            <!-- Si hay muchas páginas, mostrar primeras, últimas y actuales -->
            <template v-else>
              <button
                v-for="page in [0, 1]"
                :key="page"
                @click="goToPage(page)"
                :class="['page-btn', { active: currentPage === page }]"
              >
                {{ page + 1 }}
              </button>

              <span v-if="currentPage > 3" class="page-ellipsis">...</span>

              <button
                v-for="page in getAdjacentPages()"
                :key="page"
                @click="goToPage(page)"
                :class="['page-btn', { active: currentPage === page }]"
              >
                {{ page + 1 }}
              </button>

              <span v-if="currentPage < totalPages - 4" class="page-ellipsis">...</span>

              <button
                v-for="page in [totalPages - 2, totalPages - 1]"
                :key="page"
                @click="goToPage(page)"
                :class="['page-btn', { active: currentPage === page }]"
              >
                {{ page + 1 }}
              </button>
            </template>
          </div>

          <button
            @click="goToPage(currentPage + 1)"
            :disabled="currentPage === totalPages - 1"
            class="btn-paginate"
          >
            Siguiente →
          </button>
        </div>
      </div>
    </div>

    <!-- Modal de detalles -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>Detalle del Marbete #{{ selectedMarbeteDetail?.folio }}</h2>
          <button class="btn-close" @click="closeDetailModal">✕</button>
        </div>

        <div class="modal-body" v-if="selectedMarbeteDetail">
          <!-- Información básica -->
          <div class="detail-section">
            <h3>Información Básica</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>Folio:</label>
                <span>{{ selectedMarbeteDetail.folio }}</span>
              </div>
              <div class="detail-item">
                <label>Estado:</label>
                <span
                  :style="{
                    backgroundColor: getEstadoBadge(selectedMarbeteDetail.estado).bg,
                    color: getEstadoBadge(selectedMarbeteDetail.estado).color,
                    padding: '4px 8px',
                    borderRadius: '3px',
                    display: 'inline-block',
                    fontWeight: 'bold'
                  }"
                >
                  {{ selectedMarbeteDetail.estado }}
                </span>
              </div>
              <div class="detail-item">
                <label>Producto:</label>
                <span>{{ selectedMarbeteDetail.nombreProducto }}</span>
              </div>
              <div class="detail-item">
                <label>Clave:</label>
                <span>{{ selectedMarbeteDetail.claveProducto }}</span>
              </div>
              <div class="detail-item">
                <label>Almacén:</label>
                <span>{{ selectedMarbeteDetail.claveAlmacen }} - {{ selectedMarbeteDetail.nombreAlmacen }}</span>
              </div>
              <div class="detail-item">
                <label>Existencias Teóricas:</label>
                <span>{{ formatNumber(selectedMarbeteDetail.existenciasTeoricas) }}</span>
              </div>
            </div>
          </div>

          <!-- Información de conteos -->
          <div class="detail-section">
            <h3>Información de Conteos</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>Conteo 1:</label>
                <span>{{ selectedMarbeteDetail.conteo1Valor !== null ? formatNumber(selectedMarbeteDetail.conteo1Valor) : '-' }}</span>
              </div>
              <div class="detail-item" v-if="selectedMarbeteDetail.conteo1Fecha">
                <label>Fecha C1:</label>
                <span>{{ formatDate(selectedMarbeteDetail.conteo1Fecha) }}</span>
              </div>
              <div class="detail-item" v-if="selectedMarbeteDetail.conteo1UsuarioNombre">
                <label>Usuario C1:</label>
                <span>{{ selectedMarbeteDetail.conteo1UsuarioNombre }}</span>
              </div>
              <div class="detail-item">
                <label>Conteo 2:</label>
                <span>{{ selectedMarbeteDetail.conteo2Valor !== null ? formatNumber(selectedMarbeteDetail.conteo2Valor) : '-' }}</span>
              </div>
              <div class="detail-item" v-if="selectedMarbeteDetail.conteo2Fecha">
                <label>Fecha C2:</label>
                <span>{{ formatDate(selectedMarbeteDetail.conteo2Fecha) }}</span>
              </div>
              <div class="detail-item" v-if="selectedMarbeteDetail.conteo2UsuarioNombre">
                <label>Usuario C2:</label>
                <span>{{ selectedMarbeteDetail.conteo2UsuarioNombre }}</span>
              </div>
              <div class="detail-item">
                <label>Diferencia:</label>
                <span>{{ selectedMarbeteDetail.diferencia !== null ? formatNumber(selectedMarbeteDetail.diferencia) : '-' }}</span>
              </div>
              <div class="detail-item">
                <label>Porcentaje:</label>
                <span>{{ selectedMarbeteDetail.diferenciaPorcentaje }}</span>
              </div>
              <div class="detail-item">
                <label>Status:</label>
                <span
                  :style="{
                    backgroundColor: getConteoStatus(selectedMarbeteDetail.statusConteo).bg,
                    color: getConteoStatus(selectedMarbeteDetail.statusConteo).color,
                    padding: '4px 8px',
                    borderRadius: '3px',
                    display: 'inline-block',
                    fontWeight: 'bold'
                  }"
                >
                  {{ selectedMarbeteDetail.statusConteo }}
                </span>
              </div>
            </div>
          </div>

          <!-- Historial de conteos -->
          <div v-if="selectedMarbeteDetail.countHistory && selectedMarbeteDetail.countHistory.length > 0" class="detail-section">
            <h3>Historial de Conteos</h3>
            <table class="history-table">
              <thead>
                <tr>
                  <th>Conteo</th>
                  <th>Valor</th>
                  <th>Fecha</th>
                  <th>Usuario</th>
                  <th>Acción</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in selectedMarbeteDetail.countHistory" :key="idx">
                  <td>C{{ item.countNumber }}</td>
                  <td>{{ formatNumber(item.value) }}</td>
                  <td>{{ formatDate(item.recordedAt) }}</td>
                  <td>{{ item.recordedByNombre }}</td>
                  <td>{{ item.action }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Historial de impresiones -->
          <div v-if="selectedMarbeteDetail.printHistory && selectedMarbeteDetail.printHistory.length > 0" class="detail-section">
            <h3>Historial de Impresiones</h3>
            <table class="history-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Usuario</th>
                  <th>Tipo</th>
                  <th>Descripción</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in selectedMarbeteDetail.printHistory" :key="idx">
                  <td>{{ formatDate(item.printedAt) }}</td>
                  <td>{{ item.printedByNombre }}</td>
                  <td>{{ item.isExtraordinary ? 'Extraordinaria' : 'Normal' }}</td>
                  <td>{{ item.isExtraordinary ? '🔁 Reimpresión' : '✓ Primera' }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Información de registro -->
          <div class="detail-section">
            <h3>Información de Registro</h3>
            <div class="detail-grid">
              <div class="detail-item">
                <label>Creado por:</label>
                <span>{{ selectedMarbeteDetail.createdByFullName }}</span>
              </div>
              <div class="detail-item">
                <label>Fecha de Creación:</label>
                <span>{{ formatDate(selectedMarbeteDetail.createdAt) }}</span>
              </div>
              <div class="detail-item" v-if="selectedMarbeteDetail.primeraImpresionAt">
                <label>Primera Impresión:</label>
                <span>{{ formatDate(selectedMarbeteDetail.primeraImpresionAt) }}</span>
              </div>
              <div class="detail-item" v-if="selectedMarbeteDetail.ultimaReimpresionAt">
                <label>Última Reimpresión:</label>
                <span>{{ formatDate(selectedMarbeteDetail.ultimaReimpresionAt) }}</span>
              </div>
              <div class="detail-item">
                <label>Cancelado:</label>
                <span>{{ selectedMarbeteDetail.cancelado ? '❌ Sí' : '✓ No' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.listado-completo-marbetes {
  padding: 5px;
}

.section-card {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.title-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.subtitle {
  font-size: 13px;
  color: #666;
  margin: 5px 0 0 0;
}

.filters-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.filter-group label {
  font-weight: 500;
  color: #555;
  font-size: 13px;
}

.filter-group select,
.filter-group input {
  padding: 8px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 13px;
  background: white;
}

.filter-group select:focus,
.filter-group input:focus {
  outline: none;
  border-color: #666;
}

.filter-group.search {
  grid-column: span 1;
}

.table-section {
  background: white;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 20px;
}

.table-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.record-count {
  font-weight: 600;
  color: #333;
}

.page-info {
  font-size: 12px;
  color: #666;
}

.loading-spinner {
  text-align: center;
  padding: 40px;
  color: #666;
}

.marbetes-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.marbetes-table thead {
  background: #f5f5f5;
  border-bottom: 2px solid #ddd;
}

.marbetes-table thead th {
  padding: 12px 8px;
  text-align: left;
  font-weight: 600;
  color: #333;
}

.marbetes-table tbody tr {
  border-bottom: 1px solid #eee;
}

.marbetes-table tbody tr:hover {
  background: #f9f9f9;
}

.marbetes-table td {
  padding: 10px 8px;
}

.folio-badge {
  background: #2196F3;
  color: white;
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
}

.badge {
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  font-size: 11px;
  display: inline-block;
}

.diferencia-badge {
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
}

.diferencia-badge.igual {
  background: #c8e6c9;
  color: #2e7d32;
}

.diferencia-badge.positiva {
  background: #fff3cd;
  color: #856404;
}

.diferencia-badge.negativa {
  background: #ffcdd2;
  color: #c62828;
}

.diferencia-badge.neutral {
  background: #f5f5f5;
  color: #999;
}

.impreso-check {
  color: #4CAF50;
  font-weight: 600;
}

.no-impreso {
  color: #f44336;
  font-weight: 600;
}

.comentario-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comentario-text {
  color: #555;
  font-size: 12px;
  cursor: help;
}

.comentario-vacio {
  color: #ccc;
  font-style: italic;
}

.btn-detalles {
  padding: 6px 12px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 3px;
  font-size: 12px;
  cursor: pointer;
  font-weight: 500;
}

.btn-detalles:hover {
  background: #1976D2;
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
}

.pagination-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 15px 0;
  border-top: 1px solid #eee;
  margin-top: 20px;
}

.pagination-info-detailed {
  display: flex;
  justify-content: space-around;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  padding: 10px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}

.total-info,
.page-indicator,
.items-info {
  color: #555;
}

.total-info strong,
.page-indicator strong,
.items-info strong {
  color: #2196F3;
  font-weight: 700;
}

.pagination-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}

.btn-paginate {
  padding: 8px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
}

.btn-paginate:hover:not(:disabled) {
  background: #f5f5f5;
}

.btn-paginate:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 5px;
}

.page-btn {
  padding: 6px 10px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
}

.page-btn:hover {
  background: #f5f5f5;
}

.page-btn.active {
  background: #2196F3;
  color: white;
  border-color: #2196F3;
}

.page-ellipsis {
  padding: 6px 5px;
  color: #999;
  font-weight: 600;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  max-width: 800px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #ddd;
  background: #f8f8f8;
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
}

.btn-close:hover {
  color: #333;
}

.modal-body {
  padding: 20px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h3 {
  margin: 0 0 15px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #2196F3;
  padding-bottom: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.detail-item label {
  font-weight: 600;
  color: #555;
  font-size: 12px;
}

.detail-item span {
  color: #333;
  font-size: 13px;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin-top: 10px;
}

.history-table thead {
  background: #f5f5f5;
  border-bottom: 1px solid #ddd;
}

.history-table thead th {
  padding: 10px;
  text-align: left;
  font-weight: 600;
  color: #333;
}

.history-table tbody tr {
  border-bottom: 1px solid #eee;
}

.history-table tbody td {
  padding: 10px;
}

/* Sección de comentarios */
.comment-box {
  background: #f8f9fa;
  border-left: 4px solid #2196F3;
  padding: 12px 15px;
  border-radius: 4px;
  margin-top: 10px;
}

.comment-text {
  color: #333;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.comment-empty {
  color: #999;
  font-size: 13px;
  font-style: italic;
  margin: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .filters-row {
    grid-template-columns: 1fr;
  }

  .marbetes-table {
    font-size: 11px;
  }

  .marbetes-table td,
  .marbetes-table th {
    padding: 8px 4px;
  }

  .modal-content {
    width: 95%;
    max-height: 90vh;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>

