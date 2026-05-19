<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import TooltipHelp from '@/components/TooltipHelp.vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, ToastSuccess, LoadAlert } from '@/utils/SweetAlert';
import Swal from 'sweetalert2';

// ============================================
// Interfaces
// ============================================
interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface Almacen {
  id: number;
  clave: string;
  almacenname: string;
  activo: boolean;
}

interface MarbeteInfo {
  folio: number;
  productId: number;
  claveProducto: string;
  nombreProducto: string;
  claveAlmacen: string;
  nombreAlmacen: string;
  estado: string;
  existencias: number;
  c1Value: number | null;
  c2Value: number | null;
  c1Date: string | null;
  c2Date: string | null;
  c1User: string | null;
  c2User: string | null;
}

// ============================================
// Props
// ============================================
interface Props {
  periodos: Periodo[];
  almacenes: Almacen[];
  selectedPeriodoId?: number | null;
  selectedAlmacenId?: number | null;
}

const props = withDefaults(defineProps<Props>(), {
  selectedPeriodoId: null,
  selectedAlmacenId: null
});

// ============================================
// Estado
// ============================================
const folio = ref<string>('');
const countValue = ref<string>('');
const marbeteInfo = ref<MarbeteInfo | null>(null);
const searchingFolio = ref(false);

// Loading states
const loadingStates = ref({
  searching: false,
  registeringC1: false,
  registeringC2: false
});

// ============================================
// FASE 3 - Manejo de errores específicos
// ============================================
const handleAPIError = (error: any, contexto: string = 'operación'): string => {
  const errorMessages: Record<string, string> = {
    'LABEL_NOT_FOUND': 'El folio no fue encontrado en el sistema.',
    'INVALID_LABEL_STATE': 'El marbete no está en estado válido para registrar conteos.',
    'LABEL_CANCELLED': 'No se puede registrar conteo: el folio está CANCELADO.',
    'LABEL_NOT_PRINTED': 'El marbete debe estar IMPRESO para registrar conteos.',
    'DUPLICATE_COUNT_C1': 'El conteo C1 ya fue registrado para este folio.',
    'DUPLICATE_COUNT_C2': 'El conteo C2 ya fue registrado para este folio.',
    'C1_REQUIRED': 'Debe registrar C1 antes de registrar C2.',
    'C2_ALREADY_EXISTS': 'No se puede registrar C1 porque ya existe C2. La secuencia está rota.',
    'PERIOD_CLOSED': 'El período está cerrado. No se pueden registrar conteos.',
    'WRONG_PERIOD': 'El folio pertenece a un período diferente.',
    'WRONG_WAREHOUSE': 'El folio pertenece a otro almacén.',
    'INVALID_COUNT_VALUE': 'El valor del conteo debe ser un número válido mayor o igual a cero.'
  };

  let mensaje = `Error al realizar ${contexto}`;

  if (error?.response?.data) {
    const errorData = error.response.data;
    const errorCode = errorData.code || errorData.error || errorData.type;

    if (errorCode && errorMessages[errorCode]) {
      mensaje = errorMessages[errorCode];
    } else if (errorData.message) {
      mensaje = errorData.message;
    } else if (typeof errorData === 'string') {
      mensaje = errorData;
    }
  } else if (error?.message) {
    mensaje = error.message;
  }

  console.error(`❌ ${contexto}:`, error);
  return mensaje;
};

// ============================================
// FASE 3 - Validación de folio
// ============================================
const validateFolioForCount = async (): Promise<boolean> => {
  if (!folio.value) {
    ToastError('Error', 'Debe ingresar un folio');
    return false;
  }

  const folioNum = parseInt(folio.value);
  if (isNaN(folioNum) || folioNum <= 0) {
    ToastError('Error', 'El folio debe ser un número válido mayor a 0');
    return false;
  }

  if (!props.selectedPeriodoId || !props.selectedAlmacenId) {
    ToastError('Error', 'Debe seleccionar un período y almacén');
    return false;
  }

  return true;
};

// ============================================
// FASE 3 - Buscar información del folio
// ============================================
const searchFolio = async () => {
  if (!await validateFolioForCount()) {
    return;
  }

  loadingStates.value.searching = true;
  searchingFolio.value = true;

  try {
    const response = await axiosConfiguration.doGet(`/labels/folio/${folio.value}`, {
      periodId: props.selectedPeriodoId,
      warehouseId: props.selectedAlmacenId
    });

    marbeteInfo.value = {
      folio: response.data.folio || parseInt(folio.value),
      productId: response.data.productId || 0,
      claveProducto: response.data.productCode || response.data.claveProducto || '',
      nombreProducto: response.data.productName || response.data.nombreProducto || '',
      claveAlmacen: response.data.warehouseKey || response.data.claveAlmacen || '',
      nombreAlmacen: response.data.warehouseName || response.data.nombreAlmacen || '',
      estado: response.data.status || response.data.estado || '',
      existencias: response.data.stock || response.data.existencias || 0,
      c1Value: response.data.c1Value ?? response.data.countC1 ?? null,
      c2Value: response.data.c2Value ?? response.data.countC2 ?? null,
      c1Date: response.data.c1Date ?? response.data.c1Timestamp ?? null,
      c2Date: response.data.c2Date ?? response.data.c2Timestamp ?? null,
      c1User: response.data.c1User ?? null,
      c2User: response.data.c2User ?? null
    };

    console.log('📋 Marbete encontrado:', marbeteInfo.value);

    // Validaciones adicionales
    if (marbeteInfo.value.estado === 'CANCELADO') {
      ToastError('Advertencia', '⚠️ Este folio está CANCELADO. No se pueden registrar conteos.');
    } else if (marbeteInfo.value.estado !== 'IMPRESO') {
      ToastError('Advertencia', `⚠️ Este folio está en estado "${marbeteInfo.value.estado}". Debe estar IMPRESO para registrar conteos.`);
    }

  } catch (error) {
    const errorMessage = handleAPIError(error, 'buscar folio');
    ToastError('Error', errorMessage);
    marbeteInfo.value = null;
  } finally {
    loadingStates.value.searching = false;
    searchingFolio.value = false;
  }
};

// ============================================
// FASE 3 - Validaciones previas para registro
// ============================================
const canRegisterC1 = computed(() => {
  if (!marbeteInfo.value) return false;
  if (marbeteInfo.value.estado !== 'IMPRESO') return false;
  if (marbeteInfo.value.c1Value !== null) return false; // Ya tiene C1
  return true;
});

const canRegisterC2 = computed(() => {
  if (!marbeteInfo.value) return false;
  if (marbeteInfo.value.estado !== 'IMPRESO') return false;
  if (marbeteInfo.value.c1Value === null) return false; // Necesita C1 primero
  if (marbeteInfo.value.c2Value !== null) return false; // Ya tiene C2
  return true;
});

// ============================================
// FASE 3 - Registrar conteos
// ============================================
const registerC1 = async () => {
  // Validaciones
  if (!countValue.value) {
    ToastError('Error', 'Debe ingresar una cantidad');
    return;
  }

  const value = parseFloat(countValue.value);
  if (isNaN(value) || value < 0) {
    ToastError('Error', 'La cantidad debe ser un número válido mayor o igual a 0');
    return;
  }

  if (!canRegisterC1.value) {
    ToastError('Error', 'No se puede registrar C1 en este momento');
    return;
  }

  // Confirmación
  const result = await Swal.fire({
    title: '📝 Registrar Conteo C1',
    html: `
      <div style="text-align: left;">
        <p><strong>Folio:</strong> ${marbeteInfo.value!.folio}</p>
        <p><strong>Producto:</strong> ${marbeteInfo.value!.nombreProducto}</p>
        <p><strong>Existencias sistema:</strong> ${marbeteInfo.value!.existencias}</p>
        <hr>
        <p style="font-size: 18px;"><strong>Cantidad contada (C1):</strong> <span style="color: #007bff;">${value}</span></p>
      </div>
    `,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, registrar C1',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#007bff',
    cancelButtonColor: '#6c757d'
  });

  if (!result.isConfirmed) return;

  loadingStates.value.registeringC1 = true;
  LoadAlert(true);

  try {
    await axiosConfiguration.doPost('/labels/count/c1', {
      folio: marbeteInfo.value!.folio,
      countedValue: value,
      periodId: props.selectedPeriodoId,
      warehouseId: props.selectedAlmacenId
    });

    LoadAlert(false);
    ToastSuccess('Éxito', `✅ Conteo C1 registrado: ${value}`);

    // Limpiar y recargar
    countValue.value = '';
    await searchFolio();

  } catch (error) {
    LoadAlert(false);
    const errorMessage = handleAPIError(error, 'registrar C1');

    await Swal.fire({
      icon: 'error',
      title: 'Error al registrar C1',
      html: `<div style="text-align: left; white-space: pre-line;">${errorMessage}</div>`,
      confirmButtonColor: '#dc3545'
    });
  } finally {
    loadingStates.value.registeringC1 = false;
  }
};

const registerC2 = async () => {
  // Validaciones
  if (!countValue.value) {
    ToastError('Error', 'Debe ingresar una cantidad');
    return;
  }

  const value = parseFloat(countValue.value);
  if (isNaN(value) || value < 0) {
    ToastError('Error', 'La cantidad debe ser un número válido mayor o igual a 0');
    return;
  }

  if (!canRegisterC2.value) {
    ToastError('Error', 'No se puede registrar C2 en este momento');
    return;
  }

  // Verificar diferencia con C1
  const diferencia = Math.abs(value - (marbeteInfo.value!.c1Value || 0));
  const showWarning = diferencia > 0;

  // Confirmación con advertencia si hay diferencia
  const result = await Swal.fire({
    title: showWarning ? '⚠️ Registrar Conteo C2' : '📝 Registrar Conteo C2',
    html: `
      <div style="text-align: left;">
        <p><strong>Folio:</strong> ${marbeteInfo.value!.folio}</p>
        <p><strong>Producto:</strong> ${marbeteInfo.value!.nombreProducto}</p>
        <p><strong>Existencias sistema:</strong> ${marbeteInfo.value!.existencias}</p>
        <hr>
        <p><strong>Conteo C1:</strong> ${marbeteInfo.value!.c1Value}</p>
        <p style="font-size: 18px;"><strong>Conteo C2:</strong> <span style="color: #28a745;">${value}</span></p>
        ${showWarning ? `
          <hr>
          <p style="color: #ff6b6b; font-weight: bold;">
            ⚠️ Diferencia entre C1 y C2: ${diferencia}
          </p>
        ` : ''}
      </div>
    `,
    icon: showWarning ? 'warning' : 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, registrar C2',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#28a745',
    cancelButtonColor: '#6c757d'
  });

  if (!result.isConfirmed) return;

  loadingStates.value.registeringC2 = true;
  LoadAlert(true);

  try {
    await axiosConfiguration.doPost('/api/sigmav2/labels/counts/c2', {
      folio: marbeteInfo.value!.folio,
      countedValue: value,
      periodId: props.selectedPeriodoId,
      warehouseId: props.selectedAlmacenId
    });

    LoadAlert(false);

    await Swal.fire({
      icon: 'success',
      title: '✅ Conteo C2 Registrado',
      html: `
        <div style="text-align: left;">
          <p>✅ <strong>Folio:</strong> ${marbeteInfo.value!.folio}</p>
          <p>✅ <strong>C1:</strong> ${marbeteInfo.value!.c1Value}</p>
          <p>✅ <strong>C2:</strong> ${value}</p>
          ${showWarning ? `<p style="color: #ff6b6b;">⚠️ <strong>Diferencia:</strong> ${diferencia}</p>` : ''}
        </div>
      `,
      confirmButtonColor: '#28a745'
    });

    // Limpiar formulario
    folio.value = '';
    countValue.value = '';
    marbeteInfo.value = null;

  } catch (error) {
    LoadAlert(false);
    const errorMessage = handleAPIError(error, 'registrar C2');

    await Swal.fire({
      icon: 'error',
      title: 'Error al registrar C2',
      html: `<div style="text-align: left; white-space: pre-line;">${errorMessage}</div>`,
      confirmButtonColor: '#dc3545'
    });
  } finally {
    loadingStates.value.registeringC2 = false;
  }
};

// ============================================
// Watchers
// ============================================
watch([() => props.selectedPeriodoId, () => props.selectedAlmacenId], () => {
  // Limpiar cuando cambian los filtros
  folio.value = '';
  countValue.value = '';
  marbeteInfo.value = null;
});

// ============================================
// Formateo
// ============================================
const formatDate = (date: string | null): string => {
  if (!date) return '-';
  return new Date(date).toLocaleString('es-ES', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};
</script>

<template>
  <div class="registro-conteos">
    <!-- Header -->
    <div class="section-card">
      <div class="title-section">
        <h1 class="section-title">📝 Registro de Conteos</h1>
        <p class="subtitle">Capture los conteos C1 y C2 de los marbetes</p>
      </div>

      <!-- Mensaje de ayuda -->
      <div class="help-message help-message-info">
        <span class="help-icon">💡</span>
        <span class="help-text">
          <strong>Instrucciones:</strong> Ingrese el folio del marbete, verifique la información y registre el conteo C1.
          Después del C1, podrá registrar el C2.
        </span>
      </div>
    </div>

    <!-- Formulario -->
    <div class="section-card form-section">
      <div class="form-row">
        <!-- Input Folio -->
        <div class="form-group">
          <label for="folioInput">
            Folio del Marbete
            <TooltipHelp text="Ingrese el número de folio del marbete a contar. Presione Enter o Tab para buscar." />
          </label>
          <div class="input-with-button">
            <input
              id="folioInput"
              v-model="folio"
              type="number"
              class="form-control"
              placeholder="Ej: 1001"
              min="1"
              :disabled="loadingStates.searching"
              @keyup.enter="searchFolio"
            />
            <button
              class="btn btn-primary"
              @click="searchFolio"
              :disabled="loadingStates.searching || !folio"
            >
              <span v-if="loadingStates.searching">🔍 Buscando...</span>
              <span v-else>🔍 Buscar</span>
            </button>
          </div>
        </div>

        <!-- Input Cantidad -->
        <div class="form-group">
          <label for="countInput">
            Cantidad Contada
            <TooltipHelp text="Ingrese la cantidad física contada del producto." />
          </label>
          <input
            id="countInput"
            v-model="countValue"
            type="number"
            class="form-control"
            placeholder="Ej: 50"
            min="0"
            step="0.01"
            :disabled="!marbeteInfo || marbeteInfo.estado !== 'IMPRESO'"
          />
        </div>
      </div>

      <!-- Información del marbete -->
      <transition name="fade">
        <div v-if="marbeteInfo" class="marbete-info-card">
          <div class="info-header">
            <h3>📋 Información del Marbete</h3>
            <span :class="['estado-badge', `estado-${marbeteInfo.estado.toLowerCase()}`]">
              {{ marbeteInfo.estado }}
            </span>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">Folio:</span>
              <span class="info-value">{{ marbeteInfo.folio }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Producto:</span>
              <span class="info-value">{{ marbeteInfo.nombreProducto }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Clave:</span>
              <span class="info-value">{{ marbeteInfo.claveProducto }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Existencias Sistema:</span>
              <span class="info-value highlight">{{ marbeteInfo.existencias }}</span>
            </div>
          </div>

          <!-- Conteos registrados -->
          <div v-if="marbeteInfo.c1Value !== null || marbeteInfo.c2Value !== null" class="conteos-registrados">
            <h4>📊 Conteos Registrados</h4>
            <div class="conteos-grid">
              <div v-if="marbeteInfo.c1Value !== null" class="conteo-item conteo-c1">
                <div class="conteo-label">
                  <span class="conteo-icon">1️⃣</span>
                  <span>Conteo C1</span>
                </div>
                <div class="conteo-value">{{ marbeteInfo.c1Value }}</div>
                <div class="conteo-meta">
                  <small>{{ formatDate(marbeteInfo.c1Date) }}</small>
                  <small v-if="marbeteInfo.c1User">Por: {{ marbeteInfo.c1User }}</small>
                </div>
              </div>

              <div v-if="marbeteInfo.c2Value !== null" class="conteo-item conteo-c2">
                <div class="conteo-label">
                  <span class="conteo-icon">2️⃣</span>
                  <span>Conteo C2</span>
                </div>
                <div class="conteo-value">{{ marbeteInfo.c2Value }}</div>
                <div class="conteo-meta">
                  <small>{{ formatDate(marbeteInfo.c2Date) }}</small>
                  <small v-if="marbeteInfo.c2User">Por: {{ marbeteInfo.c2User }}</small>
                </div>
              </div>

              <!-- Diferencia -->
              <div v-if="marbeteInfo.c1Value !== null && marbeteInfo.c2Value !== null" class="conteo-diferencia">
                <div class="diferencia-label">Diferencia C1-C2:</div>
                <div :class="['diferencia-value', { 'sin-diferencia': marbeteInfo.c1Value === marbeteInfo.c2Value }]">
                  {{ Math.abs(marbeteInfo.c1Value - marbeteInfo.c2Value) }}
                </div>
              </div>
            </div>
          </div>

          <!-- Botones de acción -->
          <div class="action-buttons">
            <button
              class="btn btn-primary btn-c1"
              @click="registerC1"
              :disabled="!canRegisterC1 || loadingStates.registeringC1 || !countValue"
            >
              <span v-if="loadingStates.registeringC1">Registrando...</span>
              <span v-else>1️⃣ Registrar C1</span>
            </button>

            <button
              class="btn btn-success btn-c2"
              @click="registerC2"
              :disabled="!canRegisterC2 || loadingStates.registeringC2 || !countValue"
            >
              <span v-if="loadingStates.registeringC2">Registrando...</span>
              <span v-else>2️⃣ Registrar C2</span>
            </button>
          </div>

          <!-- Advertencias -->
          <div v-if="marbeteInfo.estado === 'CANCELADO'" class="alert alert-danger">
            ❌ Este marbete está CANCELADO. No se pueden registrar conteos.
          </div>
          <div v-else-if="marbeteInfo.estado !== 'IMPRESO'" class="alert alert-warning">
            ⚠️ Este marbete debe estar en estado IMPRESO para registrar conteos. Estado actual: {{ marbeteInfo.estado }}
          </div>
          <div v-else-if="marbeteInfo.c1Value !== null && marbeteInfo.c2Value !== null" class="alert alert-success">
            ✅ Ambos conteos (C1 y C2) ya están registrados para este folio.
          </div>
        </div>
      </transition>

      <!-- Mensaje sin folio -->
      <div v-if="!marbeteInfo && !loadingStates.searching" class="empty-state">
        <div class="empty-icon">🔍</div>
        <p>Ingrese un folio para comenzar a registrar conteos</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.registro-conteos {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px;
}

.section-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.title-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #6c757d;
  margin: 0;
  font-size: 14px;
}

/* Mensaje de ayuda */
.help-message {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 8px;
  margin-top: 16px;
  animation: slideInRight 0.5s ease;
}

.help-message-info {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.help-icon {
  font-size: 20px;
  animation: pulse 2s infinite;
}

.help-text {
  line-height: 1.5;
  font-size: 14px;
}

.help-text strong {
  font-weight: 600;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* Formulario */
.form-section {
  margin-top: 0;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 600;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-control {
  padding: 12px 16px;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
}

.form-control:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-control:disabled {
  background: #f8f9fa;
  cursor: not-allowed;
}

.input-with-button {
  display: flex;
  gap: 12px;
}

.input-with-button .form-control {
  flex: 1;
}

.input-with-button .btn {
  min-width: 140px;
}

/* Botones */
.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.btn-success {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.btn-success:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(40, 167, 69, 0.4);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Información del marbete */
.marbete-info-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  padding: 24px;
  margin-top: 24px;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #dee2e6;
}

.info-header h3 {
  margin: 0;
  font-size: 20px;
  color: #2c3e50;
}

.estado-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.estado-impreso {
  background: #d1e7dd;
  color: #0f5132;
}

.estado-cancelado {
  background: #f8d7da;
  color: #842029;
}

.estado-generado {
  background: #cfe2ff;
  color: #084298;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 13px;
  color: #6c757d;
  font-weight: 500;
}

.info-value {
  font-size: 16px;
  color: #2c3e50;
  font-weight: 600;
}

.info-value.highlight {
  color: #667eea;
  font-size: 20px;
}

/* Conteos registrados */
.conteos-registrados {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 2px solid #dee2e6;
}

.conteos-registrados h4 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #2c3e50;
}

.conteos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.conteo-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.conteo-c1 {
  border-left: 4px solid #007bff;
}

.conteo-c2 {
  border-left: 4px solid #28a745;
}

.conteo-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 8px;
}

.conteo-icon {
  font-size: 18px;
}

.conteo-value {
  font-size: 28px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 8px;
}

.conteo-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conteo-meta small {
  font-size: 12px;
  color: #6c757d;
}

.conteo-diferencia {
  background: white;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.diferencia-label {
  font-weight: 600;
  color: #6c757d;
  margin-bottom: 8px;
}

.diferencia-value {
  font-size: 32px;
  font-weight: 700;
  color: #ff6b6b;
}

.diferencia-value.sin-diferencia {
  color: #28a745;
}

/* Botones de acción */
.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 2px solid #dee2e6;
}

.action-buttons .btn {
  flex: 1;
  padding: 16px;
  font-size: 16px;
}

/* Alertas */
.alert {
  padding: 14px 18px;
  border-radius: 8px;
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.alert-danger {
  background: #f8d7da;
  color: #842029;
  border: 1px solid #f5c2c7;
}

.alert-warning {
  background: #fff3cd;
  color: #664d03;
  border: 1px solid #ffecb5;
}

.alert-success {
  background: #d1e7dd;
  color: #0f5132;
  border: 1px solid #badbcc;
}

/* Estado vacío */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #6c757d;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 16px;
  margin: 0;
}

/* Transiciones */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .input-with-button {
    flex-direction: column;
  }

  .input-with-button .btn {
    width: 100%;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .conteos-grid {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>
