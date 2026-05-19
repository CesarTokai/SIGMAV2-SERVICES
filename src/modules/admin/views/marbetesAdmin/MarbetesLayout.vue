<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ConsultaCaptura from './ConsultaCaptura.vue';
import ImpresionMarbetes from './ImpresionMarbetes.vue';
import ConteoMarbetes from './ConteoMarbetes.vue';
import ListadoCompleteMarbetes from './ListadoCompleteMarbetes.vue';
import GestionMarbetes from './GestionMarbetes.vue';

const route = useRoute();
const router = useRouter();

// Submódulos disponibles
const submodules = [
  { key: 'consulta', label: 'Consulta y Captura', icon: '📋', component: ConsultaCaptura },
  { key: 'impresion', label: 'Impresión', icon: '🖨️', component: ImpresionMarbetes },
  { key: 'conteo', label: 'Conteo', icon: '🔢', component: ConteoMarbetes },
  { key: 'gestion', label: 'Reimpresion de Marbetes', icon: '📄', component: GestionMarbetes },
  { key: 'listado', label: 'Listado Completo', icon: '📊', component: ListadoCompleteMarbetes },

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

// Información del submódulo activo
const activeSubmoduleInfo = computed(() => {
  return submodules.find(s => s.key === activeSubmodule.value) || submodules[0];
});
</script>

<template>
  <div class="marbetes-layout">
    <div class="container-fluid">
      <!-- Header Principal -->
      <div class="header-section">
        <div class="title-section">
          <h1 class="title">
            Gestión de Marbetes
          </h1>
        </div>
          <button
              v-for="submodule in submodules"
              :key="submodule.key"
              :class="['submodule-btn', { active: activeSubmodule === submodule.key }]"
              @click="changeSubmodule(submodule.key)"
          >
            <span class="icon">{{ submodule.icon }}</span>
            <span class="label">{{ submodule.label }}</span>
          </button>

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

/* Navegación de Submódulos */

.submodule-btn {
  min-width: 210px;
  max-width: 50px;
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
  .submodules-nav {
    flex-direction: column;
    gap: 10px;
  }

  .submodule-btn {
    width: 100%;
    min-width: unset;
  }
}
</style>
