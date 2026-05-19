<script setup lang="ts">
import {ref, onMounted, watch} from 'vue';
import SearchBar from '@/components/SearchBar.vue';
import TooltipHelp from '@/components/TooltipHelp.vue';
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
  almacenname: string;
  activo: boolean;
}

interface Marbete {
  productId: number;
  foliosSolicitados: number;
  foliosExistentes: number;
  claveProducto: string;
  producto: string;
  claveAlmacen: string;
  nombreAlmacen: string;
  estado: string;
  existencias: number;
  isLocked?: boolean;           // ← Bloquear si ya tiene folios ingresados
  selected?: boolean;            // ← Para seleccionar productos a generar
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
const marbetes = ref<Marbete[]>([]);
const filteredMarbetes = ref<Marbete[]>([]);
const searchQuery = ref('');
const loading = ref(false);

// ============================================
// FASE 1 - MEJORA: Loading states específicos
// ============================================
const loadingStates = ref({
  generating: false,
  loading: false,
  saving: false,
  deleting: false
});

// ============================================
// FASE 4 - Búsqueda y ordenamiento MEJORADO
// ============================================
const sortBy = ref<string>('claveProducto');
const sortDirection = ref<'ASC' | 'DESC'>('ASC');
const debouncedSearch = ref<string>('');
let searchDebounceTimeout: number | null = null;

// Función normalizadora para búsqueda flexible
const normalizeSearchText = (text: string): string => {
  return text
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ') // Reemplazar múltiples espacios por uno
    .split(' ') // Dividir por espacios
    .filter(word => word.length > 0) // Eliminar palabras vacías
    .join('|'); // Unir con | para buscar cualquiera
};

// Función para verificar si un texto coincide con la búsqueda
const matchesSearch = (text: string, searchPattern: string): boolean => {
  if (!searchPattern || searchPattern.length === 0) return true;

  // Normalizar: minúsculas, espacios múltiples, acentos, caracteres especiales
  const normalizeText = (str: string): string => {
    return str
      .toLowerCase()
      .trim()
      .replace(/\s+/g, '') // Eliminar espacios
      .normalize('NFD') // Descomponer acentos
      .replace(/[\u0300-\u036f]/g, '') // Eliminar marcas diacríticas
      .replace(/[^\w]/g, ''); // Eliminar caracteres especiales excepto letras/números
  };

  const normalizedText = normalizeText(text);
  const patterns = searchPattern
    .split('|')
    .map(p => normalizeText(p))
    .filter(p => p.length > 0);

  // Busca CUALQUIER patrón que coincida (OR)
  return patterns.some(pattern => normalizedText.includes(pattern));
};

// Estado para paginación
const page = ref(0);
const pageSize = ref(100);
const totalPages = ref(1);
const totalElements = ref(0);

// ============================================
// NUEVO: Estado para modal de resumen y marbetes cancelados
// ============================================
interface ResumenGeneracion {
  totalGenerados: number;
  generadosConExistencias: number;
  generadosSinExistencias: number;
  primerFolio: number;
  ultimoFolio: number;
  mensaje: string;
}

interface MarbeteCancelado {
  folio: number;
  producto: string;
  claveProducto: string;
  almacen: string;
  existencias: number;
  estado: string;
  fechaGeneracion: string;
  notas?: string;
  nuevaExistencia?: number;
}

const showModalResumen = ref(false);
const resumenGeneracion = ref<ResumenGeneracion | null>(null);
const marbetesCancelados = ref<MarbeteCancelado[]>([]);
const loadingCancelados = ref(false);
const mostrarTablaCancelados = ref(false);

// Función para verificar si un marbete puede ser cancelado
const puedeSerCancelado = (marbete: MarbeteCancelado): boolean => {
  // Los marbetes sin existencias NO se deben cancelar
  return marbete.existencias > 0;
};

// ============================================
// FASE 1 - MEJORA: Manejo de errores específicos
// ============================================
const handleAPIError = (error: any, contexto: string = 'operación'): string => {
  const errorMessages: Record<string, string> = {
    'PERIOD_CLOSED': 'El período está cerrado. No se pueden realizar cambios. Contacte al administrador.',
    'PERIOD_LOCKED': 'El período está bloqueado. No se permiten modificaciones.',
    'NO_STOCK': 'El producto no tiene existencias registradas en el almacén.',
    'INVALID_STATE': 'El marbete no está en estado válido para esta operación.',
    'LABEL_NOT_FOUND': 'El folio especificado no fue encontrado en el sistema.',
    'DUPLICATE_COUNT': 'Este conteo ya fue registrado anteriormente.',
    'COUNT_SEQUENCE_ERROR': 'Error en la secuencia de conteos. Debe registrar C1 antes de C2.',
    'LABEL_ALREADY_CANCELLED': 'Este marbete ya está cancelado.',
    'CATALOG_NOT_LOADED': 'Debe importar los catálogos de multialmacén antes de continuar.',
    'INVALID_QUANTITY': 'La cantidad especificada no es válida.',
    'WAREHOUSE_NOT_FOUND': 'El almacén seleccionado no existe.',
    'PRODUCT_NOT_FOUND': 'El producto especificado no existe.',
    'PERMISSION_DENIED': 'No tiene permisos para realizar esta acción.'
  };

  let mensaje = `Error al realizar ${contexto}`;
  let detalles = '';

  if (error?.response?.data) {
    const errorData = error.response.data;
    const errorCode = errorData.code || errorData.error || errorData.type;

    // Buscar mensaje específico por código
    if (errorCode && errorMessages[errorCode]) {
      mensaje = errorMessages[errorCode];
    } else if (errorData.message) {
      mensaje = errorData.message;
    } else if (typeof errorData === 'string') {
      mensaje = errorData;
    }

    // Manejar errores de validación de campos
    if (errorData.fieldErrors) {
      const errores = Object.entries(errorData.fieldErrors)
        .map(([field, msg]) => `• ${field}: ${msg}`)
        .join('\n');
      detalles = `\n\nDetalles:\n${errores}`;
    }
  } else if (error?.message) {
    mensaje = error.message;
  }
  return mensaje + detalles;
};

// Cargar períodos disponibles
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
      }
    } else if (periodos.value.length > 0 && !selectedPeriodo.value) {
      // Si no hay periodo guardado, usar el primero
      selectedPeriodo.value = periodos.value[0] || null;
      selectedPeriodoId.value = selectedPeriodo.value ? selectedPeriodo.value.id : null;
    }
  } catch (error) {
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};

// Cargar almacenes disponibles
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
      almacenname: String(item.nameWarehouse || ''),
      activo: !item.deleted
    }));
    // Seleccionar el primer almacén por defecto
    if (almacenes.value.length > 0 && !selectedAlmacen.value) {
      const firstAlmacen = almacenes.value[0];
      if (firstAlmacen) {
        selectedAlmacen.value = firstAlmacen;
        selectedAlmacenId.value = firstAlmacen.id;
      }
    }
  } catch (error) {
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

// Cargar marbetes
const loadMarbetes = async () => {
  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    marbetes.value = [];
    filteredMarbetes.value = [];
    loading.value = false;
    totalPages.value = 1;
    totalElements.value = 0;
    return;
  }


  try {
    loading.value = true;
    loadingStates.value.loading = true;
    LoadAlert(true);
    // Construir body para la nueva API
    const body: any = {
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id,
      page: page.value,
      size: pageSize.value,
      searchText: debouncedSearch.value || null, // FASE 4: Búsqueda server-side
      sortBy: sortBy.value,                       // FASE 4: Ordenamiento
      sortDirection: sortDirection.value          // FASE 4: Dirección
    };

    // Llamada a la nueva API
    const response = await axiosConfiguration.doPost('/labels/summary', body);

    // Mapear respuesta
    const content = response.data.content || response.data.data || response.data || [];

    const mapItem = (item: any) => {
      const requestedFolios = Number(item.requestedFolios ?? item.foliosSolicitados ?? 0);

      // Calcular foliosExistentes: Si el backend devuelve 0 pero hay array folios, contar array
      let existingFolios = item.foliosExistentes ?? 0;

      // Si foliosExistentes es 0 pero hay datos en el array folios, usar la longitud del array
      if (existingFolios === 0 && item.folios && Array.isArray(item.folios)) {
        existingFolios = item.folios.length;
      }
      // O si hay primerFolio y ultimoFolio, calcular la diferencia + 1
      else if (existingFolios === 0 && item.primerFolio && item.ultimoFolio) {
        existingFolios = Math.max(0, item.ultimoFolio - item.primerFolio + 1);
      }
      // Fallback a otras variantes
      else if (existingFolios === 0) {
        existingFolios = item.generatedFolios ??
                         item.foliosGenerados ??
                         item.folio_count ??
                         item.labelCount ??
                         0;
      }

      return {
        productId: Number(item.productId ?? item.id ?? 0),
        foliosSolicitados: Number.isFinite(requestedFolios) ? requestedFolios : 0,
        foliosExistentes: Number.isFinite(existingFolios) ? existingFolios : 0,
         claveProducto: String(item.productCode ?? item.claveProducto ?? '').trim(),
         producto: String(item.productName ?? item.nombreProducto ?? item.producto ?? '').trim(),
         claveAlmacen: String(item.warehouseKey ?? item.claveAlmacen ?? '').trim(),
         nombreAlmacen: String(item.warehouseName ?? item.nombreAlmacen ?? '-').trim(),
         estado: String(item.status ?? item.estado ?? '').trim(),
         existencias: Number(item.stock ?? item.existencias ?? 0),
         isLocked: Number.isFinite(requestedFolios) && requestedFolios > 0  // Bloquear si ya tiene folios
       };
    };

    marbetes.value = Array.isArray(content) ? content.map(mapItem) : [];

    // Aplicar búsqueda actual si existe
    if (searchQuery.value) {
      const normalizedQuery = normalizeSearchText(searchQuery.value);
      filteredMarbetes.value = marbetes.value.filter(item => {
        return (
          matchesSearch(item.claveProducto, normalizedQuery) ||
          matchesSearch(item.producto, normalizedQuery) ||
          matchesSearch(item.claveAlmacen, normalizedQuery) ||
          matchesSearch(item.nombreAlmacen, normalizedQuery)
        );
      });
    } else {
      filteredMarbetes.value = marbetes.value;
    }

    totalPages.value = response.data.totalPages || 1;
    totalElements.value = response.data.totalElements ?? marbetes.value.length;

    LoadAlert(false);
  } catch (error) {
    LoadAlert(false);

    // ============================================
    // FASE 1 - MEJORA: Mejor manejo de errores
    // ============================================
    const errorMessage = extractErrorMessage(error);
    ToastError('Error', errorMessage);

    marbetes.value = [];
    filteredMarbetes.value = [];
    totalPages.value = 1;
    totalElements.value = 0;
  } finally {
    loading.value = false;
    loadingStates.value.loading = false;
  }
};

// ============================================
// FASE 1 - MEJORA: Validaciones previas completas
// ============================================
const validateBeforeGenerate = (): { valid: boolean; message?: string; details?: string } => {
  // Validar periodo seleccionado
  if (!selectedPeriodo.value) {
    return {
      valid: false,
      message: 'Período no seleccionado',
      details: 'Debe seleccionar un período antes de generar marbetes.'
    };
  }

  // Validar almacén seleccionado
  if (!selectedAlmacen.value) {
    return {
      valid: false,
      message: 'Almacén no seleccionado',
      details: 'Debe seleccionar un almacén antes de generar marbetes.'
    };
  }

  // Validar estado del periodo
  if (selectedPeriodo.value.state === 'CERRADO' || selectedPeriodo.value.state === 'BLOQUEADO') {
    return {
      valid: false,
      message: 'Período cerrado',
      details: `El período está en estado "${selectedPeriodo.value.state}". No se pueden generar marbetes.`
    };
  }

  // Validar que haya productos con folios solicitados
  const productsAvailable = filteredMarbetes.value.filter(m => {
    const productId = Number(m.productId);
    const foliosSolicitados = Number(m.foliosSolicitados);
    return (
      m.productId != null &&
      m.foliosSolicitados != null &&
      !isNaN(productId) &&
      !isNaN(foliosSolicitados) &&
      productId > 0 &&
      foliosSolicitados > 0 &&
      m.foliosExistentes === 0  // Solo los no generados
    );
  });

  if (productsAvailable.length === 0) {
    return {
      valid: false,
      message: 'Sin productos para generar',
      details: 'No hay productos con folios solicitados listos para generar. Ingrese cantidades en la columna "Folios Solicitados" antes de generar.'
    };
  }

  // Validar que haya productos CON FOLIOS SOLICITADOS SIN GENERAR (no importa cuándo se ingresó)
  const productsToGenerate = productsAvailable.filter(m =>
    m.foliosSolicitados > 0 && m.foliosExistentes === 0
  );

  if (productsToGenerate.length === 0) {
    return {
      valid: false,
      message: 'Sin cambios para generar',
      details: `Ingresa cantidades en los campos "Folios Solicitados" de los productos que deseas generar. Solo se pueden generar marbetes para productos sin folios asignados aún.`
    };
  }

  // Validar productos sin existencias
  const productosSinExistencias = productsToGenerate.filter(m => m.existencias <= 0);
  if (productosSinExistencias.length > 0) {
    const lista = productosSinExistencias.slice(0, 5).map(m => `• ${m.producto}`).join('\n');
    const adicionales = productosSinExistencias.length > 5 ? `\n... y ${productosSinExistencias.length - 5} más` : '';

    return {
      valid: true, // Permitir pero advertir
      message: '⚠️ Productos sin existencias',
      details: `Se generarán marbetes para ${productosSinExistencias.length} producto(s) sin existencias:\n\n${lista}${adicionales}\n\n¿Desea continuar?`
    };
  }

  return {
    valid: true,
    message: `Se generarán marbetes para ${productsToGenerate.length} producto(s)`
  };
};

// Generar marbetes
const generarMarbetes = async () => {
  // ============================================
  // FASE 1 - MEJORA: Validaciones antes de confirmar
  // ============================================
  const validation = validateBeforeGenerate();

  if (!validation.valid) {
    await Swal.fire({
      icon: 'error',
      title: validation.message,
      text: validation.details,
      confirmButtonColor: '#dc3545'
    });
    return;
  }

  // Si hay advertencias (productos sin existencias), mostrar confirmación especial
  if (validation.details?.includes('⚠️')) {
    const confirmWarning = await Swal.fire({
      icon: 'warning',
      title: validation.message,
      html: `<div style="text-align: left; white-space: pre-line;">${validation.details}</div>`,
      showCancelButton: true,
      confirmButtonText: 'Sí, continuar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#ffc107',
      cancelButtonColor: '#6c757d'
    });

    if (!confirmWarning.isConfirmed) return;
  }

  // ============================================
  // NUEVA MEJORA: Calcular detalles para mostrar en modal
  // ============================================
  const productsToGenerate = filteredMarbetes.value
    .filter(m => {
      const productId = Number(m.productId);
      const foliosSolicitados = Number(m.foliosSolicitados);
      return (
        m.productId != null &&
        m.foliosSolicitados != null &&
        !isNaN(productId) &&
        !isNaN(foliosSolicitados) &&
        productId > 0 &&
        foliosSolicitados > 0 &&
        m.foliosExistentes === 0  // ← Solo productos sin generar
      );
    });

  const totalFolios = productsToGenerate.reduce((sum, m) => sum + Number(m.foliosSolicitados), 0);

  // Construir lista de productos para mostrar en modal
  const productsList = productsToGenerate
    .map(m => `<li style="margin: 8px 0; padding: 6px; background: #f5f5f5; border-radius: 4px;">
      <strong>${m.producto}</strong> (${m.claveProducto}) → <span style="color: #28a745; font-weight: bold;">${m.foliosSolicitados} folios</span>
    </li>`)
    .join('');

  // Confirmación normal
  const result = await Swal.fire({
    title: '¿Generar Marbetes?',
    html: `
      <div style="text-align: left;">
        <p><strong>Período:</strong> ${formatDate(selectedPeriodo.value!.date)}</p>
        <p><strong>Almacén:</strong> ${selectedAlmacen.value!.almacenname}</p>
        <hr style="margin: 15px 0; border: 1px solid #eee;">

        <div style="background: #e8f5e9; padding: 12px; border-radius: 6px; margin-bottom: 15px; border-left: 4px solid #28a745;">
          <p style="margin: 0 0 8px 0;"><strong style="font-size: 16px; color: #28a745;">📊 Total a Generar</strong></p>
          <p style="margin: 0; font-size: 14px; color: #333;">
            <strong>${productsToGenerate.length}</strong> producto(s) -
            <strong style="color: #28a745;">${totalFolios}</strong> folio(s) en total
          </p>
        </div>

        <p style="margin-bottom: 10px; font-weight: 600; color: #333;">Detalle de productos:</p>
        <ul style="list-style: none; padding: 0; margin: 0;">
          ${productsList}
        </ul>
      </div>
    `,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, generar',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#28a745',
    cancelButtonColor: '#dc3545'
  });

  if (!result.isConfirmed) return;

  try {
    // ============================================
    // FASE 1 - MEJORA: Loading state específico
    // ============================================
    loadingStates.value.generating = true;
    LoadAlert(true);

     // Construir lista de productos - CON FOLIOS SOLICITADOS SIN GENERAR
     const products = filteredMarbetes.value
       .filter(m => {
         const productId = Number(m.productId);
         const foliosSolicitados = Number(m.foliosSolicitados);
         return (
           m.productId != null &&
           m.foliosSolicitados != null &&
           !isNaN(productId) &&
           !isNaN(foliosSolicitados) &&
           productId > 0 &&
           foliosSolicitados > 0 &&
           m.foliosExistentes === 0  // ← Solo productos sin generar
         );
       })
      .map(m => ({
        productId: Number(m.productId),
        labelsToGenerate: Number(m.foliosSolicitados)
      }));

    const body = {
      warehouseId: Number(selectedAlmacen.value!.id),
      periodId: Number(selectedPeriodo.value!.id),
      products
    };

    // Llamar al API
    const response = await axiosConfiguration.doPost('/labels/generate/batch', body);

    LoadAlert(false);
    loadingStates.value.generating = false;

    // Recargar datos
    await loadMarbetes();

    // ============================================
    // FASE 1 - MEJORA: Mostrar resultado detallado
    // ============================================
    const resultData = response.data;
    let successHtml = `
      <div style="text-align: left;">
        <p>✅ <strong>Marbetes generados exitosamente</strong></p>
        <hr>
        <p>📊 <strong>Total productos procesados:</strong> ${products.length}</p>
    `;

    if (resultData?.totalGenerated) {
      successHtml += `<p>🏷️ <strong>Folios generados:</strong> ${resultData.totalGenerated}</p>`;
    }

    if (resultData?.primerFolio && resultData?.ultimoFolio) {
      successHtml += `<p>📋 <strong>Rango de folios:</strong> ${resultData.primerFolio} - ${resultData.ultimoFolio}</p>`;
    }

    successHtml += '</div>';

    await Swal.fire({
      icon: 'success',
      title: '¡Éxito!',
      html: successHtml,
      confirmButtonColor: '#28a745'
    });

  } catch (error: any) {
    LoadAlert(false);
    loadingStates.value.generating = false;

    // ============================================
    // FASE 1 - MEJORA: Usar función de manejo de errores
    // ============================================
    const errorMessage = handleAPIError(error, 'la generación de marbetes');

    await Swal.fire({
      icon: 'error',
      title: 'Error al generar marbetes',
      html: `<div style="text-align: left; white-space: pre-line;">${errorMessage}</div>`,
      confirmButtonColor: '#dc3545',
      width: '600px'
    });
  }
};

// ============================================
// FASE 1 - MEJORA: Guardar folios con validaciones completas
// ============================================
const saveFoliosRequest = async (productId: number, cantidad: string | number, rowIndex: number) => {
  // Validaciones previas
  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    ToastError('Error', 'Debe seleccionar un período y almacén');
    return;
  }

  const parsedCantidad = parseInt(String(cantidad));

  // Validar que sea un número válido
  if (isNaN(parsedCantidad)) {
    ToastError('Error', 'La cantidad debe ser un número válido');
    return;
  }

  // Validar que no sea negativo
  if (parsedCantidad < 0) {
    ToastError('Error', 'La cantidad no puede ser negativa');
    return;
  }

  // Si es 0, permitir pero no mostrar mensaje de éxito
  const isClearingValue = parsedCantidad === 0;

  try {
    loadingStates.value.saving = true;

    await axiosConfiguration.doPost('/labels/request', {
      productId: productId,
      warehouseId: selectedAlmacen.value.id,
      periodId: selectedPeriodo.value.id,
      requestedLabels: parsedCantidad
    });

     // Actualizar valor local
     if (marbetes.value[rowIndex]) {
       marbetes.value[rowIndex].foliosSolicitados = parsedCantidad;
     }
     if (filteredMarbetes.value[rowIndex]) {
       filteredMarbetes.value[rowIndex].foliosSolicitados = parsedCantidad;
     }

    // Mostrar éxito solo si no es 0
    if (!isClearingValue) {
      ToastSuccess('Guardado', `${parsedCantidad} folio(s) solicitado(s)`);
    }

  } catch (error: any) {
    loadingStates.value.saving = false;

    // Usar función de manejo de errores
    const errorMessage = handleAPIError(error, 'guardar folios solicitados');
    ToastError('Error', errorMessage);

    // Restaurar valor anterior en caso de error
    const originalValue = marbetes.value[rowIndex]?.foliosSolicitados || 0;
    const input = document.querySelector(`input[value="${cantidad}"]`) as HTMLInputElement;
    if (input) {
      input.value = String(originalValue);
    }
  } finally {
    loadingStates.value.saving = false;
  }
};

// Manejar blur del input de folios solicitados
const handleFolioBlur = (event: Event, marbete: Marbete, index: number) => {
  const input = event.target as HTMLInputElement;
  const nuevoValor = input.value;
  const parsedNuevoValor = parseInt(nuevoValor);

  // Solo guardar si el valor cambió
  if (!isNaN(parsedNuevoValor) && parsedNuevoValor !== marbete.foliosSolicitados) {
    saveFoliosRequest(marbete.productId, parsedNuevoValor, index);
  }
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

// Formatear número
const formatNumber = (value: number): string => {
  return new Intl.NumberFormat('es-MX').format(value);
};

// Formatear estado
const getEstadoClass = (estado: string): string => {
  const estadoLower = estado?.toLowerCase();
  if (estadoLower?.includes('activo') || estadoLower?.includes('disponible') || estadoLower?.includes('completo')) {
    return 'badge-success';
  } else if (estadoLower?.includes('inactivo') || estadoLower?.includes('no disponible')) {
    return 'badge-danger';
  } else if (estadoLower?.includes('pendiente') || estadoLower?.includes('parcial')) {
    return 'badge-warning';
  }
  return 'badge-secondary';
};

// ============================================
// FASE 4 - Búsqueda LOCAL mejorada (sin pasar al servidor)
// ============================================
watch(searchQuery, (newQuery) => {
  // Normalizar la búsqueda
  const normalizedQuery = normalizeSearchText(newQuery);
  debouncedSearch.value = normalizedQuery;

  // Filtrar localmente SIN hacer request al servidor
  if (!marbetes.value || marbetes.value.length === 0) {
    filteredMarbetes.value = [];
    return;
  }

  filteredMarbetes.value = marbetes.value.filter(item => {
    if (!normalizedQuery || normalizedQuery.length === 0) {
      return true;
    }

    // Buscar en: código producto, nombre producto, código almacén, nombre almacén
    return (
      matchesSearch(item.claveProducto, normalizedQuery) ||
      matchesSearch(item.producto, normalizedQuery) ||
      matchesSearch(item.claveAlmacen, normalizedQuery) ||
      matchesSearch(item.nombreAlmacen, normalizedQuery)
    );
  });
});

// ============================================
// FASE 4 - Funciones de ordenamiento
// ============================================
const handleSort = (column: string) => {
  if (sortBy.value === column) {
    // Cambiar dirección
    sortDirection.value = sortDirection.value === 'ASC' ? 'DESC' : 'ASC';
  } else {
    // Nueva columna, ordenar ASC
    sortBy.value = column;
    sortDirection.value = 'ASC';
  }

  page.value = 0; // Reset a primera página
  loadMarbetes();
};

const getSortIcon = (column: string): string => {
  if (sortBy.value !== column) return '↕️';
  return sortDirection.value === 'ASC' ? '↑' : '↓';
};

// ============================================
// NUEVO: Funciones de selección de productos
// ============================================
const toggleSelectAllProducts = () => {
  const allSelected = filteredMarbetes.value.every(m => m.selected);
  filteredMarbetes.value.forEach(m => {
    m.selected = !allSelected;
  });
};

const clearAllSelections = () => {
  filteredMarbetes.value.forEach(m => {
    m.selected = false;
  });
};

// ============================================
// NUEVO: Funciones para marbetes cancelados
// ============================================

// Cargar marbetes cancelados
const loadMarbetesCancelados = async () => {
  if (!selectedPeriodo.value || !selectedAlmacen.value) {
    marbetesCancelados.value = [];
    return;
  }

  loadingCancelados.value = true;

  try {
    const response = await axiosConfiguration.doGet('/labels/cancelled', {
      periodId: selectedPeriodo.value.id,
      warehouseId: selectedAlmacen.value.id
    });

    const data = response.data || [];
    marbetesCancelados.value = Array.isArray(data) ? data.map((item: any) => ({
      folio: item.folio ?? 0,
      claveProducto: String(item.claveProducto ?? item.productCode ?? '').trim(),
      producto: String(item.producto ?? item.nombreProducto ?? '').trim(),
      claveAlmacen: String(item.claveAlmacen ?? item.warehouseKey ?? '').trim(),
      almacen: String(item.almacen ?? item.nombreAlmacen ?? '-').trim(),
      existencias: Number(item.existencias ?? item.stock ?? 0),
      estado: String(item.estado ?? 'Cancelado'),
      fechaGeneracion: item.fechaGeneracion ?? new Date().toISOString(),
      notas: item.notas ?? '',
      nuevaExistencia: 0  // Siempre inicializar en 0
    })) : [];

  } catch (error) {
    ToastError('Error', 'No se pudieron cargar los marbetes cancelados');
    marbetesCancelados.value = [];
  } finally {
    loadingCancelados.value = false;
  }
};

// Actualizar existencias de un marbete cancelado
const actualizarExistenciasMarbete = async (marbete: MarbeteCancelado) => {
  if (marbete.nuevaExistencia === undefined || marbete.nuevaExistencia < 0) {
    ToastError('Error', 'Ingresa una existencia válida (mayor o igual a 0)');
    return;
  }

  // Los marbetes sin existencias no se deben cancelar, solo reactivar
  if (!puedeSerCancelado(marbete) && marbete.nuevaExistencia === 0) {
    ToastError('No permitido', 'Los marbetes sin existencias no se pueden mantener en estado cancelado. Ingresa una existencia mayor a 0 para reactivarlo.');
    return;
  }

  const result = await Swal.fire({
    title: '¿Actualizar existencias?',
    html: `
      <p><strong>Folio:</strong> ${marbete.folio}</p>
      <p><strong>Producto:</strong> ${marbete.producto}</p>
      <p><strong>Existencias actuales:</strong> ${marbete.existencias}</p>
      <p><strong>Nuevas existencias:</strong> ${marbete.nuevaExistencia}</p>
    `,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Sí, actualizar',
    cancelButtonText: 'Cancelar',
    confirmButtonColor: '#667eea',
    cancelButtonColor: '#6c757d'
  });

  if (!result.isConfirmed) return;

  try {
    LoadAlert(true);

    const body = {
      folio: marbete.folio,
      existenciasActuales: marbete.nuevaExistencia,
      notas: marbete.notas || ''
    };

    const response = await axiosConfiguration.doPut('/labels/cancelled/update-stock', body);

    LoadAlert(false);

    const reactivado = response.data?.reactivado ?? false;

    if (reactivado) {
      ToastSuccess('Éxito', 'Existencias actualizadas y marbete reactivado correctamente');
    } else {
      ToastSuccess('Éxito', 'Existencias actualizadas correctamente');
    }

    // Recargar ambas tablas
    await loadMarbetesCancelados();
    await loadMarbetes();
  } catch (error: any) {
    LoadAlert(false);
    ToastError('Error', error?.response?.data?.message || 'No se pudieron actualizar las existencias');
  }
};

// Cerrar modal de resumen
const cerrarModalResumen = () => {
  showModalResumen.value = false;
  resumenGeneracion.value = null;
};

// Toggle mostrar tabla de cancelados
const toggleTablaCancelados = () => {
  mostrarTablaCancelados.value = !mostrarTablaCancelados.value;
  if (mostrarTablaCancelados.value) {
    loadMarbetesCancelados();
  }
};

// Watch para cambios en período
watch(selectedPeriodoId, async (newId) => {
  if (newId == null) {
    selectedPeriodo.value = null;
    marbetes.value = [];
    filteredMarbetes.value = [];
    marbetesCancelados.value = [];
    return;
  }

  const found = periodos.value.find(p => p.id === Number(newId)) || null;
  selectedPeriodo.value = found;

  // Guardar en el store cuando cambie el periodo
  if (found) {
    periodoStore.setPeriodo(found);
  }

  searchQuery.value = '';
  page.value = 0;
  await loadMarbetes();
  if (mostrarTablaCancelados.value) {
    await loadMarbetesCancelados();
  }
});

// Watch para cambios en almacén
watch(selectedAlmacenId, async (newId) => {
  if (newId == null) {
    selectedAlmacen.value = null;
    marbetes.value = [];
    filteredMarbetes.value = [];
    marbetesCancelados.value = [];
    return;
  }

  const found = almacenes.value.find(a => a.id === Number(newId)) || null;
  selectedAlmacen.value = found;

  searchQuery.value = '';
  page.value = 0;
  await loadMarbetes();
  if (mostrarTablaCancelados.value) {
    await loadMarbetesCancelados();
  }
});

// Cambiar página
const goToPage = async (newPage: number) => {
  if (newPage < 0 || newPage >= totalPages.value) return;
  page.value = newPage;
  await loadMarbetes();
};

// Cambiar tamaño de página
const changePageSize = async (event: Event) => {
  const value = Number((event.target as HTMLSelectElement).value);
  pageSize.value = value;
  page.value = 0;
  await loadMarbetes();
};

// Montar componente
onMounted(() => {
  loadPeriodos();
  loadAlmacenes();
});

// Estado para la fila activa y hover
const activeRowIndex = ref<number | null>(null);
const hoverRowIndex = ref<number | null>(null);

function highlightRow(index: number) {
  activeRowIndex.value = index;
}

function removeHighlight() {
  activeRowIndex.value = null;
}

function setHoverRow(index: number) {
  hoverRowIndex.value = index;
}

function removeHoverRow() {
  hoverRowIndex.value = null;
}


// expose for template
// eslint-disable-next-line no-undef
defineExpose({
  highlightRow,
  removeHighlight,
  setHoverRow,
  removeHoverRow,
  activeRowIndex,
  hoverRowIndex
});
</script>

<template>
  <div class="consulta-captura">
    <div class="section-card">
        <div class="title-section">
          <h1 class="section-title">
            Consulta y Captura de Marbetes
          </h1>
          <p class="subtitle">Captura de folios solicitados y consulta de marbetes generados</p>
        </div>
        <div class="filters-wrapper">
          <div class="selectors-group">
            <div class="filter-item filter-item-small">
              <label for="periodoSelect" class="form-label"><strong>Período:</strong></label>
              <select id="periodoSelect" v-model.number="selectedPeriodoId" class="form-select">
                <option :value="null" disabled>Selecciona un período</option>
                <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
                  {{ formatDate(periodo.date) }} - {{ periodo.comments }}
                </option>
              </select>
            </div>
            <div class="filter-item">
              <label for="almacenSelect" class="form-label"><strong>Almacén:</strong></label>
              <select id="almacenSelect" v-model.number="selectedAlmacenId" class="form-select">
                <option :value="null" disabled>Selecciona un almacén</option>
                <option v-for="almacen in almacenes" :key="almacen.id" :value="almacen.id">
                  {{ almacen.almacenname }}
                </option>
              </select>
            </div>
            <div v-if="selectedPeriodo && selectedAlmacen" class="filter-info">
              <div class="info-item">
                <span class="label">Estado Período:</span>
                <span :class="['badge', getEstadoClass(selectedPeriodo.state)]">{{ selectedPeriodo.state }}</span>
              </div>
              <div class="info-item">
                <span class="label">Total Registros:</span>
                <span class="value">{{ totalElements }}</span>
              </div>
            </div>
          </div>
          <div class="searchbar-group">
            <SearchBar
              placeholder="Buscar (espacios ignorados): código, producto, almacén..."
              :modelValue="searchQuery"
              @update:modelValue="searchQuery = $event"
              class="SearchBar"
            />

            <button
              class="btn btn-primary btn-generate"
              @click="generarMarbetes"
              :disabled="!selectedPeriodo || !selectedAlmacen || loadingStates.generating"
            >
              <span v-if="loadingStates.generating">Generando...</span>
              <span v-else>Generar Marbetes</span>
            </button>
          </div>
        </div>
      </div>

    </div>

    <!-- ============================================ -->
    <!-- FASE 1: Indicador de guardado -->
    <!-- ============================================ -->
    <transition name="fade">
      <div v-if="loadingStates.saving" class="saving-indicator">
        <div class="spinner-small"></div>
        <span>Guardando...</span>
      </div>
    </transition>

  <!-- Tabla de Marbetes -->
  <div :key="`marbetes-${selectedPeriodoId ?? 'none'}-${selectedAlmacenId ?? 'none'}-${page}`">
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>Cargando marbetes...</p>
    </div>

    <div v-else-if="filteredMarbetes.length > 0 && selectedPeriodo && selectedAlmacen" class="table-section">
      <div class="table-responsive">
        <table class="table">
          <thead>
          <tr>
            <th>Folios Solicitados</th>
            <th>Folios Existentes</th>
            <th class="sortable" @click="handleSort('claveProducto')">
              Clave Producto {{ getSortIcon('claveProducto') }}
            </th>
            <th class="sortable" @click="handleSort('nombreProducto')">
              Producto {{ getSortIcon('nombreProducto') }}
            </th>
            <th class="sortable" @click="handleSort('claveAlmacen')">
              Clave Almacén {{ getSortIcon('claveAlmacen') }}
            </th>
            <th class="sortable" @click="handleSort('nombreAlmacen')">
              Almacén {{ getSortIcon('nombreAlmacen') }}
            </th>
            <th class="sortable" @click="handleSort('estado')">
              Estado {{ getSortIcon('estado') }}
            </th>
            <th class="sortable" @click="handleSort('existencias')">
              Existencias {{ getSortIcon('existencias') }}
            </th>
          </tr>

          </thead>
          <tbody>
          <tr v-for="(marbete, index) in filteredMarbetes"
              :key="`${marbete.claveProducto}-${marbete.claveAlmacen}-${marbete.nombreAlmacen}-${index}`"
              :class="{
                      'active-row': activeRowIndex === index,
                      'hover-row': hoverRowIndex === index || activeRowIndex === index
                    }"
              @mouseenter="setHoverRow(index)"
              @mouseleave="removeHoverRow"
          >
            <td class="text-center">
              <input
                  type="number"
                  min="0"
                  :value="marbete.foliosSolicitados"
                  :disabled="marbete.isLocked"
                  :class="{ 'input-locked': marbete.isLocked }"
                  @focus="highlightRow(index)"
                  @blur="removeHighlight(); handleFolioBlur($event, marbete, index)"
                  style="width: 80px; text-align: center; border-radius: 6px; border: 1px solid #ccc; padding: 4px 8px;"
                  :title="marbete.isLocked ? '❌ Bloqueado: Ya tiene folios ingresados. No se puede modificar.' : '✅ Editable: Ingresa cantidad aquí'"
              />
            </td>
            <td class="text-center">
              <span class="folio-badge folio-existentes">{{ formatNumber(marbete.foliosExistentes) }}</span>
            </td>
            <td>
              <span class="product-key">{{ marbete.claveProducto }}</span>
            </td>
            <td>
              <strong>{{ marbete.producto }}</strong>
            </td>
            <td>
              <span class="warehouse-key">{{ marbete.claveAlmacen }}</span>
            </td>
            <td>{{ marbete.nombreAlmacen }}</td>
            <td>
                    <span :class="['badge', getEstadoClass(marbete.estado)]">
                      {{ marbete.estado }}
                    </span>
            </td>
            <td class="text-center">
                    <span :class="['existencias-badge', marbete.existencias <= 10 ? 'low-stock' : '']">
                      {{ formatNumber(marbete.existencias) }}
                    </span>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- Paginador -->
      <div class="pagination-section">
        <div class="pagination-info">
          Mostrando página {{ page + 1 }} de {{ totalPages }} ({{ totalElements }} registros)
        </div>
        <div class="pagination-controls">
          <button :disabled="page === 0" @click="goToPage(0)">« Primera</button>
          <button :disabled="page === 0" @click="goToPage(page - 1)">‹ Anterior</button>
          <span class="page-indicator">Página {{ page + 1 }} / {{ totalPages }}</span>
          <button :disabled="page === totalPages - 1" @click="goToPage(page + 1)">Siguiente ›</button>
          <button :disabled="page === totalPages - 1" @click="goToPage(totalPages - 1)">Última »</button>
        </div>
        <div class="pagination-size">
          <label for="pageSizeSelect">Registros por página:</label>
          <select id="pageSizeSelect" :value="pageSize" @change="changePageSize">
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
            <option :value="200">200</option>
            <option :value="500">500</option>
          </select>
        </div>
      </div>
    </div>

    <div v-else class="no-data">
      <div class="no-data-icon">📋</div>
      <p v-if="!selectedPeriodo || !selectedAlmacen">
        Por favor, selecciona un período y un almacén para ver los marbetes.
      </p>
      <p v-else-if="searchQuery">
        No se encontraron marbetes con los criterios de búsqueda.
      </p>
      <p v-else>
        No hay marbetes registrados para este período y almacén.
      </p>
    </div>
  </div>

  <!-- ============================================ -->
  <!-- NUEVO: Modal de Resumen de Generación -->
  <!-- ============================================ -->
  <div v-if="showModalResumen" class="modal-overlay">
    <div class="modal-content modal-resumen">
      <div class="modal-header">
        <h3 class="modal-title">
          <span class="icon">📊</span>
          Resumen de Generación de Marbetes
        </h3>
        <button class="close-button" @click="cerrarModalResumen">✖️</button>
      </div>
      <div class="modal-body" v-if="resumenGeneracion">
        <div class="resumen-grid">
          <div class="resumen-item resumen-total">
            <div class="resumen-icon">📦</div>
            <div class="resumen-info">
              <span class="resumen-label">Total Generados</span>
              <span class="resumen-value">{{ resumenGeneracion.totalGenerados }}</span>
            </div>
          </div>

          <div class="resumen-item resumen-success">
            <div class="resumen-icon">✅</div>
            <div class="resumen-info">
              <span class="resumen-label">Con Existencias</span>
              <span class="resumen-value">{{ resumenGeneracion.generadosConExistencias }}</span>
            </div>
          </div>

          <div class="resumen-item resumen-warning">
            <div class="resumen-icon">⚠️</div>
            <div class="resumen-info">
              <span class="resumen-label">Sin Existencias (Cancelados)</span>
              <span class="resumen-value">{{ resumenGeneracion.generadosSinExistencias }}</span>
            </div>
          </div>

          <div class="resumen-item resumen-info-folios">
            <div class="resumen-icon">🔢</div>
            <div class="resumen-info">
              <span class="resumen-label">Rango de Folios</span>
              <span class="resumen-value">{{ resumenGeneracion.primerFolio }} - {{
                  resumenGeneracion.ultimoFolio
                }}</span>
            </div>
          </div>
        </div>

        <div class="resumen-mensaje">
          <p>{{ resumenGeneracion.mensaje }}</p>
        </div>

        <div v-if="resumenGeneracion.generadosSinExistencias > 0" class="resumen-alert">
          <span class="icon">ℹ️</span>
          <p>
            Los marbetes sin existencias han sido marcados como "Cancelados".
            Puedes actualizarlos en la tabla de Marbetes Cancelados.
          </p>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn btn-primary" @click="cerrarModalResumen">Entendido</button>
      </div>
    </div>
  </div>
</template>

<style scoped>

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title .icon {
  font-size: 24px;
}


.SearchBar {
  padding: 0;
  width: 100%;
}

.SearchBar :deep(input[type="text"]) {
  height: 42px;
  padding: 8px 15px;
  font-size: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
}

.title-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}


.btn-generate {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-generate:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-generate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.filters-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  flex-wrap: wrap;
  width: 100%;
}

.selectors-group {
  display: flex;
  align-items: flex-end;
  gap: 18px;
  flex-wrap: wrap;
}

.searchbar-group {
  min-width: 320px;
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.filter-item {
  display: flex;
  flex-direction: column;
  min-width: 180px;
}

.filter-item-small {
  min-width: auto;
  flex-basis: auto;
  width: 300px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
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
  height: 42px;
}

.form-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.filter-info {
  display: flex;
  flex-direction: row;
  gap: 18px;
  margin-left: 18px;
  align-items: center;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  font-size: 13px;
  color: #6c757d;
  font-weight: 500;
}

.info-item .value {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

/* Buscador */
.search-section {
  margin-bottom: 20px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Nuevo: marco diferenciado para el buscador */
.search-section-bordered {
  border: 2px solid #667eea;
  box-shadow: 0 0 0 2px #e0e7ff;
  background: #f8faff;
}

/* Loading */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-container p {
  color: #6c757d;
  font-size: 16px;
}

/* ============================================ */
/* NUEVO: Estilos para inputs de folios */
/* ============================================ */

.text-center input[type="number"] {
  background-color: white;
  color: #333;
  font-weight: 600;
  transition: all 0.3s ease;
  cursor: text;
}

.text-center input[type="number"]:focus {
  outline: none;
  border-color: #28a745 !important;
  box-shadow: 0 0 8px rgba(40, 167, 69, 0.3);
  background-color: #f0fdf4;
}

/* Input BLOQUEADO (ya tiene folios) */
.text-center input[type="number"].input-locked {
  background-color: #f5f5f5;
  color: #999;
  border-color: #ccc !important;
  cursor: not-allowed;
  opacity: 0.7;
}

.text-center input[type="number"].input-locked:hover {
  border-color: #e74c3c !important;
  box-shadow: 0 0 8px rgba(231, 76, 60, 0.2);
}

.text-center input[type="number"]:disabled {
  background-color: #f5f5f5;
  color: #999;
  border-color: #ddd !important;
  cursor: not-allowed;
}

/* Tabla */
.table-section {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.table-responsive {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin: 0;
}

.table thead {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.table thead th {
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: white;
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.table tbody tr {
  border-bottom: 1px solid #e0e0e0;
  transition: all 0.2s ease;
}

.table tbody tr:hover {
  background-color: #f8f9fa;
  transform: scale(1.001);
}

.table tbody td {
  padding: 16px;
  color: #495057;
  font-size: 14px;
}

.product-key {
  display: inline-block;
  padding: 4px 10px;
  background-color: #e3f2fd;
  color: #1976d2;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
}

.warehouse-key {
  display: inline-block;
  padding: 4px 10px;
  background-color: #f3e5f5;
  color: #7b1fa2;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
}

.folio-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 15px;
}


.folio-existentes {
  background-color: #cfe2ff;
  color: #084298;
}

.text-center {
  text-align: center;
}


.existencias-badge {
  display: inline-block;
  padding: 6px 14px;
  background-color: #d4edda;
  color: #155724;
  border-radius: 8px;
  font-weight: 700;
  font-size: 15px;
}

.existencias-badge.low-stock {
  background-color: #f8d7da;
  color: #721c24;
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

/* No Data */
.no-data {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.no-data-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.no-data p {
  font-size: 18px;
  color: #6c757d;
  margin: 0;
}

/* Paginación */
.pagination-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-top: 2px solid #e0e0e0;
  flex-wrap: wrap;
  gap: 15px;
}

.pagination-info {
  font-size: 14px;
  color: #6c757d;
}

.pagination-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}

.pagination-controls button {
  padding: 8px 16px;
  font-size: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: white;
  color: #495057;
  cursor: pointer;
  transition: all 0.3s ease;
}

.pagination-controls button:hover:not(:disabled) {
  background-color: #667eea;
  color: white;
  border-color: #667eea;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  padding: 8px 16px;
  font-weight: 600;
  color: #667eea;
}

.pagination-size {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-size label {
  font-size: 14px;
  color: #6c757d;
}

.pagination-size select {
  padding: 6px 12px;
  font-size: 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: white;
  cursor: pointer;
}

.pagination-size select:focus {
  outline: none;
  border-color: #667eea;
}

/* Responsive */
@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
    align-items: stretch;
  }

  .btn-generate {
    width: 100%;
    justify-content: center;
  }

  .filters-section {
    grid-template-columns: 1fr;
  }

  .pagination-section {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-controls {
    flex-wrap: wrap;
    justify-content: center;
  }

  .table {
    font-size: 12px;
  }

  .table thead th,
  .table tbody td {
    padding: 10px 8px;
  }
}

/* ============================================ */
/* NUEVO: Estilos para Modal de Resumen */
/* ============================================ */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  padding: 25px;
  max-width: 600px;
  width: 90%;
}

.modal-resumen {
  max-width: 700px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #f0f0f0;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.close-button {
  background: none;
  border: none;
  color: #666;
  font-size: 20px;
  cursor: pointer;
  padding: 5px 10px;
  transition: all 0.3s ease;
}

.close-button:hover {
  color: #333;
  transform: scale(1.1);
}

.modal-body {
  max-height: 500px;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 2px solid #f0f0f0;
}

.resumen-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.resumen-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-radius: 10px;
  background: #f8f9fa;
  border: 2px solid #e9ecef;
  transition: all 0.3s ease;
}

.resumen-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.resumen-total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.resumen-success {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
  border: none;
}

.resumen-warning {
  background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
  color: white;
  border: none;
}

.resumen-info-folios {
  background: linear-gradient(135deg, #17a2b8 0%, #138496 100%);
  color: white;
  border: none;
  grid-column: 1 / -1;
}

.resumen-icon {
  font-size: 32px;
}

.resumen-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.resumen-label {
  font-size: 14px;
  opacity: 0.9;
}

.resumen-value {
  font-size: 24px;
  font-weight: bold;
}

.resumen-mensaje {
  padding: 15px;
  background: #e7f3ff;
  border-left: 4px solid #2196F3;
  border-radius: 8px;
  margin-bottom: 15px;
}

.resumen-mensaje p {
  margin: 0;
  color: #1976D2;
  font-weight: 500;
}

.resumen-alert {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 15px;
  background: #fff3cd;
  border-left: 4px solid #ffc107;
  border-radius: 8px;
}

.resumen-alert .icon {
  font-size: 20px;
}

.resumen-alert p {
  margin: 0;
  color: #856404;
  font-size: 14px;
}

/* ============================================ */
/* NUEVO: Estilos para Tabla de Cancelados */
/* ============================================ */
.section-card {
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 5px;
}



.input-existencias:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.input-existencias::placeholder {
  color: #666;
  font-weight: 600;
}

.table-info {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.table-info p {
  margin: 0 0 10px 0;
  color: #495057;
}

.table-info p:last-child {
  margin-bottom: 0;
}


.info-text .icon {
  font-size: 18px;
}

.btn-sm {
  padding: 8px 16px;
  font-size: 14px;
}

/* Estado para la fila activa y hover */
.active-row {
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
  outline: 2px solid #007BFF;
}

.hover-row {
  background-color: #e3f0ff !important;
  transition: background 0.2s;
}

/* ============================================ */
/* FASE 1: Estilos para mensajes de ayuda */
/* ============================================ */
.help-message {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
  font-size: 13px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  animation: slideInRight 0.5s ease;
  max-width: 600px;
}

.help-icon {
  font-size: 18px;
  animation: pulse 2s infinite;
}

.help-text {
  line-height: 1.5;
}

.help-text strong {
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
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

/* Mejora del botón de generar con estado de carga */
.btn-generate {
  position: relative;
  min-width: 180px;
  transition: all 0.3s ease;
}

.btn-generate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-generate:not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.4);
}

/* Mejorar estilos de tooltips en encabezados */
th {
  white-space: nowrap;
}

th .tooltip-help {
  margin-left: 4px;
  vertical-align: middle;
}

/* Indicador de guardando */
.saving-indicator {
  position: fixed;
  bottom: 20px;
  right: 20px;
  padding: 12px 20px;
  background: rgba(40, 167, 69, 0.95);
  color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 1000;
  animation: slideInUp 0.3s ease;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive para mensajes de ayuda */
@media (max-width: 768px) {
  .help-message {
    flex-direction: column;
    text-align: center;
    max-width: 100%;
  }

  .searchbar-group {
    flex-direction: column;
    gap: 10px;
  }

  .btn-generate {
    width: 100%;
  }
}

/* ============================================ */
/* FASE 4 - Estilos para ordenamiento */
/* ============================================ */

/* Columnas ordenables */
.sortable {
  cursor: pointer;
  user-select: none;
  position: relative;
  transition: all 0.2s ease;
}

.sortable:hover {
  background-color: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.sortable:active {
  transform: scale(0.98);
}

/* Indicador visual de búsqueda activa */
.SearchBar:has(input:not(:placeholder-shown)) {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

/* Spinner mientras busca/ordena */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
}

.loading-container .spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-container p {
  color: #6c757d;
  font-size: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Spinner pequeño para indicadores */
.spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Transiciones de fade */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>



