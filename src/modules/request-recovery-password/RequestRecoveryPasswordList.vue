<template>
  <div class="recovery-password-container">
    <div class="container-fluid">
      <!-- Header Section -->
      <section class="header-section">
        <div class="header-row">
          <div class="title-wrapper">
            <h1 class="page-title">Solicitudes de Recuperación de Contraseña</h1>
            <nav style="--bs-breadcrumb-divider: '>';" aria-label="breadcrumb">
              <ol class="breadcrumb">
                <li class="breadcrumb-item"><a href="/Admin/user-management">Gestión de Usuarios</a></li>
                <li class="breadcrumb-item active" aria-current="page">Solicitudes de Recuperación</li>
              </ol>
            </nav>
            <p class="subtitle">Administra y controla las solicitudes de recuperación de contraseña</p>
          </div>
        </div>
        <!-- Tabs -->
        <div class="tabs-container">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="tab-btn"
            :class="{ 'tab-active': activeTab === tab.value }"
            @click="changeTab(tab.value)"
          >
            <component :is="'svg'" v-if="tab.value === 'all'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="tab-svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </component>
            <component :is="'svg'" v-if="tab.value === 'pending'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="tab-svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </component>
            <component :is="'svg'" v-if="tab.value === 'accepted'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="tab-svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </component>
            <component :is="'svg'" v-if="tab.value === 'rejected'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="tab-svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </component>
            <span class="tab-label">{{ tab.label }}</span>
            <span class="tab-count">{{ getTabCount(tab.value) }}</span>
          </button>
        </div>
      </section>

      <!-- Table Section -->
      <section class="table-section">
        <div class="table-wrapper">
          <table class="modern-table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Correo Electrónico</th>
                <th>Fecha de Solicitud</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in requests" :key="request.requestId" class="table-row">
                <td>
                  <div class="user-cell">
                    <div class="user-avatar">{{ getInitials(request.username) }}</div>
                    <span class="user-name">{{ request.username }}</span>
                  </div>
                </td>
                <td class="email-text">{{ request.email }}</td>
                <td>
                  <span class="date-text">{{ request.date }}</span>
                </td>
                <td>
                  <span class="status-badge"
                    :class="{
                      'status-pending':  request.status === 'PENDING',
                      'status-accepted': request.status === 'ACCEPTED',
                      'status-rejected': request.status === 'REJECTED'
                    }"
                  >
                    <span class="status-dot"></span>
                    {{ getStatusText(request.status) }}
                  </span>
                </td>
                <td>
                  <div class="action-buttons">
                    <button
                      v-if="request.status === 'PENDING'"
                      class="btn-action btn-accept"
                      @click="resolveRequest(request.requestId)"
                      title="Aceptar solicitud"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                      Aceptar
                    </button>
                    <button
                      v-if="request.status === 'PENDING'"
                      class="btn-action btn-reject"
                      @click="rejectRequest(request.requestId)"
                      title="Rechazar solicitud"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                      Rechazar
                    </button>
                    <span v-if="request.status !== 'PENDING'" class="action-completed">
                      {{ request.status === 'ACCEPTED' ? 'Aceptada' : 'Rechazada' }}
                    </span>
                  </div>
                </td>
              </tr>
              <tr v-if="!requests || requests.length === 0">
                <td colspan="5" class="empty-state">
                  <div class="empty-content">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                    </svg>
                    <p>No hay solicitudes</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Pagination Section -->
      <section class="pagination-section">
        <div class="row mt--4" style="text-align: center;">
          <div class="col-auto">
            <ul class="pagination">
              <li v-for="page in totalPages" :key="page" class="page-item">
                <button class="page-link" @click="goToPage(page)">{{ page }}</button>
              </li>
            </ul>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import axiosConfiguration from "@/config/axiosConfig.js";
import { ToastWarning, ToastSuccess } from "@/utils/SweetAlert.js";
import Swal from 'sweetalert2';

// Constantes globales
const REQUEST_STATUS = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED'
};

const API_ENDPOINTS = {
  GET_PAGE: '/auth/getPage',
  GET_HISTORY: '/auth/getHistory',
  RESOLVE_REQUEST: '/auth/resolveRequest',
  REJECT_REQUEST: '/auth/rejectRequest'
};

export default defineComponent({
  name: 'RequestRecoveryPasswordList',
  data() {
    return {
      requests: [],
      currentPage: 1,
      perPage: 8,
      totalElements: 0,
      search: '',
      activeTab: 'all',
      tabs: [
        { value: 'all', label: 'Todas', icon: '📋' },
        { value: 'pending', label: 'Pendientes', icon: '⏳' },
        { value: 'accepted', label: 'Aceptadas', icon: '✅' },
        { value: 'rejected', label: 'Rechazadas', icon: '❌' }
      ],
      loading: false,
      tabCounts: {
        all: 0,
        pending: 0,
        accepted: 0,
        rejected: 0
      }
    };
  },
  computed: {
    totalPages() {
      return Math.ceil(this.totalElements / this.perPage) || 1;
    },
    totalRequests() {
      return this.totalElements;
    },
    isPendingTab() {
      return this.activeTab === 'pending';
    },
    currentEndpoint() {
      return this.isPendingTab ? API_ENDPOINTS.GET_PAGE : API_ENDPOINTS.GET_HISTORY;
    },
    currentStatusFilter() {
      const statusMap = {
        accepted: REQUEST_STATUS.ACCEPTED,
        rejected: REQUEST_STATUS.REJECTED
      };
      return statusMap[this.activeTab] || null;
    }
  },
  methods: {
    // ============================================
    // HELPERS
    // ============================================
    getInitials(username) {
      if (!username) return '??';
      const names = username.trim().split(' ');
      if (names.length >= 2) {
        return (names[0][0] + names[1][0]).toUpperCase();
      }
      return username.substring(0, 2).toUpperCase();
    },

    getStatusText(status) {
      const statusMap = {
        [REQUEST_STATUS.PENDING]: '⏳ Pendiente',
        [REQUEST_STATUS.ACCEPTED]: '✅ Aceptada',
        [REQUEST_STATUS.REJECTED]: '❌ Rechazada'
      };
      return statusMap[status] || status;
    },

    getTabCount(tabValue) {
      return this.tabCounts[tabValue] || 0;
    },

    // ============================================
    // DATA LOADING
    // ============================================
    async changeTab(tabValue) {
      if (this.activeTab === tabValue) return;
      this.activeTab = tabValue;
      this.currentPage = 1;
      await this.loadRequests();
    },

    async loadRequests() {
      this.loading = true;
      try {
        // Para tabs "accepted" y "rejected", necesitamos filtrar manualmente
        // porque el backend ignora el parámetro status
        if (this.activeTab === 'accepted' || this.activeTab === 'rejected') {
          // Obtener TODAS las solicitudes del historial
          const response = await axiosConfiguration.doGet(API_ENDPOINTS.GET_HISTORY, {
            page: 0,
            size: 1000, // Tamaño grande para obtener todo
            search: this.search || ''
          });

          if (response?.data) {
            const allRequests = response.data.content || [];

            // Filtrar por estado manualmente
            const filteredRequests = allRequests.filter(request =>
              request.status === this.currentStatusFilter
            );

            // Aplicar paginación manual
            const startIndex = (this.currentPage - 1) * this.perPage;
            const endIndex = startIndex + this.perPage;

            this.requests = filteredRequests.slice(startIndex, endIndex);
            this.totalElements = filteredRequests.length;
          } else {
            this.requests = [];
            this.totalElements = 0;
          }
        } else {
          // Para "all" y "pending", usar el endpoint normal
          const params = {
            page: this.currentPage - 1,
            size: this.perPage,
            search: this.search || ''
          };

          const response = await axiosConfiguration.doGet(this.currentEndpoint, params);

          if (response?.data) {
            this.requests = response.data.content || [];
            this.totalElements = response.data.totalElements || 0;
          } else {
            this.requests = [];
            this.totalElements = 0;
          }
        }

        // Actualizar contadores
        await this.updateCounts();
      } catch (error) {
        this.handleError('No se pudieron cargar las solicitudes', error);
        this.requests = [];
        this.totalElements = 0;
      } finally {
        this.loading = false;
      }
    },

    async updateCounts() {
      try {
        const ts = Date.now(); // cache-buster

          // Obtener el total de solicitudes pendientes
        const pendingResp = await axiosConfiguration.doGet(API_ENDPOINTS.GET_PAGE, {
          page: 0,
          size: 1,
          _ts: ts
        });

        // Obtener TODAS las solicitudes del historial para contar manualmente
        // ya que el backend ignora el parámetro status
        const allHistoryResp = await axiosConfiguration.doGet(API_ENDPOINTS.GET_HISTORY, {
          page: 0,
          size: 1000, // Tamaño grande para obtener todas las solicitudes
          _ts: ts
        });

        const historyContent = allHistoryResp?.data?.content || [];
        const totalHistory = allHistoryResp?.data?.totalElements || 0;

        // Contar manualmente por estado
        let acceptedCount = 0;
        let rejectedCount = 0;

        historyContent.forEach(request => {
          if (request.status === 'ACCEPTED') {
            acceptedCount++;
          } else if (request.status === 'REJECTED') {
            rejectedCount++;
          }
        });

        // Log para debug
        console.log('[updateCounts] Contadores calculados:', {
          all: totalHistory,
          pending: pendingResp?.data?.totalElements ?? 0,
          accepted: acceptedCount,
          rejected: rejectedCount,
          historyContent: historyContent.length
        });

        this.tabCounts = {
          all: totalHistory,
          pending: pendingResp?.data?.totalElements ?? 0,
          accepted: acceptedCount,
          rejected: rejectedCount
        };
      } catch (error) {
        console.error('[updateCounts] Error:', error);
      }
    },


    goToPage(page) {
      if (page === this.currentPage) return;
      this.currentPage = page;
      this.loadRequests();
    },
    async getAllRequests() {
      await this.loadRequests();
    },

    // ============================================
    // ERROR HANDLING
    // ============================================
    handleError(message, error) {
      console.error('[RequestRecoveryPasswordList]', message, error);
      ToastWarning('Error', message);
    },

    // ============================================
    // ACTIONS
    // ============================================

    async rejectRequest(id) {
      const confirmed = await Swal.fire({
        title: '¿Estás seguro?',
        text: 'Se rechazará la solicitud de recuperación de contraseña',
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Sí, rechazar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#f56565'
      });

      if (!confirmed.isConfirmed) return;

      try {
        const response = await axiosConfiguration.doPost(
          API_ENDPOINTS.REJECT_REQUEST,
          { requestId: id }
        );

        if (response?.status === 200) {
          ToastSuccess('Éxito', 'Solicitud rechazada correctamente');
          await this.loadRequests();
        }
      } catch (error) {
        this.handleError('No se pudo rechazar la solicitud', error);
      }
    },

    async resolveRequest(id) {
      const confirmed = await Swal.fire({
        title: 'Aceptar solicitud',
        text: '¿Deseas aceptar esta solicitud y proporcionar la contraseña al usuario?',
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Sí, aceptar',
        cancelButtonText: 'Cancelar',
        confirmButtonColor: '#48bb78'
      });

      if (!confirmed.isConfirmed) return;

      try {
        const response = await axiosConfiguration.doPost(
          API_ENDPOINTS.RESOLVE_REQUEST,
          { requestId: id }
        );

        if (response?.status === 200 && response.data) {
          ToastSuccess('Éxito', 'Solicitud aceptada correctamente');

          const generatedPassword = response.data.generatedPassword || response.data.password || null;
          if (generatedPassword) {
            await this.showPasswordToAdmin(generatedPassword);
          }

          await this.loadRequests();
        }
      } catch (error) {
        this.handleError('No se pudo aceptar la solicitud', error);
      }
    },

    // ============================================
    // PASSWORD DISPLAY
    // ============================================
    async showPasswordToAdmin(password) {
      await Swal.fire({
        title: 'Contraseña generada/registrada',
        html: this.generatePasswordDisplayHTML(password),
        showConfirmButton: true,
        confirmButtonText: 'Cerrar',
        allowOutsideClick: false,
        allowEscapeKey: false,
        didOpen: this.setupPasswordCopy
      });
    },

    // Generar HTML para mostrar la contraseña
    generatePasswordDisplayHTML(password) {
      return `
        <p>Entrega esta contraseña al usuario que solicitó el cambio:</p>
        <div style="display:flex;gap:8px;margin-top:8px;">
          <input id="__recovery_pw" readonly style="flex:1;padding:8px;border-radius:6px;border:1px solid #e2e8f0;background:#f7fafc" value="${password}" />
          <button id="__copy_pw_btn" class="swal2-confirm swal2-styled" style="margin-left:4px;">Copiar</button>
        </div>
        <p style="font-size:0.85rem;margin-top:10px;color:#475569;">Este diálogo permanecerá hasta que lo cierres. Asegúrate de comunicar la contraseña al usuario de forma segura.</p>
      `;
    },

    // Configurar funcionalidad de copia
    setupPasswordCopy() {
      const copyBtn = document.getElementById('__copy_pw_btn');
      const input = document.getElementById('__recovery_pw');

      if (copyBtn && input) {
        copyBtn.addEventListener('click', async () => {
          try {
            await this.copyToClipboard(input.value);
            ToastSuccess('Copiado', 'Contraseña copiada al portapapeles');
          } catch (e) {
            console.error('Error al copiar contraseña:', e);
            ToastWarning('Error', 'No se pudo copiar la contraseña automáticamente');
          }
        });
      }
    },

    // Copiar texto al portapapeles with fallback
    async copyToClipboard(text) {
      if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text);
      } else {
        // Fallback para navegadores más antiguos
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
      }
    },

    // Generador de contraseñas similar al usado en AdminUserManagement
    generateRandomPassword(length = 12) {
      const minLen = 8;
      if (typeof length !== 'number' || length < minLen) length = minLen;
      const upper = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
      const lower = 'abcdefghijklmnopqrstuvwxyz';
      const numbers = '0123456789';
      const symbols = '@$!%*?&.#-_+=';
      const required = [];
      required.push(upper[Math.floor(Math.random() * upper.length)]);
      required.push(lower[Math.floor(Math.random() * lower.length)]);
      required.push(numbers[Math.floor(Math.random() * numbers.length)]);
      required.push(symbols[Math.floor(Math.random() * symbols.length)]);
      const all = upper + lower + numbers + symbols;
      const remainingLength = Math.max(0, length - required.length);
      const remaining = Array.from({ length: remainingLength }, () => all[Math.floor(Math.random() * all.length)]);
      const pwdArr = required.concat(remaining);
      for (let i = pwdArr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [pwdArr[i], pwdArr[j]] = [pwdArr[j], pwdArr[i]];
      }
      return pwdArr.join('');
    },
  },

  mounted() {
    this.getAllRequests();
  },
});
</script>

<style scoped>
/* Header */
.header-section {
  margin-bottom: 2rem;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.25rem;
}

.title-wrapper { flex: 1; }

.page-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 0.25rem 0;
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

.breadcrumb-item a {
  color: #3b82f6;
  text-decoration: none;
}

.breadcrumb-item a:hover {
  color: #2563eb;
  text-decoration: underline;
}

.breadcrumb-item.active { color: #6b7280; }

/* Tabs */
.tabs-container {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  background: white;
  padding: 0.75rem;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  margin-bottom: 1.25rem;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  background: #f7fafc;
  border: 2px solid #e2e8f0;
  border-radius: 0.5rem;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  color: #4a5568;
}

.tab-btn:hover {
  background: #edf2f7;
  border-color: #cbd5e0;
}

.tab-btn.tab-active {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  border-color: transparent;
  color: white;
}

.tab-svg {
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
}

.tab-label { font-weight: 600; white-space: nowrap; }

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 0.4rem;
  background: rgba(0,0,0,0.1);
  border-radius: 10px;
  font-size: 0.72rem;
  font-weight: 700;
}

.tab-btn.tab-active .tab-count { background: rgba(255,255,255,0.25); }

/* Table */
.table-wrapper {
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.08);
  overflow: hidden;
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
}

.modern-table thead {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.modern-table th {
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.modern-table tbody tr {
  border-bottom: 1px solid #dee2e6;
}

.modern-table tbody tr:hover { background: #f8f9fa; }

.modern-table tbody tr:last-child { border-bottom: none; }

.modern-table td {
  padding: 15px;
  color: #495057;
  font-size: 14px;
  vertical-align: middle;
}

/* User cell */
.user-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.8rem;
  flex-shrink: 0;
}

.user-name { font-weight: 600; color: #2d3748; }

.email-text { color: #4a5568; font-size: 0.875rem; }

.date-text { color: #4a5568; font-size: 0.875rem; }

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

.status-pending  { background: rgba(245,158,11,0.12); color: #b45309; }
.status-accepted { background: rgba(16,185,129,0.12); color: #065f46; }
.status-rejected { background: rgba(239,68,68,0.12);  color: #991b1b; }

.status-pending  .status-dot { background: #f59e0b; }
.status-accepted .status-dot { background: #10b981; }
.status-rejected .status-dot { background: #ef4444; }

/* Action buttons */
.action-buttons {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.45rem 0.875rem;
  font-size: 0.8rem;
  font-weight: 600;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
}

.btn-action svg {
  width: 0.9rem;
  height: 0.9rem;
}

.btn-accept { background: #10b981; color: white; }
.btn-accept:hover { background: #059669; }

.btn-reject { background: #ef4444; color: white; }
.btn-reject:hover { background: #dc2626; }

.action-completed {
  font-size: 0.8rem;
  font-weight: 600;
  color: #6b7280;
}

/* Empty state */
.empty-state { padding: 3rem 1rem; text-align: center; }

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  color: #9ca3af;
}

.empty-content svg {
  width: 3rem;
  height: 3rem;
  opacity: 0.5;
}

.empty-content p { margin: 0; font-size: 1rem; }

/* Pagination */
.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
}

.pagination {
  display: flex;
  list-style: none;
  padding: 0;
  gap: 0.25rem;
}

.page-link {
  padding: 0.4rem 0.75rem;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  background: white;
  color: #374151;
  cursor: pointer;
  font-size: 0.875rem;
}

.page-link:hover { background: #f3f4f6; }

/* Responsive */
@media (max-width: 768px) {
  .page-title { font-size: 1.5rem; }

  .table-wrapper { overflow-x: auto; }
  .modern-table { min-width: 650px; }

  .tabs-container { gap: 0.375rem; }
  .tab-btn { padding: 0.5rem 0.875rem; font-size: 0.85rem; }
}

@media (max-width: 480px) {
  .page-title { font-size: 1.25rem; }
  .tabs-container { flex-direction: column; }
  .tab-btn { width: 100%; justify-content: space-between; }
  .action-buttons { flex-direction: column; }
  .btn-action { width: 100%; justify-content: center; }
}
</style>

