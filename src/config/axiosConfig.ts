import axios from "axios";


axios.defaults.baseURL = import.meta.env.VITE_API_URL;
axios.defaults.headers.common["Content-Type"] = "application/json";

axios.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("token");
        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

axios.interceptors.response.use(
    (response) => {
        return response;
    },
    (error) => {
        // Si no hay response (p. ej. network error o request abort),
        // conservar el objeto Error para que los handlers puedan detectar cancelación.
        if (error.response === undefined) {
            if (!error.message) {
                error.message = "No se pudo conectar al servidor. Verifica tu conexión de internet.";
            }
            return Promise.reject(error);
        }

        const statusCode = error.response?.status || 500;
        const responseData = error.response?.data || {};

        // Extraer el mensaje de error del backend (múltiples formatos posibles)
        let errorMessage = 
            responseData.message || 
            responseData.error || 
            responseData.detail || 
            responseData.description ||
            'Error desconocido';

        // Si vine en un array de errores, tomar el primero
        if (Array.isArray(responseData.errors) && responseData.errors.length > 0) {
            errorMessage = responseData.errors[0].message || responseData.errors[0];
        }

        // Proporcionar mensajes descriptivos según el status code
        // ✅ SIEMPRE preservar mensaje del backend si existe, agregar prefijo según status
        let userFriendlyMessage = errorMessage;
        
        if (statusCode === 404) {
            if (errorMessage && errorMessage !== 'Error desconocido') {
                userFriendlyMessage = errorMessage; // Usar el mensaje específico del backend
            } else {
                userFriendlyMessage = '❌ Recurso no encontrado. Verifica los datos ingresados.';
            }
        } else if (statusCode === 400) {
            userFriendlyMessage = errorMessage || '❌ Solicitud inválida. Verifica los datos ingresados.';
        } else if (statusCode === 401) {
            userFriendlyMessage = '❌ Sesión expirada. Por favor, inicia sesión nuevamente.';
        } else if (statusCode === 403) {
            userFriendlyMessage = '❌ No tienes permiso para acceder a este recurso.';
        } else if (statusCode === 409) {
            userFriendlyMessage = errorMessage || '❌ Conflicto: El recurso ya existe o hay un conflicto de datos.';
        } else if (statusCode === 500) {
            // ✅ Para 500: SIEMPRE mostrar el mensaje del backend si existe
            userFriendlyMessage = errorMessage || '❌ Error del servidor. Intenta de nuevo más tarde.';
        } else if (statusCode >= 500) {
            userFriendlyMessage = errorMessage || `❌ Error del servidor (${statusCode}). Intenta de nuevo más tarde.`;
        }

        // Log detallado para debugging
        console.error(`❌ Error ${statusCode}: ${error.config?.method?.toUpperCase()} ${error.config?.url}`, {
            url: error.config?.url,
            method: error.config?.method,
            statusCode: statusCode,
            message: errorMessage,
            userMessage: userFriendlyMessage,
            requestData: error.config?.data,
            responseData: responseData
        });

        // Si hay fieldErrors, mostrarlos en consola
        if (responseData?.fieldErrors) {
            console.error("🔍 Errores de Campo:", responseData.fieldErrors);
        }

        // Crear un error personalizado con el mensaje del backend
        const customError = new Error(userFriendlyMessage);
        (customError as any).statusCode = statusCode;
        (customError as any).originalMessage = errorMessage;
        (customError as any).response = responseData;
        (customError as any).fieldErrors = responseData?.fieldErrors || {};

        return Promise.reject(customError);
    }
);

export default {
    async doPost(
        url: string,
        data: object,
        options?: { responseType?: 'arraybuffer' | 'blob' | 'document' | 'json' | 'text' | 'stream' }
    ) {
        return await axios.post(url, data, {
            headers: { 'Content-Type': 'application/json' },
            ...(options?.responseType ? { responseType: options.responseType } : {})
        });
    },
    async doGet(url: string, params?: object | { responseType?: 'arraybuffer' | 'blob' | 'document' | 'json' | 'text' | 'stream' }) {
        const config: any = {
            headers: { 'Content-Type': 'application/json' }
        };

        if (params && 'responseType' in params && params.responseType) {
            config.responseType = params.responseType;
            const { responseType, ...restParams } = params;
            config.params = restParams;
        } else {
            config.params = params;
        }

        return await axios.get(url, config);
    },
    async doDelete(url: string) {
        return await axios.delete(url, {
            headers: { 'Content-Type': 'application/json' }
        });
    },
    async doPut(url: string, data: object) {
        return await axios.put(url, data, {
            headers: { 'Content-Type': 'application/json' }
        });
    },
    async doputString(url: string, data: string) {
        return await axios.put(url, data, {
            headers: { 'Content-Type': 'application/json' }
        });
    },
    async doPostFile(url: string, data: object) {
        return await axios.post(url, data, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    },
    async doPutFile(url: string, data: object) {
        return await axios.put(url, data, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    },
};
