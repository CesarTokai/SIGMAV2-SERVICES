<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Swal from 'sweetalert2';
import ConsultaCaptura from './ConsultaCaptura.vue';
import ImpresionMarbetes from './ImpresionMarbetes.vue';
import ConteoMarbetes from './ConteoMarbetes.vue';

const route = useRoute();
const router = useRouter();

// Estado del usuario
const username = ref('');
const userRole = ref('');
const userEmail = ref('');

// Cargar información del usuario desde localStorage
onMounted(() => {
  try {
    username.value = localStorage.getItem('username') || '';
    userRole.value = localStorage.getItem('role') || '';
    userEmail.value = localStorage.getItem('email') || username.value;
  } catch (error) {
    console.error('Error al cargar información del usuario:', error);
  }
});

// Submódulos disponibles para AUXILIAR
const submodules = [
  { key: 'consulta', label: 'Consulta y Captura', icon: '📋', component: ConsultaCaptura },
  { key: 'impresion', label: 'Impresión', icon: '🖨️', component: ImpresionMarbetes },
  { key: 'conteo', label: 'Conteo', icon: '🔢', component: ConteoMarbetes },
];

// Obtener el submódulo activo de la URL o usar 'consulta' por defecto
const activeSubmodule = computed(() => {
  const submodule = route.query.submodulo as string;
  return submodule || 'consulta';
});

// Cambiar submódulo
const changeSubmodule = (key: string) => {
  router.push({
    path: route.path,
    query: { submodulo: key }
  });
};

// Componente activo
const activeComponent = computed(() => {
  const submodule = submodules.find(s => s.key === activeSubmodule.value);
  return submodule?.component || ConsultaCaptura;
});

// Función de logout
const logout = async () => {
  const confirmed = await Swal.fire({
    icon: 'warning',
    title: '¿Cerrar sesión?',
    text: '¿Estás seguro de que deseas cerrar sesión?',
    showCancelButton: true,
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Sí, cerrar sesión',
    cancelButtonText: 'Cancelar'
  });

  if (confirmed.isConfirmed) {
    // Limpiar localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('email');
    localStorage.removeItem('role');
    localStorage.removeItem('userId');

    // Redirigir al login
    router.push('/login');
  }
};
</script>

<template>
  <div class="marbetes-layout">
    <div class="container-fluid">
      <!-- Header Principal -->
      <div class="header-section">
        <div class="title-section">
          <h1 class="title">Gestión de Marbetes</h1>
          <div class="user-info">
            <span class="user-name">👤 {{ username || 'Usuario' }}</span>
            <span class="user-role">🔐 {{ userRole || 'Sin rol' }}</span>
          </div>
        </div>
        <div class="buttons-section">
          <button
              v-for="submodule in submodules"
              :key="submodule.key"
              :class="['submodule-btn', { active: activeSubmodule === submodule.key }]"
              @click="changeSubmodule(submodule.key)"
          >
            <span class="icon">{{ submodule.icon }}</span>
            <span class="label">{{ submodule.label }}</span>
          </button>
          <button class="logout-btn" @click="logout" title="Cerrar sesión">
            <span class="icon">🚪</span>
            <span class="label">Salir</span>
          </button>
        </div>
      </div>

      <div class="submodule-content">
        <component :is="activeComponent" />
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Header */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.title-section {
  flex: 1;
}

.title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title .icon {
  font-size: 32px;
}

.user-info {
  display: flex;
  gap: 16px;
  margin-top: 6px;
  font-size: 13px;
  color: #666;
}

.user-name,
.user-role {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #f5f5f5;
  border-radius: 6px;
  font-weight: 500;
}

/* Contenedor de botones */
.buttons-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Navegación de Submódulos */
.submodule-btn {
  min-width: 180px;
  max-width: 200px;
  width: auto;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 500;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  color: #495057;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s, background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.submodule-btn:hover {
  box-shadow: 0 2px 8px rgba(40,167,69,0.08);
  border-color: #20c997;
  background: #f6fefb;
}

.submodule-btn.active {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: #fff;
  border-color: #20c997;
  box-shadow: 0 4px 12px rgba(40,167,69,0.15);
}

.submodule-btn .icon {
  font-size: 16px;
}

.submodule-btn .label {
  font-weight: 500;
}

/* Botón de Logout */
.logout-btn {
  min-width: 100px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 500;
  border: 1.5px solid #dc3545;
  border-radius: 8px;
  background: #fff;
  color: #dc3545;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s, background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.logout-btn:hover {
  box-shadow: 0 2px 8px rgba(220,53,69,0.15);
  border-color: #bd2130;
  background: #fff5f5;
}

.logout-btn:active {
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
  color: #fff;
  border-color: #c82333;
  box-shadow: 0 4px 12px rgba(220,53,69,0.2);
}

.logout-btn .icon {
  font-size: 16px;
}

.logout-btn .label {
  font-weight: 500;
}

/* Contenido del Submódulo */
.submodule-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
    gap: 10px;
  }

  .buttons-section {
    width: 100%;
    flex-wrap: wrap;
    gap: 8px;
  }

  .submodule-btn,
  .logout-btn {
    flex: 1;
    min-width: 120px;
  }
}
</style>

