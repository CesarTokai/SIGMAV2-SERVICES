<template>
  <div class="recovery-requests">
    <b-container fluid>
      <b-row align-h="between" class="header">
        <b-col lg="auto">
        </b-col>
      </b-row>

      <b-row v-if="requests.pending.length" class="table-container">
        <b-col cols="12">
          <table class="table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Fecha</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in requests.pending" :key="request.requestId">
                <td>{{ request.username }}</td>
                <td>{{ request.role }}</td>
                <td>{{ request.date }}</td>
                <td>
                  <span class="status-pill pending">Pendiente</span>
                </td>
                <td>
                  <button class="btn-action success" @click="resolveRequest(request.requestId)">Aceptar</button>
                  <button class="btn-action danger" @click="rejectRequest(request.requestId)">Rechazar</button>
                </td>
              </tr>
            </tbody>
          </table>
        </b-col>
      </b-row>
      <p v-else class="no-requests">No hay solicitudes pendientes.</p>

      <b-row v-if="requests.rejected.length" class="table-container">
        <b-col cols="12">
          <table class="table">
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Rol</th>
                <th>Fecha</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in requests.rejected" :key="request.requestId">
                <td>{{ request.username }}</td>
                <td>{{ request.role }}</td>
                <td>{{ request.date }}</td>
                <td>
                  <span class="status-pill rejected">Rechazada</span>
                </td>
              </tr>
            </tbody>
          </table>
        </b-col>
      </b-row>
      <p v-else class="no-requests">No hay solicitudes rechazadas.</p>
    </b-container>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import { ToastWarning, ToastSuccess, VoidAlert } from "@/utils/SweetAlert";
import axiosConfiguration from "@/config/axiosConfig";
import { BContainer, BRow, BCol } from 'bootstrap-vue-next';

export default defineComponent({
  name: 'RecoveryRequests',
  components: {
    BContainer,
    BRow,
    BCol,
  },
  data() {
    return {
      requests: {
        pending: [],
        accepted: [],
        rejected: [],
      },
    };
  },
  methods: {
    async getRequests() {
      try {
        const response = await axiosConfiguration.doGet(`/auth/getPage`);
        const allRequests = response.data.content || [];

        // Ensure status matching is case-insensitive
        this.requests.pending = allRequests.filter((req) => (req.status || '').toString().toUpperCase() === "PENDING");

        // Filtrar solicitudes aceptadas y rechazadas
        this.requests.accepted = allRequests.filter((req) => (req.status || '').toString().toUpperCase() === "ACCEPTED");
        this.requests.rejected = allRequests.filter((req) => (req.status || '').toString().toUpperCase() === "REJECTED");
      } catch (error) {
        ToastWarning("Error", "No se pudo cargar la lista de solicitudes.");
      }
      // eslint-disable-next-line no-console
      console.log(this.requests);
    },

    async resolveRequest(requestId) {
      VoidAlert(
        "question",
        "¿Está seguro?",
        "Esta acción aceptará la solicitud de recuperación de contraseña.",
        "Sí, aceptar",
        async () => {
          try {
            const response = await axiosConfiguration.doPost(
              "/auth/resolveRequest",
              { requestId }
            );
            if (response && response.status === 200) {
              ToastSuccess("Éxito", "Solicitud aceptada.");
              this.getRequests();
            }
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
          }
        }
      );
    },
    async rejectRequest(requestId) {
      VoidAlert(
        "question",
        "¿Está seguro?",
        "Esta acción rechazará la solicitud de recuperación de contraseña.",
        "Sí, rechazar",
        async () => {
          try {
            const response = await axiosConfiguration.doPost(
              "/auth/rejectRequest",
              { requestId }
            );
            if (response && response.status === 200) {
              ToastSuccess("Éxito", "Solicitud rechazada.");
              this.getRequests();
            }
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error(error);
          }
        }
      );
    },
  },
  mounted() {
    this.getRequests();
  },
});
</script>

<style scoped>
.recovery-requests {
  padding: 0;
  background-color: transparent;
  border-radius: 0;
  box-shadow: none;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.header {
  margin-bottom: 24px;
  padding: 0;
}

.header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.header h1::before {
  content: '🔑';
  font-size: 32px;
}

.table-container {
  overflow-x: auto;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
}

.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background-color: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.table thead {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.table th {
  padding: 16px 12px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border: none;
}

.table tbody tr {
  transition: all 0.2s ease;
  border-bottom: 1px solid #e2e8f0;
}

.table tbody tr:hover {
  background-color: #f7fafc;
  transform: scale(1.01);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.table tbody tr:last-child {
  border-bottom: none;
}

.table td {
  padding: 16px 12px;
  text-align: left;
  color: #2d3748;
  font-size: 14px;
  vertical-align: middle;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: capitalize;
  letter-spacing: 0.3px;
}

.status-pill.pending {
  background: linear-gradient(135deg, #ed8936 0%, #dd6b20 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(237, 137, 54, 0.3);
}

.status-pill.pending::before {
  content: '⏳';
  font-size: 14px;
}

.status-pill.rejected {
  background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 101, 101, 0.3);
}

.status-pill.rejected::before {
  content: '❌';
  font-size: 14px;
}

.no-requests {
  text-align: center;
  color: #718096;
  font-size: 16px;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
}

.no-requests::before {
  content: '✅';
  display: block;
  font-size: 48px;
  margin-bottom: 16px;
}

.btn-action {
  margin: 4px 2px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-action:hover {
  transform: translateY(-2px);
}

.btn-action:active {
  transform: translateY(0);
}

.btn-action.success {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(72, 187, 120, 0.3);
}

.btn-action.success:hover {
  box-shadow: 0 4px 12px rgba(72, 187, 120, 0.4);
}

.btn-action.danger {
  background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 101, 101, 0.3);
}

.btn-action.danger:hover {
  box-shadow: 0 4px 12px rgba(245, 101, 101, 0.4);
}

/* Responsiveness */
@media (max-width: 768px) {
  .table-container {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }

  .table th,
  .table td {
    font-size: 12px;
    padding: 10px 8px;
  }

  .header h1 {
    font-size: 22px;
  }

  .btn-action {
    font-size: 11px;
    padding: 6px 10px;
  }
}
</style>