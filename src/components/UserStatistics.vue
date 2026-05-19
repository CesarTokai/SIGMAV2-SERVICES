<template>
  <div class="user-statistics stat card">
    <div class="stat-grid">
      <div class="stat-item">
        <div class="label">Total</div>
        <div class="value">{{ isLoading ? '...' : stats.totalUsers }}</div>
      </div>

      <div class="stat-item">
        <div class="label">Activos</div>
        <div class="value">{{ isLoading ? '...' : stats.activeUsers }}</div>
      </div>

      <div class="stat-item">
        <div class="label">Inactivos</div>
        <div class="value">{{ isLoading ? '...' : stats.inactiveUsers }}</div>
      </div>

      <div class="stat-item">
        <div class="label">Verificados</div>
        <div class="value">{{ isLoading ? '...' : stats.verifiedUsers }}</div>
      </div>

      <div class="stat-item wide">
        <div class="label">Tasa verificación</div>
        <div class="value">{{ isLoading ? '...' : (stats.verificationRate != null ? stats.verificationRate + '%' : '-') }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import axios from '../config/axiosConfig';
import { ToastError } from '../utils/SweetAlert';

export default defineComponent({
  name: 'UserStatistics',
  data() {
    return {
      stats: {
        totalUsers: 0,
        unverifiedUsers: 0,
        verificationRate: null,
        inactiveUsers: 0,
        activeUsers: 0,
        oldUnverifiedUsers: 0,
        verifiedUsers: 0,
      },
      isLoading: false,
    };
  },
  mounted() {
    this.fetchStats();
  },
  methods: {
    async fetchStats() {
      this.isLoading = true;
      try {
        const res = await axios.doGet('admin/users/statistics');
        const payload = res.data?.data ?? res.data;
        if (payload) {
          this.stats.totalUsers = payload.totalUsers ?? payload.total ?? 0;
          this.stats.unverifiedUsers = payload.unverifiedUsers ?? 0;
          this.stats.verificationRate = payload.verificationRate ?? null;
          this.stats.inactiveUsers = payload.inactiveUsers ?? 0;
          this.stats.activeUsers = payload.activeUsers ?? 0;
          this.stats.oldUnverifiedUsers = payload.oldUnverifiedUsers ?? 0;
          this.stats.verifiedUsers = payload.verifiedUsers ?? 0;
        }
      } catch (err) {
        ToastError('Error', 'No se pudieron cargar las estadísticas de usuarios.');
        // eslint-disable-next-line no-console
        console.error('UserStatistics fetch error:', err);
      } finally {
        this.isLoading = false;
      }
    },
  },
});
</script>

<style scoped>



.stat-item {
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  padding: 9px;
  text-align: center;
  border: 1px solid #e2e8f0;
  transition: all 0.3s ease;
}

.stat-item:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transform: scale(1.05);
  z-index: 1;
}

.stat-item:hover .label {
  color: rgba(255, 255, 255, 0.9);
}

.stat-item:hover .value {
  color: white;
}

.stat-item.wide {
  grid-column: span 2;
}

.label {
  font-size: 11px;
  color: #718096;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  transition: all 0.3s ease;
}

.value {
  font-size: 24px;
  font-weight: 700;
  margin-top: 8px;
  color: #2d3748;
  transition: all 0.3s ease;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: 1fr 1fr;
  }

  .value {
    font-size: 20px;
  }
}
</style>
