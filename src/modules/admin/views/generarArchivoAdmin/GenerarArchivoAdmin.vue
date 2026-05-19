<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { usePeriodoStore } from '@/store/periodoStore';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, ToastSuccess, LoadAlert } from '@/utils/SweetAlert';

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
  activo: boolean;
}

const periodoStore = usePeriodoStore();

// Estado
const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacen = ref<Almacen | null>(null);
const selectedAlmacenId = ref<number | null>(null);
const loading = ref(false);
const previewData = ref<any[]>([]);
const previewLoading = ref(false);
const totalRecords = ref<number>(0);

// Cargar períodos
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

    // Cargar periodo guardado del store
    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      selectedPeriodo.value = periodoStore.periodoSeleccionado;
      selectedPeriodoId.value = periodoStore.periodoSeleccionado.id;
    } else if (periodos.value.length > 0 && !selectedPeriodo.value) {
      selectedPeriodo.value = periodos.value[0] || null;
      selectedPeriodoId.value = selectedPeriodo.value ? selectedPeriodo.value.id : null;
      if (selectedPeriodo.value) {
        periodoStore.setPeriodo(selectedPeriodo.value);
      }
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
      nombre: String(item.nameWarehouse || ''),
      activo: !item.deleted
    }));

    // Agregar opción "Todos los almacenes"
    almacenes.value.unshift({
      id: 0,
      clave: 'TODOS',
      nombre: 'Todos los almacenes',
      activo: true
    });

    if (almacenes.value.length > 0 && !selectedAlmacen.value) {
      selectedAlmacen.value = almacenes.value[0] || null;
      selectedAlmacenId.value = selectedAlmacen.value ? selectedAlmacen.value.id : null;
    }
  } catch (error) {
    console.error('Error al cargar almacenes:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

// Manejar cambio de almacén
const handleAlmacenChange = () => {
  if (selectedAlmacenId.value !== null) {
    selectedAlmacen.value = almacenes.value.find(a => a.id === selectedAlmacenId.value) || null;
  }
  loadPreviewData();
};

// Manejar cambio de período
const handlePeriodoChange = () => {
  const periodo = periodos.value.find((p) => p.id === selectedPeriodoId.value);
  if (periodo) {
    selectedPeriodo.value = periodo;
    periodoStore.setPeriodo(periodo);
    loadPreviewData();
  }
};

// Cargar datos para preview
const loadPreviewData = async () => {
  if (!selectedPeriodo.value) {
    previewData.value = [];
    totalRecords.value = 0;
    return;
  }

  try {
    previewLoading.value = true;

    // Construir body según la lógica flexible de API
    // Si almacén es "TODOS" (id: 0), NO enviar warehouseId
    // Si es un almacén específico, enviar warehouseId
    const requestBody: any = {
      periodId: selectedPeriodo.value.id
    };

    if (selectedAlmacen.value && selectedAlmacen.value.id !== 0) {
      requestBody.warehouseId = selectedAlmacen.value.id;
    }

    const response = await axiosConfiguration.doPost('/labels/generate-file', requestBody, {
      responseType: 'blob'
    });

    // Convertir blob a texto
    const text = await response.data.text();
    const lines = text.split('\n').filter((line: string) => line.trim());

    // Función para parsear CSV de forma más robusta
    const parseCSVLine = (line: string): string[] => {
      const result: string[] = [];
      let current = '';
      let inQuotes = false;

      for (let i = 0; i < line.length; i++) {
        const char = line[i];

        if (char === '"') {
          inQuotes = !inQuotes;
        } else if (char === ',' && !inQuotes) {
          result.push(current.trim());
          current = '';
        } else {
          current += char;
        }
      }
      result.push(current.trim());
      return result;
    };

    // Parsear CSV correctamente
    const rows = lines.map((line: string) => parseCSVLine(line));

    if (rows.length > 1) {
      const headers = rows[0];
      const dataRows = rows.slice(1);

      // Guardar el total de registros (sin incluir el header)
      totalRecords.value = dataRows.length;

      // Mapear solo los primeros 10 registros para la preview
      previewData.value = dataRows.slice(0, 10).map((row: string[]) => {
        const obj: any = {};
        headers.forEach((header: string, index: number) => {
          obj[header] = row[index] || '-';
        });
        return obj;
      });

      console.log(`📊 Preview cargado: ${previewData.value.length} registros de ${totalRecords.value} total`);
    } else {
      previewData.value = [];
      totalRecords.value = 0;
    }
  } catch (error) {
    console.error('Error al cargar preview:', error);
    previewData.value = [];
    totalRecords.value = 0;
  } finally {
    previewLoading.value = false;
  }
};

// Generar y descargar archivo
const generarArchivo = async () => {
  if (!selectedPeriodo.value) {
    ToastError('Error', 'Debes seleccionar un período');
    return;
  }

  try {
    loading.value = true;
    LoadAlert(true);

    // Construir body según la lógica flexible de API
    const requestBody: any = {
      periodId: selectedPeriodo.value.id
    };

    if (selectedAlmacen.value && selectedAlmacen.value.id !== 0) {
      requestBody.warehouseId = selectedAlmacen.value.id;
    }

    // Realizar petición POST con el body requerido
    const response = await axiosConfiguration.doPost(
        '/labels/generate-file',
        requestBody,
        { responseType: 'blob' }
    );

    // Crear URL del blob y descargar
    const blob = new Blob([response.data], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;

    // Nombre del archivo con fecha y almacén (si aplica)
    const fecha = formatDate(selectedPeriodo.value.date);
    const almacenSuffix = selectedAlmacen.value && selectedAlmacen.value.id !== 0
      ? `_${selectedAlmacen.value.clave}`
      : '';
    link.download = `inventario_${fecha}${almacenSuffix}.csv`;

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    LoadAlert(false);
    ToastSuccess('Éxito', 'Archivo CSV generado y descargado correctamente');

  } catch (error: any) {
    console.error('Error al generar archivo:', error);
    LoadAlert(false);
    ToastError(
        'Error',
        error.response?.data?.message || 'No se pudo generar el archivo'
    );
  } finally {
    loading.value = false;
  }
};

// Formatear fecha de manera robusta para evitar desfase de día
const formatDate = (date: string | undefined): string => {
  if (!date) return 'N/A';
  // Caso: 'YYYY-MM-DD' puro
  if (/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    const [y, m, d] = date.split('-');
    const dateObj = new Date(Number(y), Number(m) - 1, Number(d));
    return dateObj.toLocaleDateString('es-MX', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');
  }
  // Caso: ISO o con zona horaria, extraer solo la parte de la fecha
  if (/^\d{4}-\d{2}-\d{2}T/.test(date)) {
    const match = date.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (match) {
      const [_, y, m, d] = match;
      const dateObj = new Date(Number(y), Number(m) - 1, Number(d));
      return dateObj.toLocaleDateString('es-MX', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');
    }
    // fallback
    const dateObj = new Date(date);
    return dateObj.toLocaleDateString('es-MX', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');
  }
  try {
    const dateObj = new Date(date);
    return dateObj.toLocaleDateString('es-MX', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-');
  } catch {
    return date;
  }
}

onMounted(() => {
  loadPeriodos();
  loadAlmacenes();
});
</script>

<template>
  <div class="reporte-container">
    <!-- Header Section -->
    <div class="header-section">
      <div class="title-wrapper">
        <h1 class="page-title">Generar Archivo CSV</h1>
        <p class="subtitle">Selecciona un período para generar el archivo de inventario</p>
      </div>
    </div>

    <!-- Controls Section -->
    <div class="controls-section">
      <div class="filter-item">
        <label class="filter-label">Período</label>
        <select
            id="periodoSelect"
            v-model.number="selectedPeriodoId"
            class="filter-select"
            @change="handlePeriodoChange"
        >
          <option :value="null" disabled>Selecciona un período</option>
          <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
            {{ formatDate(periodo.date) }} - {{ periodo.comments }}
          </option>
        </select>
      </div>

      <div class="filter-item">
        <label class="filter-label">Almacén</label>
        <select
            id="almacenSelect"
            v-model.number="selectedAlmacenId"
            class="filter-select"
            @change="handleAlmacenChange"
        >
          <option v-for="almacen in almacenes" :key="almacen.id" :value="almacen.id">
            {{ almacen.clave === 'TODOS' ? almacen.nombre : `${almacen.clave} - ${almacen.nombre}` }}
          </option>
        </select>
      </div>

      <div class="info-box">
        <div class="info-item">
          <span class="info-label">Formato:</span>
          <span class="info-value">CSV</span>
        </div>
        <div class="info-item">
          <span class="info-label">Tipo:</span>
          <span class="info-value">Texto plano (.csv)</span>
        </div>
        <div class="info-item">
          <span class="info-label">Codificación:</span>
          <span class="info-value">UTF-8</span>
        </div>
      </div>

      <button
          class="btn-generate-csv"
          :disabled="!selectedPeriodo || loading"
          @click="generarArchivo"
          title="Generar y descargar archivo CSV"
      >
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2m0 0v-8m0 8H3m0 0h18"/>
        </svg>
        {{ loading ? 'Generando...' : 'Generar CSV' }}
      </button>
    </div>

    <!-- Info Panel -->
    <div class="info-panel">
      <div class="info-panel-content">
        <svg class="info-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <p class="info-text">El archivo se descargará automáticamente cuando esté listo</p>
      </div>
    </div>

    <!-- Preview Section -->
    <div v-if="selectedPeriodo" class="preview-section">
      <div class="preview-header">
        <h2 class="preview-title">Vista Previa del Archivo</h2>
        <p class="preview-subtitle">Primeros 10 registros del período seleccionado</p>
      </div>

      <div v-if="previewLoading" class="preview-loading">
        <div class="spinner"></div>
        <p>Cargando vista previa...</p>
      </div>

      <div v-else-if="previewData.length > 0" class="preview-content">
        <div class="preview-table-wrapper">
          <table class="preview-table">
            <thead>
              <tr>
                <th v-for="key in Object.keys(previewData[0])" :key="key">
                  {{ key }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in previewData" :key="index">
                <td v-for="key in Object.keys(previewData[0])" :key="key" class="preview-cell">
                  {{ row[key] }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="preview-note">
          📊 Total de registros en el archivo: <strong>{{ totalRecords }}</strong> | Mostrando: {{ previewData.length }} en la vista previa
        </p>
      </div>

      <div v-else class="preview-empty">
        <p>No hay datos disponibles para este período</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.reporte-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Header Section */
.header-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.title-wrapper {
  text-align: center;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.subtitle {
  font-size: 14px;
  color: #6c757d;
  margin: 8px 0 0 0;
}

/* Controls Section */
.controls-section {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  min-width: 200px;
  flex: 1;
}

.filter-label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #495057;
  font-weight: 500;
}

.filter-select {
  width: 100%;
  padding: 10px 12px;
  font-size: 14px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  background-color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.filter-select option {
  padding: 8px;
}

/* Info Box */
.info-box {
  display: flex;
  gap: 16px;
  padding: 12px 16px;
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  align-items: center;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 12px;
  color: #868e96;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.info-value {
  font-size: 14px;
  color: #212529;
  font-weight: 500;
}

/* Generate Button */
.btn-generate-csv {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.btn-generate-csv svg {
  width: 18px;
  height: 18px;
  stroke-width: 2;
}

.btn-generate-csv:hover:not(:disabled) {
  background: linear-gradient(135deg, #20c997 0%, #1aa179 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
}

.btn-generate-csv:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

/* Info Panel */
.info-panel {
  background: #d4edda;
  border: 1px solid #c3e6cb;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: flex-start;
}

.info-panel-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.info-icon {
  width: 20px;
  height: 20px;
  color: #155724;
  flex-shrink: 0;
  margin-top: 2px;
}

.info-text {
  font-size: 14px;
  color: #155724;
  margin: 0;
  line-height: 1.5;
}

/* Preview Section */
.preview-section {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  overflow: hidden;
}

.preview-header {
  padding: 16px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #212529;
  margin: 0 0 4px 0;
}

.preview-subtitle {
  font-size: 13px;
  color: #6c757d;
  margin: 0;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #28a745;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.preview-loading p {
  color: #6c757d;
  font-size: 14px;
  margin: 0;
}

.preview-content {
  padding: 20px;
}

.preview-table-wrapper {
  overflow-x: auto;
  margin-bottom: 12px;
  border: 1px solid #dee2e6;
  border-radius: 6px;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.preview-table thead {
  background: #f8f9fa;
  border-bottom: 2px solid #dee2e6;
}

.preview-table th {
  padding: 10px 12px;
  text-align: left;
  font-weight: 600;
  color: #495057;
  white-space: nowrap;
}

.preview-table tbody tr {
  border-bottom: 1px solid #dee2e6;
}

.preview-table tbody tr:hover {
  background-color: #f8f9fa;
}

.preview-table tbody td {
  padding: 10px 12px;
  color: #495057;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-note {
  font-size: 12px;
  color: #6c757d;
  margin: 0;
  text-align: center;
}

.preview-empty {
  padding: 40px 20px;
  text-align: center;
  color: #6c757d;
}

.preview-empty p {
  margin: 0;
  font-size: 14px;
}

/* Responsive */
@media (max-width: 768px) {
  .controls-section {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-item {
    width: 100%;
    min-width: auto;
  }

  .info-box {
    flex-direction: column;
    align-items: flex-start;
  }

  .btn-generate-csv {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 600px) {
  .header-section {
    padding: 16px;
  }

  .page-title {
    font-size: 20px;
  }

  .subtitle {
    font-size: 13px;
  }

  .controls-section {
    gap: 12px;
  }

  .info-box {
    gap: 12px;
  }
}



</style>