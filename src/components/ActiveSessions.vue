<template>
  <div class="active-sessions card">
    <header class="card-header">
      <h4>Sesiones activas</h4>
      <button class="refresh" @click="fetchSessions" :disabled="isLoading" :aria-busy="isLoading">🔄</button>
    </header>

    <div class="card-body">
      <div v-if="isLoading" class="loading">Cargando...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <ul v-else class="sessions-list">
        <li v-if="items.length === 0" class="empty">No hay sesiones activas</li>
        <li v-for="user in items" :key="user.id" class="session-item">
          <div class="user-row">
            <div class="user-name">{{ user.username || user.name || user.email }}</div>
            <div class="user-meta">{{ formatDate(user.lastLogin) }}</div>
          </div>
        </li>
      </ul>
    </div>


  </div>
</template>

<script>
import { defineComponent } from 'vue';
import axios from '../config/axiosConfig';
import { ToastError } from '../utils/SweetAlert';

export default defineComponent({
  name: 'ActiveSessions',
  data() {
    return {
      items: [],
      page: 0,
      size: 5,
      total: 0,
      isLoading: false,
      error: '',
      debugMsg: '',
      debugPayload: '',
    };
  },
  computed: {
    routePath() {
      return this.$route?.path ?? '';
    },
    token() {
      try {
        return window.localStorage.getItem('token') || '';
      } catch {
        return '';
      }
    },
    axiosAvailable() {
      return !!(axios && typeof axios.doGet === 'function');
    }
  },
  mounted() {
    console.log('[ActiveSessions] mounted');
    this.debugMsg = 'mounted';
    // small delay to ensure UI updates before network
    this.$nextTick(() => {
      this.fetchSessions();
    });
  },
  methods: {
    async fetchSessions(reset = true) {
      console.log('[ActiveSessions] fetchSessions called, reset=', reset);
      if (reset) {
        this.page = 0;
        this.items = [];
        this.total = 0;
      }
      this.isLoading = true;
      this.error = '';
      this.debugPayload = '';
      try {
        const url = `admin/users?status=true&page=${this.page}&size=${this.size}`;
        console.log('[ActiveSessions] requesting URL:', url);
        if (!axios || typeof axios.doGet !== 'function') {
          const msg = 'axios.doGet no disponible';
          console.error('[ActiveSessions]', msg);
          this.debugMsg = msg;
          this.isLoading = false;
          return;
        }
        const response = await axios.doGet(url);
        console.log('[ActiveSessions] response status/data:', response && response.status, response && response.data);
        // Intentar mapear la respuesta con formas comunes
        const payload = response.data?.data ?? response.data;
        try {
          this.debugPayload = JSON.stringify(payload, null, 2).slice(0, 2000);
        } catch (e) {
          this.debugPayload = String(payload).slice(0, 2000);
        }
        console.log('[ActiveSessions] payload resolved:', payload);
        // payload podría venir con diferentes formas: array directo, { content: [] }, { items: [] }, { users: [] }
        let list = [];
        if (Array.isArray(payload)) {
          list = payload;
        } else if (payload && Array.isArray(payload.content)) {
          list = payload.content;
          this.total = payload.totalElements ?? payload.total ?? payload.totalActiveUsers ?? this.total;
        } else if (payload && Array.isArray(payload.items)) {
          list = payload.items;
          this.total = payload.total ?? payload.totalElements ?? this.total;
        } else if (payload && Array.isArray(payload.users)) {
          list = payload.users;
          this.total = payload.totalElements ?? payload.totalActiveUsers ?? payload.total ?? this.total;
        }

        // Normalizar campos (id, username, lastLogin)
        list = list.map((u) => ({
          id: u.id ?? u.userId ?? u._id ?? JSON.stringify(u),
          username: u.username ?? u.name ?? u.email ?? 'sin nombre',
          lastLogin: u.lastLogin ?? u.last_login ?? u.updatedAt ?? u.createdAt ?? null,
        }));

        this.items = reset ? list : this.items.concat(list);
        if (!this.total) {
          // si total no viene, inferir a partir del primer request
          this.total = this.items.length;
        }
        // actualizar debug con la cantidad real añadida
        this.debugMsg = `Got ${list.length} users (mapped), total=${this.total}`;
      } catch (err) {
        this.error = 'Error al cargar sesiones activas.';
        ToastError('Error', this.error);
        // eslint-disable-next-line no-console
        console.error('ActiveSessions fetch error:', err);
        this.debugMsg = `Error: ${err?.toString ? err.toString() : String(err)}`;
        if (err && err.response) {
          try { this.debugPayload = JSON.stringify(err.response.data, null, 2).slice(0,2000); } catch(e) { this.debugPayload = String(err.response.data).slice(0,2000);}
        }
      } finally {
        this.isLoading = false;
      }
    },
    async loadMore() {
      if (this.isLoading) return;
      this.page += 1;
      await this.fetchSessions(false);
    },
    formatDate(d) {
      if (!d) return '-';
      try {
        const dt = new Date(d);
        return dt.toLocaleString();
      } catch {
        return String(d);
      }
    },
  },
});
</script>

<style scoped>
.active-sessions {
  margin: 0 auto;
  overflow-x: auto;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
  border-radius: 12px;
  padding: 20px;
  max-width: 1200px;
  width: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e2e8f0;
}

.card-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1a202c;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header h4::before {
  content: '🟢';
  font-size: 16px;
}

.refresh {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: white;
  font-size: 16px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.refresh:hover:not(:disabled) {
  transform: translateY(-2px) rotate(180deg);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.card-body {
  padding-top: 0;
}

.loading {
  color: #718096;
  text-align: center;
  padding: 20px;
  font-style: italic;
}

.error {
  color: #f56565;
  background: #fff5f5;
  padding: 12px;
  border-radius: 8px;
  border-left: 4px solid #f56565;
}

.sessions-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty {
  text-align: center;
  padding: 40px 20px;
  color: #a0aec0;
  font-style: italic;
}

.empty::before {
  content: '😴';
  display: block;
  font-size: 32px;
  margin-bottom: 8px;
}

.session-item {
  padding: 12px 16px;
  border-radius: 10px;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #e2e8f0;
  transition: all 0.2s ease;
}

.session-item:hover {
  transform: translateX(4px);
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}

.user-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-weight: 600;
  color: #2d3748;
  font-size: 14px;
}

.user-meta {
  font-size: 12px;
  color: #718096;
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-meta::before {
  content: '🕐';
  font-size: 12px;
}

.card-footer {
  padding-top: 16px;
  display: flex;
  justify-content: center;
  border-top: 2px solid #e2e8f0;
  margin-top: 16px;
}

.btn.load-more {
  padding: 10px 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn.load-more:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.debug {
  margin: 12px 0 0;
  color: #718096;
  font-size: 11px;
  background: #f7fafc;
  padding: 8px;
  border-radius: 6px;
}

.debug-payload {
  white-space: pre-wrap;
  max-height: 200px;
  overflow: auto;
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 8px;
  margin-top: 6px;
  border-radius: 6px;
  font-size: 11px;
  font-family: 'Courier New', monospace;
}

.debug-extra {
  margin-top: 6px;
  font-size: 11px;
  color: #718096;
}

.debug-items {
  margin-top: 6px;
  font-size: 11px;
  color: #2d3748;
  background: #f7fafc;
  padding: 8px;
  border-radius: 6px;
  max-height: 220px;
  overflow: auto;
}
</style>
