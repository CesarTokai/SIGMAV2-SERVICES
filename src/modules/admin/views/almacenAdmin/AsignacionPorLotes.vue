<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { ToastError } from '@/utils/SweetAlert';
import Swal from 'sweetalert2';
import axiosConfig from '@/config/axiosConfig.ts';

// ========================================
// INTERFACES
// ========================================

interface User {
  id: number;
  name: string;
  email: string;
  role: string;
}

interface Warehouse {
  id: number;
  warehouseKey: string;
  nameWarehouse: string;
  observations: string;
  deleted: boolean;
}

interface UserAssignment {
  userId: number;
  email: string;
  name: string;
  role: string;
  assignedWarehouses: Array<{
    warehouseId: number;
    warehouseName: string;
    assignedAt: string;
  }>;
}

// ========================================
// REACTIVE STATE
// ========================================

const users = ref<User[]>([]);
const warehouses = ref<Warehouse[]>([]);
const selectedUserIds = ref<number[]>([]);
const selectedWarehouseIds = ref<number[]>([]);
const searchUsersTerm = ref('');
const searchWarehousesTerm = ref('');

const loading = ref(false);
const userAssignments = ref<UserAssignment[]>([]);
const loadingAssignments = ref(false);

// Computed properties
const totalUsers = computed(() => users.value.length);
const totalActiveWarehouses = computed(() => warehouses.value.filter(w => !w.deleted).length);

// Computed para Batch Assignment
const filteredUsersForBatch = computed(() => {
  if (!searchUsersTerm.value.trim()) return users.value;
  const term = searchUsersTerm.value.toLowerCase();
  return users.value.filter(u =>
    u.name.toLowerCase().includes(term) ||
    u.email.toLowerCase().includes(term)
  );
});

const filteredWarehousesForBatch = computed(() => {
  if (!searchWarehousesTerm.value.trim()) return warehouses.value;
  const term = searchWarehousesTerm.value.toLowerCase();
  return warehouses.value.filter(w =>
    w.nameWarehouse.toLowerCase().includes(term) ||
    w.warehouseKey.toLowerCase().includes(term)
  );
});

const totalBatchAssignments = computed(() => {
  return selectedUserIds.value.length * selectedWarehouseIds.value.length;
});

const selectedUsersCount = computed(() => selectedUserIds.value.length);
const selectedWarehousesCount = computed(() => selectedWarehouseIds.value.length);

// Computed para obtener los usuarios seleccionados con su información completa
const selectedUsers = computed(() => {
  return selectedUserIds.value
    .map(userId => users.value.find(u => u.id === userId))
    .filter((u): u is User => !!u);
});

// Computed para obtener los almacenes seleccionados con su información completa
const selectedWarehouses = computed(() => {
  return selectedWarehouseIds.value
    .map(warehouseId => warehouses.value.find(w => w.id === warehouseId))
    .filter((w): w is Warehouse => !!w);
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
    'ALMACENISTA': 'Logistics Lead',
    'AUXILIAR': 'Operations Specialist',
    'AUXILIAR_DE_CONTEO': 'Inventory Manager'
  };
  return roleMap[role] || role;
};

// ========================================
// API FUNCTIONS
// ========================================

const loadUsers = async () => {
  try {
    const response = await axiosConfig.doGet('/admin/users?page=0&size=20&sortBy=createdAt&sortDir=desc');

    let userData = response.data?.content || response.data?.data || response.data?.users || response.data || [];

    if (!Array.isArray(userData) && userData && typeof userData === 'object') {
      for (const key in userData) {
        if (Array.isArray(userData[key])) {
          userData = userData[key];
          break;
        }
      }
    }

    users.value = Array.isArray(userData)
      ? userData.map((u: any) => ({
          id: u.id || u.userId,
          name: u.name || u.fullName || u.username || u.email || 'Unknown',
          email: u.email || '',
          role: u.role || ''
        }))
      : [];
  } catch (error) {
    console.error('Error loading users:', error);
    ToastError('Error', 'No se pudieron cargar los usuarios');
  }
};

const loadWarehouses = async () => {
  try {
    const response = await axiosConfig.doGet('/warehouses?page=0&size=25&sortBy=warehouseKey&sortDir=asc&search=false');

    let warehouseData = response.data?.content || response.data?.data || response.data?.warehouses || response.data || [];

    if (!Array.isArray(warehouseData) && warehouseData && typeof warehouseData === 'object') {
      for (const key in warehouseData) {
        if (Array.isArray(warehouseData[key])) {
          warehouseData = warehouseData[key];
          break;
        }
      }
    }

    warehouses.value = Array.isArray(warehouseData)
      ? warehouseData.map((w: any) => ({
          id: w.id || w.warehouseId,
          warehouseKey: w.warehouseKey || w.key || '',
          nameWarehouse: w.nameWarehouse || w.name || w.warehouseName || '',
          observations: w.observations || '',
          deleted: !!w.deleted
        }))
      : [];
  } catch (error) {
    console.error('Error loading warehouses:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

const loadUserAssignments = async () => {
  loadingAssignments.value = true;
  try {
    const response = await axiosConfig.doGet('/warehouses/users-with-assignments');

    console.log('[DEBUG] Respuesta completa de /warehouses/users-with-assignments:', response);

    // Extraer datos de la respuesta - maneja múltiples formatos
    let assignmentData = response.data?.data || response.data?.content || response.data || [];

    if (!Array.isArray(assignmentData)) {
      assignmentData = [];
    }

    console.log('[DEBUG] assignmentData antes de mapeo:', assignmentData);

    // Mapear los datos directamente si ya tienen assignedWarehouses, si no, construirlos
    userAssignments.value = assignmentData
      .filter((item: any) => item && item.userId)
      .map((item: any) => {
        let assignedWarehouses = item.assignedWarehouses || [];

        // Si la API retorna warehouseIds pero no assignedWarehouses, construirlos
        if (!assignedWarehouses.length && item.warehouseIds?.length) {
          assignedWarehouses = (item.warehouseIds as number[])
            .map((warehouseId: number) => {
              const warehouse = warehouses.value.find(w => w.id === warehouseId);
              return {
                warehouseId: warehouseId,
                warehouseName: warehouse?.nameWarehouse || `Almacén ${warehouseId}`,
                assignedAt: item.assignedAt || new Date().toISOString()
              };
            });
        }

        console.log(`[DEBUG] Usuario ${item.userId}: assignedWarehouses =`, assignedWarehouses);

        return {
          userId: item.userId,
          email: item.email || '',
          name: item.name || item.email?.split('@')[0] || 'Unknown',
          role: item.role || '',
          assignedWarehouses: assignedWarehouses
        };
      });

    console.log('[DEBUG] userAssignments final:', userAssignments.value);
  } catch (error) {
    console.error('[ERROR] Error loading user assignments:', error);
    ToastError('Error', 'No se pudieron cargar las asignaciones');
  } finally {
    loadingAssignments.value = false;
  }
};

// ========================================
// SELECTION FUNCTIONS
// ========================================

const toggleUserSelection = (userId: number) => {
  const index = selectedUserIds.value.indexOf(userId);
  if (index > -1) {
    selectedUserIds.value.splice(index, 1);
  } else {
    selectedUserIds.value.push(userId);
  }
};

const toggleWarehouseSelection = (warehouseId: number) => {
  const index = selectedWarehouseIds.value.indexOf(warehouseId);
  if (index > -1) {
    selectedWarehouseIds.value.splice(index, 1);
  } else {
    selectedWarehouseIds.value.push(warehouseId);
  }
};

// ========================================
// ASSIGNMENT FUNCTIONS
// ========================================

const confirmBatchAssignment = async () => {
  if (selectedUserIds.value.length === 0) {
    ToastError('Error', 'Please select at least one user');
    return;
  }

  if (selectedWarehouseIds.value.length === 0) {
    ToastError('Error', 'Please select at least one warehouse');
    return;
  }

  const confirmed = await Swal.fire({
    icon: 'question',
    title: 'Confirm Batch Assignment',
    html: `
      <p>You are about to create <strong>${totalBatchAssignments.value} assignments</strong>:</p>
      <p>${selectedUserIds.value.length} users × ${selectedWarehouseIds.value.length} warehouses</p>
      <p>Do you want to continue?</p>
    `,
    showCancelButton: true,
    confirmButtonColor: '#4F46E5',
    cancelButtonColor: '#6B7280',
    confirmButtonText: 'Yes, create assignments',
    cancelButtonText: 'Cancel'
  });

  if (!confirmed.isConfirmed) return;

  loading.value = true;
  try {
    // Crear asignaciones para cada usuario
    const promises = selectedUserIds.value.map(userId =>
      axiosConfig.doPost(`/warehouses/users/${userId}/assign`, {
        warehouseIds: selectedWarehouseIds.value
      })
    );

    await Promise.all(promises);

    await Swal.fire({
      icon: 'success',
      title: 'Batch Assignment Complete!',
      text: `${totalBatchAssignments.value} assignments created successfully`,
      confirmButtonColor: '#4F46E5',
      timer: 3000
    });

    // Limpiar selecciones
    selectedUserIds.value = [];
    selectedWarehouseIds.value = [];
    searchUsersTerm.value = '';
    searchWarehousesTerm.value = '';

    // Actualizar tabla de asignaciones
    await loadUserAssignments();
  } catch (error: any) {
    console.error('Error creating batch assignments:', error);
    const msg = error?.response?.data?.message || 'Failed to create batch assignments';
    ToastError('Error', msg);
  } finally {
    loading.value = false;
  }
};

const removeAssignment = async (userId: number, warehouseId: number) => {
  const confirmed = await Swal.fire({
    icon: 'warning',
    title: 'Confirm Removal',
    text: '¿Deseas eliminar esta asignación de almacenes?',
    showCancelButton: true,
    confirmButtonColor: '#EF4444',
    cancelButtonColor: '#6B7280',
    confirmButtonText: 'Yes, remove',
    cancelButtonText: 'Cancel'
  });

  if (!confirmed.isConfirmed) return;

  loading.value = true;
  try {
    console.log(`[DEBUG] Eliminando almacén ${warehouseId} del usuario ${userId}`);
    const response = await axiosConfig.doDelete(`/warehouses/users/${userId}/warehouses/${warehouseId}`);
    console.log('[DEBUG] Respuesta DELETE:', response);

    await Swal.fire({
      icon: 'success',
      title: 'Success!',
      text: 'Asignación eliminada correctamente',
      confirmButtonColor: '#4F46E5',
      timer: 2000
    });

    // Actualizar tabla de asignaciones
    console.log('[DEBUG] Recargando asignaciones de usuarios');
    await loadUserAssignments();
    console.log('[DEBUG] userAssignments después de recarga:', userAssignments.value);
  } catch (error: any) {
    console.error('Error removing assignment:', error);
    const msg = error?.response?.data?.message || 'Failed to remove assignment';
    ToastError('Error', msg);
  } finally {
    loading.value = false;
  }
};

// ========================================
// LIFECYCLE HOOKS
// ========================================

onMounted(async () => {
  await loadUsers();
  await loadWarehouses();
  await loadUserAssignments();
});
</script>

<template>
  <div class="asignacion-lotes">
    <div class="container-fluid">
      <div class="header-section">
        <div class="title-section">
          <h1 class="page-title">Asignación de varios Almacenes</h1>
          <p class="subtitle">
            Asignar múltiples usuarios a múltiples almacenes simultáneamente
          </p>
        </div>

        <!-- Assignment Insights Card -->
        <div class="insights-card-header">
          <h3 class="insights-title-compact">Totales</h3>
          <div class="stats-compact">
            <div class="stat-compact">
              <div class="stat-label-compact">TOTAL USUARIOS</div>
              <div class="stat-value-compact">{{ totalUsers }}</div>
            </div>
            <div class="stat-compact">
              <div class="stat-label-compact">ALMACENES</div>
              <div class="stat-value-compact">{{ totalActiveWarehouses }}</div>
            </div>
          </div>
        </div>
      </div>



      <div class="batch-layout">
        <div class="batch-content">
          <!-- Left: Select Users -->
          <div class="batch-panel">
            <div class="batch-panel-header">
              <h3 class="batch-panel-title">SELECION DE VARIOS MULTIALMACENES</h3>
              <span class="badge-selected">{{ selectedUsersCount }} Seleccion</span>
            </div>

            <div class="search-box-batch">
              <input
                v-model="searchUsersTerm"
                type="text"
                class="search-input-batch"
                placeholder="Search staff..."
              />
            </div>

            <div class="batch-list">
              <div
                v-for="user in filteredUsersForBatch"
                :key="user.id"
                class="batch-item"
                :class="{ selected: selectedUserIds.includes(user.id) }"
                @click="toggleUserSelection(user.id)"
              >
                <input
                  type="checkbox"
                  :checked="selectedUserIds.includes(user.id)"
                  @change.stop="toggleUserSelection(user.id)"
                  class="batch-checkbox"
                />
                <div class="user-avatar-small" :style="{ backgroundColor: getUserColor(user.name) }">
                  {{ getUserInitials(user.name) }}
                </div>
                <div class="batch-item-info">
                  <span class="batch-item-name">{{ user.name }}</span>
                  <span class="batch-item-meta">{{ formatRole(user.role) }}</span>
                </div>
              </div>

              <div v-if="filteredUsersForBatch.length === 0" class="empty-state-small">
                <p>USUARIOS NO ENCONTRADOS</p>
              </div>
            </div>
          </div>

          <!-- Right: Select Warehouses -->
          <div class="batch-panel">
            <div class="batch-panel-header">
              <h3 class="batch-panel-title">SELECCIONA VARIOS ALMACENES</h3>
              <span class="badge-selected">{{ selectedWarehousesCount }} Selected</span>
            </div>

            <div class="search-box-batch">
              <input
                v-model="searchWarehousesTerm"
                type="text"
                class="search-input-batch"
                placeholder="Search locations..."
              />
            </div>

            <div class="batch-list">
              <div
                v-for="warehouse in filteredWarehousesForBatch"
                :key="warehouse.id"
                class="batch-item"
                :class="{ selected: selectedWarehouseIds.includes(warehouse.id) }"
                @click="toggleWarehouseSelection(warehouse.id)"
              >
                <input
                  type="checkbox"
                  :checked="selectedWarehouseIds.includes(warehouse.id)"
                  @change.stop="toggleWarehouseSelection(warehouse.id)"
                  class="batch-checkbox"
                />
                <span class="warehouse-icon-small">📍</span>
                <div class="batch-item-info">
                  <span class="batch-item-name">{{ warehouse.nameWarehouse }}</span>
                  <span class="batch-item-meta">{{ warehouse.warehouseKey }}</span>
                </div>
              </div>

              <div v-if="filteredWarehousesForBatch.length === 0" class="empty-state-small">
                <p>ALMACENES NO ENCONTRADOS</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Batch Summary and Confirm (Below panels) -->
        <div class="batch-footer">
          <!-- Batch Summary -->
          <div class="batch-summary" v-if="selectedUsersCount > 0 || selectedWarehousesCount > 0">
            <div class="summary-icon">ℹ️</div>
            <div class="summary-content">
              <p class="summary-text">
                <strong>{{ selectedUsersCount }} USUARIOS</strong> selected ×
                <strong>{{ selectedWarehousesCount }} ALMACENES</strong> selected =
                <strong class="highlight">{{ totalBatchAssignments }} total de asignaciones</strong> to be created.
              </p>
            </div>
          </div>

          <!-- Confirm Button -->
          <button
            class="btn-confirm-batch"
            @click="confirmBatchAssignment"
            :disabled="totalBatchAssignments === 0 || loading"
          >
            <span v-if="loading" class="spinner-small"></span>
            <span v-else>Confirmar asignación de Almacenes</span>
          </button>
        </div>
      </div>

      <!-- Selection Summary Section -->
      <div v-if="selectedUsersCount > 0 || selectedWarehousesCount > 0" class="selection-summary-section">
        <div class="summary-header">
          <h3 class="summary-title">📊 RESUMEN DE SELECCIONES</h3>
          <p class="summary-subtitle">{{ selectedUsersCount }} usuarios × {{ selectedWarehousesCount }} almacenes = {{ totalBatchAssignments }} asignaciones</p>
        </div>

        <div class="summary-content-grid">
          <!-- Selected Users -->
          <div class="summary-panel">
            <div class="panel-header">
              <h4 class="panel-title">👥 Usuarios Seleccionados ({{ selectedUsersCount }})</h4>
            </div>
            <div class="panel-items">
              <div
                v-for="user in selectedUsers"
                :key="user.id"
                class="summary-item"
              >
                <div class="item-avatar" :style="{ backgroundColor: getUserColor(user.name) }">
                  {{ getUserInitials(user.name) }}
                </div>
                <div class="item-info">
                  <div class="item-name">{{ user.name }}</div>
                  <div class="item-meta">{{ user.email }}</div>
                </div>
                <button
                  @click.stop="toggleUserSelection(user.id)"
                  class="btn-remove-item"
                  title="Remover usuario"
                >
                  ✕
                </button>
              </div>

              <div v-if="selectedUsers.length === 0" class="empty-summary">
                Sin usuarios seleccionados
              </div>
            </div>
          </div>

          <!-- Selected Warehouses -->
          <div class="summary-panel">
            <div class="panel-header">
              <h4 class="panel-title">📍 Almacenes Seleccionados ({{ selectedWarehousesCount }})</h4>
            </div>
            <div class="panel-items">
              <div
                v-for="warehouse in selectedWarehouses"
                :key="warehouse.id"
                class="summary-item"
              >
                <div class="item-icon">📦</div>
                <div class="item-info">
                  <div class="item-name">{{ warehouse.nameWarehouse }}</div>
                  <div class="item-meta">{{ warehouse.warehouseKey }}</div>
                </div>
                <button
                  @click.stop="toggleWarehouseSelection(warehouse.id)"
                  class="btn-remove-item"
                  title="Remover almacén"
                >
                  ✕
                </button>
              </div>

              <div v-if="selectedWarehouses.length === 0" class="empty-summary">
                Sin almacenes seleccionados
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Assigned Users Table -->
      <div class="assigned-users-section">
        <h3 class="section-title">Asignacion de Almacenes</h3>

        <div class="table-responsive">
          <table class="table-assigned-users">
            <thead>
              <tr>
                <th class="th-user">Usuario</th>
                <th class="th-role">Rol</th>
                <th class="th-email">Correo</th>
                <th class="th-warehouses">Almacenes Asignados</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="assignment in userAssignments"
                :key="assignment.userId"
                class="tr-assigned-user"
              >
                <td class="td-user">
                  <div class="user-info">
                    <div class="user-avatar" :style="{ backgroundColor: getUserColor(assignment.name) }">
                      {{ getUserInitials(assignment.name) }}
                    </div>
                    <div class="user-details">
                      <span class="user-name">{{ assignment.name }}</span>
                      <span class="user-meta">{{ assignment.email }}</span>
                    </div>
                  </div>
                </td>
                <td class="td-role">{{ formatRole(assignment.role) }}</td>
                <td class="td-email">{{ assignment.email }}</td>
                <td class="td-warehouses">
                  <div class="warehouses-list">
                    <div
                      v-for="warehouse in assignment.assignedWarehouses"
                      :key="warehouse.warehouseId"
                      class="warehouse-item"
                    >
                      <div class="warehouse-info">
                        <span class="warehouse-name">{{ warehouse.warehouseName }}</span>
                        <span class="warehouse-date" v-if="warehouse.assignedAt">
                          ({{ new Date(warehouse.assignedAt).toLocaleDateString() }})
                        </span>
                      </div>
                      <button
                        @click.stop="removeAssignment(assignment.userId, warehouse.warehouseId)"
                        class="btn-action-remove"
                        title="Eliminar asignación"
                      >
                        ❌
                      </button>
                    </div>
                  </div>
                </td>
              </tr>

              <tr v-if="userAssignments.length === 0">
                <td colspan="4" class="td-empty">
                  No se encontraron tareas
                </td>
              </tr>
            </tbody>
          </table>
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
  gap: 2rem;
  margin-bottom: 1rem;
}

.title-section {
  flex: 1;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1E293B;
  margin: 0 0 0.5rem 0;
}

.subtitle {
  color: #64748B;
  font-size: 0.938rem;
  margin: 0;
}

.insights-card-header {
  background: white;
  padding: 1rem 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #E2E8F0;
}

.insights-title-compact {
  font-size: 0.875rem;
  font-weight: 600;
  color: #4F46E5;
  margin: 0 0 0.625rem 0;
  text-align: center;
}

.stats-compact {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.stat-compact {
  text-align: center;
}

.stat-label-compact {
  font-size: 0.625rem;
  font-weight: 600;
  color: #64748B;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 0.25rem;
}

.stat-value-compact {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1E293B;
}

/* ========================================
   BATCH ASSIGNMENT STYLES
   ======================================== */

.batch-layout {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.batch-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.batch-footer {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  background: white;
  padding: 1.25rem 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #E2E8F0;
}

.batch-panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #E2E8F0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: 600px;
}

.batch-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #E2E8F0;
  background: #F8FAFC;
}

.batch-panel-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748B;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0;
}

.badge-selected {
  display: inline-block;
  padding: 0.375rem 0.75rem;
  background: #4F46E5;
  color: white;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
}

.search-box-batch {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #E2E8F0;
}

.search-input-batch {
  width: 100%;
  padding: 0.625rem 1rem;
  border: 1px solid #CBD5E1;
  border-radius: 8px;
  font-size: 0.875rem;
  background: white;
  transition: all 0.2s;
}

.search-input-batch:focus {
  outline: none;
  border-color: #4F46E5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.batch-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem;
}

.batch-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
  margin-bottom: 0.5rem;
}

.batch-item:hover {
  background: #F8FAFC;
  border-color: #E2E8F0;
}

.batch-item.selected {
  background: #EEF2FF;
  border-color: #4F46E5;
}

.batch-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #4F46E5;
  flex-shrink: 0;
}

.user-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.75rem;
  color: white;
  flex-shrink: 0;
}

.warehouse-icon-small {
  font-size: 1.25rem;
  flex-shrink: 0;
}

.batch-item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.batch-item-name {
  font-weight: 500;
  color: #1E293B;
  font-size: 0.875rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.batch-item-meta {
  font-size: 0.75rem;
  color: #64748B;
  display: block;
}

.empty-state-small {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  text-align: center;
  color: #64748B;
}

.empty-state-small p {
  margin: 0;
  font-size: 0.875rem;
}

/* ========================================
   BATCH SUMMARY & FOOTER
   ======================================== */

.batch-summary {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex: 1;
  padding: 0.75rem 1rem;
  background: #F8FAFC;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
}

.summary-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.summary-content {
  flex: 1;
}

.summary-text {
  margin: 0;
  font-size: 0.813rem;
  color: #1E293B;
  line-height: 1.5;
}

.summary-text strong {
  color: #4F46E5;
}

.summary-text .highlight {
  color: #10B981;
}

.btn-confirm-batch {
  padding: 0.75rem 1.5rem;
  background: #4F46E5;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  white-space: nowrap;
  min-width: 220px;
}

.btn-confirm-batch:hover:not(:disabled) {
  background: #4338CA;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.btn-confirm-batch:disabled {
  background: #CBD5E1;
  cursor: not-allowed;
  transform: none;
}

.spinner-small {
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========================================
   SELECTION SUMMARY SECTION
   ======================================== */

.selection-summary-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 1.5rem;
  margin-top: 2rem;
  border: 2px solid #E0E7FF;
}

.summary-header {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #F0F9FF;
}

.summary-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #1E293B;
  margin: 0 0 0.5rem 0;
}

.summary-subtitle {
  font-size: 0.875rem;
  color: #64748B;
  margin: 0;
  font-weight: 500;
}

.summary-content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.summary-panel {
  background: #F8FAFC;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
  overflow: hidden;
}

.panel-header {
  padding: 1rem 1.25rem;
  background: #EFF6FF;
  border-bottom: 1px solid #E2E8F0;
}

.panel-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: #0C4A6E;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.panel-items {
  padding: 0.75rem;
  max-height: 300px;
  overflow-y: auto;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background: white;
  border-radius: 6px;
  margin-bottom: 0.5rem;
  border: 1px solid #E2E8F0;
  transition: all 0.2s;
}

.summary-item:last-child {
  margin-bottom: 0;
}

.summary-item:hover {
  background: #F0F9FF;
  border-color: #4F46E5;
}

.item-avatar,
.item-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  color: white;
  flex-shrink: 0;
}

.item-icon {
  background: #FEF08A;
  color: #854D0E;
  border-radius: 6px;
}

.item-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.item-name {
  font-weight: 500;
  color: #1E293B;
  font-size: 0.875rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta {
  font-size: 0.75rem;
  color: #64748B;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-remove-item {
  background: transparent;
  border: none;
  color: #EF4444;
  cursor: pointer;
  font-size: 1.25rem;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  border-radius: 4px;
}

.btn-remove-item:hover {
  background: #FEE2E2;
  color: #DC2626;
}

.empty-summary {
  padding: 1rem;
  text-align: center;
  color: #94A3B8;
  font-size: 0.875rem;
  font-style: italic;
}

/* ========================================
   ASSIGNED USERS TABLE
   ======================================== */

.assigned-users-section {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 1.5rem;
  margin-top: 2rem;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1E293B;
  margin: 0 0 1.5rem 0;
}

.table-responsive {
  overflow-x: auto;
}

.table-assigned-users {
  width: 100%;
  border-collapse: collapse;
}

.table-assigned-users th,
.table-assigned-users td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid #E2E8F0;
}

.table-assigned-users th {
  background: #F8FAFC;
  color: #4F46E5;
  font-size: 0.875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table-assigned-users td {
  color: #1E293B;
  font-size: 0.875rem;
}

.tr-assigned-user:hover {
  background: #F8FAFC;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  color: white;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.user-name {
  font-weight: 500;
  color: #1E293B;
  font-size: 0.875rem;
}

.user-meta {
  font-size: 0.75rem;
  color: #64748B;
}

.warehouses-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.warehouse-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem;
  background: #F8FAFC;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
}

.warehouse-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
  min-width: 0;
}

.warehouse-name {
  font-size: 0.875rem;
  color: #1E293B;
  font-weight: 500;
  word-break: break-word;
}

.warehouse-date {
  font-size: 0.75rem;
  color: #64748B;
}

.btn-action-remove {
  background: transparent;
  border: none;
  color: #EF4444;
  cursor: pointer;
  font-size: 1.25rem;
  transition: color 0.2s;
  flex-shrink: 0;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-action-remove:hover {
  color: #DC2626;
}

/* ========================================
   TABLE COLUMN WIDTHS
   ======================================== */

.th-user {
  min-width: 200px;
}

.td-user {
  min-width: 200px;
}

.th-role {
  min-width: 120px;
}

.td-role {
  min-width: 120px;
}

.th-email {
  min-width: 150px;
}

.td-email {
  min-width: 150px;
}

.th-warehouses {
  min-width: 350px;
}

.td-warehouses {
  min-width: 350px;
}

.td-empty {
  text-align: center;
  color: #64748B;
  padding: 2rem !important;
}

/* ========================================
   RESPONSIVE DESIGN
   ======================================== */

@media (max-width: 1200px) {
  .batch-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .asignacion-lotes {
    padding: 1rem;
  }

  .header-section {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }

  .stats-compact {
    gap: 2rem;
  }

  .batch-content {
    grid-template-columns: 1fr;
  }

  .batch-panel {
    max-height: 400px;
  }

  .batch-footer {
    flex-direction: column;
    gap: 1rem;
  }

  .btn-confirm-batch {
    width: 100%;
  }

  .summary-content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
