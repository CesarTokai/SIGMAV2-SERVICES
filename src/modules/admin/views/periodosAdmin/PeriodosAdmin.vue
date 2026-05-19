<template>
  <div class="periodos-admin">
    <div class="container-fluid">

      <!-- Header -->
      <div class="header-section">
        <div class="title-wrapper">
          <h1 class="page-title">Gestión de Períodos</h1>
          <nav style="--bs-breadcrumb-divider: '>';" aria-label="breadcrumb">
            <ol class="breadcrumb">
              <li class="breadcrumb-item active" aria-current="page">Gestión de Períodos</li>
            </ol>
          </nav>
          <p class="subtitle">Gestión de períodos del sistema</p>
        </div>
        <button class="btn-add" @click="openAddModal">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          Agregar Período
        </button>
      </div>

      <!-- Barra de controles -->
      <div class="controls-section">
        <div class="search-wrapper">
          <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            type="text"
            v-model="searchQuery"
            class="search-input"
            placeholder="Buscar por período o comentarios..."
          />
        </div>
        <div class="filter-wrapper" v-if="periodos.length > 0">
          <select id="periodoSelect" v-model="selectedPeriodoId" @change="handlePeriodoChange" class="filter-select">
            <option :value="null" disabled>Selecciona un período</option>
            <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
              {{ formatDate(periodo.date) }} - {{ periodo.comments }}
            </option>
          </select>
        </div>
      </div>

      <!-- Tabla -->
      <div class="table-section" v-if="filteredPeriodos.length > 0">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Fecha</th>
              <th>Comentarios</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="periodo in filteredPeriodos" :key="periodo.id">
              <td class="id-cell">#{{ periodo.id }}</td>
              <td>{{ formatDate(periodo.date) }}</td>
              <td>{{ periodo.comments }}</td>
              <td>
                <span class="status-badge" :class="periodo.state === 'OPEN' ? 'status-active' : 'status-inactive'">
                  <span class="status-dot"></span>
                  {{ periodo.state }}
                </span>
              </td>
              <td>
                <div class="action-buttons">
                  <!-- Ver -->
                  <button class="btn-action btn-view" @click="viewPeriodo(periodo)" title="Ver detalles">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                  </button>
                  <!-- Editar -->
                  <button class="btn-action btn-edit" @click="openEditModal(periodo)" title="Editar">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536M9 11l6.586-6.586a2 2 0 012.828 2.828L11.828 13.828 9 14l.172-2.828z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 19h14" />
                    </svg>
                  </button>
                  <!-- Eliminar -->
                  <button class="btn-action btn-delete" @click="confirmDelete(periodo.id)" title="Eliminar">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Sin datos -->
      <div v-else class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
        </svg>
        <p>{{ searchQuery ? 'No se encontraron períodos.' : 'No hay períodos registrados.' }}</p>
      </div>

    </div>

    <!-- Modal agregar/editar -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ isEditing ? 'Editar Período' : 'Agregar Período' }}</h2>
          <button class="btn-close" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label for="fecha">Fecha del Período <span class="required">*</span></label>
            <input type="date" id="fecha" v-model="formData.fecha" class="form-control" :class="{ 'is-invalid': errors.fecha }" :disabled="isEditing" />
            <span v-if="errors.fecha" class="error-message">{{ errors.fecha }}</span>
          </div>
          <div class="form-group">
            <label for="comentarios">Comentarios <span class="required">*</span></label>
            <textarea id="comentarios" v-model="formData.comentarios" class="form-control" rows="4" placeholder="Ingrese comentarios sobre el período..." :class="{ 'is-invalid': errors.comentarios }"></textarea>
            <span v-if="errors.comentarios" class="error-message">{{ errors.comentarios }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeModal">Cancelar</button>
          <button class="btn-save" @click="savePeriodo">{{ isEditing ? 'Actualizar' : 'Guardar' }}</button>
        </div>
      </div>
    </div>

    <!-- Modal ver detalles -->
    <div v-if="showViewModal" class="modal-overlay" @click.self="closeViewModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Detalles del Período</h2>
          <button class="btn-close" @click="closeViewModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="detail-row"><strong>ID:</strong><span>{{ viewData.id }}</span></div>
          <div class="detail-row"><strong>Fecha:</strong><span>{{ formatDate(viewData.date) }}</span></div>
          <div class="detail-row"><strong>Comentarios:</strong><span>{{ viewData.comments }}</span></div>
          <div class="detail-row"><strong>Estado:</strong><span>{{ viewData.state }}</span></div>
          <div class="detail-row" v-if="viewData.updatedAt"><strong>Última Actualización:</strong><span>{{ formatDate(viewData.updatedAt) }}</span></div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeViewModal">Cerrar</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { usePeriodoStore } from '@/store/periodoStore';
import SearchBar from '@/components/SearchBar.vue';
import axiosConfiguration from '@/config/axiosConfig';
import { VoidAlert, ToastError, LoadAlert } from '@/utils/SweetAlert';
import Swal from 'sweetalert2';

const periodoStore = usePeriodoStore();
const selectedPeriodoId = ref<number | null>(null);
const periodos = ref<any[]>([]);
const searchQuery = ref('');
const showModal = ref(false);
const showViewModal = ref(false);
const isEditing = ref(false);
const currentPeriodoId = ref<number | null>(null);
const formData = ref({
  fecha: '',
  comentarios: ''
});
const viewData = ref<any>({});
const errors = ref<any>({});

const fetchPeriodos = async () => {
  try {
    LoadAlert(true);
    // Ajusta la URL según tu API
    const response = await axiosConfiguration.doGet('/periods?page=0&size=20');
    periodos.value = response.data.content;
    // Log para depuración: muestra las fechas crudas recibidas
    if (Array.isArray(response.data.content)) {
      response.data.content.forEach((p: any, i: number) => {
        console.log(`[Periodo #${i}] id=${p.id} date=`, p.date);
      });
    }
    // Validar si el periodo guardado sigue existiendo
    periodoStore.cargarPeriodoGuardado();
    const guardado = periodoStore.periodoSeleccionado;
    if (guardado && !periodos.value.some(p => p.id === guardado.id)) {
      // El periodo guardado ya no existe, limpiar
      periodoStore.setPeriodo(null);
      localStorage.removeItem('selectedPeriodo');
      selectedPeriodoId.value = null;
      console.warn('⚠️ El período guardado ya no existe. Se ha limpiado la selección.');
    } else if (guardado) {
      selectedPeriodoId.value = guardado.id;
    } else if (periodos.value.length > 0) {
      selectedPeriodoId.value = null; // Forzar selección manual
    }
    LoadAlert(false);
  } catch (error) {
    LoadAlert(false);
    console.error('Error al cargar los períodos:', error);
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};

onMounted(() => {
  fetchPeriodos();
  // Al cargar, usar el periodo guardado si existe
  periodoStore.cargarPeriodoGuardado();
  if (periodoStore.periodoSeleccionado) {
    selectedPeriodoId.value = periodoStore.periodoSeleccionado.id;
  } else if (periodos.value.length > 0) {
    selectedPeriodoId.value = null; // Forzar selección manual
  }
});

const handlePeriodoChange = () => {
  const periodo = periodos.value.find(p => p.id === selectedPeriodoId.value);
  if (periodo) {
    periodoStore.setPeriodo(periodo);
  }
};

const filteredPeriodos = computed(() => {
  const query = searchQuery.value.toLowerCase();
  return periodos.value.filter(periodo => {
    const date = (periodo.date || '').toLowerCase();
    const comments = (periodo.comments || '').toLowerCase();
    const state = (periodo.state || '').toLowerCase();
    return (
      date.includes(query) ||
      comments.includes(query) ||
      state.includes(query)
    );
  });
});

const openAddModal = () => {
  isEditing.value = false;
  currentPeriodoId.value = null;
  formData.value = {
    fecha: '',
    comentarios: ''
  };
  errors.value = {};
  showModal.value = true;
};

const openEditModal = (periodo: any) => {
  isEditing.value = true;
  currentPeriodoId.value = periodo.id;
  formData.value = {
    fecha: periodo.date,
    comentarios: periodo.comments
  };
  errors.value = {};
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
  formData.value = {
    fecha: '',
    comentarios: ''
  };
  errors.value = {};
};

const viewPeriodo = (periodo: any) => {
  viewData.value = { ...periodo };
  showViewModal.value = true;
};

const closeViewModal = () => {
  showViewModal.value = false;
  viewData.value = {};
};

const validateForm = (): boolean => {
  errors.value = {};
  let isValid = true;

  if (!formData.value.fecha) {
    errors.value.fecha = 'La fecha es requerida';
    isValid = false;
  }

  if (!formData.value.comentarios || formData.value.comentarios.trim() === '') {
    errors.value.comentarios = 'Los comentarios son requeridos';
    isValid = false;
  } else if (formData.value.comentarios.length < 10) {
    errors.value.comentarios = 'Los comentarios deben tener al menos 10 caracteres';
    isValid = false;
  }

  return isValid;
};

const registrarPeriodo = async () => {
  if (!validateForm()) {
    return;
  }
  const body = {
    date: formData.value.fecha,
    comments: formData.value.comentarios,
  };
  try {
    LoadAlert(true);
    await axiosConfiguration.doPost('/periods', body);
    LoadAlert(false);
    closeModal();
    await fetchPeriodos();
    Swal.fire({
      icon: 'success',
      title: '¡Registro exitoso!',
      text: 'El período se registró correctamente.',
      timer: 2000,
      showConfirmButton: false
    });
    console.log('Período registrado con éxito:', body);
  } catch (error: any) {
    LoadAlert(false);
    let mensaje = 'No se pudo guardar el período';
    if (error && error.response && error.response.data && error.response.data.message) {
      mensaje = error.response.data.message;
    }
    console.error('Error al guardar el período:', error);
    ToastError('Error', mensaje);
  }
};

const actualizarPeriodo = async () => {
  if (!validateForm() || !currentPeriodoId.value) {
    return;
  }
  const body = {
    comments: formData.value.comentarios
  };
  try {
    LoadAlert(true);
    await axiosConfiguration.doPut(`/periods/${currentPeriodoId.value}/comments`, body);
    LoadAlert(false);
    closeModal();
    await fetchPeriodos();
    Swal.fire({
      icon: 'success',
      title: '¡Actualización exitosa!',
      text: 'Los comentarios del período se actualizaron correctamente.',
      timer: 2000,
      showConfirmButton: false
    });
  } catch (error) {
    LoadAlert(false);
    console.error('Error al actualizar el período:', error);
    ToastError('Error', 'No se pudo actualizar el período');
  }
};

const savePeriodo = () => {
  if (isEditing.value) {
    actualizarPeriodo();
  } else {
    registrarPeriodo();
  }
};

const confirmDelete = (id: number) => {
  VoidAlert(
    'warning',
    '¿Estás seguro?',
    'Esta acción no se puede revertir',
    'Sí, eliminar',
    () => deletePeriodo(id)
  );
};

const deletePeriodo = async (id: number) => {
  try {
    LoadAlert(true);
    await axiosConfiguration.doDelete(`/periods/${id}`);
    LoadAlert(false);
    await fetchPeriodos();
    Swal.fire({
      icon: 'success',
      title: '¡Eliminado!',
      text: 'El período fue eliminado correctamente.',
      timer: 2000,
      showConfirmButton: false
    });
  } catch (error) {
    LoadAlert(false);
    console.error('Error al eliminar el período:', error);
    ToastError('Error', 'No se pudo eliminar el período');
  }
};

const formatDate = (date: any): string => {
  if (!date) return 'N/A';

  // 👇 Manejo si el backend devuelve array [year, month, day]
  if (Array.isArray(date)) {
    const [y, m, d] = date;
    const dateObj = new Date(y, m - 1, d);
    return dateObj.toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  if (typeof date === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      const [y, m, d] = date.split('-');
      const dateObj = new Date(Number(y), Number(m) - 1, Number(d));
      return dateObj.toLocaleDateString('es-ES', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    }
    if (/^\d{4}-\d{2}-\d{2}T/.test(date)) {
      return new Date(date).toLocaleDateString('es-ES', {
        year: 'numeric', month: 'long', day: 'numeric'
      });
    }
  }

  try {
    return new Date(date).toLocaleDateString('es-ES', {
      year: 'numeric', month: 'long', day: 'numeric'
    });
  } catch {
    return String(date);
  }
};

// Si el usuario no selecciona ningún período, limpiar el periodo guardado
watch(selectedPeriodoId, (nuevo) => {
  if (!nuevo) {
    periodoStore.setPeriodo(null);
    localStorage.removeItem('selectedPeriodo');
    console.info('ℹ️ Se limpió el período guardado porque no hay ninguno seleccionado.');
  }
});

</script>

<style scoped>
/* Header */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.title-wrapper { flex: 1; min-width: 250px; }

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

/* Breadcrumb */
.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  padding: 0.4rem 0;
  margin: 0.25rem 0;
  list-style: none;
  background: transparent;
  font-size: 0.875rem;
}

.breadcrumb-item { display: flex; align-items: center; }

.breadcrumb-item + .breadcrumb-item::before {
  content: var(--bs-breadcrumb-divider, '>');
  padding: 0 0.5rem;
  color: #6b7280;
}

.breadcrumb-item.active { color: #6b7280; }

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

.btn-add svg { width: 1.1rem; height: 1.1rem; }
.btn-add:hover { opacity: 0.9; }

/* Controls */
.controls-section {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
  background: white;
  padding: 0.75rem 1rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.search-wrapper {
  position: relative;
  flex: 1;
  min-width: 250px;
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

.filter-wrapper { min-width: 220px; }

.filter-select {
  width: 100%;
  padding: 0.625rem 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.5rem;
  font-size: 0.95rem;
  background: white;
  cursor: pointer;
}

.filter-select:focus {
  outline: none;
  border-color: #28a745;
}

/* Tabla */
.table-section {
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.08);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.data-table th {
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.data-table tbody tr { border-bottom: 1px solid #dee2e6; }
.data-table tbody tr:hover { background: #f8f9fa; }
.data-table tbody tr:last-child { border-bottom: none; }

.data-table td {
  padding: 15px;
  color: #495057;
  font-size: 14px;
  vertical-align: middle;
}

.id-cell { font-weight: 600; color: #6b7280; }

/* Status badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.35rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-dot {
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-active  { background: rgba(16,185,129,0.12); color: #065f46; }
.status-active  .status-dot { background: #10b981; }
.status-inactive { background: rgba(239,68,68,0.12); color: #991b1b; }
.status-inactive .status-dot { background: #ef4444; }

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

.btn-action svg { width: 1.1rem; height: 1.1rem; stroke: white; }

.btn-view   { background: #3b82f6; }
.btn-view:hover { background: #2563eb; }
.btn-edit   { background: #f59e0b; }
.btn-edit:hover { background: #d97706; }
.btn-delete { background: #ef4444; }
.btn-delete:hover { background: #dc2626; }

/* Empty state */
.empty-state {
  background: white;
  padding: 3rem 1rem;
  text-align: center;
  color: #9ca3af;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.empty-state svg {
  width: 3rem;
  height: 3rem;
  opacity: 0.4;
  margin-bottom: 0.75rem;
}

.empty-state p { margin: 0; font-size: 1rem; }

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 0.75rem;
  width: 90%;
  max-width: 540px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0,0,0,0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  border-radius: 0.75rem 0.75rem 0 0;
}

.modal-header h2 { margin: 0; font-size: 1.25rem; color: white; }

.btn-close {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  font-size: 1.5rem;
  font-weight: 700;
  cursor: pointer;
  width: 2rem;
  height: 2rem;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.btn-close:hover { background: rgba(255,255,255,0.3); }

.modal-body { padding: 1.5rem; }

.form-group { margin-bottom: 1.25rem; }

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
}

.required { color: #ef4444; }

.form-control {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.5rem;
  font-size: 0.95rem;
  box-sizing: border-box;
  font-family: inherit;
}

.form-control:focus { outline: none; border-color: #28a745; }
.form-control.is-invalid { border-color: #ef4444; }

textarea.form-control { resize: vertical; min-height: 90px; }

.error-message { display: block; margin-top: 4px; font-size: 0.8rem; color: #ef4444; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
}

.btn-cancel {
  padding: 0.575rem 1.25rem;
  background: #6b7280;
  color: white;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel:hover { background: #4b5563; }

.btn-save {
  padding: 0.575rem 1.25rem;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-save:hover { opacity: 0.9; }

/* Detalle */
.detail-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.875rem;
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 0.5rem;
}

.detail-row strong { min-width: 160px; color: #374151; font-size: 0.9rem; }
.detail-row span { color: #111827; font-size: 0.9rem; }

/* Responsive */
@media (max-width: 768px) {
  .header-section { flex-direction: column; }
  .btn-add { width: 100%; justify-content: center; }
  .controls-section { flex-direction: column; }
  .search-wrapper, .filter-wrapper { width: 100%; }
  .data-table { min-width: 600px; }
  .table-section { overflow-x: auto; }
}
</style>
