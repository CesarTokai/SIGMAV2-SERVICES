<script setup lang="ts">
import {ref, onMounted, computed, watch} from 'vue';
import axios from '@/config/axiosConfig';
import {ToastError, VoidAlert} from '@/utils/SweetAlert';
import Swal from 'sweetalert2';

interface Warehouse {
  id: number;
  warehouseKey: string;
  nameWarehouse: string;
  observations: string;
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
  createdByEmail: string | null;
  updatedByEmail: string | null;
  assignedUsersCount: number;
  deleted: boolean;
}

interface WarehouseForm {
  warehouseKey: string;
  nameWarehouse: string;
  observations: string;
}

const getWarehouseKeyNumber = (key: string): number => {
  const match = key.match(/\d+/g);
  if (!match) return Number.MAX_SAFE_INTEGER;
  const value = Number(match[match.length - 1]);
  return Number.isFinite(value) ? value : Number.MAX_SAFE_INTEGER;
};

// Estado
const warehouses = ref<Warehouse[]>([]);
const currentView = ref<'list' | 'create' | 'edit'>('list');
const loading = ref(false);
const searchTerm = ref('');
const selectedWarehouse = ref<Warehouse | null>(null);

// Formulario
const warehouseForm = ref<WarehouseForm>({
  warehouseKey: '',
  nameWarehouse: '',
  observations: ''
});

// Estados para paginación
const page = ref(0);
const pageSize = ref(100);
const totalPages = ref(1);
const totalElements = ref(0);
const showImportModal = ref(false);
const showConfirmModal = ref(false);
const importFile = ref<File | null>(null);


// Cargar almacenes
const loadWarehouses = async () => {
  loading.value = true;
  try {
    const url = `/warehouses?page=${page.value}&size=${pageSize.value}&sortBy=warehouseKey&sortDir=asc&search=false`;
    console.log('🔵 [GET] /warehouses - Parámetros:', {
      page: page.value,
      pageSize: pageSize.value
    });

    const response = await axios.doGet(url);
    console.log('✅ [GET] /warehouses - Respuesta:', response.data);

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
      totalPages.value = response.data.totalPages;
      totalElements.value = response.data.totalElements;
      console.log('📊 Almacenes cargados:', warehouses.value.length, 'registros');
    }
  } catch (error: any) {
    console.error('❌ [GET] /warehouses - Error:', error.response?.data || error.message);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  } finally {
    loading.value = false;
  }
};

// Filtrar almacenes
const filteredWarehouses = ref<Warehouse[]>([]);
const filterWarehouses = () => {
  if (!searchTerm.value || !searchTerm.value.trim()) {
    filteredWarehouses.value = warehouses.value;
    return;
  }
  const term = searchTerm.value.trim().toLowerCase();
  filteredWarehouses.value = warehouses.value.filter(warehouse => {
    const key = (warehouse.warehouseKey || '').toLowerCase();
    const name = (warehouse.nameWarehouse || '').toLowerCase();
    const obs = (warehouse.observations || '').toLowerCase();
    const users = String(warehouse.assignedUsersCount || '').toLowerCase();
    const created = warehouse.createdAt ? new Date(warehouse.createdAt).toLocaleDateString('es-MX', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    }).toLowerCase() : '';
    return (
        key.includes(term) ||
        name.includes(term) ||
        obs.includes(term) ||
        users.includes(term) ||
        created.includes(term)
    );
  });
};

// Cambiar vista
const changeView = (view: 'list' | 'create' | 'edit', warehouse?: Warehouse) => {
  currentView.value = view;

  if (view === 'list') {
    resetForm();
  } else if (view === 'create') {
    resetForm();
    showImportModal.value = true;
  } else if (view === 'edit' && warehouse) {
    selectedWarehouse.value = warehouse;
    warehouseForm.value = {
      warehouseKey: warehouse.warehouseKey,
      nameWarehouse: warehouse.nameWarehouse,
      observations: warehouse.observations
    };
    showImportModal.value = true;
  }
};

// Resetear formulario
const resetForm = () => {
  warehouseForm.value = {
    warehouseKey: '',
    nameWarehouse: '',
    observations: ''
  };
  selectedWarehouse.value = null;
};

// Crear almacén
const createWarehouse = async () => {
  if (!validateForm()) return;

  loading.value = true;
  try {
    const payload = warehouseForm.value;
    console.log('🟢 [POST] /warehouses - Payload:', payload);

    const response = await axios.doPost('/warehouses', payload);
    console.log('✅ [POST] /warehouses - Respuesta:', response.data);

    if (response.data.success) {
      console.log('📦 Almacén creado exitosamente. ID:', response.data.data?.id);
      await Swal.fire({
        icon: 'success',
        title: '¡Éxito!',
        text: 'Almacén creado exitosamente',
        confirmButtonColor: '#10b981',
        timer: 2000
      });
      await loadWarehouses();
      closeImportModal(); // Cierra el modal correctamente
    } else if (response.data.message) {
      console.warn('⚠️ [POST] /warehouses - Mensaje del servidor:', response.data.message);
      ToastError('Error', response.data.message);
    } else {
      ToastError('Error', 'No se pudo crear el almacén');
    }
  } catch (error: any) {
    console.error('❌ [POST] /warehouses - Error:', error.response?.data || error.message);
    const msg = error?.response?.data?.message || error?.message || 'No se pudo crear el almacén';
    ToastError('Error', msg);
  } finally {
    loading.value = false;
  }
};

// Actualizar almacén
const updateWarehouse = async () => {
  if (!validateForm() || !selectedWarehouse.value) return;

  loading.value = true;
  try {
    const payload = warehouseForm.value;
    const warehouseId = selectedWarehouse.value.id;
    console.log('🟡 [PUT] /warehouses/' + warehouseId + ' - Payload:', payload);
    console.log('📋 Cambios detectados:', getChanges.value);

    const response = await axios.doPut(
        `/warehouses/${warehouseId}`,
        payload
    );
    console.log('✅ [PUT] /warehouses/' + warehouseId + ' - Respuesta:', response.data);

    if (response.data.success) {
      console.log('✏️ Almacén actualizado exitosamente');
      await Swal.fire({
        icon: 'success',
        title: '¡Éxito!',
        text: 'Almacén actualizado exitosamente',
        confirmButtonColor: '#10b981',
        timer: 2000
      });
      await loadWarehouses();
      closeImportModal(); // Cierra el modal correctamente
      showConfirmModal.value = false;
    } else if (response.data.message) {
      console.warn('⚠️ [PUT] /warehouses/' + warehouseId + ' - Mensaje:', response.data.message);
      ToastError('Error', response.data.message);
    } else {
      ToastError('Error', 'No se pudo actualizar el almacén');
    }
  } catch (error: any) {
    console.error('❌ [PUT] /warehouses - Error:', error.response?.data || error.message);
    const msg = error?.response?.data?.message || error?.message || 'No se pudo actualizar el almacén';
    ToastError('Error', msg);
  } finally {
    loading.value = false;
  }
};

// Validar si hubo cambios
const hasChanges = computed(() => {
  if (!selectedWarehouse.value) return false;
  return (
      warehouseForm.value.warehouseKey !== selectedWarehouse.value.warehouseKey ||
      warehouseForm.value.nameWarehouse !== selectedWarehouse.value.nameWarehouse ||
      warehouseForm.value.observations !== selectedWarehouse.value.observations
  );
});

// Obtener cambios para mostrar en modal
const getChanges = computed(() => {
  if (!selectedWarehouse.value) return [];
  const changes: any[] = [];

  if (warehouseForm.value.warehouseKey !== selectedWarehouse.value.warehouseKey) {
    changes.push({
      field: 'Clave del Almacén',
      old: selectedWarehouse.value.warehouseKey,
      new: warehouseForm.value.warehouseKey
    });
  }

  if (warehouseForm.value.nameWarehouse !== selectedWarehouse.value.nameWarehouse) {
    changes.push({
      field: 'Nombre del Almacén',
      old: selectedWarehouse.value.nameWarehouse,
      new: warehouseForm.value.nameWarehouse
    });
  }

  if (warehouseForm.value.observations !== selectedWarehouse.value.observations) {
    changes.push({
      field: 'Observaciones',
      old: selectedWarehouse.value.observations || '(vacío)',
      new: warehouseForm.value.observations || '(vacío)'
    });
  }

  return changes;
});

// Abrir modal de confirmación
const openConfirmModal = () => {
  if (!hasChanges.value) {
    ToastError('Info', 'No hay cambios para guardar');
    return;
  }
  showConfirmModal.value = true;
};

// Eliminar almacén
const deleteWarehouse = (warehouse: Warehouse) => {
  VoidAlert(
      'warning',
      '¿Estás seguro?',
      `¿Deseas eliminar el almacén "${warehouse.nameWarehouse}"? Esta acción no se puede deshacer.`,
      'Sí, eliminar',
      async () => {
        loading.value = true;
        try {
          console.log('🔴 [DELETE] /warehouses/' + warehouse.id);
          console.log('📦 Almacén a eliminar:', warehouse);

          const response = await axios.doDelete(`/warehouses/${warehouse.id}`);
          console.log('✅ [DELETE] /warehouses/' + warehouse.id + ' - Respuesta:', response.data);

          if (response.data.success) {
            console.log('🗑️ Almacén eliminado exitosamente');
            await Swal.fire({
              icon: 'success',
              title: '¡Eliminado!',
              text: 'Almacén eliminado exitosamente',
              confirmButtonColor: '#10b981',
              timer: 2000
            });
            await loadWarehouses();
          }
        } catch (error: any) {
          console.error('❌ [DELETE] /warehouses/' + warehouse.id + ' - Error:', error.response?.data || error.message);
          ToastError('Error', 'No se pudo eliminar el almacén');
        } finally {
          loading.value = false;
        }
      }
  );
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

// Validar formulario
const validateForm = (): boolean => {
  if (!warehouseForm.value.warehouseKey.trim()) {
    ToastError('Error', 'La clave del almacén es requerida');
    return false;
  }

  // Validar que la clave solo contenga letras mayúsculas, números y guiones medios
  const keyPattern = /^[A-Z0-9-]+$/;
  if (!keyPattern.test(warehouseForm.value.warehouseKey)) {
    ToastError('Error', 'La clave debe contener solo letras mayúsculas, números y guiones medios');
    return false;
  }

  if (!warehouseForm.value.nameWarehouse.trim()) {
    ToastError('Error', 'El nombre del almacén es requerido');
    return false;
  }
  return true;
};

// Convertir clave a mayúsculas y filtrar caracteres no válidos
const formatWarehouseKey = (event: Event) => {
  const input = event.target as HTMLInputElement;
  warehouseForm.value.warehouseKey = input.value.toUpperCase().replace(/[^A-Z0-9-]/g, '');
};

// Formatear fecha
const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('es-MX', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
};

// Montar componente
onMounted(() => {
  loadWarehouses();
});

// Cambiar página
const goToPage = async (newPage: number) => {
  if (newPage < 0 || newPage >= totalPages.value) return;
  page.value = newPage;
  await loadWarehouses();
};

// Cambiar tamaño de página
const changePageSize = async (event: Event) => {
  pageSize.value = Number((event.target as HTMLSelectElement).value);
  page.value = 0;
  await loadWarehouses();
};

// Abrir modal de importación y poner en modo 'create'
const openImportModal = () => {
  showImportModal.value = true;
  importFile.value = null;
  currentView.value = 'create';
};

// Cerrar modal y regresar a modo 'list'
const closeImportModal = () => {
  showImportModal.value = false;
  importFile.value = null;
  currentView.value = 'list';
  resetForm();
};


watch([warehouses, searchTerm], () => {
  filterWarehouses();
}, {immediate: true});
</script>

<template>
  <div class="warehouse-container">
    <div class="container-fluid">
      <div v-if="currentView === 'list'" class="warehouse-list">
        <!-- Header -->
        <div class="header-section">
          <div class="title-wrapper">
            <h1 class="page-title">Gestión de Almacenes</h1>
            <p class="subtitle">Administra todos los almacenes del sistema</p>

            <nav class="breadcrumb-nav" aria-label="breadcrumb">
              <ol class="breadcrumb-list">
                <li class="breadcrumb-item">
                  <router-link to="/Admin/AsignacionIndividual" class="breadcrumb-link">Asignación de almacenes -></router-link>
                </li>
              </ol>
            </nav>

          </div>
          <button class="btn-add btn-primary btn-import" @click="openImportModal">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            Nuevo Almacén
          </button>
        </div>

        <!-- Barra de búsqueda -->
        <div class="search-action-bar">
          <div class="search-wrapper">
            <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none"
                 stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
            <input v-model="searchTerm" type="text" placeholder="Buscar por nombre, clave o descripción..."
                   class="search-input"/>
          </div>
        </div>


        <!-- Loading -->
        <div v-if="loading" class="loading-container">
          <div class="spinner"></div>
          <p>Cargando almacenes...</p>
        </div>

        <!-- Tabla de almacenes -->
        <div v-else-if="filteredWarehouses.length > 0" class="table-container">
          <table class="warehouse-table">
            <thead>
            <tr>
              <th>Clave</th>
              <th>Nombre</th>
              <th>Observaciones</th>
              <th>Usuarios Asignados</th>
              <th>Fecha de Creación</th>
              <th>Acciones</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="warehouse in filteredWarehouses" :key="warehouse.id">
              <td>
                <span class="warehouse-key">{{ warehouse.warehouseKey }}</span>
              </td>
              <td>
                <strong>{{ warehouse.nameWarehouse }}</strong>
              </td>
              <td>
                <span class="observations">{{ warehouse.observations || 'Sin observaciones' }}</span>
              </td>
              <td class="text-center">
                <span class="badge">{{ warehouse.assignedUsersCount }}</span>
              </td>
              <td>{{ formatDate(warehouse.createdAt) }}</td>
              <td>
                <div class="action-buttons">
                  <button @click="changeView('edit', warehouse)" class="btn-action btn-edit" title="Editar">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M15.232 5.232l3.536 3.536M9 11l6.586-6.586a2 2 0 012.828 2.828L11.828 13.828 9 14l.172-2.828z"/>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 19h14"/>
                    </svg>
                  </button>
                  <button @click="deleteWarehouse(warehouse)" class="btn-action btn-delete" title="Eliminar">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>

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

        <!-- Sin resultados -->
        <div v-else class="no-results">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
          </svg>
          <h3>No se encontraron almacenes</h3>
          <p>{{ searchTerm ? 'Intenta con otro término de búsqueda' : 'Comienza creando tu primer almacén' }}</p>
        </div>
      </div>

      <div v-if="showImportModal" class="modal-overlay" @click.self="closeImportModal">
        <div class="modal-content">
          <div class="modal-header" :class="{ 'modal-header-edit': currentView === 'edit' }">
            <div class="modal-header-content">
              <h2>{{ currentView === 'create' ? '➕ Registrar Almacén' : '✏️ Actualizar Almacén' }}</h2>
              <p class="modal-subtitle">{{
                  currentView === 'create' ? 'Crea un nuevo almacén en el sistema' : 'Modifica la información del almacén'
                }}</p>
            </div>
            <button class="btn-close" @click="closeImportModal">&times;</button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="currentView === 'create' ? createWarehouse() : openConfirmModal()">
              <div class="form-group">
                <label for="warehouseKey" class="form-label">
                  Clave del Almacén <span class="required">*</span>
                </label>
                <input
                    id="warehouseKey"
                    v-model="warehouseForm.warehouseKey"
                    @input="formatWarehouseKey"
                    type="text"
                    class="form-control"
                    placeholder="Ej: ALM-001"
                    required
                />
                <small class="form-help-text">Solo letras mayúsculas, números y guiones medios</small>
              </div>
              <div class="form-group">
                <label for="nameWarehouse" class="form-label">
                  Nombre del Almacén <span class="required">*</span>
                </label>
                <input
                    id="nameWarehouse"
                    v-model="warehouseForm.nameWarehouse"
                    type="text"
                    class="form-control"
                    placeholder="Ej: Almacén Central"
                    required
                />
              </div>
              <div class="form-group">
                <label for="observations" class="form-label">
                  Observaciones
                </label>
                <textarea
                    id="observations"
                    v-model="warehouseForm.observations"
                    class="form-control form-textarea"
                    rows="4"
                    placeholder="Agrega notas o descripción adicional..."
                ></textarea>
              </div>
              <div class="modal-footer">
                <button type="button" @click="closeImportModal" class="btn btn-secondary">
                  ✕ Cancelar
                </button>
                <button v-if="currentView === 'create'" type="submit" class="btn btn-primary" :disabled="loading">
                  <span v-if="loading" class="spinner-small"></span>
                  {{ loading ? 'Registrando...' : '✓ Registrar Almacén' }}
                </button>
                <button v-else type="button" @click="openConfirmModal" class="btn btn-primary"
                        :disabled="loading || !hasChanges">
                  <span v-if="loading" class="spinner-small"></span>
                  {{ loading ? 'Procesando...' : '✓ Actualizar Almacén' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- Modal de Confirmación de Cambios -->
      <div v-if="showConfirmModal" class="modal-overlay" @click.self="showConfirmModal = false">
        <div class="modal-content modal-confirm">
          <div class="modal-header modal-header-confirm">
            <div class="modal-header-content">
              <h2>✓ Confirmar Cambios</h2>
              <p class="modal-subtitle">Revisa los cambios antes de guardar</p>
            </div>
            <button class="btn-close" @click="showConfirmModal = false">&times;</button>
          </div>
          <div class="modal-body">
            <div v-if="selectedWarehouse" class="confirm-content">
              <div class="warehouse-info">
                <p class="info-label">📦 Almacén a actualizar:</p>
                <p class="warehouse-name">{{ selectedWarehouse.nameWarehouse }}</p>
              </div>

              <div v-if="getChanges.length > 0" class="changes-list">
                <p class="changes-title">📝 Cambios a realizar:</p>
                <div v-for="(change, idx) in getChanges" :key="idx" class="change-item">
                  <div class="change-field">
                    <span class="field-name">{{ change.field }}</span>
                  </div>
                  <div class="change-values">
                    <div class="value-row">
                      <div class="value-item">
                        <span class="value-label">Anterior:</span>
                        <span class="value-old">{{ change.old }}</span>
                      </div>
                      <div class="value-arrow">→</div>
                      <div class="value-item">
                        <span class="value-label">Nuevo:</span>
                        <span class="value-new">{{ change.new }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="warning-box">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 9v2m0 4v2m.073-6.071l.318-1.318A1 1 0 0012 8a1 1 0 00-.927.729l-.318 1.318m0 2.144l.318 1.318A1 1 0 0012 16a1 1 0 00.927-.729l.318-1.318M12 2C6.477 2 2 6.477 2 12s4.477 10 10 10 10-4.477 10-10S17.523 2 12 2z"/>
                </svg>
                <span>Por favor, revisa los cambios antes de confirmar.</span>
              </div>
            </div>
          </div>
          <div class="modal-footer modal-footer-confirm">
            <button @click="showConfirmModal = false" class="btn btn-secondary" :disabled="loading">
              ✕ Cancelar
            </button>
            <button @click="updateWarehouse" class="btn btn-primary" :disabled="loading">
              <span v-if="loading" class="spinner-small"></span>
              {{ loading ? 'Actualizando...' : '✓ Confirmar Actualización' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.title-wrapper {
  flex: 1;
  min-width: 250px;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
}

.subtitle {
  color: #6b7280;
  font-size: 0.95rem;
  margin: 0.25rem 0 0 0;
}

.breadcrumb-list {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  font-size: 0.875rem;
}

.breadcrumb-item.active {
  color: #64748b;
  font-weight: 500;
}

.breadcrumb-link {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.breadcrumb-link:hover {
  color: #2563eb;
  text-decoration: underline;
}

.breadcrumb-separator {
  color: #cbd5e1;
  margin: 0 0.25rem;
}

/* Botón agregar */
.btn-add {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  border: none;
  border-radius: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-add svg {
  width: 1.1rem;
  height: 1.1rem;
}

.btn-add:hover {
  opacity: 0.9;
}

/* Barra de búsqueda */
.search-action-bar {
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 0.75rem 1rem;
  margin-bottom: 1.25rem;
}

.search-wrapper {
  position: relative;
  flex: 1;
}

.search-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  width: 1.1rem;
  height: 1.1rem;
  color: #9ca3af;
}

.search-input {
  width: 100%;
  padding: 0.625rem 1rem 0.625rem 2.75rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.5rem;
  font-size: 0.95rem;
  background: white;
  box-sizing: border-box;
}

.search-input:focus {
  outline: none;
  border-color: #28a745;
}

/* Loading */
.loading-container {
  text-align: center;
  padding: 4rem 2rem;
  color: #6b7280;
}

.spinner {
  border: 3px solid #f3f4f6;
  border-top: 3px solid #28a745;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

.spinner-small {
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  width: 14px;
  height: 14px;
  animation: spin 1s linear infinite;
  display: inline-block;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* Tabla */
.table-container {
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.warehouse-table {
  width: 100%;
  border-collapse: collapse;
}

.warehouse-table thead {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.warehouse-table th {
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.warehouse-table tbody tr {
  border-bottom: 1px solid #dee2e6;
}

.warehouse-table tbody tr:hover {
  background: #f8f9fa;
}

.warehouse-table tbody tr:last-child {
  border-bottom: none;
}

.warehouse-table td {
  padding: 15px;
  color: #495057;
  font-size: 14px;
  vertical-align: middle;
}

.warehouse-key {
  background: #20c997;
  color: white;
  padding: 0.2rem 0.65rem;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.82rem;
}

.observations {
  color: #64748b;
  font-size: 0.875rem;
}

.badge {
  background: #10b981;
  color: white;
  padding: 0.2rem 0.65rem;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.82rem;
  display: inline-block;
}

.text-center {
  text-align: center;
}

/* Botones de acción */
.action-buttons {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.btn-action {
  padding: 0.45rem;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-action svg {
  width: 1.1rem;
  height: 1.1rem;
  stroke: white;
}

.btn-edit {
  background: #f59e0b;
}

.btn-edit:hover {
  background: #d97706;
}

.btn-delete {
  background: #ef4444;
}

.btn-delete:hover {
  background: #dc2626;
}

/* Sin resultados */
.no-results {
  text-align: center;
  padding: 3rem 1rem;
  color: #9ca3af;
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.no-results svg {
  width: 3rem;
  height: 3rem;
  opacity: 0.4;
  margin-bottom: 0.75rem;
}

.no-results h3 {
  font-size: 1.25rem;
  color: #374151;
  margin: 0 0 0.375rem 0;
}

.no-results p {
  margin: 0;
  font-size: 0.9rem;
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

.btn-pagination:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-pagination:hover:not(:disabled) {
  background: #28a745;
  color: white;
  border-color: #28a745;
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

/* ======================================
   MODAL STYLING - IMPROVED
   ====================================== */

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: white;
  border-radius: 0.875rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  border: none;
  padding: 2rem 1.75rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.modal-header-edit {
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
}

.modal-header-confirm {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-bottom: 2px solid #93c5fd;
}

.modal-header-content h2 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 700;
  color: white;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.modal-header-confirm .modal-header-content h2 {
  color: #1e40af;
}

.modal-subtitle {
  margin: 0.5rem 0 0 0;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 400;
}

.modal-header-confirm .modal-subtitle {
  color: #0369a1;
}

.btn-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  font-size: 1.75rem;
  cursor: pointer;
  color: white;
  padding: 0;
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.btn-close:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: rotate(90deg);
}

.modal-body {
  padding: 2rem 1.75rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 0.75rem;
  font-size: 0.95rem;
  letter-spacing: 0.3px;
}

.required {
  color: #ef4444;
  font-weight: 700;
}

.form-control {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.625rem;
  font-size: 0.95rem;
  font-family: inherit;
  transition: all 0.3s ease;
  background: white;
}

.form-control::placeholder {
  color: #d1d5db;
}

.form-control:focus {
  outline: none;
  border-color: #28a745;
  box-shadow: 0 0 0 4px rgba(40, 167, 69, 0.1);
  background: #fafafa;
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
}

.form-textarea:focus {
  border-color: #28a745;
}

.form-help-text {
  display: block;
  color: #9ca3af;
  font-size: 0.825rem;
  margin-top: 0.5rem;
  font-weight: 400;
}

.modal-footer {
  border-top: 2px solid #e5e7eb;
  padding: 1.5rem 1.75rem;
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  background: #fafafa;
}

.modal-footer-confirm {
  background: #f0f9ff;
  border-top-color: #bfdbfe;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 0.625rem;
  border: none;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-height: 44px;
}

.btn-primary {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(40, 167, 69, 0.3);
}

.btn-primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}

.btn-secondary:hover:not(:disabled) {
  background: #d1d5db;
  transform: translateY(-1px);
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ======================================
   CONFIRM MODAL SPECIFIC
   ====================================== */

.confirm-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.warehouse-info {
  background: linear-gradient(135deg, #ecfdf5 0%, #f0fdf4 100%);
  border: 2px solid #86efac;
  border-radius: 0.75rem;
  padding: 1.25rem;
  backdrop-filter: blur(10px);
}

.info-label {
  margin: 0 0 0.5rem 0;
  font-size: 0.75rem;
  font-weight: 700;
  color: #059669;
  text-transform: uppercase;
  letter-spacing: 0.75px;
}

.warehouse-name {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #15803d;
}

.changes-list {
  background: white;
  border: 2px solid #f3f4f6;
  border-radius: 0.75rem;
  padding: 1.25rem;
}

.changes-title {
  margin: 0 0 1.25rem 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #1f2937;
}

.change-item {
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 2px solid #f3f4f6;
}

.change-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.change-field {
  margin-bottom: 0.75rem;
}

.field-name {
  display: inline-block;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  color: #374151;
  padding: 0.5rem 0.875rem;
  border-radius: 0.5rem;
  font-weight: 600;
  font-size: 0.875rem;
  border-left: 3px solid #28a745;
}

.change-values {
  margin-top: 0.75rem;
}

.value-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.value-item {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 160px;
}

.value-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 0.375rem;
}

.value-old {
  font-size: 0.9rem;
  color: #dc2626;
  font-family: 'Courier New', monospace;
  padding: 0.5rem 0.625rem;
  background: #fee2e2;
  border-radius: 0.5rem;
  word-break: break-word;
  border-left: 2px solid #dc2626;
}

.value-new {
  font-size: 0.9rem;
  color: #15803d;
  font-family: 'Courier New', monospace;
  padding: 0.5rem 0.625rem;
  background: #dcfce7;
  border-radius: 0.5rem;
  word-break: break-word;
  border-left: 2px solid #15803d;
}

.value-arrow {
  color: #9ca3af;
  font-weight: 700;
  font-size: 1.25rem;
  min-width: 2rem;
  text-align: center;
}

.warning-box {
  display: flex;
  gap: 0.875rem;
  align-items: flex-start;
  background: linear-gradient(135deg, #fef3c7 0%, #fef08a 100%);
  border: 2px solid #fcd34d;
  border-radius: 0.75rem;
  padding: 1rem;
  color: #92400e;
  font-size: 0.9rem;
  font-weight: 500;
}

.warning-box svg {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
  stroke: #d97706;
  margin-top: 0.125rem;
}


</style>

