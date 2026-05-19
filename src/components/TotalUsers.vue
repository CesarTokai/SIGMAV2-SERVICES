<template>
  <div class="total-users stat card">
    <div class="stat-left">
      <div class="stat-icon">👥</div>
    </div>
    <div class="stat-body">
      <div class="stat-title">Total usuarios</div>
      <div class="stat-value">{{ isLoading ? '...' : total }}</div>

    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import axios from '../config/axiosConfig';
import { ToastError } from '../utils/SweetAlert';

export default defineComponent({
  name: 'TotalUsers',
  data() {
    return {
      total: 0,
      active: 0,
      isLoading: false,
      error: '',
    };
  },
  mounted() {
    this.fetchTotals();
  },
  methods: {
    async fetchTotals() {
      this.isLoading = true;
      this.error = '';
      try {
        const url = 'admin/users?page=0&size=20&sortBy=createdAt&sortDir=desc';
        const res = await axios.doGet(url);
        const payload = res.data?.data ?? res.data;
        // payload may contain totalElements and totalActiveUsers
        this.total = payload?.totalElements ?? payload?.total ?? 0;
        this.active = payload?.totalActiveUsers ?? payload?.totalActive ?? 0;
      } catch (err) {
        this.error = 'No se pudo cargar el total de usuarios.';
        ToastError('Error', this.error);
        // eslint-disable-next-line no-console
        console.error('TotalUsers fetch error:', err);
      } finally {
        this.isLoading = false;
      }
    },
  },
});
</script>

<style scoped>
.total-users {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  cursor: pointer;
  margin: 0 auto;
  text-align: center;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.total-users:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
  justify-content: center;
  padding: 0;
  width: 100%;
}

.stat-left {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.stat-icon {
  font-size: 28px;
}

.stat-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-value {
  font-size: 40px;
  font-weight: 700;
  color: white;
  line-height: 1;
}

.stat-title {
  color: rgba(255, 255, 255, 0.9);
  font-size: 15px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
</style>

