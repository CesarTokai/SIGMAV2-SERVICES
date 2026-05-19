<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, ToastSuccess, LoadAlert } from '@/utils/SweetAlert';
import Swal from 'sweetalert2';
import { usePeriodoStore } from '@/store/periodoStore';

// ========================================
// 📌 APIs UTILIZADAS
// ========================================
// 1️⃣ GET /labels/selected-info
// - warehouseId es OPCIONAL
// - Si NO se envía: consulta en TODOS los almacenes
// - Si se envía: consulta en ese almacén específico
// - Retorna warehouseId en cada marbete
// - Retorna objeto "warehouses" con mapeo ID -> Nombre
//
// 2️⃣ POST /labels/print-selected-with-qr
// - Impresión CON CÓDIGOS QR
// - warehouseId es OPCIONAL
// - Si NO se envía: imprime marbetes de MÚLTIPLES almacenes
// - Si se envía: imprime marbetes de ese almacén específico
// - Requiere: folios[], periodId
// ========================================

interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface Almacen {
  id: number;
  clave: string;
  nombre: string;
}

interface MarbeteSeleccionado {
  folio: number;
  claveProducto: string;
  nombreProducto: string;
  conteo1Valor: number | null;
  conteo2Valor: number | null;
  diferencia: number | null;
  statusConteo: string;
  mensaje: string;
  estado?: string; // IMPRESO, GENERADO, CANCELADO
  warehouseId: number; // ID del almacén (ahora requerido en la respuesta)
  claveAlmacen?: string; // Clave del almacén
  nombreAlmacen?: string; // Nombre del almacén
}

// Store
const periodoStore = usePeriodoStore();

// Estado
const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacenId = ref<number | null | 'todos'>(null);

const foliosInput = ref<string>('');
const marbetesInfo = ref<MarbeteSeleccionado[]>([]);
const selectedFolios = ref<number[]>([]);

// Estados de carga
const isConsultingInfo = ref<boolean>(false);
const isPrinting = ref<boolean>(false);

// Cargar períodos
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      const periodoGuardado = periodos.value.find(p => p.id === periodoStore.periodoSeleccionado?.id);
      if (periodoGuardado) {
        selectedPeriodoId.value = periodoGuardado.id;
      }
    } else if (periodos.value.length > 0) {
      selectedPeriodoId.value = periodos.value[0]?.id || null;
    }
  } catch (error) {
    console.error('Error al cargar períodos:', error);
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};

// Cargar almacenes
const loadAlmacenes = async () => {
  try {
    const response = await axiosConfiguration.doGet('/warehouses', {
      page: 0,
      size: 100,
      sortBy: 'warehouseKey',
      sortDir: 'asc',
      search: false
    });
    const data = response.data.data || [];
    almacenes.value = data.map((item: any) => ({
      id: item.id,
      clave: String(item.warehouseKey || ''),
      nombre: String(item.nameWarehouse || '')
    }));
  } catch (error) {
    console.error('Error al cargar almacenes:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

// Parsear folios desde texto
const parseFolios = (text: string): number[] => {
  return text
    .split(',')
    .map(f => parseInt(f.trim()))
    .filter(f => !isNaN(f) && f > 0);
};

// Consultar información de marbetes seleccionados
const consultarMarbetes = async () => {
  if (!selectedPeriodoId.value) {
    ToastError('Error', 'Selecciona un período');
    return;
  }

  if (!selectedAlmacenId.value) {
    ToastError('Error', 'Selecciona un almacén');
    return;
  }

  const folios = parseFolios(foliosInput.value);
  if (folios.length === 0) {
    ToastError('Error', 'Ingresa al menos un folio válido');
    return;
  }

  if (folios.length > 500) {
    ToastError('Error', 'Máximo 500 marbetes por consulta');
    return;
  }

  try {
    isConsultingInfo.value = true;
    LoadAlert(true);

    const body: any = {
      folios: folios.join(','),
      periodId: selectedPeriodoId.value,
    };
    // Si selecciona un almacén específico, incluirlo en la consulta
    if (selectedAlmacenId.value && selectedAlmacenId.value !== 'todos') {
      body.warehouseId = selectedAlmacenId.value;
    }
    // Si selecciona 'todos', NO incluir warehouseId - consultará en todos los almacenes

    const response = await axiosConfiguration.doGet('/labels/selected-info', body);

    LoadAlert(false);

    if (response.data.data && Array.isArray(response.data.data)) {
      marbetesInfo.value = response.data.data;
      selectedFolios.value = folios;

      // 🎯 AUTO-DETECTAR ALMACÉN DESDE LA RESPUESTA
      if (marbetesInfo.value.length > 0) {
        // Obtener todos los warehouseIds únicos (la API ahora garantiza warehouseId)
        const uniqueWarehouseIds = [...new Set(marbetesInfo.value.map(m => m.warehouseId))];

        // Si hay exactamente 1 almacén, auto-seleccionarlo
        if (uniqueWarehouseIds.length === 1 && uniqueWarehouseIds[0] !== undefined) {
          selectedAlmacenId.value = uniqueWarehouseIds[0];
          const almacen = marbetesInfo.value[0];
          const almacenNombre = almacen?.nombreAlmacen || almacen?.claveAlmacen || 'desconocido';
          console.log(`✅ Almacén detectado automáticamente: ${almacenNombre}`);

          ToastSuccess(
            'Consulta exitosa',
            `Se consultaron ${response.data.total} marbetes (Almacén: ${almacenNombre})`
          );
        }
        // Si hay múltiples almacenes, mostrar resumen pero permitir imprimir directamente
        else if (uniqueWarehouseIds.length > 1) {
          const almacenesMap = response.data.warehouses || {};
          const almacenesInfo = Object.values(almacenesMap).join(', ');

          console.warn(`⚠️ Se encontraron marbetes de múltiples almacenes: ${almacenesInfo}`);

          ToastSuccess(
            'Consulta exitosa',
            `Se consultaron ${response.data.total} marbetes de ${uniqueWarehouseIds.length} almacenes: ${almacenesInfo}`
          );
          selectedAlmacenId.value = 'todos';
        }
      }
    } else {
      ToastError('Error', 'No se encontraron marbetes');
      marbetesInfo.value = [];
    }
  } catch (error: any) {
    LoadAlert(false);
    console.error('Error al consultar marbetes:', error);
    ToastError('Error', error?.response?.data?.message || 'No se pudieron consultar los marbetes');
    marbetesInfo.value = [];
  } finally {
    isConsultingInfo.value = false;
  }
};

// Imprimir marbetes seleccionados con QR
const imprimirMarbetesQR = async () => {
  if (selectedFolios.value.length === 0) {
    ToastError('Error', 'Consulta marbetes primero');
    return;
  }

  if (!selectedPeriodoId.value) {
    ToastError('Error', 'Selecciona un período');
    return;
  }


  // Verificar si hay marbetes cancelados
  const marbetesCancelados = marbetesInfo.value.filter(m => m.estado === 'CANCELADO');
  if (marbetesCancelados.length > 0) {
    const foliosCancelados = marbetesCancelados.map(m => m.folio).join(', ');
    await Swal.fire({
      title: '⚠️ Marbetes Cancelados',
      html: `
        <div style="text-align: left; color: #333;">
          <p>Los siguientes marbetes están <strong>CANCELADOS</strong> y no pueden ser descargados:</p>
          <p style="background: #ffebee; padding: 10px; border-radius: 4px; color: #c62828; font-weight: 600;">
            ${foliosCancelados}
          </p>
          <hr>
          <p style="font-size: 13px; color: #666;">Por favor, intenta solo con marbetes que no estén cancelados.</p>
        </div>
      `,
      icon: 'error',
      confirmButtonText: 'Entendido',
      confirmButtonColor: '#d32f2f'
    });
    return;
  }

  const result = await Swal.fire({
    title: '🔲 Descargar Marbetes con QR',
    html: `
      <div style="text-align: left;">
        <p><strong>Folios a imprimir:</strong> ${selectedFolios.value.join(', ')}</p>
        <p><strong>Total de marbetes:</strong> ${selectedFolios.value.length}</p>
        <p><strong>Almacén(es):</strong> ${selectedAlmacenId.value === 'todos' ? '✓ Múltiples almacenes' : selectedAlmacenId.value}</p>
        <p><strong>Formato:</strong> ✓ CON CÓDIGOS QR</p>
        <hr>
        <p style="font-size: 13px; color: #666;">Se descargará un PDF con los marbetes seleccionados incluyendo códigos QR</p>
      </div>
    `,
    icon: 'info',
    showCancelButton: true,
    confirmButtonText: 'Sí, descargar',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#28a745',
    cancelButtonColor: '#6c757d'
  });

  if (result.isConfirmed) {
    try {
      isPrinting.value = true;
      LoadAlert(true);

      const body: any = {
        folios: selectedFolios.value,
        periodId: selectedPeriodoId.value,
      };

      // Si hay almacén específico, incluirlo. Si es 'todos', NO incluir warehouseId
      if (selectedAlmacenId.value && selectedAlmacenId.value !== 'todos') {
        body.warehouseId = selectedAlmacenId.value as number;
      }

      console.log(`📥 Usando endpoint: /labels/print-selected-with-qr`);
      console.log(`📋 Imprimiendo ${selectedFolios.value.length} marbetes CON QR`);
      console.log(`📦 Almacén(es): ${selectedAlmacenId.value === 'todos' ? 'MÚLTIPLES' : selectedAlmacenId.value}`);

      const response = await axiosConfiguration.doPost('/labels/print-selected-with-qr', body, {
        responseType: 'blob'
      });

      LoadAlert(false);

      // Crear blob del PDF
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);

      // Generar nombre del archivo
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
      const filename = `marbetes_qr_${timestamp}.pdf`;

      // Crear link de descarga
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      window.URL.revokeObjectURL(url);

      ToastSuccess('Descarga completada', `Se descargó el PDF con ${selectedFolios.value.length} marbetes con QR`);
    } catch (error: any) {
      LoadAlert(false);
      console.error('Error al imprimir marbetes:', error);
      ToastError('Error', error?.response?.data?.message || 'No se pudieron imprimir los marbetes');
    } finally {
      isPrinting.value = false;
    }
  }
};

// Limpiar formulario
const limpiar = () => {
  foliosInput.value = '';
  marbetesInfo.value = [];
  selectedFolios.value = [];
};

// Formatear número
const formatNumber = (value: number | null): string => {
  if (value === null) return '-';
  return new Intl.NumberFormat('es-MX').format(value);
};

// Obtener badge de estado
const getStatusBadge = (status: string) => {
  const styles: Record<string, { bg: string; color: string }> = {
    'COMPLETO': { bg: '#4CAF50', color: 'white' },
    'INCOMPLETO': { bg: '#FF9800', color: 'white' },
    'PENDIENTE': { bg: '#2196F3', color: 'white' }
  };
  return styles[status] || { bg: '#9E9E9E', color: 'white' };
};

// Obtener badge del estado del marbete
const getEstadoBadge = (estado: string) => {
  const styles: Record<string, { bg: string; color: string }> = {
    'IMPRESO': { bg: '#4CAF50', color: 'white' },
    'GENERADO': { bg: '#2196F3', color: 'white' },
    'CANCELADO': { bg: '#f44336', color: 'white' },
    'PENDIENTE': { bg: '#FF9800', color: 'white' }
  };
  return styles[estado] || { bg: '#9E9E9E', color: 'white' };
};

// Computed
const totalMarbetes = computed(() => marbetesInfo.value.length);


onMounted(() => {
  loadPeriodos();
  loadAlmacenes();
});
</script>

<template>
  <div class="gestion-marbetes">
    <!-- Header -->
    <div class="section-card">
      <div class="title-section">
        <h1 class="section-title">🔲 Reimpresión de Marbetes con QR</h1>
        <p class="subtitle">Selecciona e imprime marbetes específicos con códigos QR incorporados</p>
      </div>

      <!-- Filtros -->
      <div class="filters-row">
        <div class="filter-group">
          <label>Período:</label>
          <select v-model.number="selectedPeriodoId">
            <option :value="null" disabled>Selecciona un período</option>
            <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
              {{ new Date(periodo.date).toLocaleDateString('es-ES') }} - {{ periodo.comments }}
            </option>
          </select>
        </div>

        <div class="filter-group">
          <label>Almacén:</label>
          <select v-model="selectedAlmacenId">
            <option :value="null" disabled>Selecciona un almacén</option>
            <option value="todos"> TODOS LOS ALMACENES</option>
            <option v-for="almacen in almacenes" :key="almacen.id" :value="almacen.id">
              {{ almacen.clave }} - {{ almacen.nombre }}
            </option>
          </select>
        </div>
      </div>

      <!-- Folios Input -->
      <div class="folios-input-section">
        <label>Folios a Consultar:</label>
        <div class="input-hint">Ingresa folios separados por comas: 1,2,3,42 (máximo 500)</div>
        <div class="input-row">
          <input
            v-model="foliosInput"
            type="text"
            placeholder="Ej: 1, 5, 10, 23, 45..."
            @keydown.enter="consultarMarbetes"
          />
          <button @click="consultarMarbetes" :disabled="isConsultingInfo">
            {{ isConsultingInfo ? '⏳ Consultando...' : '🔍 Consultar' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="totalMarbetes > 0" class="results-section">
      <div class="table-section">
        <table class="marbetes-table">
          <thead>
            <tr>
              <th>Folio</th>
              <th>Clave</th>
              <th>Producto</th>
              <th>C1</th>
              <th>C2</th>
              <th>Diferencia</th>
              <th>Status</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="marbete in marbetesInfo" :key="marbete.folio">
              <td class="folio-cell">
                <span class="folio-badge">{{ marbete.folio }}</span>
              </td>
              <td class="clave-cell">{{ marbete.claveProducto }}</td>
              <td class="producto-cell" :title="marbete.nombreProducto">
                {{ marbete.nombreProducto }}
              </td>
              <td class="conteo-cell">{{ formatNumber(marbete.conteo1Valor) }}</td>
              <td class="conteo-cell">{{ formatNumber(marbete.conteo2Valor) }}</td>
              <td class="diferencia-cell">
                <span
                  :class="[
                    'diferencia-badge',
                    marbete.diferencia === 0 ? 'igual' : marbete.diferencia! > 0 ? 'positiva' : 'negativa'
                  ]"
                >
                  {{ marbete.diferencia! > 0 ? '+' : '' }}{{ formatNumber(marbete.diferencia) }}
                </span>
              </td>
              <td class="status-cell">
                <span
                  class="badge"
                  :style="{
                    backgroundColor: getStatusBadge(marbete.statusConteo).bg,
                    color: getStatusBadge(marbete.statusConteo).color
                  }"
                >
                  {{ marbete.statusConteo }}
                </span>
              </td>
              <td class="estado-cell">
                <span
                  class="estado-badge"
                  :style="{
                    backgroundColor: getEstadoBadge(marbete.estado || 'PENDIENTE').bg,
                    color: getEstadoBadge(marbete.estado || 'PENDIENTE').color
                  }"
                >
                  {{ marbete.estado || 'PENDIENTE' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="actions-section">
        <button @click="limpiar" class="btn-secondary" :disabled="isPrinting">Limpiar</button>
        <button @click="imprimirMarbetesQR" :disabled="isPrinting" class="btn-submit">
          {{ isPrinting ? '⏳ Generando PDF...' : '🔲 Descargar con QR' }}
        </button>
      </div>
    </div>

    <div class="info-box">
      <h3>ℹ️ Cómo Usar</h3>
      <ul>
        <li><strong>Paso 1:</strong> Selecciona período y almacén (o "Todos los almacenes")</li>
        <li><strong>Paso 2:</strong> Ingresa los folios separados por comas (máximo 500)
          <ul>
            <li>1 folio: <strong>195</strong></li>
            <li>Varios: <strong>195, 200, 205, 210</strong></li>
          </ul>
        </li>
        <li><strong>Paso 3:</strong> Haz clic en "Consultar" para ver la información
          <ul>
            <li>Si los folios son de un solo almacén, se auto-selecciona</li>
            <li>Si son de múltiples almacenes, se marca "Todos los almacenes"</li>
          </ul>
        </li>
        <li><strong>Paso 4:</strong> Haz clic en "🔲 Descargar con QR" para obtener el PDF con códigos QR</li>
        <li><strong> Novedad:</strong> Ahora puedes imprimir marbetes de múltiples almacenes en un solo PDF</li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.gestion-marbetes {
  padding: 5px;
}

.section-card {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.title-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.subtitle {
  font-size: 13px;
  color: #666;
  margin: 5px 0 0 0;
}

.filters-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-group label {
  font-weight: 500;
  color: #555;
  font-size: 13px;
}

.filter-group select {
  padding: 8px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 13px;
  background: white;
}

.filter-group select:focus {
  outline: none;
  border-color: #2196F3;
}

.folios-input-section {
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.folios-input-section label {
  display: block;
  font-weight: 500;
  color: #555;
  font-size: 13px;
  margin-bottom: 6px;
}

.input-hint {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.input-row {
  display: flex;
  gap: 10px;
}

.input-row input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 13px;
}

.input-row input:focus {
  outline: none;
  border-color: #2196F3;
}

.input-row button {
  padding: 10px 24px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
}

.input-row button:hover:not(:disabled) {
  background: #1976D2;
}

.input-row button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.results-section {
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
}

.table-section {
  margin-bottom: 20px;
  overflow-x: auto;
}

.marbetes-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.marbetes-table thead {
  background: #f5f5f5;
  border-bottom: 2px solid #ddd;
}

.marbetes-table thead th {
  padding: 12px 8px;
  text-align: left;
  font-weight: 600;
  color: #333;
}

.marbetes-table tbody tr {
  border-bottom: 1px solid #eee;
}

.marbetes-table tbody tr:hover {
  background: #f9f9f9;
}

.marbetes-table td {
  padding: 10px 8px;
}

.folio-badge {
  background: #2196F3;
  color: white;
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
}

.badge {
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
  font-size: 11px;
}

.estado-cell {
  padding: 10px 8px;
  font-size: 12px;
}

.estado-badge {
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.diferencia-badge {
  padding: 4px 8px;
  border-radius: 3px;
  font-weight: 600;
  display: inline-block;
}

.diferencia-badge.igual {
  background: #c8e6c9;
  color: #2e7d32;
}

.diferencia-badge.positiva {
  background: #fff3cd;
  color: #856404;
}

.diferencia-badge.negativa {
  background: #ffcdd2;
  color: #c62828;
}

.actions-section {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid #ddd;
  align-items: center;
}


.btn-secondary,
.btn-submit {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

.btn-secondary:hover:not(:disabled) {
  background: #eee;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-submit {
  background: #28a745;
  color: white;
}

.btn-submit:hover:not(:disabled) {
  background: #20c997;
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.info-box {
  background: #e3f2fd;
  border-left: 4px solid #2196F3;
  padding: 16px;
  border-radius: 4px;
}

.info-box h3 {
  margin: 0 0 12px 0;
  color: #1976D2;
  font-size: 14px;
  font-weight: 600;
}

.info-box ul {
  margin: 0;
  padding-left: 20px;
  color: #555;
  font-size: 12px;
  line-height: 1.8;
}

.info-box li {
  margin-bottom: 6px;
}

.info-box li:last-child {
  margin-bottom: 0;
}

.info-box ul ul {
  margin-top: 6px;
  margin-bottom: 6px;
}

@media (max-width: 768px) {
  .filters-row {
    grid-template-columns: 1fr;
  }

  .actions-section {
    flex-direction: column;
  }

  .btn-secondary,
  .btn-primary,
  .btn-submit {
    width: 100%;
  }
}
</style>

