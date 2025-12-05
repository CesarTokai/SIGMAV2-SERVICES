// ============================================
// SERVICIO DE API PARA MARBETES
// ============================================

import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/sigmav2';

// Configurar interceptor para agregar token automáticamente
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// ============================================
// SERVICIO DE MARBETES
// ============================================
const MarbetesService = {

  /**
   * 1. SOLICITAR FOLIOS
   * Crear una solicitud de marbetes para un producto
   */
  solicitarFolios: async (productId, warehouseId, periodId, cantidad) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/labels/request`, {
        productId: productId,
        warehouseId: warehouseId,
        periodId: periodId,
        requestedLabels: cantidad
      });
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al solicitar folios'
      };
    }
  },

  /**
   * 2. GENERAR MARBETES (NUEVA VERSIÓN CON VALIDACIÓN)
   * Genera marbetes y retorna información detallada
   */
  generarMarbetes: async (productId, warehouseId, periodId, cantidad) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/labels/generate`, {
        productId: productId,
        warehouseId: warehouseId,
        periodId: periodId,
        labelsToGenerate: cantidad
      });

      // La respuesta ahora incluye información detallada
      return {
        success: true,
        data: {
          totalGenerados: response.data.totalGenerados,
          conExistencias: response.data.generadosConExistencias,
          sinExistencias: response.data.generadosSinExistencias,
          primerFolio: response.data.primerFolio,
          ultimoFolio: response.data.ultimoFolio,
          mensaje: response.data.mensaje
        }
      };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al generar marbetes'
      };
    }
  },

  /**
   * 3. CONSULTAR RESUMEN DE MARBETES
   * Obtiene la lista de productos con sus folios solicitados/generados
   */
  consultarResumen: async (periodId, warehouseId, page = 0, size = 10, searchText = '') => {
    try {
      const response = await axios.post(`${API_BASE_URL}/labels/summary`, {
        periodId: periodId,
        warehouseId: warehouseId,
        page: page,
        size: size,
        searchText: searchText,
        sortBy: 'claveProducto',
        sortDirection: 'ASC'
      });
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al consultar resumen'
      };
    }
  },

  /**
   * 4. CONSULTAR MARBETES CANCELADOS (NUEVO)
   * Obtiene la lista de marbetes sin existencias
   */
  consultarCancelados: async (periodId, warehouseId) => {
    try {
      const response = await axios.get(`${API_BASE_URL}/labels/cancelled`, {
        params: {
          periodId: periodId,
          warehouseId: warehouseId
        }
      });
      return { success: true, data: response.data };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al consultar marbetes cancelados'
      };
    }
  },

  /**
   * 5. ACTUALIZAR EXISTENCIAS DE MARBETE CANCELADO (NUEVO)
   * Actualiza las existencias y puede reactivar el marbete automáticamente
   */
  actualizarExistencias: async (folio, existenciasActuales, notas = '') => {
    try {
      const response = await axios.put(`${API_BASE_URL}/labels/cancelled/update-stock`, {
        folio: folio,
        existenciasActuales: existenciasActuales,
        notas: notas
      });
      return {
        success: true,
        data: response.data,
        reactivado: response.data.reactivado
      };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al actualizar existencias'
      };
    }
  },

  /**
   * 6. IMPRIMIR MARBETES
   * Genera el PDF de marbetes para un rango de folios
   */
  imprimirMarbetes: async (periodId, warehouseId, folioInicial, folioFinal) => {
    try {
      const response = await axios.post(
        `${API_BASE_URL}/labels/print`,
        {
          periodId: periodId,
          warehouseId: warehouseId,
          startFolio: folioInicial,
          endFolio: folioFinal
        },
        {
          responseType: 'blob' // Importante para descargar archivos
        }
      );

      // Crear link de descarga
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `marbetes_${folioInicial}_${folioFinal}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();

      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al imprimir marbetes'
      };
    }
  },

  /**
   * 7. CONTEO DE MARBETES (DIAGNÓSTICO)
   * Obtiene el total de marbetes generados para un periodo/almacén
   */
  contarMarbetes: async (periodId, warehouseId) => {
    try {
      const response = await axios.get(`${API_BASE_URL}/labels/debug/count`, {
        params: {
          periodId: periodId,
          warehouseId: warehouseId
        }
      });
      return { success: true, total: response.data.totalLabels };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Error al contar marbetes'
      };
    }
  }
};

export default MarbetesService;

