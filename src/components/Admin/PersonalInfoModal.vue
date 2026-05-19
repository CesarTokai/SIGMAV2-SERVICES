<template>
  <transition name="modal-fade">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')" role="dialog" aria-modal="true">
      <div class="modal-content modal-info" aria-labelledby="personal-info-title">
        <header class="modal-header improved">
          <div class="modal-header-left">
            <h2 id="personal-info-title">Información Personal</h2>
            <p class="modal-subtitle">Detalles y contacto del usuario</p>
          </div>
          <button class="modal-close" @click="$emit('close')" aria-label="Cerrar ventana">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </header>

        <div class="modal-body">
          <div v-if="loading || isLoading" class="loading-block">
            <div class="spinner" aria-hidden="true"></div>
            <div class="loading-text">Cargando información...</div>
          </div>

          <div v-else-if="error || errorMessage" class="error-message">
            {{ error || errorMessage }}
          </div>

          <div v-else class="content-grid">
            <aside class="avatar-column">
              <div class="avatar-card">
                <template v-if="displayInfo && displayInfo.hasImage && !imageFailed">
                  <img :src="`/api/sigmav2/personal-information/user/${displayInfo.userId}/image`" alt="Foto de usuario" class="avatar-img" @error="onImageError" />
                </template>
                <template v-else>
                  <div class="avatar-fallback" aria-hidden="true">{{ initials }}</div>
                  <div class="avatar-unavailable">Imagen no disponible</div>
                </template>
              </div>

              <div class="avatar-meta">
                <div class="meta-name">{{ displayName }}</div>
                <div class="meta-id">ID: <span class="meta-id-value">{{ displayInfo?.userId ?? displayInfo?.id ?? '-' }}</span></div>
              </div>
            </aside>

            <section class="details-column">
              <dl class="details-list">
                <!-- Información Personal -->
                <div class="section-title">Información Personal</div>
                <div class="detail-row">
                  <dt class="detail-label">Nombre</dt>
                  <dd class="detail-value">{{ displayInfo?.name || '-' }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Apellido Paterno</dt>
                  <dd class="detail-value">{{ displayInfo?.firstLastName || '-' }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Apellido Materno</dt>
                  <dd class="detail-value">{{ displayInfo?.secondLastName || '-' }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Teléfono</dt>
                  <dd class="detail-value">{{ displayInfo?.phoneNumber || '-' }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Correo Electrónico</dt>
                  <dd class="detail-value">{{ displayInfo?.email || '-' }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Rol</dt>
                  <dd class="detail-value">{{ normalizedRole ? toDisplayRole(normalizedRole) : '-' }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Estado</dt>
                  <dd class="detail-value">
                    <span :class="['status-badge', normalizedStatus === 'active' ? 'active' : 'inactive']">
                      {{ normalizedStatus === 'active' ? 'Activo' : (normalizedStatus === 'inactive' ? 'Inactivo' : '-') }}
                    </span>
                  </dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Verificado</dt>
                  <dd class="detail-value">
                    <span :class="['status-badge', displayInfo?.verified ? 'verified' : 'unverified']">
                      {{ displayInfo?.verified ? '✓ Verificado' : '✗ No Verificado' }}
                    </span>
                  </dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Cuenta Bloqueada</dt>
                  <dd class="detail-value">
                    <span :class="['status-badge', displayInfo?.accountLocked ? 'locked' : 'unlocked']">
                      {{ displayInfo?.accountLocked ? 'Bloqueada' : 'Desbloqueada' }}
                    </span>
                  </dd>
                </div>

                <div class="detail-row">
                  <dt class="detail-label">Comentarios</dt>
                  <dd class="detail-value">{{ displayInfo?.comments || '-' }}</dd>
                </div>

                <!-- Seguridad -->
                <div class="section-title">Seguridad</div>
                <div class="detail-row">
                  <dt class="detail-label">Intentos Fallidos</dt>
                  <dd class="detail-value">
                    <span :class="['attempts-badge', getAttemptsClass(displayInfo?.attempts)]">
                      {{ displayInfo?.attempts ?? 0 }}
                    </span>
                  </dd>
                </div>

                <div class="detail-row">
                  <dt class="detail-label">Solicitudes de Cambio de Contraseña Pendientes</dt>
                  <dd class="detail-value">
                    <span :class="['status-badge', (displayInfo?.pendingPasswordChangeRequests || 0) > 0 ? 'warning' : 'safe']">
                      {{ displayInfo?.pendingPasswordChangeRequests ?? 0 }}
                    </span>
                  </dd>
                </div>

                <!-- Actividad -->
                <div class="section-title">Actividad</div>
                <div class="detail-row">
                  <dt class="detail-label">Estado de Actividad</dt>
                  <dd class="detail-value">
                    <span :class="['status-badge', normalizedStatus === 'active' ? 'active' : 'inactive']">
                      {{ normalizedStatus === 'active' ? 'Activo' : 'Inactivo' }}
                    </span>
                  </dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Último Login</dt>
                  <dd class="detail-value">{{ formatDate(displayInfo?.lastLoginAt) }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Última Actividad</dt>
                  <dd class="detail-value">{{ formatDate(displayInfo?.lastActivityAt) }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Fecha de Registro</dt>
                  <dd class="detail-value">{{ formatDate(displayInfo?.createdAt || displayInfo?.created_at) }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Última Actualización</dt>
                  <dd class="detail-value">{{ formatDate(displayInfo?.updatedAt || displayInfo?.updated_at) }}</dd>
                </div>
                <div class="detail-row">
                  <dt class="detail-label">Último Cambio de Contraseña</dt>
                  <dd class="detail-value">{{ formatDate(displayInfo?.lastPasswordChangeAt) }}</dd>
                </div>

                <!-- Almacenes -->
                <div class="section-title">Almacenes Asignados</div>
                <div class="detail-row">
                  <dt class="detail-label">Almacenes</dt>
                  <dd class="detail-value">
                    <template v-if="displayInfo?.assignedWarehouses && displayInfo.assignedWarehouses.length > 0">
                      <div class="warehouses-list">
                        <span v-for="(warehouse, index) in displayInfo.assignedWarehouses" :key="index" class="warehouse-badge">
                          {{ warehouse }}
                        </span>
                      </div>
                    </template>
                    <template v-else>
                      <span class="text-muted">Sin almacenes asignados</span>
                    </template>
                  </dd>
                </div>
              </dl>
            </section>
          </div>
        </div>

        <footer class="modal-actions improved">
          <button type="button" class="btn-cancel" @click="$emit('close')">Cerrar</button>
        </footer>
      </div>
    </div>
  </transition>
</template>

<script>
import axiosConfiguration from "@/config/axiosConfig.js";

export default {
  name: 'PersonalInfoModal',
  props: {
    visible: Boolean,
    personalInfo: Object,
    loading: Boolean,
    error: String,
    userId: [Number, String],
    toDisplayRole: {
      type: Function,
      default: (role) => {
        const roleMap = {
          'ADMINISTRADOR': 'Administrador',
          'ALMACENISTA': 'Almacenista',
          'AUXILIAR': 'Auxiliar',
          'AUXILIAR_DE_CONTEO': 'Auxiliar de Conteo'
        };
        return roleMap[role] || role;
      }
    },
    formatDate: {
      type: Function,
      default: (date) => {
        if (!date) return '-';
        try {
          return new Date(date).toLocaleDateString('es-MX', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
          });
        } catch (e) {
          return '-';
        }
      }
    }
  },
  emits: ['close', 'data-loaded'],
  data() {
    return {
      imageFailed: false,
      isLoading: false,
      errorMessage: null,
      userInfo: null
    };
  },
  watch: {
    visible(val) {
      if (val) {
        this.imageFailed = false;
        if (this.userId) {
          this.loadUserData();
        }
      }
    }
  },
  methods: {
    async loadUserData() {
      if (!this.userId) return;

      this.isLoading = true;
      this.errorMessage = null;

      try {
        const response = await axiosConfiguration.doGet(`/admin/users/${this.userId}`);

        // Extraer los datos del usuario (puede venir en response.data.data o response.data)
        let data = response?.data?.data || response?.data || response;

        // Mapear datos del endpoint al formato del modal
        this.userInfo = {
          id: data?.id || null,
          userId: data?.id || null,
          email: data?.email || null,
          name: data?.name || null,
          firstLastName: data?.firstLastName || null,
          secondLastName: data?.secondLastName || null,
          phoneNumber: data?.phoneNumber || null,
          comments: data?.comments || null,
          role: data?.role || null,
          rawRole: data?.role || null,
          status: data?.status !== undefined ? data?.status : null,
          fullName: [data?.name, data?.firstLastName, data?.secondLastName]
            .filter(Boolean)
            .join(' ') || null,
          hasImage: data?.hasImage || false,
          // Datos de actividad del mismo endpoint
          createdAt: data?.createdAt || null,
          updatedAt: data?.updatedAt || null,
          verified: data?.verified || false,
          accountLocked: data?.accountLocked || false,
          attempts: data?.attempts || 0,
          pendingPasswordChangeRequests: data?.pendingPasswordChangeRequests || 0,
          lastLoginAt: data?.lastLoginAt || null,
          lastActivityAt: data?.lastActivityAt || null,
          lastPasswordChangeAt: data?.passwordChangedAt || null,
          assignedWarehouses: data?.assignedWarehouses || []
        };

        console.log('═══════════════════════════════════════════════════════════');
        console.log('✅ USER INFO MAPEADO - CAMPOS DE ACTIVIDAD:');
        console.log('  ├─ lastLoginAt:', this.userInfo.lastLoginAt);
        console.log('  ├─ lastActivityAt:', this.userInfo.lastActivityAt);
        console.log('  ├─ createdAt:', this.userInfo.createdAt);
        console.log('  ├─ updatedAt:', this.userInfo.updatedAt);
        console.log('  ├─ lastPasswordChangeAt:', this.userInfo.lastPasswordChangeAt);
        console.log('  ├─ status:', this.userInfo.status);
        console.log('  ├─ verified:', this.userInfo.verified);
        console.log('  ├─ accountLocked:', this.userInfo.accountLocked);
        console.log('  ├─ attempts:', this.userInfo.attempts);
        console.log('  └─ pendingPasswordChangeRequests:', this.userInfo.pendingPasswordChangeRequests);
        console.log('═══════════════════════════════════════════════════════════');
        console.log('📋 USER INFO COMPLETO:');
        console.log(this.userInfo);
        console.log('═══════════════════════════════════════════════════════════');

        this.$emit('data-loaded', this.userInfo);
      } catch (error) {
        this.errorMessage = 'Error al cargar la información del usuario.';
        console.error('❌ ERROR CARGANDO USUARIO:');
        console.error('  ├─ Status:', error.response?.status);
        console.error('  ├─ Message:', error.response?.data?.message);
        console.error('  ├─ URL:', error.config?.url);
        console.error('  └─ Error completo:', error);
        console.log('═══════════════════════════════════════════════════════════');
      } finally {
        this.isLoading = false;
      }
    },
    onImageError() {
      this.imageFailed = true;
    },
    getAttemptsClass(attempts) {
      if (!attempts || attempts === 0) return 'safe';
      if (attempts <= 2) return 'warning';
      return 'danger';
    }
  },
  computed: {
    displayInfo() {
      return this.userInfo || this.personalInfo;
    },
    initials() {
      const info = this.displayInfo;
      const name = (info && info.name) || info?.fullName || '';
      if (!name) return '';
      const parts = name.trim().split(/\s+/).filter(Boolean);
      if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
      return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    },
    displayName() {
      const info = this.displayInfo;
      if (!info) return '-';
      return info.fullName || [info.name, info.firstLastName, info.secondLastName].filter(Boolean).join(' ') || '-';
    },
    normalizedRole() {
      const info = this.displayInfo;
      const role = (info && (info.role || info.rawRole)) || null;
      return role;
    },
    normalizedStatus() {
      const info = this.displayInfo;
      if (!info || info.status === undefined || info.status === null) return null;
      const s = info.status;
      if (typeof s === 'boolean') return s ? 'active' : 'inactive';
      if (typeof s === 'string') return s;
      return null;
    }
  }
};
</script>

<style scoped>
/* Modal improvements: header gradient, larger avatar, two-column layout */
.modal-content.modal-info {
  width: 100%;
  max-width: 900px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 30px 60px rgba(2,6,23,0.35);
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}
.modal-header.improved {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}
.modal-header.improved h2 {
  margin: 0;
  font-size: 1.25rem;
  letter-spacing: 0.2px;
}
.modal-subtitle {
  margin: 4px 0 0 0;
  font-size: 0.9rem;
  color: rgba(255,255,255,0.9);
}
.modal-close {
  background: rgba(255,255,255,0.1);
  border: none;
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display:flex;
  align-items:center;
  justify-content:center;
  cursor: pointer;
}
.modal-close svg { width: 18px; height: 18px; }

.modal-body {
  padding: 20px 24px;
  background: #fff;
  overflow-y: auto;
  flex: 1;
}
.loading-block { display:flex; align-items:center; gap:12px; padding:24px; }
.spinner {
  width:32px; height:32px; border-radius:50%; border:3px solid #eef2ff; border-top-color:#6366f1; animation:spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.loading-text { color:var(--text-primary, #111827); }

.content-grid {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 20px;
  align-items: start;
}
.avatar-column { display:flex; flex-direction:column; align-items:center; }
.avatar-card { width:140px; height:140px; border-radius:12px; display:flex; align-items:center; justify-content:center; background: linear-gradient(180deg, #f8fafc, #eef2ff); border:1px solid #e6eefc; overflow:hidden; }
.avatar-img { width:100%; height:100%; object-fit:cover; display:block; }
.avatar-fallback { width:100%; height:100%; display:flex; align-items:center; justify-content:center; font-size:38px; font-weight:700; color:#374151; background: linear-gradient(135deg,#f3f4f6,#e6eefc); }
.avatar-unavailable { margin-top:10px; color:#9ca3af; font-size:0.9rem; }
.avatar-meta { text-align:center; margin-top:12px; }
.meta-name { font-weight:600; color:#111827; }
.meta-id { color:#6b7280; font-size:0.9rem; margin-top:4px; }
.meta-id-value { font-weight:600; color:#374151; }

.details-column { min-width:0; }
.details-list { display:flex; flex-direction:column; gap:12px; }
.detail-row { display:flex; gap:12px; align-items:flex-start; }
.detail-label { width:170px; color:#6b7280; font-weight:600; flex-shrink:0; }
.detail-value { color:#111827; word-break:break-word; flex:1; }

/* Section Titles */
.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: #000000;
  margin-top: 16px;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 2px solid #e0e7ff;
}

.section-title:first-child {
  margin-top: 0;
}

/* Status Badges */
.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-badge.active {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.inactive {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge.verified {
  background: #dbeafe;
  color: #1e40af;
}

.status-badge.unverified {
  background: #fef3c7;
  color: #92400e;
}

.status-badge.locked {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge.unlocked {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.session-active {
  background: #dcfce7;
  color: #14532d;
}

.status-badge.session-inactive {
  background: #f3f4f6;
  color: #6b7280;
}

.status-badge.safe {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.warning {
  background: #fef3c7;
  color: #92400e;
}

/* Attempts Badge */
.attempts-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 700;
}

.attempts-badge.safe {
  background: #d1fae5;
  color: #065f46;
}

.attempts-badge.warning {
  background: #fef3c7;
  color: #92400e;
}

.attempts-badge.danger {
  background: #fee2e2;
  color: #991b1b;
}

/* Warehouses */
.warehouses-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.warehouse-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  background: #ede9fe;
  color: #5b21b6;
  font-size: 0.85rem;
  font-weight: 600;
}

.text-muted {
  color: #9ca3af;
  font-style: italic;
}

.modal-actions.improved {
  display:flex;
  justify-content:flex-end;
  gap:12px;
  padding:16px 24px;
  background:#fff;
  border-top:1px solid #f1f5f9;
}

.btn-cancel {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-cancel:hover {
  background: linear-gradient(135deg, #20c997 0%, #28a745 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
}

/* Responsive */
@media (max-width: 700px) {
  .content-grid { grid-template-columns: 1fr; }
  .avatar-card { width:120px; height:120px; }
  .detail-label { width:120px; }
}

.error-message { padding:18px; color:#b91c1c; background:#fff5f5; border:1px solid #fee2e2; border-radius:8px; }
</style>
