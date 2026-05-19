<template>
  <div class="change-user-role">
    <h2>Cambiar Rol de Usuario</h2>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Email</th>
          <th>Rol Actual</th>
          <th>Acción</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.id }}</td>
          <td>{{ user.email }}</td>
          <td>{{ user.role }}</td>
          <td>
            <button @click="openRoleChangeModal(user)">Cambiar Rol</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="showModal" class="modal">
      <div class="modal-content">
        <h3>Cambiar Rol para {{ selectedUser.email }}</h3>
        <label for="newRole">Nuevo Rol:</label>
        <select v-model="newRole" id="newRole">
          <option value="ADMINISTRADOR">Administrador</option>
          <option value="ALMACENISTA">Almacenista</option>
          <option value="AUXILIAR">Auxiliar</option>
        </select>
        <label for="reason">Razón:</label>
        <textarea v-model="reason" id="reason" placeholder="Escribe la razón del cambio"></textarea>
        <button @click="assignRole">Confirmar</button>
        <button @click="closeModal">Cancelar</button>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      users: [],
      showModal: false,
      selectedUser: null,
      newRole: '',
      reason: '',
    };
  },
  methods: {
    fetchUsers() {
      axios
        .get('/api/sigmav2/admin/users?page=0&size=20&sortBy=createdAt&sortDir=desc')
        .then((response) => {
          this.users = response.data.users;
        })
        .catch((error) => {
          console.error('Error al obtener usuarios:', error);
        });
    },
    openRoleChangeModal(user) {
      this.selectedUser = user;
      this.showModal = true;
    },
    closeModal() {
      this.showModal = false;
      this.selectedUser = null;
      this.newRole = '';
      this.reason = '';
    },
    assignRole() {
      if (!this.newRole || !this.reason) {
        alert('Por favor, completa todos los campos.');
        return;
      }

      const payload = {
        userId: this.selectedUser.id,
        role: this.newRole,
        reason: this.reason,
      };

      axios
        .post('/api/sigmav2/personal-information/assign-role', payload)
        .then((response) => {
          alert(response.data.message);
          this.closeModal();
          this.fetchUsers();
        })
        .catch((error) => {
          console.error('Error al asignar rol:', error);
        });
    },
  },
  mounted() {
    this.fetchUsers();
  },
};
</script>

<style scoped>
.change-user-role {
  padding: 20px;
}

h2 {
  color: #667eea;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
}

table th,
table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

table th {
  background-color: #667eea;
  color: white;
}

button {
  background-color: #667eea;
  color: white;
  border: none;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
}

button:hover {
  background-color: #764ba2;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.modal-content h3 {
  margin-top: 0;
}

.modal-content label {
  display: block;
  margin-top: 10px;
}

.modal-content textarea {
  width: 100%;
  height: 80px;
  margin-top: 5px;
}
</style>
