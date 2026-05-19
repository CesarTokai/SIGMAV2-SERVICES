# 🎯 FLUJO SIMPLIFICADO - Sistema de Impresión de Marbetes

**Fecha:** 2025-12-29  
**Versión:** 2.0 SIMPLIFICADA  
**Cambio Principal:** Eliminada la necesidad de solicitud previa

---

## 🚀 ¿Qué Cambió?

### ❌ ANTES (Complicado):
```
1. POST /labels/request          → Solicitar folios
2. POST /labels/generate         → Generar marbetes
3. POST /labels/pending-print-count → Verificar
4. POST /labels/print           → Imprimir
```

### ✅ AHORA (Simplificado):

#### Opción 1: Un Solo Paso 🎉
```
POST /labels/generate-and-print  → ¡Genera e imprime en 1 llamada!
```

#### Opción 2: Dos Pasos
```
1. POST /labels/generate/batch   → Genera marbetes
2. POST /labels/print            → Imprime
```

---

## 🎯 OPCIÓN 1: API Todo-en-Uno (RECOMENDADA)

### POST `/api/sigmav2/labels/generate-and-print`

**¿Qué hace?**
1. Genera los marbetes
2. Los imprime automáticamente
3. Te devuelve el PDF listo

### 📥 Request:
```json
{
  "warehouseId": 8,
  "periodId": 1,
  "products": [
    {
      "productId": 94,
      "labelsToGenerate": 5
    },
    {
      "productId": 95,
      "labelsToGenerate": 3
    }
  ]
}
```

### 📤 Response:
```
HTTP 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="marbetes_P1_A8_20251229_150000.pdf"

[PDF BINARIO CON 8 MARBETES]
```

### 💡 Código Frontend:
```javascript
async function generarEImprimir() {
  try {
    const response = await axios.post(
      '/api/sigmav2/labels/generate-and-print',
      {
        warehouseId: almacenSeleccionado,
        periodId: periodoActual,
        products: [
          { productId: 94, labelsToGenerate: 5 },
          { productId: 95, labelsToGenerate: 3 }
        ]
      },
      { responseType: 'blob' }
    );

    // Descargar PDF
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'marbetes.pdf';
    link.click();

    alert('✅ Marbetes generados e impresos exitosamente');
  } catch (error) {
    alert('❌ Error: ' + error.response?.data?.message);
  }
}
```

---

## 🎯 OPCIÓN 2: Dos Pasos (Si necesitas más control)

### Paso 1: POST `/api/sigmav2/labels/generate/batch`

**¿Qué hace?**
Genera los marbetes (sin necesidad de solicitud previa)

### 📥 Request:
```json
{
  "warehouseId": 8,
  "periodId": 1,
  "products": [
    {
      "productId": 94,
      "labelsToGenerate": 5
    }
  ]
}
```

### 📤 Response:
```
HTTP 200 OK
```

### Paso 2: POST `/api/sigmav2/labels/print`

**¿Qué hace?**
Imprime los marbetes que estén pendientes

### 📥 Request:
```json
{
  "warehouseId": 8,
  "periodId": 1
}
```

### 📤 Response:
```
HTTP 200 OK
Content-Type: application/pdf

[PDF BINARIO]
```

### 💡 Código Frontend:
```javascript
async function generarEImprimir() {
  try {
    // PASO 1: Generar
    await axios.post('/api/sigmav2/labels/generate/batch', {
      warehouseId: almacenSeleccionado,
      periodId: periodoActual,
      products: [
        { productId: 94, labelsToGenerate: 5 }
      ]
    });

    // PASO 2: Imprimir
    const response = await axios.post(
      '/api/sigmav2/labels/print',
      {
        warehouseId: almacenSeleccionado,
        periodId: periodoActual
      },
      { responseType: 'blob' }
    );

    // Descargar PDF
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'marbetes.pdf';
    link.click();

    alert('✅ Completado');
  } catch (error) {
    alert('❌ Error: ' + error.response?.data?.message);
  }
}
```

---

## 📊 Comparación de Métodos

| Característica | `/generate-and-print` | `/generate/batch` + `/print` |
|----------------|----------------------|------------------------------|
| **Pasos** | 1 | 2 |
| **Complejidad** | ⭐ Muy simple | ⭐⭐ Simple |
| **Control** | Automático | Manual |
| **Uso recomendado** | Frontend directo | Cuando necesitas verificar antes |
| **Velocidad** | Rápida | Rápida |

---

## 🔄 Flujo Interno Simplificado

### Antes (Complicado):
```
Usuario → /request → Crea solicitud en label_requests
       → /generate → Valida solicitud → Genera marbetes
       → /print → Imprime
```

### Ahora (Simplificado):
```
Usuario → /generate-and-print → Genera marbetes directamente
                              → Imprime automáticamente
                              → Retorna PDF
```

O

```
Usuario → /generate/batch → Genera marbetes directamente
       → /print → Imprime
```

---

## 🎯 ¿Qué Se Eliminó?

### ❌ Ya NO necesitas:
1. ~~`POST /labels/request`~~ (Ya no existe la tabla `label_requests`)
2. ~~Verificar con `/pending-print-count` antes de imprimir~~ (Opcional ahora)
3. ~~Preocuparte por solicitudes previas~~
4. ~~Gestionar estados de solicitud~~

### ✅ Solo necesitas:
1. Llamar a `/generate-and-print` con los productos y cantidad
2. Recibir el PDF
3. ¡Listo!

---

## 💡 Ejemplos Prácticos

### Ejemplo 1: Imprimir 1 marbete de 1 producto

```javascript
// SUPER SIMPLE - Una sola llamada
const response = await axios.post(
  '/api/sigmav2/labels/generate-and-print',
  {
    warehouseId: 8,
    periodId: 1,
    products: [
      { productId: 94, labelsToGenerate: 1 }
    ]
  },
  { responseType: 'blob' }
);

// Descargar
const blob = new Blob([response.data], { type: 'application/pdf' });
const url = window.URL.createObjectURL(blob);
window.open(url); // Abre en nueva pestaña
```

### Ejemplo 2: Imprimir múltiples productos

```javascript
const response = await axios.post(
  '/api/sigmav2/labels/generate-and-print',
  {
    warehouseId: 8,
    periodId: 1,
    products: [
      { productId: 94, labelsToGenerate: 10 },
      { productId: 95, labelsToGenerate: 5 },
      { productId: 96, labelsToGenerate: 15 }
    ]
  },
  { responseType: 'blob' }
);

// Total: 30 marbetes en un solo PDF
```

### Ejemplo 3: Con manejo de errores completo

```javascript
async function imprimirMarbetes(productos) {
  try {
    // Mostrar loading
    showLoading('Generando e imprimiendo marbetes...');

    const response = await axios.post(
      '/api/sigmav2/labels/generate-and-print',
      {
        warehouseId: obtenerAlmacenActual(),
        periodId: obtenerPeriodoActual(),
        products: productos
      },
      { responseType: 'blob' }
    );

    // Descargar PDF
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `marbetes_${Date.now()}.pdf`;
    link.click();

    hideLoading();
    showSuccess('✅ Marbetes generados e impresos exitosamente');

  } catch (error) {
    hideLoading();
    
    if (error.response?.data?.message) {
      showError('❌ ' + error.response.data.message);
    } else {
      showError('❌ Error generando marbetes');
    }
    
    console.error('Error completo:', error);
  }
}

// Uso
imprimirMarbetes([
  { productId: 94, labelsToGenerate: 5 }
]);
```

---

## 🔍 ¿Cuándo Usar Qué?

### Usa `/generate-and-print` cuando:
- ✅ Quieres generar e imprimir en un solo paso
- ✅ No necesitas verificar nada antes de imprimir
- ✅ Quieres el código más simple posible
- ✅ **Caso de uso típico: 90% de los casos**

### Usa `/generate/batch` + `/print` cuando:
- ✅ Necesitas generar ahora pero imprimir después
- ✅ Quieres verificar algo antes de imprimir
- ✅ Necesitas mostrar un preview o confirmación
- ✅ **Caso de uso: 10% de los casos**

---

## ⚠️ Notas Importantes

### 1. Almacén Consistente
Siempre usa el **mismo `warehouseId`** en todas las operaciones:
```javascript
const ALMACEN = 8; // Definir una sola vez

// ✅ BIEN
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: ALMACEN,
  ...
});

// ❌ MAL
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: 8,  // Genera en almacén 8
  ...
});
await axios.post('/api/sigmav2/labels/print', {
  warehouseId: 10, // Intenta imprimir del almacén 10
  ...
});
```

### 2. Estados de Marbetes
Los marbetes pasan por estos estados:
- `GENERADO` → Recién creado, listo para imprimir
- `IMPRESO` → Ya fue impreso
- `CANCELADO` → Cancelado, no se puede imprimir

### 3. Reimpresión
Si necesitas reimprimir:
```javascript
await axios.post('/api/sigmav2/labels/print', {
  warehouseId: 8,
  periodId: 1,
  folios: [1001, 1002, 1003], // Folios específicos
  forceReprint: true // Permite reimprimir
});
```

---

## 🎉 Beneficios de la Simplificación

### Para el Frontend:
- ✅ Menos código
- ✅ Menos llamadas a API
- ✅ Menos puntos de fallo
- ✅ Más rápido de implementar
- ✅ Más fácil de mantener

### Para el Backend:
- ✅ Menos tablas (eliminamos `label_requests`)
- ✅ Menos validaciones complejas
- ✅ Código más limpio
- ✅ Menos bugs potenciales

### Para el Usuario:
- ✅ Experiencia más fluida
- ✅ Menos pasos
- ✅ Más rápido
- ✅ Menos confusión

---

## 📝 Migración desde el Sistema Antiguo

### Si usabas el flujo antiguo:

**Antes:**
```javascript
await axios.post('/api/sigmav2/labels/request', {...});
await axios.post('/api/sigmav2/labels/generate', {...});
await axios.post('/api/sigmav2/labels/pending-print-count', {...});
await axios.post('/api/sigmav2/labels/print', {...});
```

**Ahora:**
```javascript
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: 8,
  periodId: 1,
  products: [{ productId: 94, labelsToGenerate: 5 }]
}, { responseType: 'blob' });
```

---

## 🐛 Solución de Problemas

### Error: "No hay marbetes pendientes"
**Causa:** Los marbetes se generaron en otro almacén  
**Solución:** Verifica que uses el mismo `warehouseId`

### Error: "El PDF está vacío"
**Causa:** No se generaron marbetes  
**Solución:** Revisa los logs del backend

### Error: "Producto no existe"
**Causa:** El producto no está en el catálogo de inventario  
**Solución:** Carga primero los catálogos de inventario

---

## 📚 Documentos Relacionados

- `FLUJO-DETALLADO-SOLICITUD-GENERACION-IMPRESION.md` (OBSOLETO - Sistema antiguo)
- `GUIA-COMPLETA-APIS-MARBETES.md` (Actualizar con nueva API)
- `ANALISIS-PROFUNDO-FUNCIONES-IMPRESION.md` (Análisis técnico)

---

**Documento generado:** 2025-12-29  
**Versión:** 2.0 SIMPLIFICADA  
**Cambio Principal:** ¡Ya no necesitas solicitar folios primero! 🎉  
**Estado:** ✅ IMPLEMENTADO Y LISTO PARA USAR

