<template>
  <div class="admin-user-management">
      <div class="content">

        <!-- Header Section -->
        <div class="header-section">
          <div class="title-wrapper">
            <h1 class="page-title">
              Gestión de Usuarios
            </h1>
            <nav style="--bs-breadcrumb-divider: '>';" aria-label="breadcrumb">
              <ol class="breadcrumb">
                <li class="breadcrumb-item active" aria-current="page">Gestión de Usuarios</li>
                <li class="breadcrumb-item"><a href="/Admin/recovery-requests">Solicitudes de Recuperación</a></li>
              </ol>
            </nav>
            <p class="subtitle">Administra y controla todos los usuarios del sistema</p>
          </div>
          <button class="btn-add-user" @click="openAddModal">
            Nuevo Usuario
          </button>
        </div>


        <!-- Stats Cards -->
        <div class="stats-container">
          <!-- Total Usuarios -->
          <div class="stat-card">
            <div class="stat-icon blue">
              <!-- ícono: grupo de personas -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-1a4 4 0 00-5.477-3.72M17 20H7m10 0v-1a4 4 0 00-4-4H7a4 4 0 00-4 4v1m0 0H2v-1a4 4 0 015.477-3.72M7 20v-1a4 4 0 014-4h2a4 4 0 014 4v1M9 7a4 4 0 108 0 4 4 0 00-8 0z" />
              </svg>
            </div>
            <div class="stat-content">
              <p class="stat-label">Total Usuarios</p>
              <p class="stat-value">{{ users.length }}</p>
            </div>
          </div>

          <!-- Administradores -->
          <div class="stat-card">
            <div class="stat-icon purple">
              <!-- ícono: escudo / admin -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <div class="stat-content">
              <p class="stat-label">Administrador</p>
              <p class="stat-value">{{ adminCount }}</p>
            </div>
          </div>

          <!-- ALMACENISTA -->
          <div class="stat-card">
            <div class="stat-icon orange">
              <!-- ícono: almacén / caja -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 10V11" />
              </svg>
            </div>
            <div class="stat-content">
              <p class="stat-label">Almacenista</p>
              <p class="stat-value">{{ almacenistaCount }}</p>
            </div>
          </div>

          <!-- AUXILIAR -->
          <div class="stat-card">
            <div class="stat-icon teal">
              <!-- ícono: persona con llave -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            </div>
            <div class="stat-content">
              <p class="stat-label">Auxiliar</p>
              <p class="stat-value">{{ auxiliarCount }}</p>
            </div>
          </div>

          <!-- AUXILIAR_DE_CONTEO -->
          <div class="stat-card">
            <div class="stat-icon blue">
              <!-- ícono: portapapeles / conteo -->
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01m-.01 4h.01" />
              </svg>
            </div>
            <div class="stat-content">
              <p class="stat-label">Aux. Conteo</p>
              <p class="stat-value">{{ auxiliarConteoCount }}</p>
            </div>
          </div>
        </div>

        <!-- Search and Filter Section -->
        <div class="control-panel">
          <div class="search-wrapper">
            <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              v-model="searchQuery"
              class="search-input"
              placeholder="Buscar por nombre, email o ID..."
            />
          </div>
          <div class="filter-wrapper">
            <select v-model="roleFilter" class="filter-select">
              <option value="">Todos los roles</option>
              <option value="ADMINISTRADOR">Administradores</option>
              <option value="ALMACENISTA">Usuarios Almacenistas</option>
              <option value="AUXILIAR">Usuarios Auxiliares</option>
              <option value="AUXILIAR_DE_CONTEO">Usuarios Auxiliar de conteo</option>
            </select>
          </div>
        </div>

        <!-- Users Table -->
        <div class="table-section">
          <table class="user-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Usuario</th>
                <th>Email</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Fecha Registro</th>
                <th class="actions-header">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="paginatedUsers.length === 0">
                <td colspan="7" class="no-data">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                  </svg>
                  <p>No se encontraron usuarios</p>
                </td>
              </tr>
              <tr v-for="user in paginatedUsers" :key="user.id" class="user-row">
                <td class="user-id">#{{ user.id }}</td>
                <td>
                  <div class="user-info">
                    <div class="avatar" :style="{ backgroundColor: getUserColor(user.name) }">
                      {{ getInitials(user.name) }}
                    </div>
                    <span class="user-name">{{ user.name }}</span>
                  </div>
                </td>
                <td class="user-email">{{ user.email }}</td>
                <td>
                  <span class="badge" :class="user.roleKey">
                    {{ toDisplayRole(user.rawRole) }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="user.status || 'active'">
                    <span class="status-dot"></span>
                    {{ user.status === 'active' ? 'Activo' : 'Inactivo' }}
                  </span>
                </td>
                <td class="user-date">{{ formatDate(user.createdAt) }}</td>
                <td class="actions-cell">
                  <div class="action-buttons">
                    <!-- Ver detalles -->
                    <button class="btn-action btn-view" @click="viewUser(user)" title="Ver detalles">
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                    </button>
                    <!-- Editar / cambiar rol -->
                    <button class="btn-action btn-edit" @click="editUser(user)" title="Editar usuario">
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536M9 11l6.586-6.586a2 2 0 012.828 2.828L11.828 13.828 9 14l.172-2.828z" />
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 19h14" />
                      </svg>
                    </button>
                    <!-- Activar / Desactivar -->
                    <button class="btn-action btn-toggle" @click="confirmToggle(user)" :title="user.status === 'active' ? 'Desactivar usuario' : 'Activar usuario'">
                      <svg v-if="user.status === 'active'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728L5.636 5.636m12.728 12.728A9 9 0 015.636 5.636" />
                      </svg>
                      <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </button>
                    <!-- Eliminar -->
                    <button class="btn-action btn-delete" @click="confirmDelete(user.id)" title="Eliminar usuario">
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

        <!-- Pagination -->
        <div class="pagination-container">
          <div class="pagination-info">
            Mostrando {{ startIndex + 1 }} - {{ endIndex }} de {{ totalFilteredUsers }} usuarios
          </div>
          <div class="pagination">
            <button class="pagination-btn" @click="prevPage" :disabled="currentPage === 1">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
              Anterior
            </button>

            <div class="page-numbers">
              <button
                v-for="page in visiblePages"
                :key="page"
                class="page-number"
                :class="{ active: page === currentPage }"
                @click="goToPage(page)"
              >
                {{ page }}
              </button>
            </div>

            <button class="pagination-btn" @click="nextPage" :disabled="currentPage === totalPages">
              Siguiente
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>

        <!-- Modal para agregar/editar usuario -->
        <transition name="modal-fade">
          <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
            <div class="modal-card">
              <!-- Modal Header -->
              <div class="modal-header-card">
                <div class="header-content">
                  <h2 class="modal-title">
                    {{ isEditing ? ' Editar Usuario' : ' Agregar Nuevo Usuario' }}
                  </h2>
                  <p class="modal-subtitle">{{ isEditing ? 'Actualiza los datos del usuario' : 'Completa el formulario para registrar un nuevo usuario' }}</p>
                </div>
                <button class="btn-modal-close" @click="closeModal" title="Cerrar">✕</button>
              </div>

              <!-- Modal Body -->
              <form @submit.prevent="saveUser" class="modal-body">
                <!-- Información Personal -->
                <div class="form-section">
                  <h3 class="form-section-title"> Información Personal</h3>

                  <div class="form-grid-2">
                    <div class="form-group">
                      <label for="name">Nombre <span class="required">*</span></label>
                      <input
                        type="text"
                        id="name"
                        v-model="form.name"
                        placeholder="Ej: Juan"
                        required
                      />
                    </div>

                    <div class="form-group">
                      <label for="firstLastName">Apellido Paterno <span class="required">*</span></label>
                      <input
                        type="text"
                        id="firstLastName"
                        v-model="form.firstLastName"
                        placeholder="Ej: García"
                        required
                      />
                    </div>
                  </div>

                  <div class="form-grid-2">
                    <div class="form-group">
                      <label for="secondLastName">Apellido Materno</label>
                      <input
                        type="text"
                        id="secondLastName"
                        v-model="form.secondLastName"
                        placeholder="Ej: López"
                      />
                    </div>

                    <div class="form-group">
                      <label for="phoneNumber">Teléfono</label>
                      <input
                        type="tel"
                        id="phoneNumber"
                        v-model="form.phoneNumber"
                        placeholder="Ej: 7341218659"
                      />
                    </div>
                  </div>

                  <div class="form-group">
                    <label for="email">Correo Electrónico <span class="required">*</span></label>
                    <input
                      type="email"
                      id="email"
                      v-model="form.email"
                      placeholder="usuario@tokai.com.mx"
                      required
                    />
                  </div>
                </div>

                <!-- Configuración -->
                <div class="form-section">
                  <h3 class="form-section-title">️ Configuración</h3>

                  <div class="form-grid-2">
                    <div class="form-group">
                      <label for="role">Rol <span class="required">*</span></label>
                      <select id="role" v-model="form.role" required>
                        <option value="" disabled>Selecciona un rol</option>
                        <option value="ADMINISTRADOR"> Administrador</option>
                        <option value="ALMACENISTA"> Almacenista</option>
                        <option value="AUXILIAR"> Auxiliar</option>
                        <option value="AUXILIAR_DE_CONTEO"> Auxiliar de Conteo</option>
                      </select>
                    </div>

                    <div class="form-group">
                      <label for="status">Estado <span class="required">*</span></label>
                      <select id="status" v-model="form.status" required>
                        <option value="" disabled>Selecciona estado</option>
                        <option value="active">✓ Activo</option>
                        <option value="inactive">✕ Inactivo</option>
                      </select>
                    </div>
                  </div>
                </div>

                <!-- Seguridad -->
                <div class="form-section">
                  <h3 class="form-section-title"> Seguridad</h3>

                  <div class="form-group">
                    <label for="password">Contraseña</label>
                    <div class="password-input-wrapper">
                      <input
                        :type="showPassword ? 'text' : 'password'"
                        id="password"
                        v-model="form.password"
                        placeholder="Mínimo 8 caracteres con mayúscula, número y símbolo"
                        :readonly="passwordLocked"
                        @input="checkPasswordMatch"
                      />
                      <button
                        type="button"
                        @click="showPassword = !showPassword"
                        class="toggle-password"
                        tabindex="-1"
                      >
                        {{ showPassword ? '👁️‍🗨️' : '👁️' }}
                      </button>
                    </div>
                    <small class="hint-text">La contraseña debe tener mayúsculas, minúsculas, números y caracteres especiales (@$!%*?&.#-_+=)</small>
                  </div>

                  <div class="form-group">
                    <label for="confirmPassword">Confirmar Contraseña</label>
                    <div class="password-input-wrapper">
                      <input
                        :type="showPassword ? 'text' : 'password'"
                        id="confirmPassword"
                        v-model="confirmPassword"
                        placeholder="Repite la contraseña"
                        @focus="passwordLocked = true"
                        @blur="passwordLocked = false"
                        @input="checkPasswordMatch"
                      />
                      <button
                        type="button"
                        @click="showPassword = !showPassword"
                        class="toggle-password"
                        tabindex="-1"
                      >
                        {{ showPassword ? '👁️‍🗨️' : '👁️' }}
                      </button>
                    </div>
                    <small v-if="confirmPassword && !passwordMatch" class="error-text"> Las contraseñas no coinciden</small>
                    <small v-if="passwordMatch && form.password && confirmPassword" class="success-text">✓ Las contraseñas coinciden</small>
                  </div>
                </div>

                <!-- Comentarios -->
                <div class="form-section">
                  <h3 class="form-section-title"> Notas Adicionales</h3>

                  <div class="form-group">
                    <label for="comments">Comentarios</label>
                    <textarea
                      id="comments"
                      v-model="form.comments"
                      placeholder="Información adicional sobre el usuario..."
                      rows="3"
                    ></textarea>
                  </div>
                </div>

                <!-- Modal Actions -->
                <div class="modal-actions">
                  <button type="button" class="btn-cancel" @click="closeModal">Cancelar</button>
                  <button type="submit" class="btn-submit">
                    {{ isEditing ? ' Actualizar' : ' Guardar' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </transition>

        <!-- Modal para cambio de rol: usar componente reutilizable -->
        <UserRoleModal
          :visible="showRoleModal"
          :user="roleModalUser"
          @close="onRoleModalClose"
          @saved="onRoleSaved"
        />

        <!-- Modal para información personal -->
        <PersonalInfoModal
          :visible="showPersonalInfoModal"
          :personalInfo="personalInfo"
          :loading="loadingPersonalInfo"
          :error="personalInfoError"
          :userId="personalInfoUserId"
          :toDisplayRole="toDisplayRole"
          :formatDate="formatDate"
          @close="closePersonalInfoModal"
        />
      </div>
  </div>
</template>

<script>
import adminSidebar from "@/components/AdminSidebar.vue";
import { LoadAlert, ToastSuccess, ToastError } from '@/utils/SweetAlert';
import axiosConfiguration from "@/config/axiosConfig.js";
import Swal from "sweetalert2";
import UserRoleModal from '@/components/Admin/UserRoleModal.vue';
import PersonalInfoModal from '@/components/Admin/PersonalInfoModal.vue';

export default {
  components: { adminSidebar, UserRoleModal, PersonalInfoModal },
  data() {
    return {
      users: [],
      searchQuery: '',
      roleFilter: '',
      currentPage: 1,
      usersPerPage: 20,
      totalUsers: 0,
      totalPages: 0,
      showModal: false,
      isEditing: false,
      form: {
        id: null,
        name: '',
        firstLastName: '',
        secondLastName: '',
        phoneNumber: '',
        comments: '',
        email: '',
        role: '',
        status: '',
        createdAt: new Date(),
        password: ''
      },
      adminCount: 0,
      regularUserCount: 0,
      almacenistaCount: 0,
      auxiliarCount: 0,
      auxiliarConteoCount: 0,
      allSelected: false,
      // control para el modal de cambio de rol
      showRoleModal: false,
      roleModalUser: null,
      // NUEVO: control para modal de información personal
      showPersonalInfoModal: false,
      personalInfo: null,
      personalInfoUserId: null,
      loadingPersonalInfo: false,
      personalInfoError: null,
      showPassword: false,
      confirmPassword: '',
      passwordLocked: false,
      passwordMatch: false,
    };
  },
  computed: {
    searchFilteredUsers() {
      const q = (this.searchQuery || '').toString().trim().toLowerCase();
      return this.users.filter(user => {
        const name = (user.name || this.deriveNameFromEmail(user.email) || '').toString().toLowerCase();
        const email = (user.email || '').toString().toLowerCase();
        const matchesSearch = q === '' || name.includes(q) || email.includes(q) || (`${user.id}`.includes(q));
        const matchesRole = this.roleFilter ? ((user.rawRole || '').toString().toUpperCase() === (this.roleFilter || '').toString().toUpperCase()) : true;
        return matchesSearch && matchesRole;
      });
    },
    totalFilteredUsers() {
      return this.searchFilteredUsers.length;
    },
    startIndex() {
      return (this.currentPage - 1) * this.usersPerPage;
    },
    endIndex() {
      return Math.min(this.startIndex + this.usersPerPage, this.totalFilteredUsers);
    },
    paginatedUsers() {
      return this.searchFilteredUsers.slice(this.startIndex, this.endIndex);
    },
     visiblePages() {
       const pages = [];
       const total = this.totalPages || 1;
       for (let i = 1; i <= total; i++) pages.push(i);
       return pages;
     }
   },
    watch: {
    },
   methods: {
    deriveNameFromEmail(email) {
      if (!email) return '';
      const local = email.split('@')[0] || email;
      return local.split(/[._\-]/).map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ');
    },
    toRoleKey(rawRole) {
      if (!rawRole) return 'unknown';
      return rawRole.toString().toLowerCase().replace(/[^a-z0-9_]/g, '_');
    },
    toDisplayRole(rawRole) {
      if (!rawRole) return '';
      // reemplaza guiones/underscores con espacios y capitaliza cada palabra
      const parts = rawRole.toString().toLowerCase().split(/[_\s]+/);
      const transformed = parts.map(p => p.charAt(0).toUpperCase() + p.slice(1)).join(' ');
      // corregir 'De' a minúscula si aparece (ej. AUXILIAR_DE_CONTEO -> Auxiliar de Conteo)
      return transformed.replace(/\bDe\b/g, 'de');
    },
    formatDate(dateString) {
      if (!dateString) return '-';
      try {
        return new Date(dateString).toLocaleDateString('es-MX', {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        });
      } catch (e) {
        return '-';
      }
    },
    async fetchUsers() {
      try {
        const pageParam = Math.max(0, this.currentPage - 1);

        const response = await axiosConfiguration.doGet(
          `/admin/users?page=${pageParam}&size=${this.usersPerPage}&sortBy=createdAt&sortDir=desc`
        );

        const data = response.data || {};
        const apiUsers = Array.isArray(data.users) ? data.users : [];

        this.users = apiUsers.map(u => {
          const name = u.name || this.deriveNameFromEmail(u.email);
          const rawRole = u.role || '';
          const status = (typeof u.status === 'boolean') ? (u.status ? 'active' : 'inactive') : (u.status || 'inactive');
          return {
            id: u.id,
            email: u.email,
            name,
            rawRole,
            roleKey: this.toRoleKey(rawRole),
            status,
            createdAt: u.createdAt,
            selected: false,
            original: u
          };
        });

        this.totalUsers = data.totalElements != null ? data.totalElements : this.users.length;
        this.totalPages = data.totalPages != null ? data.totalPages : Math.ceil(this.totalUsers / this.usersPerPage) || 1;
        this.adminCount = this.users.filter(u => u.rawRole === 'ADMINISTRADOR').length;
        this.regularUserCount = this.users.filter(u => u.rawRole !== 'ADMINISTRADOR').length;
        this.almacenistaCount = this.users.filter(u => u.rawRole === 'ALMACENISTA').length;
        this.auxiliarCount = this.users.filter(u => u.rawRole === 'AUXILIAR').length;
        this.auxiliarConteoCount = this.users.filter(u => u.rawRole === 'AUXILIAR_DE_CONTEO').length;

      } catch (error) {
        // Error al cargar usuarios
      }
    },

    async toggleStatus(userId) {
      try {
        const response = await axiosConfiguration.doPost(`/admin/users/${userId}/toggle-status`, {});

        const updated = response && response.data ? response.data : null;

        // Actualizar el estado en el modelo local usando la respuesta si existe
        this.users = this.users.map((user) => {
          if (user.id === userId) {
            let newStatus;
            if (updated && updated.status !== undefined) {
              // Si el backend devuelve booleano o string
              if (typeof updated.status === 'boolean') {
                newStatus = updated.status ? 'active' : 'inactive';
              } else {
                newStatus = updated.status;
              }
            } else {
              // Fallback: invertir el estado actual
              newStatus = user.status === 'active' ? 'inactive' : 'active';
            }
            user.status = newStatus;
            // mantener referencia al objeto original si viene info adicional
            user.original = { ...(user.original || {}), ...updated };
          }
          return user;
        });
        Swal.fire("Estado Actualizado", "El estado del usuario ha sido cambiado.", "success");

        // REFRESH: recargar lista desde backend para mantener consistencia
        await this.fetchUsers();
      } catch (error) {
        Swal.fire("Error", "No se pudo cambiar el estado del usuario.", "error");
      }
    },

    async confirmToggle(user) {
      try {
        const result = await Swal.fire({
          title: user.status === 'active' ? 'Desactivar usuario' : 'Activar usuario',
          text: `¿Estás seguro de ${user.status === 'active' ? 'desactivar' : 'activar'} el usuario ${user.email}?`,
          icon: 'warning',
          showCancelButton: true,
          confirmButtonText: '<span style="color: white; font-weight: 600;">Sí</span>',
          cancelButtonText: 'Cancelar',
          confirmButtonColor: '#3b82f6',
          cancelButtonColor: '#6b7280',
          buttonsStyling: true
        });
        if (result && result.isConfirmed) {
          await this.toggleStatus(user.id);
        }
      } catch (e) {
        Swal.fire("Error", "No se pudo procesar la solicitud.", "error");
      }
    },

    toggleSelectAll() {
      this.allSelected = !this.allSelected;
      this.users.forEach(user => (user.selected = this.allSelected));
    },
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--;
        this.fetchUsers();
      }
    },
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++;
        this.fetchUsers();
      }
    },
    goToPage(page) {
      if (page >= 1 && page <= this.totalPages) {
        this.currentPage = page;
        this.fetchUsers();
      }
    },
    openAddModal() {
      this.isEditing = false;
      this.form = {
        id: null,
        name: '',
        firstLastName: '',
        secondLastName: '',
        phoneNumber: '',
        comments: '',
        email: '',
        role: '',
        status: 'active',
        createdAt: new Date(),
        password: ''
      };
      this.showModal = true;
    },
    closeModal() {
      this.showModal = false;
    },
    async viewUser(user) {
      // Abre el modal con el userId - el modal cargará los datos desde el API
      this.showPersonalInfoModal = true;
      this.personalInfoUserId = user.id;
      this.personalInfo = null;
      this.loadingPersonalInfo = false;
      this.personalInfoError = null;
    },
    closePersonalInfoModal() {
      this.showPersonalInfoModal = false;
      this.personalInfo = null;
      this.personalInfoUserId = null;
      this.personalInfoError = null;
    },
    editUser(user) {
      // Abrir modal para cambiar rol (mantener modal original para creación)
      this.roleModalUser = user;
      this.showRoleModal = true;
    },
     // manejador cuando el modal de rol emite saved
     async onRoleSaved(updatedData) {
       // updatedData puede contener el usuario actualizado o { id, role }
       this.showRoleModal = false;
       this.roleModalUser = null;
       try {
         // Recargamos la tabla completa desde el backend para obtener los datos actualizados
         await this.fetchUsers();
         ToastSuccess('Rol actualizado correctamente');
       } catch (e) {
         console.error('Error al recargar usuarios después de actualizar rol:', e);
         ToastError('Error al recargar la tabla');
       }
     },
    onRoleModalClose() {
      this.showRoleModal = false;
      this.roleModalUser = null;
    },
    async confirmDelete(userId) {
      try {
        const user = this.users.find(u => u.id === userId) || {};
        const result = await Swal.fire({
          title: 'Eliminar usuario',
          text: `¿Estás seguro de eliminar al usuario ${user.email || ('#' + userId)}? Esta acción no se puede deshacer.`,
          icon: 'warning',
          showCancelButton: true,
          confirmButtonText: '<span style="color: white; font-weight: 600;">Sí, eliminar</span>',
          cancelButtonText: 'Cancelar',
          confirmButtonColor: '#dc2626',
          cancelButtonColor: '#6b7280',
          customClass: {
            confirmButton: 'swal2-confirm-custom',
            cancelButton: 'swal2-cancel-custom'
          },
          buttonsStyling: true
        });

        if (result && result.isConfirmed) {
          await this.deleteUser(userId);
        }
      } catch (e) {
        console.error('Error en confirmDelete:', e);
        ToastError('Error al eliminar');
      }
    },
    async deleteUser(userId) {
      try {
        console.log('🔴 [DELETE] /admin/users/' + userId);
        LoadAlert(true);
        const response = await axiosConfiguration.doDelete(`/admin/users/${userId}`);
        console.log('✅ [DELETE] /admin/users/' + userId + ' - Respuesta:', response.data);
        LoadAlert(false);

        if (response && response.data && response.data.success) {
          ToastSuccess('Usuario eliminado', 'El usuario ha sido eliminado correctamente');
          // REFRESH: recargar lista desde backend para mantener consistencia
          await this.fetchUsers();
        } else {
          const errorMessage = response?.data?.message || 'No se pudo eliminar el usuario';
          ToastError('Error al eliminar', errorMessage);
        }
      } catch (error) {
        console.error('❌ [DELETE] /admin/users/' + userId + ' - Error:', error.response?.data || error.message);
        LoadAlert(false);

        // Mensajes de error más específicos
        let errorMessage = 'No se pudo eliminar el usuario';
        if (error.response) {
          if (error.response.status === 404) {
            errorMessage = 'Usuario no encontrado';
          } else if (error.response.status === 403) {
            errorMessage = 'No tienes permisos para eliminar este usuario';
          } else if (error.response.status === 409) {
            errorMessage = 'El usuario no puede ser eliminado debido a dependencias';
          } else if (error.response.data && error.response.data.message) {
            errorMessage = error.response.data.message;
          }
        } else if (error.request) {
          errorMessage = 'No se pudo conectar con el servidor';
        }

        ToastError('Error al eliminar', errorMessage);
      }
    },
    async saveUser() {
      try {
        if (this.isEditing) {
          ToastSuccess('Usuario actualizado', 'Cambios aplicados (simulado)');
        } else {
          if (!this.form.name || !this.form.firstLastName) {
            ToastError('Campos requeridos', 'Nombre y Apellido Paterno son obligatorios');
            return;
          }
          const providedPwd = (this.form.password || '').toString();
          if (providedPwd && providedPwd.length > 0) {
            const check = this.validatePassword(providedPwd);
            if (!check.valid) {
              ToastError('Contraseña inválida', check.message);
              return;
            }
          }

          // Si no proporcionó contraseña, generamos una que cumpla la política
          const password = (providedPwd && providedPwd.length > 0) ? providedPwd : this.generateRandomPassword(12);

           const payload = {
             name: this.form.name.trim(),
             firstLastName: this.form.firstLastName.trim(),
             secondLastName: this.form.secondLastName ? this.form.secondLastName.trim() : null,
             phoneNumber: this.form.phoneNumber ? this.form.phoneNumber.trim() : null,
             comments: this.form.comments ? this.form.comments.trim() : null,
             email: this.form.email.toLowerCase(), // Convertir email a minúsculas
             password,
             role: this.form.role || 'ALMACENISTA',
             status: this.form.status === 'active',
             preVerified: true
           };

            try {
              console.log('🟢 [POST] /admin/users - Payload:', payload);
              LoadAlert(true);
              const response = await axiosConfiguration.doPost('/admin/users', payload);
              console.log('✅ [POST] /admin/users - Respuesta:', response.data);
              LoadAlert(false);
              ToastSuccess('Usuario creado', `Contraseña: ${password}`);
            } catch (apiError) {
              console.error('❌ [POST] /admin/users - Error:', apiError.response?.data || apiError.message);
              LoadAlert(false);
              // Mostrar el mensaje de error específico del backend
              const errorMessage = apiError.message || 'No se pudo crear el usuario';
              ToastError('Error al crear usuario', errorMessage);
              return;
            }
         }
         this.closeModal();
         await this.fetchUsers();
       } catch (e) {
         ToastError('Error guardando usuario', e && e.message ? e.message : '');
       }
     },
    generateRandomPassword(length = 12) {
      // Asegurar longitud mínima
      const minLen = 8;
      if (typeof length !== 'number' || length < minLen) length = minLen;

      const upper = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
      const lower = 'abcdefghijklmnopqrstuvwxyz';
      const numbers = '0123456789';
      // símbolos permitidos por la política
      const symbols = '@$!%*?&.#-_+=';

      // Asegurar al menos: 1 mayúscula, 1 minúscula, 1 dígito y 1 símbolo
      const required = [];
      required.push(upper[Math.floor(Math.random() * upper.length)]);
      required.push(lower[Math.floor(Math.random() * lower.length)]);
      // incluir tanto dígito como símbolo para mayor seguridad (cumple 'y/o')
      required.push(numbers[Math.floor(Math.random() * numbers.length)]);
      required.push(symbols[Math.floor(Math.random() * symbols.length)]);

      const all = upper + lower + numbers + symbols;
      const remainingLength = Math.max(0, length - required.length);
      const remaining = Array.from({ length: remainingLength }, () => all[Math.floor(Math.random() * all.length)]);

      const pwdArr = required.concat(remaining);
      // shuffle usando Fisher-Yates
      for (let i = pwdArr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [pwdArr[i], pwdArr[j]] = [pwdArr[j], pwdArr[i]];
      }
      return pwdArr.join('');
    },
    validatePassword(pwd) {
      // Si no se proporciona contraseña, será generada -> válida
      if (!pwd || pwd.length === 0) return { valid: true };
      if (pwd.includes(' ')) return { valid: false, message: 'La contraseña no puede contener espacios.' };
      if (pwd.length < 8) return { valid: false, message: 'La contraseña debe tener al menos 8 caracteres.' };
      const hasUpper = /[A-Z]/.test(pwd);
      const hasLower = /[a-z]/.test(pwd);
      const hasDigit = /[0-9]/.test(pwd);
      const hasSpecial = /[@$!%*?&.#\-_+=]/.test(pwd);
      if (!hasUpper || !hasLower) return { valid: false, message: 'La contraseña debe contener mayúsculas y minúsculas.' };
      if (!hasDigit || !hasSpecial) return { valid: false, message: 'La contraseña debe contener al menos un número y un carácter especial (@$!%*?&.#-_+=).' };
      return { valid: true };
    },
    getInitials(name) {
      if (!name) return '';
      return name.split(' ').map(p => p.charAt(0)).slice(0,2).join('').toUpperCase();
    },
    getUserColor(name) {
      const s = name || '';
      let hash = 0;
      for (let i = 0; i < s.length; i++) hash = s.charCodeAt(i) + ((hash << 5) - hash);
      const hue = Math.abs(hash) % 360;
      return `hsl(${hue} 70% 45%)`;
    },

    checkPasswordMatch() {
      // Solo marcar como coincidente si ambos campos tienen valor y son iguales
      if (this.form.password && this.confirmPassword) {
        this.passwordMatch = this.form.password === this.confirmPassword;
      } else {
        this.passwordMatch = false;
      }
    },
  },
  mounted() {
    this.fetchUsers();
  }
};
</script>

<style scoped>
/* Variables de colores */
:root {
  --primary-color: #4F46E5;
  --primary-dark: #4338CA;
  --secondary-color: #10B981;
  --danger-color: #EF4444;
  --warning-color: #F59E0B;
  --info-color: #3B82F6;
  --text-primary: #1F2937;
  --text-secondary: #6B7280;
  --bg-light: #F9FAFB;
  --border-color: #E5E7EB;
}



/* Layout */
.layout {
  display: flex;
  padding-left: 0; /* Espaciado entre el lado izquierdo y el contenido */
}

.layout > admin-sidebar {
  width: 250px;
  flex-shrink: 0;
}

.layout > .content {
  flex: 1;
  padding: 1rem;
}

/* Header Section */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
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
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.subtitle {
  color: var(--text-secondary);
  font-size: 1rem;
  margin: 0;
}

/* Breadcrumb */
.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  padding: 0.5rem 0;
  margin: 0.5rem 0;
  list-style: none;
  background-color: transparent;
  font-size: 0.875rem;
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

.btn-add-user {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 6px rgba(0, 70, 229, 0.2);
}

.btn-add-user:hover {
  background: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 70, 229, 0.3);
}

.btn-add-user svg {
  width: 1.25rem;
  height: 1.25rem;
}

/* Stats Cards */
.stats-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); /* Más pequeño */
  gap: 0.75rem; /* Menor separación */
  margin-bottom: 1.25rem;
}

.stat-card {
  background: white;
  padding: 0.75rem; /* Menor padding */
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.75rem; /* Menor separación interna */
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  min-width: 0;
}

.stat-icon {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg {
  width: 1.2rem;
  height: 1.2rem;
  color: white;
}

.stat-icon.blue   { background: #3b82f6; }
.stat-icon.purple { background: #7c3aed; }
.stat-icon.orange { background: #f59e0b; }
.stat-icon.teal   { background: #0d9488; }

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 0.75rem; /* Más pequeño */
  color: var(--text-secondary);
  margin: 0 0 0.15rem 0;
  font-weight: 500;
}

.stat-value {
  font-size: 1.2rem; /* Más pequeño */
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

/* Control Panel */
.control-panel {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
}

.search-wrapper {
  position: relative;
  flex: 1;
  min-width: 300px;
}

.search-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  width: 1.25rem;
  height: 1.25rem;
  color: var(--text-secondary);
}

.search-input {
  width: 100%;
  padding: 0.875rem 1rem 0.875rem 3rem;
  border: 2px solid var(--border-color);
  border-radius: 0.75rem;
  font-size: 1rem;
  transition: all 0.3s ease;
  background: white;
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.filter-wrapper {
  min-width: 200px;
}

.filter-select {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 2px solid var(--border-color);
  border-radius: 0.75rem;
  font-size: 1rem;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.table-section {
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.table thead {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.table thead th {
  padding: 15px;
  text-align: left;
  font-weight: 600;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.table tbody tr {
  border-bottom: 1px solid #dee2e6;
  transition: background-color 0.2s ease;
}

.table tbody tr:hover {
  background-color: #f8f9fa;
}

.table tbody td {
  padding: 15px;
  color: #495057;
  font-size: 14px;
}



.user-table {
  width: 100%;
  border-collapse: collapse;
  margin: 0;
}

.user-table thead {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.user-table th {
  padding: 15px;
  text-align: left;
  font-weight: 600;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.user-table tbody tr {
  border-bottom: 1px solid #dee2e6;
  transition: background-color 0.2s ease;
}

.user-table tbody tr:hover {
  background-color: #f8f9fa;
}

.user-table td {
  padding: 15px;
  color: #495057;
  font-size: 14px;
}

.user-row {
  transition: background-color 0.2s ease;
}

.user-row:hover {
  background: var(--bg-light);
}

.user-id {
  font-weight: 600;
  color: var(--text-secondary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 0.875rem;
}

.user-name {
  font-weight: 500;
  color: var(--text-primary);
}

.user-email {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.user-date {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.badge {
  display: inline-block;
  padding: 0.375rem 0.875rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.badge.admin {
  background: rgba(79, 70, 229, 0.1);
  color: var(--primary-color);
}

.badge.user {
  background: rgba(16, 185, 129, 0.1);
  color: var(--secondary-color);
}

/* Roles reales del backend */
.badge.administrador {
  background: rgba(79, 70, 229, 0.1);
  color: var(--primary-color);
}

.badge.almacenista {
  background: rgba(245, 158, 11, 0.1);
  color: var(--warning-color);
}

.badge.auxiliar {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
}

.badge.auxiliar_de_conteo {
  background: rgba(59, 130, 246, 0.08);
  color: var(--info-color);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.875rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-badge.active {
  background: rgba(16, 185, 129, 0.1);
  color: var(--secondary-color);
}

.status-badge.inactive {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger-color);
}

.status-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
}

.status-badge.active .status-dot {
  background: var(--secondary-color);
}

.status-badge.inactive .status-dot {
  background: var(--danger-color);
}

.actions-cell {
  width: 150px;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.btn-action {
  padding: 0.5rem;
  border-radius: 0.5rem;
  transition: all 0.2s ease;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-action svg {
  width: 1.25rem;
  height: 1.25rem;
  stroke: white;
}

/* Botón Ver - Azul */
.btn-view {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.btn-view:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

/* Botón Editar - Naranja */
.btn-edit {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.btn-edit:hover {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.4);
}

/* Botón Toggle - Morado */
.btn-toggle {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  color: white;
}

.btn-toggle:hover {
  background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.4);
}

.btn-toggle svg {
  stroke: white;
}

/* Botón Eliminar - Rojo */
.btn-delete {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.btn-delete svg {
  stroke: white;
}

.btn-delete:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}

.no-data {
  text-align: center;
  padding: 3rem 1rem !important;
  color: var(--text-secondary);
}

.no-data svg {
  width: 4rem;
  height: 4rem;
  margin-bottom: 1rem;
  color: var(--text-secondary);
  opacity: 0.5;
}

.no-data p {
  margin: 0;
  font-size: 1.125rem;
}

/* Pagination */
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: white;
  border-radius: 1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  flex-wrap: wrap;
  gap: 1rem;
}

.pagination-info {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pagination-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 2px solid var(--border-color);
  background: white;
  border-radius: 0.5rem;
  color: var(--text-primary);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-btn:hover:not(:disabled) {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-btn svg {
  width: 1rem;
  height: 1rem;
}

.page-numbers {
  display: flex;
  gap: 0.25rem;
}

.page-number {
  width: 2.5rem;
  height: 2.5rem;
  border: 2px solid var(--border-color);
  background: white;
  border-radius: 0.5rem;
  color: var(--text-primary);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-number:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.page-number.active {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: white;
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
  padding: 1rem;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-card {
  background: white;
  border-radius: 8px;
  width: 100%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}

.modal-header-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px;
  border-bottom: 1px solid #ddd;
  background: #f8f8f8;
  gap: 20px;
}

.header-content {
  flex: 1;
}

.modal-title {
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.modal-subtitle {
  margin: 0;
  font-size: 13px;
  color: #666;
}

.btn-modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
  line-height: 1;
  min-width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-modal-close:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #333;
}

.modal-body {
  padding: 24px;
}

/* Form Sections in Modal */
.form-section {
  margin-bottom: 24px;
}

.form-section:last-of-type {
  margin-bottom: 20px;
}

.form-section-title {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid #2196F3;
  padding-bottom: 8px;
}

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-weight: 500;
  font-size: 13px;
  color: #555;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.required {
  color: #f44336;
  font-weight: 600;
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 10px 12px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: white;
  font-family: inherit;
  transition: all 0.3s ease;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #2196F3;
  box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: #999;
  font-size: 12px;
}

.form-group textarea {
  resize: vertical;
  font-family: inherit;
}

select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%232196F3' d='M10.293 3.293L6 7.586 1.707 3.293A1 1 0 00.293 4.707l5 5a1 1 0 001.414 0l5-5a1 1 0 10-1.414-1.414z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 10px center;
  padding-right: 32px;
}

/* Password Input */
.password-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.password-input-wrapper input {
  padding-right: 40px;
  width: 100%;
}

.toggle-password {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  transition: all 0.3s ease;
  border-radius: 4px;
}

.toggle-password:hover {
  background: rgba(33, 150, 243, 0.1);
}

.hint-text {
  color: #999;
  font-size: 11px;
  margin-top: 4px;
  display: block;
}

.error-text {
  color: #f44336;
  font-size: 11px;
  margin-top: 4px;
  display: block;
  font-weight: 500;
}

.success-text {
  color: #4CAF50;
  font-size: 11px;
  margin-top: 4px;
  display: block;
  font-weight: 500;
}




.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #ddd;
}

.btn-cancel,
.btn-submit {
  padding: 10px 20px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn-cancel {
  background: #f5f5f5;
  color: #333;
  border-color: #ddd;
}

.btn-cancel:hover {
  background: #eee;
  border-color: #999;
}

.btn-submit {
  background: #28a745;
  color: white;
}

.btn-submit:hover {
  background: #20c997;
  box-shadow: 0 2px 8px rgba(40, 167, 69, 0.2);
}

.btn-submit:active {
  transform: translateY(1px);
}

.input-success {
  border: 2px solid #22c55e !important;
  box-shadow: 0 0 0 2px #bbf7d0 !important;
}

.modal-fade-enter-active .modal-content {
  animation: modalSlideIn 0.3s ease;
}

.modal-fade-leave-active .modal-content {
  animation: modalSlideOut 0.3s ease;
}

@keyframes modalSlideIn {
  from {
    transform: translateY(-50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes modalSlideOut {
  from {
    transform: translateY(0);
    opacity: 1;
  }
  to {
    transform: translateY(-50px);
    opacity: 0;
  }
}

/* Responsive Design */
@media (max-width: 768px) {
  .admin-user-management {
    padding: 1rem;
  }

  .page-title {
    font-size: 1.5rem;
  }

  .stats-container {
    grid-template-columns: 1fr;
  }

  .control-panel {
    flex-direction: column;
  }

  .search-wrapper,
  .filter-wrapper {
    min-width: 100%;
  }

  .table-container {
    overflow-x: auto;
  }

  .user-table {
    min-width: 800px;
  }

  .pagination-container {
    flex-direction: column;
    text-align: center;
  }

  .modal-content {
    max-width: 95vw;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}

/* Estilos globales para SweetAlert2 */
:deep(.swal2-confirm) {
  color: white !important;
}

:deep(.swal2-styled.swal2-confirm) {
  color: white !important;
  font-weight: 600 !important;
}

:deep(.swal2-styled.swal2-cancel) {
  font-weight: 600 !important;
}


.personal-info-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

</style>












