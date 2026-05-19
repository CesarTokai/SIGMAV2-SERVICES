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
  id: number;
  folio: number;
  claveProducto: string;
  producto: string;
  claveAlmacen: string;
  almacen: string;
  existenciasEsperadas: number;
  unidadMedida: string;
  impreso: boolean;
  existQty: number;
  existQtyUnidad: string;
  conteo1: number | null;
  conteo2: number | null;
  diferencia: number | null;
  estado: string;
  cancelado: boolean;
  mensaje?: string;
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

// Ref para comentarios (opcional)
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
  if (diferenciaConteos.value === null) {
    return 'diferencia-neutral';
  }

  if (diferenciaConteos.value === 0) {
    return 'diferencia-igual';
  } else if (diferenciaConteos.value > 0) {
    return 'diferencia-positiva';
  } else {
    return 'diferencia-negativa';
  }
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
    if (almacenes.value.length > 0 && !selectedAlmacen.value) {
      const firstAlmacen = almacenes.value[0];
      if (firstAlmacen) {
        selectedAlmacen.value = firstAlmacen;
        selectedAlmacenId.value = firstAlmacen.id;
      }
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

  // Validar que tengamos período y almacén seleccionados
  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    ToastError('Selecciona período y almacén', 'Para buscar por folio primero selecciona un período y almacén');
    return;
  }

  try {
    LoadAlert(true);

    console.log('🔍 Buscando folio específico:', { folio: raw, periodId: selectedPeriodo.value.id, warehouseId: selectedAlmacen.value.id });

    // Usar POST /labels/for-count para obtener información completa
    const body = {
      folio: parseInt(raw),
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id
    };

    const response = await axiosConfiguration.doPost('/labels/for-count', body);

    LoadAlert(false);

    if (!response.data) {
      ToastError('Sin resultados', `No se encontró el folio ${raw} en el período seleccionado`);
      marbeteActual.value = null;
      resultadosBusqueda.value = [];
      return;
    }

    // 🔍 DEBUG: Ver qué datos devuelve la API
    console.log('📡 Respuesta completa de la API:', response.data);
    console.log('🔎 Campos disponibles:', Object.keys(response.data));

    // Mapear la respuesta al formato esperado
    const marbete: MarbeteConteo = {
      id: response.data.id ?? response.data.folio ?? 0,
      folio: response.data.folio ?? parseInt(raw),
      claveProducto: String(response.data.claveProducto ?? response.data.productCode ?? '').trim(),
      producto: String(response.data.descripcionProducto ?? response.data.productName ?? response.data.producto ?? '').trim(),
      claveAlmacen: String(response.data.claveAlmacen ?? response.data.warehouseKey ?? '').trim(),
      almacen: String(response.data.nombreAlmacen ?? response.data.warehouseName ?? response.data.almacen ?? '').trim(),
      existenciasEsperadas: Number(response.data.existenciasEsperadas ?? response.data.expectedStock ?? response.data.existencias ?? 0),
      unidadMedida: String(response.data.unidadMedida ?? response.data.unit ?? '').trim(),
      impreso: Boolean(response.data.impreso ?? response.data.isPrinted ?? false),
      existQty: Number(response.data.existQty ?? response.data.currentStock ?? 0),
      existQtyUnidad: String(response.data.existQtyUnidad ?? response.data.unit ?? '').trim(),
      conteo1: response.data.conteo1 ?? response.data.count1 ?? null,
      conteo2: response.data.conteo2 ?? response.data.count2 ?? null,
      diferencia: response.data.diferencia ?? response.data.difference ?? null,
      estado: response.data.estado ?? response.data.status ?? response.data.mensaje ?? 'Pendiente',
      cancelado: Boolean(response.data.cancelado ?? response.data.isCanceled ?? false),
      mensaje: response.data.mensaje ?? response.data.message ?? undefined
    };

    if (marbete.cancelado) {
      ToastError('Marbete Cancelado', marbete.mensaje || 'Este marbete ha sido cancelado y no puede ser usado');
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
      ToastSuccess('Marbete encontrado', `Folio ${marbete.folio} - ${marbete.producto}`);
    }

    // Seleccionar directamente el marbete encontrado
    seleccionarMarbete(marbete);

  } catch (error: any) {
    LoadAlert(false);
    console.error('Error al buscar folio:', error);

    // ✅ MEJORADO: Usar función centralizada de extracción de errores
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
    // 300ms para que el ENTER del folio esté completamente muerto
    setTimeout(() => {
      conteo2InputRef.value?.focus();
      conteo2InputRef.value?.select();
    }, 300);
  } else {
    conteo1Input.value = null;
    conteo2Input.value = null;
    conteo1Display.value = '';
    conteo2Display.value = '';
    // 300ms para que el ENTER del folio esté completamente muerto
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

  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    ToastError('Error', 'Período y almacén son requeridos');
    return;
  }

  const necesitaConteo1 = marbeteActual.value.conteo1 === null && conteo1Input.value !== null;
  const puedeActualizarConteo1 = marbeteActual.value.conteo1 !== null && conteo1Input.value !== null && conteo1Input.value !== marbeteActual.value.conteo1;
  const puedeGuardarConteo2 = conteo2Input.value !== null;

  // ✅ SIN VALIDACIÓN RESTRICTIVA: Puede guardar C1 o C2 directamente sin dependencias
  if (!necesitaConteo1 && !puedeActualizarConteo1 && !puedeGuardarConteo2) {
    ToastError('Sin datos', 'Ingresa un conteo para guardar');
    return;
  }

  // ═════════════════════════════════════════════════════════════
  // GUARDAR/ACTUALIZAR C1
  // ═════════════════════════════════════════════════════════════
  if (necesitaConteo1 || puedeActualizarConteo1) {
    const folioAGuardar = marbeteActual.value.folio;
    const valorAGuardar = conteo1Input.value;
    const esActualizacion = marbeteActual.value.conteo1 !== null;

    // ✅ CAPTURAR COMENTARIO ANTES DE LIMPIAR
    const comentarioAGuardar = comentarioInput.value?.trim() || null;

    limpiarFormulario();
    setTimeout(() => {
      folioInputRef.value?.focus();
      folioInputRef.value?.select();
    }, 50);

    try {
      LoadAlert(true);

      // ✅ Body DEBE incluir: folio, periodId, warehouseId, countedValue
      const body: any = {
        folio: folioAGuardar,
        periodId: selectedPeriodo.value?.id,
        warehouseId: selectedAlmacen.value?.id,
        countedValue: valorAGuardar
      };

      // ✅ AGREGAR comentario SOLO si está escrito (evitar enviar "null" como string)
      const commentTrimmed = comentarioAGuardar;
      if (commentTrimmed && commentTrimmed !== 'null' && commentTrimmed !== 'undefined') {
        body.comment = commentTrimmed;
      }
      // Si no hay comentario, NO se incluye el campo - backend lo recibe como null/undefined

      // Validación de campos requeridos
      if (!body.folio || !body.periodId || !body.warehouseId || body.countedValue === null) {
        throw new Error(`❌ Datos incompletos. Folio: ${body.folio}, Período: ${body.periodId}, Almacén: ${body.warehouseId}, Valor: ${body.countedValue}`);
      }

      console.log('📤 Enviando C1:', { esActualizacion, body });

      if (esActualizacion) {
        // Actualizar C1 con PUT /labels/counts/c1/update
        await axiosConfiguration.doPut('/labels/counts/c1/update', body);
      } else {
        // Guardar C1 nuevo con POST /labels/counts/c1
        await axiosConfiguration.doPost('/labels/counts/c1', body);
      }

      LoadAlert(false);
      ToastSuccess(
          esActualizacion ? '✅ Primer conteo actualizado' : '✅ Primer conteo guardado',
          `Folio ${folioAGuardar}: ${valorAGuardar} unidades`
      );
    } catch (error: any) {
      LoadAlert(false);
      console.error('❌ Error al guardar C1:', error);

      const mensajeError = error.message ||
          error.originalMessage ||
          'No se pudo guardar el conteo. Verifica los datos.';

      ToastError('❌ Error al guardar conteo', mensajeError);
    }
    return;
  }

  // ═════════════════════════════════════════════════════════════
  // GUARDAR/ACTUALIZAR C2
  // ═════════════════════════════════════════════════════════════
  if (puedeGuardarConteo2) {
    const folioAGuardar = marbeteActual.value.folio;
    const valorAGuardar = conteo2Input.value;
    const esActualizacion = marbeteActual.value.conteo2 !== null;

    // ✅ CAPTURAR COMENTARIO ANTES DE LIMPIAR
    const comentarioAGuardar = comentarioInput.value?.trim() || null;

    limpiarFormulario();
    setTimeout(() => {
      folioInputRef.value?.focus();
      folioInputRef.value?.select();
    }, 50);

    try {
      LoadAlert(true);

      // ✅ Body DEBE incluir: folio, periodId, warehouseId, countedValue
      const body: any = {
        folio: folioAGuardar,
        periodId: selectedPeriodo.value?.id,
        warehouseId: selectedAlmacen.value?.id,
        countedValue: valorAGuardar
      };

      // ✅ AGREGAR comentario SOLO si está escrito (evitar enviar "null" como string)
      const commentTrimmed = comentarioAGuardar;
      if (commentTrimmed && commentTrimmed !== 'null' && commentTrimmed !== 'undefined') {
        body.comment = commentTrimmed;
      }
      // Si no hay comentario, NO se incluye el campo - backend lo recibe como null/undefined

      // Validación de campos requeridos
      if (!body.folio || !body.periodId || !body.warehouseId || body.countedValue === null) {
        throw new Error(`❌ Datos incompletos. Folio: ${body.folio}, Período: ${body.periodId}, Almacén: ${body.warehouseId}, Valor: ${body.countedValue}`);
      }

      console.log('📤 Enviando C2:', { esActualizacion, body });

      if (esActualizacion) {
        // Actualizar C2 con PUT /labels/counts/c2/update
        await axiosConfiguration.doPut('/labels/counts/c2/update', body);
      } else {
        // Guardar C2 nuevo con POST /labels/counts/c2
        await axiosConfiguration.doPost('/labels/counts/c2', body);
      }

      LoadAlert(false);
      ToastSuccess(
          esActualizacion ? '✅ Segundo conteo actualizado' : '✅ Segundo conteo guardado',
          `Folio ${folioAGuardar}: ${valorAGuardar} unidades`
      );
    } catch (error: any) {
      LoadAlert(false);
      console.error('❌ Error al guardar C2:', error);

      const mensajeError = error.message ||
          error.originalMessage ||
          'No se pudo guardar el conteo. Verifica los datos.';

      ToastError('❌ Error al guardar conteo', mensajeError);
    }
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
        <p><strong>Producto:</strong> ${marbeteActual.value.producto}</p>
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
      ToastSuccess('✅ Folio cancelado', `El folio ${marbeteActual.value.folio} fue cancelado correctamente.`);
      limpiarFormulario();

      // Enfocar nuevamente en el buscador
      nextTick(() => {
        folioInputRef.value?.focus();
      });
    } catch (error: any) {
      LoadAlert(false);
      console.error('❌ Error al cancelar folio:', error);

      const mensajeError = error.message ||
          error.originalMessage ||
          'No se pudo cancelar el folio. Intenta de nuevo.';

      ToastError('❌ Error al cancelar folio', mensajeError);
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
  // No interferir si el foco está en algún input
  const activeTag = document.activeElement?.tagName;
  if (activeTag === 'INPUT' || activeTag === 'TEXTAREA' || activeTag === 'SELECT') return;

  // Alt + F: Focus en folio para buscar nuevo marbete
  if (event.altKey && event.key === 'f') {
    event.preventDefault();
    limpiarFormulario();
    folioInputRef.value?.focus();
  }

  // Alt + L: Limpiar formulario
  if (event.altKey && event.key === 'l') {
    event.preventDefault();
    limpiarFormulario();
  }

  // Escape: Limpiar formulario
  if (event.key === 'Escape') {
    limpiarFormulario();
    folioInputRef.value?.focus();
  }
};

// Computed para determinar si el primer conteo debe estar deshabilitado
const conteo1Deshabilitado = computed<boolean>(() => {
  // Deshabilitar solo si no hay marbete seleccionado
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
          Conteo de Marbetes
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

    <!-- Información del marbete -->
    <div class="marbete-info">
      <div class="info-columns">
        <!-- Columna 1: Existencias, Conteo 1, Conteo 2 -->
        <div class="info-column">
          <div class="info-field">
            <label>Existencias ({{ marbeteActual?.existQtyUnidad || '-' }}):</label>
            <span class="exist-qty">{{ marbeteActual ? formatNumber(Math.round(marbeteActual.existQty * 100) / 100) : '-' }}</span>
          </div>
          <div class="info-field">
            <label>Conteo 1:</label>
            <span>{{ marbeteActual ? formatNumber(marbeteActual.conteo1) : '-' }}</span>
          </div>
          <div class="info-field">
            <label>Conteo 2:</label>
            <span>{{ marbeteActual ? formatNumber(marbeteActual.conteo2) : '-' }}</span>
          </div>
        </div>

        <!-- Columna 2: Clave, Estado, Diferencia -->
        <div class="info-column">
          <div class="info-field">
            <label>Clave:</label>
            <span>{{ marbeteActual?.claveProducto || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Estado:</label>
            <span :class="['estado-badge', marbeteActual?.estado?.toLowerCase()]">
              {{ marbeteActual?.estado || '-' }}
            </span>
          </div>
          <div class="info-field">
            <label>Diferencia:</label>
            <div class="diferencia-display" :class="getDiferenciaClass()">
              <span v-if="conteo1Input !== null && conteo2Input !== null && diferenciaConteos !== null">
                {{ diferenciaConteos > 0 ? '+' : '' }}{{ diferenciaConteos }}
              </span>
              <span v-else>-</span>
            </div>
          </div>
        </div>

        <!-- Columna 3: Almacén, Producto -->
        <div class="info-column">
          <div class="info-field">
            <label>Almacén:</label>
            <span>{{ marbeteActual?.almacen || '-' }}</span>
          </div>
          <div class="info-field">
            <label>Producto:</label>
            <span>{{ marbeteActual?.producto || '-' }}</span>
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
          <span v-if="marbeteActual?.conteo1 && conteo1Input === marbeteActual.conteo1" class="note">(Registrado)</span>
          <span v-else-if="marbeteActual?.conteo1 && conteo1Input !== marbeteActual.conteo1" class="note">(Modificado)</span>
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

.section-card {
  background: white;
  padding: 20px;
  margin-bottom: 20px;
  border: 1px solid #ddd;
  border-radius: 4px;
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

.info-row {
  display: grid;
  grid-template-columns: repeat(3, 3fr);
  gap: 15px;
  margin-bottom: 10px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-columns {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.info-column {
  display: flex;
  flex-direction: column;
  gap: 15px;
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
  background: #e3f2fd;
  color: #1976d2;
}

.estado-badge.generado {
  background: #fff3e0;
  color: #f57c00;
}

.estado-badge.cancelado {
  background: #ffebee;
  color: #d32f2f;
}

.estado-badge.pendiente {
  background: #f3e5f5;
  color: #7b1fa2;
}

/* Existencias cantidad */
.exist-qty {
  font-weight: 600;
  color: #2e7d32;
  font-size: 15px;
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
@media (max-width: 768px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .info-row {
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


