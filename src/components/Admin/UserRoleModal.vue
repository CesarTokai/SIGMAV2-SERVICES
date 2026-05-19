<template>
  <transition name="modal-fade">
    <div v-if="visible" class="modal-overlay" @click.self="close">
      <div class="modal-content">
        <div class="modal-header">
          <h2>
            <!-- fixed title -->
            Cambiar Rol de Usuario
          </h2>
          <button class="modal-close" @click="close">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form @submit.prevent="saveRole" class="modal-form">
          <div class="form-grid">
            <div class="form-group">
              <label>Nombre</label>
              <input type="text" :value="displayName" readonly />
            </div>

            <div class="form-group">
              <label for="role">Rol de Usuario</label>
              <select id="role" v-model="selectedRole" required>
                <option value="" disabled>Seleccione un rol</option>
                <option value="ADMINISTRADOR">ADMINISTRADOR</option>
                <option value="ALMACENISTA">ALMACENISTA</option>
                <option value="AUXILIAR">AUXILIAR</option>
                <option value="AUXILIAR_DE_CONTEO">AUXILIAR_DE_CONTEO</option>
              </select>
            </div>
          </div>

          <div class="modal-actions">
            <button type="button" class="btn-cancel" @click="close">Cancelar</button>
            <button type="submit" class="btn-save">Guardar</button>
          </div>
        </form>
      </div>
    </div>
  </transition>
</template>

<script>
import { LoadAlert, ToastSuccess, ToastError } from '@/utils/SweetAlert';
import axiosConfiguration from '@/config/axiosConfig.js';

export default {
  name: 'UserRoleModal',
  props: {
    visible: { type: Boolean, default: false },
    user: { type: Object, default: null }
  },
  data() {
    return {
      selectedRole: ''
    };
  },
  computed: {
    displayName() {
      if (!this.user) return '';
      return this.user.name || (this.user.email ? this.user.email.split('@')[0] : '');
    }
  },
  watch: {
    user: {
      immediate: true,
      handler(u) {
        this.selectedRole = (u && (u.rawRole || u.role)) || '';
      }
    },
    visible(val) {
      if (val && this.user) {
        this.selectedRole = (this.user.rawRole || this.user.role) || '';
      }
    }
  },
  methods: {
    close() {
      this.$emit('close');
    },
    async saveRole() {
      if (!this.user || !this.user.id) return;
      if (!this.selectedRole) {
        ToastError('Seleccione un rol válido');
        return;
      }

      const userId = this.user.id;
      const payload = { role: this.selectedRole };
      try {
        LoadAlert(true);
        const response = await axiosConfiguration.doputString(`/admin/users/${userId}/role`, payload);
        LoadAlert(false);
        ToastSuccess('Rol actualizado');
        // emit saved with updated data (backend response if available)
        this.$emit('saved', response && response.data ? response.data : { id: userId, role: this.selectedRole });
        this.close();
      } catch (err) {
        LoadAlert(false);
        console.error('Error actualizando rol:', err);
        ToastError('No se pudo actualizar el rol');
      }
    }
  }
};
</script>

<style scoped>
/* Reuse modal styles similar to parent so it looks consistent */
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
}
.modal-content {
  background: white;
  border-radius: 1rem;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1);
}
.modal-header {
  display:flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #E5E7EB;
}
.modal-form { padding: 1rem; }
.form-grid { display:grid; grid-template-columns: 1fr; gap:1rem; }
.form-group label { font-weight:600; margin-bottom:0.25rem; display:block; }
.form-group input[readonly] { background:#F3F4F6; border:1px solid #E5E7EB; padding:0.5rem; border-radius:6px; }
.form-group select { padding:0.5rem; border:1px solid #E5E7EB; border-radius:6px; width:100%; }
.modal-actions { display:flex; justify-content:flex-end; gap:0.5rem; padding-top:0.75rem; }
.btn-cancel { background:#F3F4F6; border:none; padding:0.5rem 1rem; border-radius:6px; }
.btn-save {   background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
; border:none; padding:0.5rem 1rem; border-radius:6px; }
</style>

