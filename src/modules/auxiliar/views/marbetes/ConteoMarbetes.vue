<script setup lang="ts" xmlns="http://www.w3.org/1999/html">
import {ref, onMounted, nextTick, computed, onUnmounted, watch} from 'vue';
import axiosConfiguration from '@/config/axiosConfig';
import {ToastError, ToastSuccess, LoadAlert} from '@/utils/SweetAlert';
import { extractErrorMessage } from '@/utils/errorExtractor';
import Swal from 'sweetalert2';
import { usePeriodoStore } from '@/store/periodoStore';

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

interface MarbeteConteo {
  folio: number;
  periodId: number;
  warehouseId: number;
  claveAlmacen: string;
  nombreAlmacen: string;
  claveProducto: string;
  descripcionProducto: string;
  unidadMedida: string;
  cancelado: boolean;
  conteo1: number | null;
  conteo2: number | null;
  diferencia: number | null;
  estado: string;
  impreso: boolean;
  existQty: number;
  existQtyUnidad: string;
  conteo1Comentario?: string;
  conteo1UsuarioNombre?: string;
  conteo2Comentario?: string;
  conteo2UsuarioNombre?: string;
}

// Store
const periodoStore = usePeriodoStore();

// Estado
const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const selectedPeriodo = ref<Periodo | null>(null);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacen = ref<Almacen | null>(null);
const selectedAlmacenId = ref<number | null>(null);

const folioInput = ref<string>('');
const marbeteActual = ref<MarbeteConteo | null>(null);
const resultadosBusqueda = ref<MarbeteConteo[]>([]);

// Refs para los inputs
const folioInputRef = ref<HTMLInputElement | null>(null);

// Eliminar función buscarMarbeteLocal y toda referencia a marbetes y loadMarbetes
// Definir los refs para los inputs de conteo y sus displays
const conteo1Input = ref<number | null>(null);
const conteo2Input = ref<number | null>(null);
const conteo1Display = ref<string>('');
const conteo2Display = ref<string>('');
const conteo1InputRef = ref<HTMLInputElement | null>(null);
const conteo2InputRef = ref<HTMLInputElement | null>(null);

const comentarioInput = ref<string>('');

// Calcular diferencia entre conteos
const diferenciaConteos = computed(() => {
  if (conteo1Input.value !== null && conteo2Input.value !== null) {
    return conteo2Input.value - conteo1Input.value;
  }
  return null;
});

// Obtener clase CSS para la diferencia según su valor
const getDiferenciaClass = (): string => {
  if (diferenciaConteos.value === null) return 'diferencia-neutral';
  if (diferenciaConteos.value === 0) return 'diferencia-igual';
  return diferenciaConteos.value > 0 ? 'diferencia-positiva' : 'diferencia-negativa';
};

// Cargar períodos
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

    // Cargar periodo guardado del store
    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      // Usar el periodo seleccionado del store
      const periodoGuardado = periodos.value.find(p => p.id === periodoStore.periodoSeleccionado?.id);
      if (periodoGuardado) {
        selectedPeriodo.value = periodoGuardado;
        selectedPeriodoId.value = periodoGuardado.id;
        console.log('✅ Periodo cargado desde store:', periodoGuardado);
      }
    } else if (periodos.value.length > 0 && !selectedPeriodo.value) {
      // Si no hay periodo guardado, usar el primero
      selectedPeriodo.value = periodos.value[0] || null;
      selectedPeriodoId.value = selectedPeriodo.value ? selectedPeriodo.value.id : null;
    }
  } catch (error) {
    console.error('Error al cargar períodos:', error);
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};


// Cargar almacenes
const loadAlmacenes = async () => {
  try {
    // Cargar solo almacenes asignados al usuario actual
    const response = await axiosConfiguration.doGet('/warehouses/my-warehouses');
    const data = Array.isArray(response.data) ? response.data : (response.data?.data || []);

    console.log('🏢 Almacenes cargados (raw):', data);

    almacenes.value = data.map((item: any) => ({
      id: item.id,
      clave: String(item.warehouseKey || ''),
      nombre: String(item.nameWarehouse || ''),
      activo: !item.deleted
    }));

    console.log('🏢 Almacenes mapeados:', almacenes.value);

    if (almacenes.value.length > 0) {
      const firstAlmacen = almacenes.value[0]!;
      selectedAlmacen.value = firstAlmacen;
      selectedAlmacenId.value = firstAlmacen.id;
      console.log('✅ Almacén auto-seleccionado:', firstAlmacen);
    } else {
      console.warn('⚠️ NO hay almacenes disponibles');
      ToastError('Advertencia', 'No tienes almacenes asignados');
    }
  } catch (error) {
    console.error('Error al cargar almacenes:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

// Watchers para sincronizar los IDs seleccionados con los objetos completos
watch(selectedPeriodoId, (newId) => {
  if (newId !== null) {
    selectedPeriodo.value = periodos.value.find(p => p.id === newId) || null;
    if (selectedPeriodo.value) {
      // Guardar en el store cuando cambie el periodo
      periodoStore.setPeriodo(selectedPeriodo.value);
      console.log('Periodo seleccionado:', selectedPeriodo.value);
    }
  }
});

watch(selectedAlmacenId, (newId) => {
  if (newId !== null) {
    selectedAlmacen.value = almacenes.value.find(a => a.id === newId) || null;
    console.log('Almacén seleccionado:', selectedAlmacen.value);
  }
});

// Buscar marbete específico por folio usando API
const buscarMarbetePorFolio = async () => {
  const raw = String(folioInput.value || '').trim();
  if (!raw) {
    marbeteActual.value = null;
    resultadosBusqueda.value = [];
    ToastError('Error', 'Ingresa un folio para buscar');
    return;
  }

  // ✅ VALIDAR que período y almacén sean seleccionados REQUERIDOS
  if (!selectedPeriodo.value) {
    ToastError('Falta período', 'Selecciona un período antes de buscar');
    return;
  }

  if (!selectedAlmacen.value) {
    ToastError('Falta almacén', 'Selecciona un almacén antes de buscar');
    return;
  }

  try {
    LoadAlert(true);

    // ✅ REQUERIR warehouseId - No es opcional
    const body: any = {
      folio: parseInt(raw),
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id  // ✅ REQUERIDO
    };

    console.log('🔍 Buscando folio específico:', body);

    // Usar el mismo endpoint pero con el folio específico en el body
    const response = await axiosConfiguration.doPost('/labels/for-count', body);

    LoadAlert(false);

    if (!response.data) {
      ToastError('Sin resultados', `No se encontró el folio ${raw} en el período y almacén seleccionados`);
      marbeteActual.value = null;
      resultadosBusqueda.value = [];
      return;
    }

    // 🔍 DEBUG: Ver qué datos devuelve la API
    console.log('📡 Respuesta completa de la API:', response.data);
    console.log('🔎 Campos disponibles:', Object.keys(response.data));

    // Mapear la respuesta — campos exactos del API /labels/for-count
    const marbete: MarbeteConteo = {
      folio:               response.data.folio ?? parseInt(raw),
      periodId:            Number(response.data.periodId ?? 0),
      warehouseId:         Number(response.data.warehouseId ?? 0),
      claveAlmacen:        String(response.data.claveAlmacen ?? '').trim(),
      nombreAlmacen:       String(response.data.nombreAlmacen ?? '').trim(),
      claveProducto:       String(response.data.claveProducto ?? '').trim(),
      descripcionProducto: String(response.data.descripcionProducto ?? '').trim(),
      unidadMedida:        String(response.data.unidadMedida ?? '').trim(),
      cancelado:           Boolean(response.data.cancelado ?? false),
      conteo1:             response.data.conteo1 ?? null,
      conteo2:             response.data.conteo2 ?? null,
      diferencia:          response.data.diferencia ?? null,
      estado:              String(response.data.estado ?? 'Pendiente'),
      impreso:             Boolean(response.data.impreso ?? false),
      existQty:            Number(response.data.existQty ?? 0),
      existQtyUnidad:      String(response.data.existQtyUnidad ?? '').trim(),
      conteo1Comentario:   response.data.conteo1Comentario ?? undefined,
      conteo1UsuarioNombre: response.data.conteo1UsuarioNombre ?? undefined,
      conteo2Comentario:   response.data.conteo2Comentario ?? undefined,
      conteo2UsuarioNombre: response.data.conteo2UsuarioNombre ?? undefined
    };

    if (marbete.cancelado) {
      ToastError('Marbete Cancelado', 'Este marbete ha sido cancelado y no puede ser usado');
      marbeteActual.value = null;
      resultadosBusqueda.value = [];
      return;
    }

    console.log('✅ Marbete encontrado:', marbete);

    // ✅ AUTO-ACTUALIZAR: Si el folio pertenece a otro almacén, actualizar la selección
    if (response.data.warehouseId && response.data.warehouseId !== selectedAlmacen.value?.id) {
      const almacenDelFolio = almacenes.value.find(a => a.id === response.data.warehouseId);
      if (almacenDelFolio) {
        selectedAlmacen.value = almacenDelFolio;
        selectedAlmacenId.value = almacenDelFolio.id;
        console.log(`🔄 Almacén actualizado automáticamente a: ${almacenDelFolio.nombre} (ID: ${almacenDelFolio.id})`);
        ToastSuccess(
          'Marbete encontrado + Almacén actualizado',
          `Folio ${marbete.folio} pertenece a ${almacenDelFolio.nombre}`
        );
      }
    } else {
      ToastSuccess('Marbete encontrado', `Folio ${marbete.folio} - ${marbete.descripcionProducto}`);
    }

    // Seleccionar directamente el marbete encontrado
    seleccionarMarbete(marbete);

  } catch (error: any) {
    LoadAlert(false);
    console.error('Error al buscar folio:', error);

    const mensajeError = extractErrorMessage(error);
    ToastError('Error al buscar folio', mensajeError);

    marbeteActual.value = null;
    resultadosBusqueda.value = [];
  }
};


// Seleccionar un marbete de los resultados
const seleccionarMarbete = (marbete: MarbeteConteo) => {
  marbeteActual.value = marbete;
  resultadosBusqueda.value = [];

  if (marbete.conteo1 !== null && marbete.conteo1 !== undefined) {
    conteo1Input.value = marbete.conteo1;
    conteo1Display.value = formatNumberWithCommas(marbete.conteo1);

    if (marbete.conteo2 !== null && marbete.conteo2 !== undefined) {
      conteo2Input.value = marbete.conteo2;
      conteo2Display.value = formatNumberWithCommas(marbete.conteo2);
    } else {
      conteo2Input.value = null;
      conteo2Display.value = '';
    }
    setTimeout(() => {
      conteo2InputRef.value?.focus();
      conteo2InputRef.value?.select();
    }, 300);
  } else {
    conteo1Input.value = null;
    conteo2Input.value = null;
    conteo1Display.value = '';
    conteo2Display.value = '';
    setTimeout(() => {
      conteo1InputRef.value?.focus();
      conteo1InputRef.value?.select();
    }, 300);
  }

  folioInput.value = String(marbete.folio);
};

// Guardar conteo
const guardarConteo = async () => {
  if (!marbeteActual.value) {
    ToastError('Error', 'Busca un marbete primero');
    return;
  }

  // ── Capturar TODO antes de limpiar el formulario ─────────────────────
  const folio          = marbeteActual.value.folio;
  const periodId       = marbeteActual.value.periodId ?? selectedPeriodo.value?.id;
  const warehouseId    = marbeteActual.value.warehouseId ?? selectedAlmacen.value?.id;
  const comentario     = comentarioInput.value?.trim() || null;
  const c1Valor        = conteo1Input.value;
  const c2Valor        = conteo2Input.value;
  const c1Existente    = marbeteActual.value.conteo1;
  const c2Existente    = marbeteActual.value.conteo2;

  // ── Determinar qué operaciones hacer ─────────────────────────────────
  const debeGuardarC1    = c1Valor !== null && (c1Existente === null || c1Valor !== c1Existente);
  const esActualizacionC1 = c1Existente !== null;
  const debeGuardarC2    = c2Valor !== null && (c2Existente === null || c2Valor !== c2Existente);
  const esActualizacionC2 = c2Existente !== null;

  if (!debeGuardarC1 && !debeGuardarC2) {
    ToastError('Sin cambios', 'No hay cambios en los conteos para guardar');
    return;
  }

  console.log('📋 Operaciones a realizar:', {
    debeGuardarC1, esActualizacionC1, c1Valor,
    debeGuardarC2, esActualizacionC2, c2Valor,
    folio, periodId, warehouseId
  });

  // ── Limpiar UI inmediatamente ─────────────────────────────────────────
  limpiarFormulario();
  setTimeout(() => { folioInputRef.value?.focus(); folioInputRef.value?.select(); }, 50);

  LoadAlert(true);
  try {
    // ── GUARDAR/ACTUALIZAR C1 ─────────────────────────────────────────
    if (debeGuardarC1) {
      const body: any = { folio, periodId, warehouseId, countedValue: c1Valor };
      if (comentario) body.comment = comentario;

      console.log('📤 C1:', esActualizacionC1 ? 'PUT /c1/update' : 'POST /c1', body);

      if (esActualizacionC1) {
        await axiosConfiguration.doPut('/labels/counts/c1/update', body);
      } else {
        await axiosConfiguration.doPost('/labels/counts/c1', body);
      }
    }

    // ── GUARDAR/ACTUALIZAR C2 ─────────────────────────────────────────
    if (debeGuardarC2) {
      const body: any = { folio, periodId, warehouseId, countedValue: c2Valor };
      if (comentario) body.comment = comentario;

      console.log('📤 C2:', esActualizacionC2 ? 'PUT /c2/update' : 'POST /c2', body);

      if (esActualizacionC2) {
        await axiosConfiguration.doPut('/labels/counts/c2/update', body);
      } else {
        await axiosConfiguration.doPost('/labels/counts/c2', body);
      }
    }

    LoadAlert(false);

    // ── Toast de resultado ────────────────────────────────────────────
    if (debeGuardarC1 && debeGuardarC2) {
      ToastSuccess('✅ Conteos guardados', `Folio ${folio}: C1=${c1Valor}, C2=${c2Valor}`);
    } else if (debeGuardarC1) {
      ToastSuccess(
        esActualizacionC1 ? '✅ Conteo 1 actualizado' : '✅ Conteo 1 guardado',
        `Folio ${folio}: ${c1Valor} unidades`
      );
    } else {
      ToastSuccess(
        esActualizacionC2 ? '✅ Conteo 2 actualizado' : '✅ Conteo 2 guardado',
        `Folio ${folio}: ${c2Valor} unidades`
      );
    }
  } catch (error: any) {
    LoadAlert(false);
    console.error('❌ Error al guardar conteo:', error);
    const mensajeError = extractErrorMessage(error);
    ToastError('❌ Error al guardar conteo', mensajeError);
  }
};

// Cancelar marbete
const cancelarMarbete = async () => {
  if (!marbeteActual.value) {
    ToastError('Error', 'Busca un marbete primero');
    return;
  }

  // Diálogo de confirmación con campo de motivo
  const { value: motivo } = await Swal.fire({
    title: '¿Cancelar este folio?',
    html: `
      <div style="text-align: left; margin: 15px 0;">
        <p><strong>Folio:</strong> ${marbeteActual.value.folio}</p>
        <p><strong>Producto:</strong> ${marbeteActual.value.descripcionProducto}</p>
        <label style="display: block; margin-top: 15px; margin-bottom: 8px; font-weight: 500;">
          Motivo de cancelación:
        </label>
      </div>
    `,
    input: 'textarea' as any,
    inputPlaceholder: 'Ejemplo: Producto dañado, error administrativo, etc.',
    inputAttributes: {
      'rows': '3',
      'style': 'width: 90%; max-width: 400px; padding: 8px; border: 1px solid #ccc; border-radius: 4px; font-family: Arial, sans-serif; box-sizing: border-box;'
    } as any,
    icon: 'warning' as any,
    showCancelButton: true,
    confirmButtonText: 'Sí, cancelar',
    cancelButtonText: 'No',
    reverseButtons: true,
    inputValidator: (value: any) => {
      if (!value || !value.trim()) {
        return 'Debes ingresar un motivo de cancelación';
      }
    }
  });

  // Si el usuario confirma y proporciona el motivo
  if (motivo !== undefined) {
    try {
      LoadAlert(true);

      // Enviar solo folio y motivo (periodId y warehouseId son opcionales)
      await axiosConfiguration.doPost('/labels/cancel', {
        folio: marbeteActual.value.folio,
        motivoCancelacion: motivo.trim()
      });

      LoadAlert(false);
      ToastSuccess('Folio cancelado', `El folio ${marbeteActual.value.folio} fue cancelado correctamente.`);
      limpiarFormulario();

      // Enfocar nuevamente en el buscador
      nextTick(() => {
        folioInputRef.value?.focus();
      });
    } catch (error: any) {
      LoadAlert(false);
      console.error('Error al cancelar folio:', error);
      ToastError('Error', error?.response?.data?.message || 'No se pudo cancelar el folio');
    }
  }
};

// Limpiar formulario
const limpiarFormulario = () => {
  folioInput.value = '';
  conteo1Input.value = null;
  conteo2Input.value = null;
  conteo1Display.value = '';
  conteo2Display.value = '';
  comentarioInput.value = '';
  marbeteActual.value = null;
  resultadosBusqueda.value = [];
};

// Formatear fecha
const formatDate = (date: string): string => {
  if (!date) return 'N/A';
  return new Date(date).toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
};

const formatNumber = (value: number | null): string => {
  if (value === null) return '-';
  return new Intl.NumberFormat('es-MX').format(value);
};

// Formatear número con separador de miles (sin decimales)
const formatNumberWithCommas = (value: number | string): string => {
  if (!value) return '';
  const numStr = value.toString().replace(/[^0-9]/g, '');
  if (!numStr) return '';
  return new Intl.NumberFormat('es-MX').format(parseInt(numStr));
};

// Validar y formatear input de conteo (solo números enteros, sin decimales)
const handleConteoInput = (event: Event, inputRef: 'conteo1' | 'conteo2') => {
  const input = event.target as HTMLInputElement;
  let value = input.value;

  // Eliminar todo excepto números
  value = value.replace(/[^0-9]/g, '');

  // Actualizar el valor numérico real
  const numValue = value ? parseInt(value) : null;

  if (inputRef === 'conteo1') {
    conteo1Input.value = numValue;
    conteo1Display.value = value ? formatNumberWithCommas(value) : '';
  } else {
    conteo2Input.value = numValue;
    conteo2Display.value = value ? formatNumberWithCommas(value) : '';
  }

  // Actualizar el input con el formato
  input.value = inputRef === 'conteo1' ? conteo1Display.value : conteo2Display.value;
};


// Prevenir entrada de caracteres no numéricos
const preventNonNumeric = (event: KeyboardEvent) => {
  const char = event.key;
  // Permitir: números, backspace, delete, tab, escape, enter, flechas
  const allowedKeys = ['Backspace', 'Delete', 'Tab', 'Escape', 'Enter', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'];

  if (allowedKeys.includes(char)) {
    return true;
  }

  // Solo permitir números (0-9)
  if (!/^[0-9]$/.test(char)) {
    event.preventDefault();
    return false;
  }

  return true;
};

// Manejar Enter en el input del primer conteo
const handleConteo1KeyPress = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    event.preventDefault();
    event.stopImmediatePropagation();
    if (conteo1Input.value !== null) {
      guardarConteo();
    }
  }
};

// Manejar Enter en el input del segundo conteo
const handleConteo2KeyPress = (event: KeyboardEvent) => {
  if (event.key === 'Enter') {
    event.preventDefault();
    event.stopImmediatePropagation();
    if (conteo2Input.value !== null) {
      guardarConteo();
    }
  }
};

// Atajos de teclado globales
const handleGlobalKeyPress = (event: KeyboardEvent) => {
  const activeTag = document.activeElement?.tagName;
  if (activeTag === 'INPUT' || activeTag === 'TEXTAREA' || activeTag === 'SELECT') return;

  if (event.altKey && event.key === 'f') {
    event.preventDefault();
    limpiarFormulario();
    folioInputRef.value?.focus();
  }

  if (event.altKey && event.key === 'l') {
    event.preventDefault();
    limpiarFormulario();
  }

  if (event.key === 'Escape') {
    limpiarFormulario();
    folioInputRef.value?.focus();
  }
};

// Computed para determinar si el primer conteo debe estar deshabilitado
const conteo1Deshabilitado = computed<boolean>(() => {
  // Solo deshabilitar si no hay marbete seleccionado — siempre editable
  return !marbeteActual.value;
});

// Computed para determinar si el segundo conteo debe estar deshabilitado
const conteo2Deshabilitado = computed<boolean>(() => {
  // Deshabilitar si no hay marbete seleccionado O si no hay primer conteo aún
  return !marbeteActual.value || conteo1Input.value === null;
});

onMounted(() => {
  // Inicialización asíncrona: cargar periodos y almacenes y luego marbetes si aplica
  const init = async () => {
    await loadPeriodos();
    await loadAlmacenes();

    // Enfocar input de folio al cargar
    nextTick(() => {
      folioInputRef.value?.focus();
    });
  };

  init();

  // Agregar listener para atajos de teclado globales
  window.addEventListener('keydown', handleGlobalKeyPress);
});

onUnmounted(() => {
  // Limpiar listener de atajos de teclado
  window.removeEventListener('keydown', handleGlobalKeyPress);
});
</script>

<template>
  <div class="conteo-marbetes">
    <div class="section-card">
      <div class="title-section">
        <h1 class="section-title">
          Conteo de Marbetesss
        </h1>
        <p class="subtitle">Ingreso de conteo de marbetes</p>
      </div>
      <div class="periodo-selector">
        <label for="periodoSelect">Período:</label>
        <select id="periodoSelect" v-model.number="selectedPeriodoId">
          <option :value="null" disabled>Selecciona un período</option>
          <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
            {{ formatDate(periodo.date) }} - {{ periodo.comments }}
          </option>
        </select>
      </div>
    </div>

    <!-- Información del marbete - 4 Columnas mostrando todos los 21 campos -->
    <div class="marbete-info">
      <div class="info-columns-4">
        <!-- COLUMNA 1: Identificadores -->
        <div class="info-column">
          <h4 class="column-header">Identificadores</h4>
          <div class="info-field">
            <label>Folio:</label>
            <span>{{ marbeteActual?.folio || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Period ID:</label>
            <span>{{ marbeteActual?.periodId || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Warehouse ID:</label>
            <span>{{ marbeteActual?.warehouseId || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Clave Almacén:</label>
            <span>{{ marbeteActual?.claveAlmacen || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Almacén:</label>
            <span>{{ marbeteActual?.nombreAlmacen || '-' }}</span>
          </div>
        </div>

        <!-- COLUMNA 2: Información del Producto -->
        <div class="info-column">
          <h4 class="column-header">Producto</h4>
          <div class="info-field">
            <label>Clave Producto:</label>
            <span>{{ marbeteActual?.claveProducto || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Descripción:</label>
            <span>{{ marbeteActual?.descripcionProducto || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Unidad Medida:</label>
            <span>{{ marbeteActual?.unidadMedida || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Existencias ({{ marbeteActual?.existQtyUnidad || '-' }}):</label>
            <span class="exist-qty">{{ marbeteActual ? formatNumber(Math.round(marbeteActual.existQty * 100) / 100) : '-' }}</span>
          </div>
        </div>

        <!-- COLUMNA 3: Conteos y Usuarios -->
        <div class="info-column">
          <h4 class="column-header">Conteos</h4>
          <div class="info-field">
            <label>Conteo 1:</label>
            <span>{{ marbeteActual ? formatNumber(marbeteActual.conteo1) : '-' }}</span>
          </div>
          <div class="info-field">
            <label>Usuario C1:</label>
            <span>{{ marbeteActual?.conteo1UsuarioNombre || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Comentario C1:</label>
            <span class="comment-text">{{ marbeteActual?.conteo1Comentario || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Conteo 2:</label>
            <span>{{ marbeteActual ? formatNumber(marbeteActual.conteo2) : '-' }}</span>
          </div>
          <div class="info-field">
            <label>Usuario C2:</label>
            <span>{{ marbeteActual?.conteo2UsuarioNombre || '-' }}</span>
          </div>
        </div>

        <!-- COLUMNA 4: Estado y Banderas -->
        <div class="info-column">
          <h4 class="column-header">Estado</h4>
          <div class="info-field">
            <label>Comentario C2:</label>
            <span class="comment-text">{{ marbeteActual?.conteo2Comentario || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Diferencia:</label>
            <div class="diferencia-display" :class="getDiferenciaClass()">
              <span v-if="conteo1Input !== null && conteo2Input !== null && diferenciaConteos !== null">
                {{ diferenciaConteos > 0 ? '+' : '' }}{{ diferenciaConteos }}
              </span>
              <span v-else>{{ marbeteActual?.diferencia !== null ? marbeteActual?.diferencia : '-' }}</span>
            </div>
          </div>
          <div class="info-field">
            <label>Estado:</label>
            <span :class="['estado-badge', marbeteActual?.estado?.toLowerCase()]">
              {{ marbeteActual?.estado || '-' }}
            </span>
          </div>
          <div class="info-field">
            <label>Impreso:</label>
            <span :class="['badge', marbeteActual?.impreso ? 'badge-success' : 'badge-warning']">
              {{ marbeteActual?.impreso ? 'Sí' : 'No' }}
            </span>
          </div>
          <div class="info-field">
            <label>Cancelado:</label>
            <span :class="['badge', marbeteActual?.cancelado ? 'badge-danger' : 'badge-success']">
              {{ marbeteActual?.cancelado ? 'Sí' : 'No' }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Buscar folio -->
    <div class="search-section">
      <label for="folioInput">Buscar Folio:</label>
      <div class="search-row">
        <input
            id="folioInput"
            ref="folioInputRef"
            type="text"
            v-model="folioInput"
            placeholder="Ingresa folio"
            tabindex="1"
            @keydown.enter.stop.prevent="buscarMarbetePorFolio"
        />
        <button @click="buscarMarbetePorFolio" tabindex="-1">Buscar</button>
      </div>
    </div>

    <!-- Captura de conteos -->
    <div class="conteos-section">
      <div class="conteo-field">
        <label for="conteo1Input">
          Primer Conteo
          <span v-if="marbeteActual?.conteo1 !== null && marbeteActual?.conteo1 !== undefined" class="note">(Actualizar)</span>
        </label>
        <input
            id="conteo1Input"
            ref="conteo1InputRef"
            type="text"
            v-model="conteo1Display"
            placeholder="0"
            tabindex="-1"
            :disabled="conteo1Deshabilitado"
            @input="(e) => handleConteoInput(e, 'conteo1')"
            @keydown="preventNonNumeric"
            @keydown.enter.stop.prevent="handleConteo1KeyPress"
        />
      </div>

      <div class="conteo-field">
        <label for="conteo2Input">
          Segundo Conteo
          <span v-if="conteo2Deshabilitado" class="note">(Requiere 1er conteo)</span>
        </label>
        <input
            id="conteo2Input"
            ref="conteo2InputRef"
            type="text"
            v-model="conteo2Display"
            placeholder="0"
            tabindex="-1"
            :disabled="conteo2Deshabilitado"
            @input="(e) => handleConteoInput(e, 'conteo2')"
            @keydown="preventNonNumeric"
            @keydown.enter.stop.prevent="handleConteo2KeyPress"
        />
      </div>

      <div class="conteo-field">
        <label>Diferencia</label>
        <div class="diferencia">
          <span v-if="conteo1Input !== null && conteo2Input !== null && diferenciaConteos !== null">
            {{ diferenciaConteos > 0 ? '+' : '' }}{{ diferenciaConteos }}
          </span>
          <span v-else>-</span>
        </div>
      </div>
    </div>

    <!-- Comentario (Opcional) -->
    <div class="comentario-section">
      <label for="comentarioInput">📝 Comentario (Opcional)</label>
      <textarea
          id="comentarioInput"
          v-model="comentarioInput"
          placeholder="Ingresa un comentario (máx. 600 caracteres)"
          maxlength="600"
          rows="3"
      />
      <span class="char-count">{{ comentarioInput.length }}/600</span>
    </div>

    <!-- Acciones -->
    <div class="actions">
      <button @click="guardarConteo" tabindex="-1" :disabled="!marbeteActual">Guardar</button>
      <button @click="limpiarFormulario" tabindex="-1">Limpiar</button>
      <button @click="cancelarMarbete" tabindex="-1" :disabled="!marbeteActual" class="btn-cancel">Cancelar Marbete</button>
    </div>
  </div>
</template>

<style scoped>
/* Layout principal */
.conteo-marbetes {
  padding: 5px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* Header */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ddd;
}

.header h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.periodo-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.periodo-selector label {
  font-weight: 500;
  color: #555;
}

.periodo-selector select {
  padding: 8px 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
  background: white;
}

.periodo-selector select:focus {
  outline: none;
  border-color: #666;
}

/* Información del marbete */
.marbete-info {
  background: #f8f8f8;
  padding: 15px;
  margin-bottom: 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.info-columns {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.info-columns-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.info-column {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.column-header {
  margin: 0 0 10px 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-color);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--primary-color);
}

.info-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-field label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.info-field span {
  font-size: 14px;
  color: #333;
  font-weight: 400;
}

/* Estado badge */
.estado-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.estado-badge.impreso {
  background: #e5e7eb;
  color: #374151;
}

.estado-badge.generado {
  background: #fef3c7;
  color: #92400e;
}

.estado-badge.cancelado {
  background: #fee2e2;
  color: #991b1b;
}

.estado-badge.pendiente {
  background: #dbeafe;
  color: #1e40af;
}

/* General badges */
.badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.badge-success {
  background: #d1fae5;
  color: #065f46;
}

.badge-warning {
  background: #fef3c7;
  color: #92400e;
}

.badge-danger {
  background: #fee2e2;
  color: #991b1b;
}

/* Existencias cantidad */
.exist-qty {
  font-weight: 600;
  color: #2e7d32;
  font-size: 15px;
}

/* Comentarios */
.comment-text {
  font-size: 13px;
  color: #555;
  font-style: italic;
  white-space: normal;
  word-wrap: break-word;
  max-height: 60px;
  overflow-y: auto;
}

.diferencia-display {
  padding: 10px;
  border-radius: 4px;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.diferencia-neutral {
  background: #f5f5f5;
  color: #999;
  border: 1px solid #ddd;
}

.diferencia-igual {
  background: #c8e6c9;
  color: #2e7d32;
  border: 1px solid #81c784;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(46, 125, 50, 0.2);
}

.diferencia-positiva {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffc107;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(255, 193, 7, 0.3);
}

.diferencia-negativa {
  background: #ffcdd2;
  color: #c62828;
  border: 1px solid #f44336;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(244, 67, 54, 0.2);
}

/* Búsqueda */
.search-section {
  margin-bottom: 20px;
}

.search-section label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
}

.search-row {
  display: flex;
  gap: 10px;
}

.search-row input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 14px;
}

.search-row input:focus {
  outline: none;
  border-color: #666;
}

.search-row button {
  padding: 10px 24px;
  border: 1px solid #333;
  background: #333;
  color: white;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
}

.search-row button:hover {
  background: #555;
  border-color: #555;
}

/* Conteos */
.conteos-section {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.conteo-field {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.conteo-field label {
  margin-bottom: 8px;
  font-weight: 500;
  color: #555;
  font-size: 14px;
}

.note {
  font-size: 11px;
  color: #888;
  font-weight: 400;
}

.conteo-field input {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 16px;
  text-align: center;
  font-weight: 500;
}

.conteo-field input:focus {
  outline: none;
  border-color: #666;
}

.conteo-field input:disabled {
  background: #f0f0f0;
  color: #999;
  cursor: not-allowed;
}

.diferencia {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  background: #fafafa;
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Comentario */
.comentario-section {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.comentario-section label {
  font-weight: 500;
  color: #555;
  font-size: 14px;
}

.comentario-section textarea {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-family: Arial, sans-serif;
  font-size: 13px;
  resize: vertical;
  min-height: 80px;
}

.comentario-section textarea:focus {
  outline: none;
  border-color: #2196F3;
  box-shadow: 0 0 4px rgba(33, 150, 243, 0.3);
}

.char-count {
  font-size: 11px;
  color: #999;
  text-align: right;
}

/* Acciones */
.actions {
  display: flex;
  gap: 10px;
}

.actions button {
  padding: 12px 24px;
  border: 1px solid #333;
  background: white;
  color: #333;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
}

.actions button:hover:not(:disabled) {
  background: #f5f5f5;
}

.actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.actions button:first-child {
  background: #333;
  color: white;
}

.actions button:first-child:hover:not(:disabled) {
  background: #555;
}

.btn-cancel {
  background: #d32f2f !important;
  color: white !important;
  border-color: #d32f2f !important;
}

.btn-cancel:hover:not(:disabled) {
  background: #b71c1c !important;
  border-color: #b71c1c !important;
}

/* Responsive */
@media (max-width: 1400px) {
  .info-columns-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .info-columns {
    grid-template-columns: 1fr;
  }

  .info-columns-4 {
    grid-template-columns: 1fr;
  }

  .conteos-section {
    flex-direction: column;
  }

  .actions {
    flex-direction: column;
  }

  .actions button {
    width: 100%;
  }
}
</style>

