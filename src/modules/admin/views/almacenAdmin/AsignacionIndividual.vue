<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ToastError, VoidAlert } from '@/utils/SweetAlert';
import Swal from 'sweetalert2';
import axiosConfig from '@/config/axiosConfig.ts';

// ========================================
// INTERFACES
// ========================================

interface User {
  id: number;
  email: string;
  role: string;
  status?: boolean;
}

interface Warehouse {
  id: number;
  warehouseKey: string;
  nameWarehouse: string;
  observations: string;
  deleted: boolean;
}

interface AssignedWarehouse {
  userId: number;
  warehouseId: number;
  assignedAt?: string;
  assignedBy?: number;
  isActive?: boolean;
  // Propiedades adicionales del almacén (enriquecidas desde warehouses)
  id?: number;
  warehouseKey?: string;
  nameWarehouse?: string;
  observations?: string;
  deleted?: boolean;
}

interface UserWithWarehouses {
  id: number;
  email: string;
  role: string;
  status?: boolean;
  warehouses: AssignedWarehouse[];
}

// ========================================
// REACTIVE STATE
// ========================================

const users = ref<User[]>([]);
const warehouses = ref<Warehouse[]>([]);
const assignedWarehouses = ref<AssignedWarehouse[]>([]);
const availableWarehouses = ref<Warehouse[]>([]);
const usersWithWarehouses = ref<UserWithWarehouses[]>([]);

const selectedUser = ref<User | null>(null);
const selectedWarehouseId = ref<number | null>(null);

const loading = ref(false);
const loadingAssignments = ref(false);
const loadingUsersList = ref(false);

// Pagination
const currentPage = ref(0);
const pageSize = 3;
const usersListCurrentPage = ref(0);
const usersListPageSize = 5;

// Computed properties
const totalUsers = computed(() => users.value.length);
const totalActiveWarehouses = computed(() => warehouses.value.filter(w => !w.deleted).length);
const totalAssignments = computed(() => assignedWarehouses.value.length);
const totalPages = computed(() => Math.ceil(totalAssignments.value / pageSize));

const paginatedAssignments = computed(() => {
  const start = currentPage.value * pageSize;
  const end = start + pageSize;
  return assignedWarehouses.value.slice(start, end);
});

const paginationStart = computed(() => {
  return currentPage.value * pageSize + 1;
});

const usersListTotalPages = computed(() => Math.ceil(selectableUsersWithWarehouses.value.length / usersListPageSize));

const paginatedUsersList = computed(() => {
  const start = usersListCurrentPage.value * usersListPageSize;
  const end = start + usersListPageSize;
  return selectableUsersWithWarehouses.value.slice(start, end);
});

const usersListPaginationStart = computed(() => {
  return usersListCurrentPage.value * usersListPageSize + 1;
});

// Filtrar usuarios excluyendo ADMINISTRADOR y AUXILIAR_DE_CONTEO
// Solo AUXILIAR y ALMACENISTA requieren asignación
const selectableUsers = computed(() => {
  return users.value.filter(u =>
    u.role !== 'ADMINISTRADOR' && u.role !== 'AUXILIAR_DE_CONTEO'
  );
});

// Filtrar usuarios con almacenes excluyendo ADMINISTRADOR y AUXILIAR_DE_CONTEO
const selectableUsersWithWarehouses = computed(() => {
  return usersWithWarehouses.value.filter(u =>
    u.role !== 'ADMINISTRADOR' && u.role !== 'AUXILIAR_DE_CONTEO'
  );
});

// ========================================
// UTILITY FUNCTIONS
// ========================================

const getUserInitials = (name: string): string => {
  if (!name) return '?';
  const parts = name.trim().split(' ').filter(p => p.length > 0);
  if (parts.length >= 2) {
    const firstChar = parts[0]?.[0];
    const secondChar = parts[1]?.[0];
    if (firstChar && secondChar) {
      return (firstChar + secondChar).toUpperCase();
    }
  }
  return name.substring(0, 2).toUpperCase();
};

const getUserColor = (name: string): string => {
  const colors: string[] = ['#60A5FA', '#8B5CF6', '#EC4899', '#10B981', '#F59E0B'];
  if (!name) return colors[0]!;
  const index = name.charCodeAt(0) % colors.length;
  return colors[index]!;
};

const formatRole = (role: string): string => {
  const roleMap: Record<string, string> = {
    'ADMINISTRADOR': 'Administrador',
    'ALMACENISTA': 'Almacenista',
    'AUXILIAR': 'Auxiliar',
    'AUXILIAR_DE_CONTEO': 'Auxiliar de conteo'
  };
  return roleMap[role] || role;
};

const formatAssignmentDate = (dateString?: string): string => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });
};

// ========================================
// PAGINATION FUNCTIONS
// ========================================

const nextPage = () => {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++;
  }
};

const previousPage = () => {
  if (currentPage.value > 0) {
    currentPage.value--;
  }
};

const nextUsersListPage = () => {
  if (usersListCurrentPage.value < usersListTotalPages.value - 1) {
    usersListCurrentPage.value++;
  }
};

const previousUsersListPage = () => {
  if (usersListCurrentPage.value > 0) {
    usersListCurrentPage.value--;
  }
};

// ========================================
// API FUNCTIONS
// ========================================

const loadUsers = async () => {
  try {
    console.log('📥 [GET] /admin/users - Cargando usuarios...');
    const response = await axiosConfig.doGet('/admin/users?page=0&size=20&sortBy=createdAt&sortDir=desc');
    const userData = response.data?.users || [];
    users.value = Array.isArray(userData) ? userData : [];
    console.log(`✅ [GET] /admin/users - ${users.value.length} usuarios cargados`);
  } catch (error) {
    console.error('❌ [GET] /admin/users - Error:', error);
    ToastError('Error', 'No se pudieron cargar los usuarios');
  }
};

const loadWarehouses = async () => {
  try {
    console.log('📥 [GET] /warehouses - Cargando almacenes...');
    const response = await axiosConfig.doGet('/warehouses?page=0&size=25&sortBy=warehouseKey&sortDir=asc&search=false');
    const warehouseData = response.data?.data || [];
    warehouses.value = Array.isArray(warehouseData) ? warehouseData : [];
    availableWarehouses.value = warehouses.value.filter(w => !w.deleted);
    console.log(`✅ [GET] /warehouses - ${warehouses.value.length} almacenes cargados`);
  } catch (error) {
    console.error('❌ [GET] /warehouses - Error:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

const loadAllUsersWithWarehouses = async () => {
  loadingUsersList.value = true;
  try {
    const usersArray = users.value;
    const usersData: UserWithWarehouses[] = [];

    for (const user of usersArray) {
      try {
        const response = await axiosConfig.doGet(`/warehouses/users/${user.id}`);

        let warehouseList = [];
        if (Array.isArray(response.data)) {
          warehouseList = response.data;
        } else if (response.data?.data && Array.isArray(response.data.data)) {
          warehouseList = response.data.data;
        } else if (response.data?.content && Array.isArray(response.data.content)) {
          warehouseList = response.data.content;
        }

        // Enriquecer los datos del almacén
        const enrichedWarehouses = warehouseList
          .map((assignment: any) => {
            const warehouseInfo = warehouses.value.find(w => w.id === assignment.warehouseId);
            return {
              ...assignment,
              id: assignment.warehouseId,
              warehouseKey: warehouseInfo?.warehouseKey || '',
              nameWarehouse: warehouseInfo?.nameWarehouse || 'Almacén desconocido',
              observations: warehouseInfo?.observations || '',
              deleted: warehouseInfo?.deleted || false
            };
          })
          .filter((w: any) => !w.deleted);

        usersData.push({
          id: user.id,
          email: user.email,
          role: user.role,
          status: user.status,
          warehouses: enrichedWarehouses
        });
      } catch (error) {
        console.error(`Error loading warehouses for user ${user.id}:`, error);
        usersData.push({
          id: user.id,
          email: user.email,
          role: user.role,
          status: user.status,
          warehouses: []
        });
      }
    }

    usersWithWarehouses.value = usersData;
  } catch (error) {
    console.error('[ERROR] Error loading all users with warehouses:', error);
    ToastError('Error', 'No se pudieron cargar los usuarios con almacenes');
  } finally {
    loadingUsersList.value = false;
  }
};

const loadUserWarehouses = async (userId: number) => {
  loadingAssignments.value = true;
  try {
    console.log(`📥 [GET] /warehouses/users/${userId} - Cargando almacenes del usuario...`);
    const response = await axiosConfig.doGet(`/warehouses/users/${userId}`);

    // Manejar diferentes formatos de respuesta
    let warehouseList = [];
    if (Array.isArray(response.data)) {
      warehouseList = response.data;
    } else if (response.data?.data && Array.isArray(response.data.data)) {
      warehouseList = response.data.data;
    } else if (response.data?.content && Array.isArray(response.data.content)) {
      warehouseList = response.data.content;
    }


    // Enriquecer los datos: fusionar con la información del almacén desde warehouses
    const enrichedWarehouses = warehouseList
      .map((assignment: any) => {
        const warehouseInfo = warehouses.value.find(w => w.id === assignment.warehouseId);
        return {
          ...assignment,
          id: assignment.warehouseId,
          warehouseKey: warehouseInfo?.warehouseKey || '',
          nameWarehouse: warehouseInfo?.nameWarehouse || 'Almacén desconocido',
          observations: warehouseInfo?.observations || '',
          deleted: warehouseInfo?.deleted || false
        };
      })
      .filter((w: any) => !w.deleted); // Filtrar almacenes eliminados

    console.log(`✅ [GET] ${enrichedWarehouses.length} almacenes después del filtro`);

    assignedWarehouses.value = enrichedWarehouses;
    updateAvailableWarehouses();
  } catch (error) {
    console.error('❌ Error loading user warehouses:', error);
    assignedWarehouses.value = [];
    availableWarehouses.value = [];
  } finally {
    loadingAssignments.value = false;
  }
};

// ========================================
// SELECTION FUNCTIONS
// ========================================

const onUserSelect = async () => {
  selectedWarehouseId.value = null;

  if (selectedUser.value) {
    await loadUserWarehouses(selectedUser.value.id);
    currentPage.value = 0;
  } else {
    assignedWarehouses.value = [];
    availableWarehouses.value = warehouses.value.filter(w => !w.deleted);
  }
};

// ========================================
// ASSIGNMENT FUNCTIONS
// ========================================

const createAssignment = async () => {
  // Validar inputs requeridos
  if (!selectedUser.value || !selectedWarehouseId.value) {
    ToastError('Error', 'Selecciona un usuario y un almacén');
    return;
  }

  loading.value = true;
  try {
    const payload = {
      warehouseIds: [selectedWarehouseId.value]
    };

    const response = await axiosConfig.doPost(
      `/warehouses/users/${selectedUser.value.id}/assign`,
      payload
    );

    // Mostrar alerta de éxito
    await Swal.fire({
      icon: 'success',
      title: '¡Asignación Exitosa!',
      text: `Almacén asignado correctamente a ${selectedUser.value.email}`,
      confirmButtonColor: '#4F46E5',
      timer: 2000
    });
    Swal.close();

    // Limpiar y recargar datos
    selectedWarehouseId.value = null;
    await loadUserWarehouses(selectedUser.value.id);
    await loadAllUsersWithWarehouses();
  } catch (error: any) {
    console.error('❌ Error creating assignment:', error);
    // Extraer mensaje de error del backend o usar mensaje genérico
    const fieldErrors = error?.response?.data?.fieldErrors;
    const msg = error?.response?.data?.message || 'No se pudo crear la asignación';

    if (fieldErrors) {
      const errorMessages = Object.entries(fieldErrors)
        .map(([field, message]) => `${field}: ${message}`)
        .join('\n');
      ToastError('Error de Validación', errorMessages);
    } else {
      ToastError('Error', msg);
    }
  } finally {
    loading.value = false;
  }
};

const updateAvailableWarehouses = () => {
  const assignedIds = assignedWarehouses.value.map(w => w.warehouseId);
  availableWarehouses.value = warehouses.value.filter(
    w => !assignedIds.includes(w.id) && !w.deleted
  );
};

const unassignWarehouse = (warehouse: AssignedWarehouse) => {
  if (!selectedUser.value) return;

  // Validar que warehouseId existe
  if (!warehouse.warehouseId) {
    console.error('[ERROR] warehouse.warehouseId es undefined o null:', warehouse);
    ToastError('Error', 'ID del almacén inválido. Recarga la página.');
    return;
  }

  VoidAlert(
    'warning',
    '¿Revocar asignación?',
    `¿Deseas revocar el acceso del usuario "${selectedUser.value.email}" al almacén "${warehouse.nameWarehouse}"?`,
    'Sí, revocar',
    async () => {
      loading.value = true;
      try {
        const deleteUrl = `/warehouses/users/${selectedUser.value!.id}/warehouses/${warehouse.warehouseId}`;
        console.log('🔴 [DELETE]', deleteUrl);

        await axiosConfig.doDelete(deleteUrl);

        // Alerta de éxito
        await Swal.fire({
          icon: 'success',
          title: '¡Revocado!',
          text: `Acceso revocado a ${warehouse.nameWarehouse}`,
          confirmButtonColor: '#28a745',
          timer: 2000
        });
        Swal.close();

        // Recargar datos
        await loadUserWarehouses(selectedUser.value!.id);
        await loadAllUsersWithWarehouses();
      } catch (error: any) {
        console.error('❌ Error al revocar asignación:', error);
        const fieldErrors = error?.response?.data?.fieldErrors;
        const msg = error?.response?.data?.message || 'No se pudo revocar la asignación';

        if (fieldErrors) {
          const errorMessages = Object.entries(fieldErrors)
            .map(([field, message]) => `${field}: ${message}`)
            .join('\n');
          ToastError('Error de Validación', errorMessages);
        } else {
          ToastError('Error', msg);
        }
      } finally {
        loading.value = false;
      }
    }
  );
};

// ========================================
// LIFECYCLE HOOKS
// ========================================

onMounted(async () => {
  await loadUsers();
  await loadWarehouses();
  await loadAllUsersWithWarehouses();
});
</script>

<template>
    <div class="container-fluid">
      <div class="header-wrapper">
        <div class="header-section">
          <div class="title-section">
              <h2 class="page-title">Asignación Individual</h2>
              <p class="subtitle">
                Asignar un usuario a varios almacenes de forma individual
              </p>
            <nav class="breadcrumb-nav" aria-label="breadcrumb">
              <ol class="breadcrumb-list">
                <li class="breadcrumb-item">
                  <router-link to="/Admin/Almacen" class="breadcrumb-link"> <- Gestión de Almacenes</router-link>
                </li>
              </ol>
            </nav>
          </div>
        </div>

      </div>


      <div class="single-layout">
        <!-- Columna Izquierda: Formulario -->
        <div class="assignment-compact-card">
          <h3 class="card-title">Nueva Asignación</h3>
          <div class="form-grid-compact">
            <div class="form-group">
              <label class="form-label">Usuario</label>
              <select v-model="selectedUser" class="form-select" @change="onUserSelect">
                <option :value="null">Selecciona usuario...</option>
                <option v-for="user in selectableUsers" :key="user.id" :value="user">
                  {{ user.email }}
                </option>
              </select>
              <p v-if="selectableUsers.length === 0" class="info-text">
                ℹ Admins y auxiliares de conteo tienen acceso sin restricciones
              </p>
            </div>

            <div class="form-group">
              <label class="form-label">Almacén</label>
              <select v-model="selectedWarehouseId" class="form-select">
                <option :value="null">Selecciona almacén...</option>
                <option v-for="warehouse in availableWarehouses" :key="warehouse.id" :value="warehouse.id">
                  {{ warehouse.nameWarehouse }}
                </option>
              </select>
            </div>

            <button
              class="btn-create-assignment-compact"
              @click="createAssignment"
              :disabled="!selectedUser || !selectedWarehouseId || loading"
            >
              <span v-if="loading" class="spinner-small"></span>
              <span v-else>Asignar</span>
            </button>

            <div class="assignments-compact-card" v-if="selectedUser" style="margin-top: 1rem; border-top: 1px solid #e2e8f0;">
              <div class="assignments-header" style="margin-top: 1rem;">
                <h3 class="assignments-title">Almacenes del Usuario</h3>
                <span class="count-badge">{{ assignedWarehouses.length }}</span>
              </div>

              <div class="assignments-list-compact">
                <div v-if="loadingAssignments" class="loading-state-compact">
                  <div class="spinner-small"></div>
                  <p>Cargando...</p>
                </div>

                <div v-else-if="assignedWarehouses.length > 0" class="warehouses-items">
                  <div v-for="assignment in assignedWarehouses" :key="`${assignment.warehouseId}-${selectedUser?.id}`" class="warehouse-row">
                    <div class="warehouse-info-compact">
                      <span class="warehouse-key-badge">{{ assignment.warehouseKey }}</span>
                      <div class="warehouse-details">
                        <span class="warehouse-name-compact">{{ assignment.nameWarehouse }}</span>
                        <span class="warehouse-date">{{ formatAssignmentDate(assignment.assignedAt) }}</span>
                      </div>
                    </div>
                    <button
                      class="btn-remove-compact"
                      @click="unassignWarehouse(assignment)"
                      :disabled="loading"
                      title="Remover almacén"
                    >
                      ✕
                    </button>
                  </div>
                </div>

                <div v-else class="empty-assignment">
                  <p>Sin almacenes asignados</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Columna Derecha: Usuarios con Almacenes -->
        <div class="users-list-side-panel">
          <div class="section-header">
            <h2 class="section-title-compact"> Usuarios con Almacenes</h2>
            <p class="section-subtitle-compact">Asignaciones activas</p>
          </div>

          <div class="users-side-content" v-if="!loadingUsersList">
            <div v-if="selectableUsersWithWarehouses.length > 0" class="users-side-list">
              <div v-for="userWithWarehouses in selectableUsersWithWarehouses" :key="userWithWarehouses.id" class="user-side-item">
                <div class="user-side-header">
                  <div class="user-side-avatar" :style="{ backgroundColor: getUserColor(userWithWarehouses.email) }">
                    {{ getUserInitials(userWithWarehouses.email) }}
                  </div>
                  <div class="user-side-info">
                    <div class="user-side-email">{{ userWithWarehouses.email }}</div>
                    <div class="user-side-role">{{ formatRole(userWithWarehouses.role) }}</div>
                  </div>
                  <span class="warehouse-count-side">{{ userWithWarehouses.warehouses.length }}</span>
                </div>
              </div>
            </div>

            <div v-else class="empty-users-side">
              <p>Sin asignaciones</p>
            </div>
          </div>

          <div v-else class="loading-users-side">
            <div class="spinner-small"></div>
            <p>Cargando...</p>
          </div>
        </div>
      </div>

      <!-- Users with Warehouses List Section - Full Width Below -->
      <div class="users-list-section">
        <div class="section-header">
          <h2 class="section-title">📋 Todos los Usuarios con Almacenes</h2>
          <p class="section-subtitle">Lista de todas las asignaciones activas</p>
        </div>

        <div class="users-table-wrapper" v-if="!loadingUsersList">
          <div v-if="paginatedUsersList.length > 0" class="users-table">
            <div class="table-header">
              <div class="table-col-email">Usuario</div>
              <div class="table-col-role">Rol</div>
              <div class="table-col-almacenes">Almacenes Asignados</div>
              <div class="table-col-count">Total</div>
            </div>

            <div class="table-body">
              <div v-for="userWithWarehouses in paginatedUsersList" :key="userWithWarehouses.id" class="table-row">
                <div class="table-col-email">
                  <div class="user-info-table">
                    <div class="user-avatar-table" :style="{ backgroundColor: getUserColor(userWithWarehouses.email) }">
                      {{ getUserInitials(userWithWarehouses.email) }}
                    </div>
                    <span class="user-email-table">{{ userWithWarehouses.email }}</span>
                  </div>
                </div>
                <div class="table-col-role">
                  <span class="role-badge-table">{{ formatRole(userWithWarehouses.role) }}</span>
                </div>
                <div class="table-col-almacenes">
                  <div v-if="userWithWarehouses.warehouses.length > 0" class="almacenes-tags">
                    <span v-for="warehouse in userWithWarehouses.warehouses.slice(0, 3)" :key="warehouse.warehouseId" class="almacen-tag">
                      {{ warehouse.warehouseKey }}
                    </span>
                    <span v-if="userWithWarehouses.warehouses.length > 3" class="almacen-tag-more">
                      +{{ userWithWarehouses.warehouses.length - 3 }}
                    </span>
                  </div>
                  <span v-else class="no-almacenes">Sin asignaciones</span>
                </div>
                <div class="table-col-count">
                  <span v-if="userWithWarehouses.warehouses.length > 0" class="count-badge-table">
                    {{ userWithWarehouses.warehouses.length }}
                  </span>
                  <span v-else class="count-badge-empty">0</span>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-state-large">
            <p>No hay usuarios con almacenes asignados</p>
          </div>
        </div>

        <div v-else class="loading-state">
          <div class="spinner"></div>
          <p>Cargando usuarios y almacenes...</p>
        </div>

        <!-- Pagination for Users List -->
        <div class="table-footer" v-if="selectableUsersWithWarehouses.length > usersListPageSize">
          <div class="pagination-info">
            Mostrando {{ usersListPaginationStart }} de {{ selectableUsersWithWarehouses.length }} usuarios
          </div>
          <div class="pagination-controls">
            <button
              class="btn-pagination"
              @click="previousUsersListPage"
              :disabled="usersListCurrentPage === 0"
            >
              ANTERIOR
            </button>
            <button
              class="btn-pagination"
              @click="nextUsersListPage"
              :disabled="usersListCurrentPage >= usersListTotalPages - 1"
            >
              SIGUIENTE
            </button>
          </div>
        </div>
      </div>
    </div>

</template>

<style scoped>


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

.section-title-secondary {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
}

/* ========================================
   CONTENEDOR PRINCIPAL
   ======================================== */

.asignacion-individual {
  padding: 1.5rem;
  background: #f5f7fa;
  min-height: 100vh;
}

/* ========================================
   HEADER SECTION
   ======================================== */

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.title-section {
  flex: 1;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1a202c;

}

.subtitle {
  color: #6b7280;
  font-size: 0.95rem;
  margin: 0.25rem 0 0 0;
}


.breadcrumb-item {
  display: flex;
  align-items: center;
}

.breadcrumb-item + .breadcrumb-item::before {
  content: var(--bs-breadcrumb-divider, '>');
  padding: 0 0.5rem;
  color: #6b7280;
}

.breadcrumb-item a {
  color: #3b82f6;
  text-decoration: none;
  transition: color 0.2s ease;
}

.breadcrumb-item a:hover {
  color: #2563eb;
  text-decoration: underline;
}

.breadcrumb-item.active {
  color: #6b7280;
}

/* ========================================
   SINGLE LAYOUT (Dos Columnas)
   ======================================== */

.single-layout {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

/* ========================================
   CARDS BASE
   ======================================== */

.assignment-compact-card,
.assignments-compact-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

/* ========================================
   ASSIGNMENT COMPACT CARD
   ======================================== */

.assignment-compact-card {
  height: fit-content;
}

.card-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 1.25rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-grid-compact {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.form-select {
  padding: 0.75rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.938rem;
  color: #475569;
  background: white;
  transition: all 0.2s;
  cursor: pointer;
}

.form-select:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.info-text {
  font-size: 0.75rem;
  color: #0369a1;
  margin: 0.5rem 0 0 0;
  padding: 0.5rem;
  background: #dbeafe;
  border-radius: 4px;
}

.btn-create-assignment-compact {
  padding: 0.75rem 1.25rem;
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.938rem;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.btn-create-assignment-compact:hover:not(:disabled) {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.btn-create-assignment-compact:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
}

/* ========================================
   ASSIGNMENTS COMPACT CARD
   ======================================== */

.assignments-compact-card {
  max-height: 600px;
  display: flex;
  flex-direction: column;
}

.assignments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e2e8f0;
}

.assignments-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.count-badge {
  display: inline-block;
  padding: 0.375rem 0.75rem;
  background: #dbeafe;
  color: #0369a1;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 700;
}

.assignments-list-compact {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow-y: auto;
  flex: 1;
}

.warehouse-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 6px;
  border-left: 3px solid #4f46e5;
  transition: all 0.2s;
}

.warehouse-row:hover {
  background: #f1f5f9;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.warehouse-info-compact {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.warehouse-key-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background: #4f46e5;
  color: white;
  border-radius: 4px;
  font-size: 0.688rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.warehouse-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.warehouse-name-compact {
  font-size: 0.938rem;
  font-weight: 500;
  color: #1e293b;
}

.warehouse-date {
  font-size: 0.75rem;
  color: #94a3b8;
}

.btn-remove-compact {
  padding: 0.5rem 0.75rem;
  background: #fee2e2;
  color: #dc2626;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
}

.btn-remove-compact:hover:not(:disabled) {
  background: #fecaca;
  transform: scale(1.05);
}

.btn-remove-compact:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-state-compact {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem 1rem;
  color: #94a3b8;
  font-size: 0.938rem;
}

.empty-assignment {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  text-align: center;
  color: #94a3b8;
  font-size: 0.938rem;
  background: #f8fafc;
  border-radius: 6px;
}

.empty-assignment p {
  margin: 0;
}

.warehouses-items {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* ========================================
   SPINNER
   ======================================== */

.spinner-small {
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  animation: spin 0.8s linear infinite;
}

.spinner {
  border: 3px solid #f0f0f0;
  border-top: 3px solid #4f46e5;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========================================
   USERS LIST SIDE PANEL (Columna Derecha)
   ======================================== */

.users-list-side-panel {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  max-height: 800px;
}

.section-title-compact {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.section-subtitle-compact {
  font-size: 0.75rem;
  color: #64748b;
  margin: 0.25rem 0 0 0;
}

.users-side-content {
  flex: 1;
  overflow-y: auto;
}

.users-side-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.user-side-item {
  padding: 0.875rem;
  background: #f8fafc;
  border-radius: 6px;
  border-left: 3px solid #4f46e5;
  transition: all 0.2s;
}

.user-side-item:hover {
  background: #f1f5f9;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.user-side-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  justify-content: space-between;
}

.user-side-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.75rem;
  color: white;
  flex-shrink: 0;
}

.user-side-info {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  flex: 1;
  min-width: 0;
}

.user-side-email {
  font-size: 0.813rem;
  font-weight: 500;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-side-role {
  font-size: 0.688rem;
  color: #94a3b8;
}

.warehouse-count-side {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 0.5rem;
  background: #dbeafe;
  color: #0369a1;
  border-radius: 12px;
  font-size: 0.688rem;
  font-weight: 700;
  flex-shrink: 0;
}

.empty-users-side {
  text-align: center;
  padding: 2rem 1rem;
  color: #94a3b8;
  font-size: 0.875rem;
}

.empty-users-side p {
  margin: 0;
}

.loading-users-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  color: #94a3b8;
  gap: 0.5rem;
}

.loading-users-side p {
  font-size: 0.813rem;
  margin: 0;
}

/* ========================================
   USERS LIST SECTION
   ======================================== */

.users-list-section {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.section-header {
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
}

.section-subtitle {
  color: #64748b;
  font-size: 0.938rem;
  margin: 0;
}

/* ========================================
   USERS TABLE SECTION
   ======================================== */

.users-table-wrapper {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: white;
}

.users-table {
  display: flex;
  flex-direction: column;
  min-width: 100%;
}

.table-header {
  display: grid;
  grid-template-columns: 2fr 1fr 2fr 0.8fr;
  gap: 1rem;
  padding: 1rem 1.5rem;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-weight: 600;
  font-size: 0.813rem;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  position: sticky;
  top: 0;
}

.table-body {
  display: flex;
  flex-direction: column;
}

.table-row {
  display: grid;
  grid-template-columns: 2fr 1fr 2fr 0.8fr;
  gap: 1rem;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  align-items: center;
  transition: all 0.2s;
}

.table-row:hover {
  background: #f8fafc;
}

.table-row:last-child {
  border-bottom: none;
}

.table-col-email {
  display: flex;
  align-items: center;
}

.table-col-role,
.table-col-almacenes,
.table-col-count {
  display: flex;
  align-items: center;
}

.user-info-table {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.user-avatar-table {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.75rem;
  color: white;
  flex-shrink: 0;
}

.user-email-table {
  font-size: 0.938rem;
  font-weight: 500;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-badge-table {
  display: inline-block;
  padding: 0.375rem 0.75rem;
  background: #e2e8f0;
  color: #475569;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.almacenes-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  align-items: center;
}

.almacen-tag {
  display: inline-block;
  padding: 0.25rem 0.625rem;
  background: #4f46e5;
  color: white;
  border-radius: 4px;
  font-size: 0.688rem;
  font-weight: 600;
  white-space: nowrap;
}

.almacen-tag-more {
  display: inline-block;
  padding: 0.25rem 0.625rem;
  background: #dbeafe;
  color: #0369a1;
  border-radius: 4px;
  font-size: 0.688rem;
  font-weight: 600;
  white-space: nowrap;
}

.no-almacenes {
  color: #94a3b8;
  font-size: 0.813rem;
}

.count-badge-table {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 0.5rem;
  background: #dbeafe;
  color: #0369a1;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
}

.count-badge-empty {
  color: #94a3b8;
  font-size: 0.813rem;
  font-weight: 600;
}

/* ========================================
   OLD STYLES - KEPT FOR COMPATIBILITY
   ======================================== */

.users-grid {
  display: grid;
  gap: 1.5rem;
}

.users-cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
}

.user-card {
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.2s;
}

.user-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
  border-color: #cbd5e1;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.user-header-info {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  flex: 1;
}

.user-avatar-large {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1rem;
  color: white;
  flex-shrink: 0;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.user-email {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.938rem;
}

.user-role-badge {
  font-size: 0.75rem;
  color: #64748b;
  background: #e2e8f0;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  width: fit-content;
}

.warehouse-count-badge {
  display: inline-block;
  padding: 0.5rem 0.875rem;
  background: #dbeafe;
  color: #0369a1;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.no-warehouse-badge {
  display: inline-block;
  padding: 0.5rem 0.875rem;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.card-body {
  padding: 1.25rem;
}

.warehouses-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.warehouse-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem;
  background: #f8fafc;
  border-radius: 6px;
  border-left: 3px solid #4f46e5;
}

.warehouse-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.warehouse-key {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background: #4f46e5;
  color: white;
  border-radius: 4px;
  font-size: 0.688rem;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.warehouse-name {
  font-size: 0.875rem;
  color: #1e293b;
  font-weight: 500;
}

.assigned-date {
  font-size: 0.75rem;
  color: #94a3b8;
  white-space: nowrap;
}

.no-warehouses-message {
  text-align: center;
  padding: 2rem 1rem;
  color: #94a3b8;
  font-size: 0.875rem;
}

.no-warehouses-message p {
  margin: 0;
}

.empty-state-large {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  text-align: center;
  color: #94a3b8;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px dashed #e2e8f0;
}

.empty-state-large p {
  margin: 0;
  font-size: 0.938rem;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  color: #94a3b8;
  text-align: center;
}

/* ========================================
   TABLE FOOTER (Pagination)
   ======================================== */

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
  margin-top: 1rem;
  border-radius: 0 0 12px 12px;
  flex-wrap: wrap;
  gap: 1rem;
}

.pagination-info {
  font-size: 0.813rem;
  color: #64748b;
  font-weight: 500;
}

.pagination-controls {
  display: flex;
  gap: 0.5rem;
}

.btn-pagination {
  padding: 0.5rem 1rem;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 0.813rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn-pagination:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #1e293b;
}

.btn-pagination:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ========================================
   RESPONSIVE DESIGN
   ======================================== */

@media (max-width: 1024px) {
  .single-layout {
    grid-template-columns: 1fr;
  }

  .users-cards-container {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  }
}

@media (max-width: 768px) {
  .asignacion-individual {
    padding: 1rem;
  }

  .header-section {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .single-layout {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .users-cards-container {
    grid-template-columns: 1fr;
  }

  .table-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}

</style>
