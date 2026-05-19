📋 RESUMEN: Mejora de Manejo de Errores en ConteoMarbetes & ConsultaCaptura

==============================================================================
🎯 PROBLEMA
==============================================================================
Los errores del backend estaban siendo ocultados. El usuario solo veía:
- "❌ Error del servidor. Intenta de nuevo más tarde."

Pero en la consola Browser estaba el mensaje real:
- "Marbete con folio 32 no encontrado en periodo 4"


==============================================================================
✅ SOLUCIÓN IMPLEMENTADA
==============================================================================

### 1️⃣ Mejorar axiosConfig.ts (Interceptor de Respuestas)
- ✅ Para status 500: PRESERVAR el mensaje del backend (no reemplazar)
- ✅ Para status 404: PRESERVAR el mensaje del backend

ANTES:
```typescript
} else if (statusCode === 500) {
  userFriendlyMessage = '❌ Error del servidor. Intenta de nuevo más tarde.';
}
```

DESPUÉS:
```typescript
} else if (statusCode === 500) {
  // ✅ Para 500: SIEMPRE mostrar el mensaje del backend si existe
  userFriendlyMessage = errorMessage || '❌ Error del servidor. Intenta de nuevo más tarde.';
}
```

---

### 2️⃣ Nueva Utilidad: errorExtractor.ts
Archivo central para extraer mensajes de error de forma consistente.

**Función principal: `extractErrorMessage(error: any): string`**
Busca el mensaje en este orden de prioridad:
1. `error.message` - Mensaje personalizado de axiosConfig
2. `error.originalMessage` - Mensaje original del backend (si fue capturado)
3. `error.response.data.message` - Respuesta HTTP del backend
4. `error.response.data.error` - Campo error común
5. Array de errores - Si hay múltiples, toma el primero
6. Fallback - "Error desconocido"

**Ubicación:** `src/utils/errorExtractor.ts`

---

### 3️⃣ Actualizar todos los ConteoMarbetes.vue

Se actualizaron 4 módulos:
- ✅ `src/modules/almacenista/views/marbetes/ConteoMarbetes.vue`
- ✅ `src/modules/auxiliar_de_conteo/views/marbetes/ConteoMarbetes.vue`
- ✅ `src/modules/auxiliar/views/marbetes/ConteoMarbetes.vue`
- ✅ `src/modules/admin/views/marbetesAdmin/ConteoMarbetes.vue`

**Cambios en cada archivo:**
```typescript
// Importar
import { extractErrorMessage } from '@/utils/errorExtractor';

// En el catch block
} catch (error: any) {
  LoadAlert(false);
  console.error('Error al buscar folio:', error);
  
  const mensajeError = extractErrorMessage(error);  // 🎯 NUEVO
  ToastError('Error al buscar folio', mensajeError);

  marbeteActual.value = null;
  resultadosBusqueda.value = [];
}
```

---

### 4️⃣ Actualizar todos los ConsultaCaptura.vue

Se actualizaron 2 módulos:
- ✅ `src/modules/auxiliar_de_conteo/views/marbetes/ConsultaCaptura.vue`
- ✅ `src/modules/admin/views/marbetesAdmin/ConsultaCaptura.vue`

**Mismo patrón que ConteoMarbetes.vue**, solo cambiar:
```typescript
// ANTES
const errorMessage = handleAPIError(error, 'cargar marbetes');

// DESPUÉS
const errorMessage = extractErrorMessage(error);  // 🎯 NUEVO
```

---

==============================================================================
🔄 FLUJO DE ERRORES MEJORADO
==============================================================================

ANTES:
┌─────────────────────────────────────────┐
│ Backend error: "Folio no encontrado"    │
└──────────┬──────────────────────────────┘
           │
           ↓
┌─────────────────────────────────────────┐
│ axiosConfig.ts interceptor              │
│ Crea Error con message="Error interno " │
└──────────┬──────────────────────────────┘
           │
           ↓
┌─────────────────────────────────────────┐
│ ConteoMarbetes.vue catch block          │
│ Muestra only error.message genérico     │
└─────────────────────────────────────────┘


DESPUÉS:
┌─────────────────────────────────────────┐
│ Backend error: "Folio no encontrado"    │
└──────────┬──────────────────────────────┘
           │
           ↓
┌─────────────────────────────────────────┐
│ axiosConfig.ts interceptor              │
│ ✅ Preserva msg original                │
│ Error.message = "Folio no encontrado"   │
│ Error.originalMessage = "Folio..."      │
└──────────┬──────────────────────────────┘
           │
           ↓
┌─────────────────────────────────────────┐
│ extractErrorMessage()                   │
│ ✅ Busca en múltiples campos             │
│ Devuelve: "Folio no encontrado"         │
└──────────┬──────────────────────────────┘
           │
           ↓
┌─────────────────────────────────────────┐
│ Usuario ve: "Error - Folio no encontrado"
│ ✅ EN EL TOAST, claro y específico      │
└─────────────────────────────────────────┘


==============================================================================
🧪 EJEMPLO DE USO
==============================================================================

Cuando buscas folio 32 en periodo 4 que no existe:

ANTES:
❌ Error
Error del servidor. Intenta de nuevo más tarde.

DESPUÉS:
❌ Error al buscar folio
Marbete con folio 32 no encontrado en periodo 4


==============================================================================
📁 ARCHIVOS MODIFICADOS
==============================================================================

1. src/config/axiosConfig.ts
   - ✅ Líneas 50-73: Mejorar preservación de mensajes por status code

2. src/utils/errorExtractor.ts (NUEVO)
   - ✅ Función: extractErrorMessage()
   - ✅ Función: extractFullError()
   - ✅ Función: getErrorToastMessage()

3. src/modules/almacenista/views/marbetes/ConteoMarbetes.vue
   - ✅ Importar errorExtractor
   - ✅ Actualizar catch block (línea ~254)

4. src/modules/auxiliar_de_conteo/views/marbetes/ConteoMarbetes.vue
   - ✅ Importar errorExtractor
   - ✅ Actualizar catch block (línea ~185)

5. src/modules/auxiliar/views/marbetes/ConteoMarbetes.vue
   - ✅ Importar errorExtractor
   - ✅ Actualizar catch block (línea ~252)

6. src/modules/admin/views/marbetesAdmin/ConteoMarbetes.vue
   - ✅ Importar errorExtractor
   - ✅ Actualizar catch block (línea ~265)

7. src/modules/auxiliar_de_conteo/views/marbetes/ConsultaCaptura.vue
   - ✅ Importar errorExtractor
   - ✅ Actualizar catch block (línea ~265)

8. src/modules/admin/views/marbetesAdmin/ConsultaCaptura.vue
   - ✅ Importar errorExtractor
   - ✅ Reemplazar handleAPIError por extractErrorMessage (línea ~352)


==============================================================================
🎓 VENTAJAS
==============================================================================

1. ✅ Usuario recibe mensajes claros del backend
2. ✅ Debugging más fácil (mensaje real en el toast)
3. ✅ Código centralizado (errorExtractor.ts)
4. ✅ Consistencia en todos los módulos
5. ✅ Manejo de múltiples formatos de error del backend
6. ✅ Fallback robusto si no hay mensaje


==============================================================================
🚀 SIGUIENTE PASO
==============================================================================

Probar la aplicación en desarrollo:
```bash
npm run dev
```

→ Buscar un folio que no existe
→ Verificar que ves el mensaje del backend en el toast (no genérico)
→ Revisar consola para logs detallados

