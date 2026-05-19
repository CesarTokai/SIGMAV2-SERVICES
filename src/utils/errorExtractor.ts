/**
 * Utilidad mejorada para extraer mensajes de error del backend
 * Maneja errores personalizados de axiosConfig.ts
 */

export interface ExtractedError {
  message: string;
  originalMessage?: string;
  statusCode?: number;
  fieldErrors?: Record<string, string>;
}

/**
 * Extrae un mensaje de error amigable del objeto error
 * Prioriza: error.message → error.originalMessage → error.response.data.message → fallback
 */
export const extractErrorMessage = (error: any): string => {
  // Caso 1: Error personalizado de axiosConfig.ts (tiene .message con mensaje amigable)
  if (error?.message && typeof error.message === 'string') {
    return error.message;
  }

  // Caso 2: Mensaje original del backend (si axiosConfig extrajo uno)
  if (error?.originalMessage && typeof error.originalMessage === 'string') {
    return error.originalMessage;
  }

  // Caso 3: Response data del axios original
  if (error?.response?.data?.message && typeof error.response.data.message === 'string') {
    return error.response.data.message;
  }

  // Caso 4: Otros campos comunes de error
  if (error?.response?.data?.error && typeof error.response.data.error === 'string') {
    return error.response.data.error;
  }

  // Caso 5: Array de errores
  if (Array.isArray(error?.response?.data?.errors) && error.response.data.errors.length > 0) {
    const firstError = error.response.data.errors[0];
    if (typeof firstError === 'string') return firstError;
    if (firstError?.message) return firstError.message;
  }

  // Fallback
  return 'Error desconocido. Por favor intenta nuevamente.';
};

/**
 * Extrae información completa del error (mensaje + status + fieldErrors)
 */
export const extractFullError = (error: any): ExtractedError => {
  return {
    message: extractErrorMessage(error),
    originalMessage: error?.originalMessage,
    statusCode: error?.statusCode || error?.response?.status,
    fieldErrors: error?.fieldErrors || {}
  };
};

/**
 * Hook helper: devuelve un mensaje contextualizado para toasts
 */
export const getErrorToastMessage = (error: any, contexto?: string): string => {
  const mensaje = extractErrorMessage(error);
  
  // Si el mensaje ya tiene emoji o es específico del backend, usarlo como es
  if (mensaje.includes('❌') || mensaje.includes('⚠️') || mensaje.length > 50) {
    return mensaje;
  }

  // Si es un mensaje muy genérico, agregar contexto
  if (contexto && (mensaje === 'Error desconocido' || mensaje === 'Error desconocido. Por favor intenta nuevamente.')) {
    return `❌ Error al ${contexto}. Por favor intenta nuevamente.`;
  }

  return mensaje;
};

