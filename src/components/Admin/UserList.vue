<template>
  <div class="user-list">
    <b-container fluid>
      <b-row align-h="between" class="header">
        <b-col lg="auto">
          <h1 class="title">Lista de Usuarios</h1>
        </b-col>
      </b-row>

      <b-row v-if="users.length" class="table-container">
        <b-col cols="12">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Email</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Verificado</th>
                <th>Intentos</th>
                <th>Creado</th>
                <th>Actualizado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.email }}</td>
                <td>{{ user.role }}</td>
                <td>
                  <span :class="{'badge-active': user.status, 'badge-inactive': !user.status}">
                    {{ user.status ? 'Activo' : 'Inactivo' }}
                  </span>
                </td>
                <td>{{ user.verified ? 'Sí' : 'No' }}</td>
                <td>{{ user.attempts }}</td>
                <td>{{ user.createdAt }}</td>
                <td>{{ user.updatedAt }}</td>
                <td>
                  <button class="btn btn-sm btn-primary" @click="confirmToggleStatus(user.id, user.status)">Cambiar estado</button>
                  <button class="btn btn-sm btn-danger" @click="confirmDeleteUser(user.id)">Eliminar</button>
                </td>
              </tr>
            </tbody>
          </table>
        </b-col>
      </b-row>
      <p v-else class="no-users">No hay usuarios disponibles.</p>
    </b-container>
  </div>
</template>

<script>
import axiosConfiguration from "../../config/axiosConfig.ts";
import { BContainer, BRow, BCol } from "bootstrap-vue-next";
import Swal from "sweetalert2";

export default {
  components: {
    BContainer,
    BRow,
    BCol,
  },
  data() {
    return {
      users: [],
    };
  },
  methods: {
    async fetchUsers() {
      try {
        const response = await axiosConfiguration.doGet(
          `/admin/users?page=0&size=20&sortBy=createdAt&sortDir=desc`
        );
        this.users = response.data.users;
      } catch (error) {
        console.error("Error al cargar los usuarios:", error);
      }
    },
    async confirmDeleteUser(userId) {
      const result = await Swal.fire({
        title: "¿Estás seguro?",
        text: "No podrás revertir esta acción.",
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#d33",
        cancelButtonColor: "#3085d6",
        confirmButtonText: "Sí, eliminar",
        cancelButtonText: "Cancelar",
      });

      if (result.isConfirmed) {
        this.deleteUser(userId);
      }
    },

    async confirmUpdateUser(userId) {
      const result = await Swal.fire({
        title: "Actualizar Usuario",
        text: "¿Deseas actualizar la información de este usuario?",
        icon: "info",
        showCancelButton: true,
        confirmButtonColor: "#3085d6",
        cancelButtonColor: "#d33",
        confirmButtonText: "Sí, actualizar",
        cancelButtonText: "Cancelar",
      });

      if (result.isConfirmed) {
        this.updateUser(userId);
      }
    },
    async updateUser(userId) {
      try {
        const updatedUser = await axiosConfiguration.doPut(
          `/admin/users/${userId}`,
          { /* datos actualizados */ }
        );
        this.users = this.users.map((user) =>
          user.id === userId ? updatedUser.data : user
        );
        Swal.fire("Actualizado", "El usuario ha sido actualizado.", "success");
      } catch (error) {
        console.error("Error al actualizar el usuario:", error);
        Swal.fire("Error", "No se pudo actualizar el usuario.", "error");
      }
    },
    async confirmToggleStatus(userId, currentStatus) {
      const result = await Swal.fire({
        title: "Cambiar Estado",
        text: `¿Deseas ${currentStatus ? "desactivar" : "activar"} este usuario?`,
        icon: "question",
        showCancelButton: true,
        confirmButtonColor: "#3085d6",
        cancelButtonColor: "#d33",
        confirmButtonText: "Sí, cambiar",
        cancelButtonText: "Cancelar",
      });

      if (result.isConfirmed) {
        this.toggleStatus(userId);
      }
    },
    async toggleStatus(userId) {
      try {
        await axiosConfiguration.doPost(
          `admin/users/${userId}/toggle-status`
        );
        this.users = this.users.map((user) => {
          if (user.id === userId) {
            user.status = !user.status;
          }
          return user;
        });
        Swal.fire("Estado Actualizado", "El estado del usuario ha sido cambiado.", "success");
      } catch (error) {
        console.error("Error al cambiar el estado del usuario:", error);
        Swal.fire("Error", "No se pudo cambiar el estado del usuario.", "error");
      }
    },
  },
  mounted() {
    this.fetchUsers();
  },
};
</script>

<style scoped>
.user-list {
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

.title {
  font-size: 28px;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.title::before {
  content: '👥';
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

.badge-active,
.badge-inactive {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.badge-active {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(72, 187, 120, 0.3);
}

.badge-active::before {
  content: '✓';
  font-size: 14px;
  font-weight: bold;
}

.badge-inactive {
  background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 101, 101, 0.3);
}

.badge-inactive::before {
  content: '✕';
  font-size: 14px;
  font-weight: bold;
}

.btn {
  margin: 4px 2px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-primary {
  background: linear-gradient(135deg, #4299e1 0%, #3182ce 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(66, 153, 225, 0.3);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(66, 153, 225, 0.4);
}

.btn-warning {
  background: linear-gradient(135deg, #ed8936 0%, #dd6b20 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(237, 137, 54, 0.3);
}

.btn-warning:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(237, 137, 54, 0.4);
}

.btn-danger {
  background: linear-gradient(135deg, #f56565 0%, #e53e3e 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(245, 101, 101, 0.3);
}

.btn-danger:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 101, 101, 0.4);
}

.btn:active {
  transform: translateY(0);
}

.no-users {
  text-align: center;
  color: #718096;
  font-size: 16px;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
}

.no-users::before {
  content: '📭';
  display: block;
  font-size: 48px;
  margin-bottom: 16px;
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

  .title {
    font-size: 22px;
  }

  .btn {
    padding: 6px 10px;
    font-size: 11px;
  }
}

@media (max-width: 1200px) {
  .table td:nth-child(6),
  .table th:nth-child(6) {
    display: none;
  }
}
</style>
