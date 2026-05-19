<template>
  <div class="periodo-selector-wrapper">
    <div class="periodo-label-import-group">
      <label :for="inputId" class="form-label periodo-label-minimal">
        <strong>{{ label }}</strong>
      </label>
      <select
        :id="inputId"
        :value="modelValue"
        @change="handleChange"
        class="form-select periodo-select-small"
        :disabled="disabled"
      >
        <option :value="null" disabled>{{ placeholder }}</option>
        <option
          v-for="periodo in periodos"
          :key="periodo.id"
          :value="periodo.id"
        >
          {{ formatDate(periodo.date) }} - {{ periodo.comments }}
        </option>
      </select>
    </div>

    <!-- Info adicional del período seleccionado (opcional) -->
    <div v-if="showInfo && selectedPeriodo" class="periodo-info">
      <div class="info-item">
        <span class="label">Estado:</span>
        <span :class="['badge', getEstadoClass(selectedPeriodo.state)]">
          {{ selectedPeriodo.state }}
        </span>
      </div>
      <div v-if="showTotalCount && totalCount !== null" class="info-item">
        <span class="label">{{ totalCountLabel }}:</span>
        <span class="value">{{ totalCount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface Props {
  modelValue: number | null;
  periodos: Periodo[];
  label?: string;
  placeholder?: string;
  inputId?: string;
  disabled?: boolean;
  showInfo?: boolean;
  showTotalCount?: boolean;
  totalCount?: number | null;
  totalCountLabel?: string;
}

const props = withDefaults(defineProps<Props>(), {
  label: 'Período',
  placeholder: 'Selecciona un período',
  inputId: 'periodoSelect',
  disabled: false,
  showInfo: true,
  showTotalCount: false,
  totalCount: null,
  totalCountLabel: 'Total'
});

const emit = defineEmits<{
  'update:modelValue': [value: number | null];
  'change': [periodo: Periodo | null];
}>();

// Computed para obtener el período seleccionado
const selectedPeriodo = computed(() => {
  if (!props.modelValue) return null;
  return props.periodos.find(p => p.id === props.modelValue) || null;
});

// Manejar cambio de selección
const handleChange = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  const value = target.value === 'null' || target.value === '' ? null : Number(target.value);

  emit('update:modelValue', value);

  const periodo = value ? props.periodos.find(p => p.id === value) || null : null;
  emit('change', periodo);
};

// Formatear fecha (robusto, sin desfase)
const formatDate = (date: string): string => {
  if (!date) return 'N/A';
  // Si es solo fecha (YYYY-MM-DD), crear fecha local (no UTC)
  if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    const [y, m, d] = date.split('-');
    const dateObj = new Date(Number(y), Number(m) - 1, Number(d));
    return dateObj.toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
  // Si es ISO, usar toLocaleDateString
  if (/^\d{4}-\d{2}-\d{2}T/.test(date)) {
    const dateObj = new Date(date);
    return dateObj.toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
  // Si es timestamp u otro formato, intentar parsear
  try {
    const dateObj = new Date(date);
    return dateObj.toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  } catch {
    return date;
  }
};

// Formatear estado
const getEstadoClass = (estado: string): string => {
  const estadoLower = estado?.toLowerCase();
  if (estadoLower?.includes('activo') || estadoLower?.includes('disponible')) {
    return 'badge-success';
  } else if (estadoLower?.includes('inactivo') || estadoLower?.includes('no disponible')) {
    return 'badge-danger';
  } else if (estadoLower?.includes('pendiente')) {
    return 'badge-warning';
  }
  return 'badge-secondary';
};
</script>

<style scoped>
.periodo-selector-wrapper {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.periodo-label-import-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.periodo-label-minimal {
  min-width: unset;
  width: auto;
  margin-bottom: 0;
  font-size: 15px;
  padding-right: 6px;
}

.form-label {
  display: block;
  margin-bottom: 0;
  font-weight: 600;
  color: #495057;
}

.form-select {
  width: 100%;
  padding: 10px 15px;
  font-size: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background-color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.form-select:focus {
  outline: none;
  border-color: #20c997;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.form-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: #f5f5f5;
}

.periodo-select-small {
  width: 180px !important;
  min-width: 120px;
  max-width: 220px;
}

.periodo-info {
  display: flex;
  gap: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  font-weight: 600;
  color: #6c757d;
  font-size: 14px;
}

.info-item .value {
  font-size: 18px;
  font-weight: bold;
  color: #007bff;
}

.badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  text-transform: capitalize;
}

.badge-success {
  background-color: #d4edda;
  color: #155724;
}

.badge-danger {
  background-color: #f8d7da;
  color: #721c24;
}

.badge-warning {
  background-color: #fff3cd;
  color: #856404;
}

.badge-secondary {
  background-color: #e2e3e5;
  color: #383d41;
}

/* Responsive */
@media (max-width: 768px) {
  .periodo-selector-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .periodo-info {
    width: 100%;
  }
}
</style>
