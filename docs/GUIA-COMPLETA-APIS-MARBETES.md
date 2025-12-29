# 📚 Guía Completa de APIs del Módulo de Marbetes

**Fecha:** 2025-12-29  
**Versión:** 1.0  
**Base URL:** `/api/sigmav2/labels`

---

## 📋 Índice

1. [Flujo Completo del Proceso](#flujo-completo-del-proceso)
2. [APIs en Orden Lógico de Uso](#apis-en-orden-lógico-de-uso)
3. [APIs de Consulta](#apis-de-consulta)
4. [APIs de Conteo](#apis-de-conteo)
5. [APIs de Reportes](#apis-de-reportes)
6. [APIs de Administración](#apis-de-administración)
7. [Ejemplos de Uso](#ejemplos-de-uso)

---

## 🔄 Flujo Completo del Proceso

```
┌─────────────────────────────────────────────────────────────┐
│                    PROCESO DE MARBETES                      │
└─────────────────────────────────────────────────────────────┘

1. PREPARACIÓN
   ├─ Cargar catálogos de inventario (externo)
   └─ Crear periodo de inventario (externo)

2. SOLICITUD DE FOLIOS
   └─ POST /labels/request
      → Solicitar folios para productos específicos

3. GENERACIÓN DE MARBETES
   ├─ POST /labels/generate (un producto)
   └─ POST /labels/generate/batch (múltiples productos)
      → Genera los marbetes con folios asignados

4. VERIFICACIÓN
   ├─ POST /labels/pending-print-count
   │  → Ver cuántos marbetes hay pendientes
   └─ GET /labels/debug/count
      → Verificar total de marbetes generados

5. IMPRESIÓN
   └─ POST /labels/print
      → Genera PDF e imprime marbetes

6. CONTEO FÍSICO
   ├─ POST /labels/for-count
   │  → Buscar marbete para contar
   ├─ POST /labels/counts/c1
   │  → Registrar primer conteo
   └─ POST /labels/counts/c2
      → Registrar segundo conteo

7. VALIDACIÓN Y AJUSTES
   ├─ PUT /labels/counts/c1
   │  → Actualizar primer conteo
   ├─ PUT /labels/counts/c2
   │  → Actualizar segundo conteo
   └─ POST /labels/cancel
      → Cancelar marbete si es necesario

8. REPORTES Y ANÁLISIS
   ├─ POST /labels/summary
   ├─ POST /labels/reports/distribution
   ├─ POST /labels/reports/with-differences
   └─ ...otros reportes...

9. GENERACIÓN DE ARCHIVO
   └─ POST /labels/generate-file
      → Genera archivo TXT para el sistema principal
```

---

## 📝 APIs en Orden Lógico de Uso

### ═══════════════════════════════════════
### FASE 1: PREPARACIÓN Y SOLICITUD
### ═══════════════════════════════════════

---

## 1️⃣ POST `/labels/request`
**Solicitar Folios para un Producto**

### 📌 Propósito:
Crear una solicitud de folios para un producto específico en un periodo y almacén.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

### 📥 Request:
```json
{
  "productId": 123,
  "warehouseId": 14,
  "periodId": 1,
  "requestedLabels": 10
}
```

### 📤 Response:
```
HTTP 201 Created
(Sin cuerpo)
```

### 🎯 Cuándo usar:
- Al inicio del proceso de inventario
- Para solicitar folios de un producto específico
- Antes de generar los marbetes

### ⚠️ Reglas de negocio:
- Si `requestedLabels = 0` y el producto NO existe en inventario → Cancela la solicitud
- Si `requestedLabels = 0` y el producto SÍ existe en inventario → Error (debe solicitar al menos 1)
- No permite solicitar 0 folios para productos con existencias

### 💡 Ejemplo de uso:
```javascript
// Solicitar 10 folios para el producto 123
await axios.post('/api/sigmav2/labels/request', {
  productId: 123,
  warehouseId: 14,
  periodId: 1,
  requestedLabels: 10
});
```

---

## 2️⃣ POST `/labels/generate`
**Generar Marbetes para una Solicitud**

### 📌 Propósito:
Generar los marbetes físicamente a partir de una solicitud aprobada.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

### 📥 Request:
```json
{
  "requestId": 456,
  "periodId": 1,
  "warehouseId": 14
}
```

### 📤 Response:
```json
{
  "totalGenerados": 10,
  "generadosConExistencias": 8,
  "generadosSinExistencias": 2,
  "folioInicial": 1001,
  "folioFinal": 1010
}
```

### 🎯 Cuándo usar:
- Después de solicitar folios
- Para crear los marbetes que se van a imprimir
- Se ejecuta una sola vez por solicitud

### ⚠️ Reglas de negocio:
- Asigna folios consecutivos automáticamente
- Crea marbetes en estado `GENERADO` si hay existencias
- Crea marbetes en estado `CANCELADO` si NO hay existencias
- Valida que los catálogos estén cargados

### 💡 Ejemplo de uso:
```javascript
// Generar marbetes para la solicitud 456
const response = await axios.post('/api/sigmav2/labels/generate', {
  requestId: 456,
  periodId: 1,
  warehouseId: 14
});

console.log(`Generados: ${response.data.totalGenerados}`);
console.log(`Folios: ${response.data.folioInicial} - ${response.data.folioFinal}`);
```

---

## 3️⃣ POST `/labels/generate/batch`
**Generar Marbetes para Múltiples Productos**

### 📌 Propósito:
Generar marbetes para múltiples productos a la vez (proceso masivo).

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

### 📥 Request:
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "products": [
    {
      "productId": 123,
      "requestedLabels": 10
    },
    {
      "productId": 124,
      "requestedLabels": 5
    },
    {
      "productId": 125,
      "requestedLabels": 15
    }
  ]
}
```

### 📤 Response:
```
HTTP 200 OK
(Sin cuerpo)
```

### 🎯 Cuándo usar:
- Para generar marbetes de múltiples productos al mismo tiempo
- Útil para procesos de carga masiva desde Excel
- Más eficiente que llamar `/labels/request` y `/labels/generate` múltiples veces

### 💡 Ejemplo de uso:
```javascript
// Generar marbetes para 3 productos a la vez
await axios.post('/api/sigmav2/labels/generate/batch', {
  periodId: 1,
  warehouseId: 14,
  products: [
    { productId: 123, requestedLabels: 10 },
    { productId: 124, requestedLabels: 5 },
    { productId: 125, requestedLabels: 15 }
  ]
});
```

---

### ═══════════════════════════════════════
### FASE 2: VERIFICACIÓN Y CONSULTA
### ═══════════════════════════════════════

---

## 4️⃣ POST `/labels/pending-print-count`
**Contar Marbetes Pendientes de Impresión**

### 📌 Propósito:
Verificar cuántos marbetes están pendientes de impresión (estado GENERADO).

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

### 📥 Request:
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "productId": 123  // Opcional
}
```

### 📤 Response:
```json
{
  "count": 10,
  "periodId": 1,
  "warehouseId": 14,
  "warehouseName": "Almacén Central",
  "periodName": "2025-12-01"
}
```

### 🎯 Cuándo usar:
- **ANTES de imprimir** para verificar si hay marbetes pendientes
- Para mostrar al usuario cuántos marbetes se van a imprimir
- Para validar en el frontend

### ⚠️ Importante:
- Si `count = 0`, no se puede imprimir (no hay marbetes pendientes)
- Si `count > 0`, se puede llamar a `/labels/print`

### 💡 Ejemplo de uso:
```javascript
// Verificar marbetes pendientes antes de imprimir
const response = await axios.post('/api/sigmav2/labels/pending-print-count', {
  periodId: 1,
  warehouseId: 14
});

if (response.data.count === 0) {
  alert('No hay marbetes pendientes de impresión');
} else {
  alert(`Hay ${response.data.count} marbetes pendientes`);
  // Ahora sí llamar a /labels/print
}
```

---

## 5️⃣ GET `/labels/debug/count`
**Contar Total de Marbetes Generados**

### 📌 Propósito:
Diagnóstico: Ver cuántos marbetes existen en total (todos los estados).

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

### 📥 Request:
```
GET /api/sigmav2/labels/debug/count?periodId=1&warehouseId=14
```

### 📤 Response:
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "totalLabels": 150,
  "userId": 10,
  "userRole": "ADMINISTRADOR"
}
```

### 🎯 Cuándo usar:
- Para diagnosticar problemas
- Para verificar que los marbetes se generaron correctamente
- Para debugging

---

## 6️⃣ POST `/labels/summary`
**Resumen de Marbetes por Producto**

### 📌 Propósito:
Obtener un resumen de todos los marbetes agrupados por producto.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

### 📥 Request:
```json
{
  "periodId": 1,
  "warehouseId": 14
}
```

### 📤 Response:
```json
[
  {
    "productId": 123,
    "productCode": "PROD001",
    "productName": "Producto A",
    "totalLabels": 10,
    "generados": 8,
    "impresos": 8,
    "contados": 5,
    "cancelados": 2
  },
  {
    "productId": 124,
    "productCode": "PROD002",
    "productName": "Producto B",
    "totalLabels": 5,
    "generados": 5,
    "impresos": 5,
    "contados": 5,
    "cancelados": 0
  }
]
```

### 🎯 Cuándo usar:
- Para mostrar un dashboard con el estado general
- Para ver el progreso del inventario
- Para identificar productos con problemas

---

### ═══════════════════════════════════════
### FASE 3: IMPRESIÓN
### ═══════════════════════════════════════

---

## 7️⃣ POST `/labels/print`
**Imprimir Marbetes** ⭐ **API PRINCIPAL DE IMPRESIÓN**

### 📌 Propósito:
Generar PDF e imprimir marbetes físicos.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

### 📥 Request - Modo Automático:
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "productId": 123  // Opcional: filtrar por producto
}
```

### 📥 Request - Modo Selectivo:
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "folios": [1001, 1002, 1003],
  "forceReprint": false  // true para reimprimir
}
```

### 📤 Response - Éxito:
```
HTTP 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="marbetes_P1_A14_20251229_143052.pdf"

[BINARY PDF DATA]
```

### 📤 Response - Error:
```json
HTTP 400 Bad Request
{
  "error": "Estado inválido",
  "message": "No hay marbetes pendientes de impresión para el periodo y almacén especificados"
}
```

### 🎯 Cuándo usar:
- **Después de generar los marbetes**
- **Después de verificar con `/pending-print-count`**
- Para reimprimir marbetes perdidos o dañados

### ⚠️ Reglas de negocio:
- Solo imprime marbetes en estado `GENERADO`
- Cambia el estado a `IMPRESO` después de generar el PDF
- No imprime marbetes `CANCELADOS`
- Límite máximo: 500 marbetes por impresión
- Valida que los catálogos estén cargados
- Valida que todos los productos y almacenes existan

### 💡 Ejemplo de uso:
```javascript
// Imprimir todos los marbetes pendientes
async function imprimirMarbetes() {
  try {
    // 1. Verificar si hay marbetes pendientes
    const count = await axios.post('/api/sigmav2/labels/pending-print-count', {
      periodId: 1,
      warehouseId: 14
    });

    if (count.data.count === 0) {
      alert('No hay marbetes pendientes de impresión');
      return;
    }

    // 2. Imprimir
    const response = await axios.post('/api/sigmav2/labels/print', {
      periodId: 1,
      warehouseId: 14
    }, {
      responseType: 'blob'  // Importante para PDFs
    });

    // 3. Descargar el PDF
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'marbetes.pdf';
    link.click();

  } catch (error) {
    if (error.response && error.response.data) {
      // Manejar error estructurado
      const errorData = error.response.data;
      alert(errorData.message || errorData.error);
    }
  }
}
```

---

### ═══════════════════════════════════════
### FASE 4: CONTEO FÍSICO
### ═══════════════════════════════════════

---

## 8️⃣ POST `/labels/for-count`
**Buscar Marbete para Contar**

### 📌 Propósito:
Obtener la información de un marbete para realizar el conteo físico.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

### 📥 Request:
```json
{
  "folio": 1001,
  "periodId": 1,
  "warehouseId": 14
}
```

### 📤 Response:
```json
{
  "folio": 1001,
  "productId": 123,
  "productCode": "PROD001",
  "productName": "Producto A",
  "estado": "IMPRESO",
  "existenciasSistema": 100,
  "c1": null,
  "c2": null,
  "canCount": true,
  "canCountC1": true,
  "canCountC2": false
}
```

### 🎯 Cuándo usar:
- Cuando el usuario escanea o ingresa un folio
- Antes de registrar un conteo
- Para mostrar la información del producto

### 💡 Ejemplo de uso:
```javascript
// Buscar marbete para contar
const response = await axios.post('/api/sigmav2/labels/for-count', {
  folio: 1001,
  periodId: 1,
  warehouseId: 14
});

console.log(`Producto: ${response.data.productName}`);
console.log(`Existencias en sistema: ${response.data.existenciasSistema}`);
console.log(`Puede contar C1: ${response.data.canCountC1}`);
```

---

## 9️⃣ POST `/labels/counts/c1`
**Registrar Primer Conteo (C1)**

### 📌 Propósito:
Registrar el primer conteo físico de un marbete.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

### 📥 Request:
```json
{
  "folio": 1001,
  "countedValue": 95
}
```

### 📤 Response:
```json
{
  "id": 789,
  "folio": 1001,
  "userId": 10,
  "countNumber": 1,
  "countedValue": 95,
  "roleAtTime": "ALMACENISTA",
  "isFinal": false,
  "createdAt": "2025-12-29T14:30:00"
}
```

### 🎯 Cuándo usar:
- Después de buscar el marbete con `/for-count`
- Para registrar el primer conteo físico
- Solo se puede registrar una vez

### ⚠️ Reglas de negocio:
- El marbete debe estar en estado `IMPRESO`
- No se puede registrar C1 si ya existe
- No se puede registrar C1 si ya existe C2
- No se puede registrar si está cancelado

### 💡 Ejemplo de uso:
```javascript
// Registrar primer conteo
await axios.post('/api/sigmav2/labels/counts/c1', {
  folio: 1001,
  countedValue: 95
});
```

---

## 🔟 POST `/labels/counts/c2`
**Registrar Segundo Conteo (C2)**

### 📌 Propósito:
Registrar el segundo conteo físico de un marbete (para verificación).

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

### 📥 Request:
```json
{
  "folio": 1001,
  "countedValue": 93
}
```

### 📤 Response:
```json
{
  "id": 790,
  "folio": 1001,
  "userId": 11,
  "countNumber": 2,
  "countedValue": 93,
  "roleAtTime": "AUXILIAR_DE_CONTEO",
  "isFinal": false,
  "createdAt": "2025-12-29T15:00:00"
}
```

### 🎯 Cuándo usar:
- Después de registrar C1
- Para verificar el primer conteo
- Cuando hay discrepancias

### ⚠️ Reglas de negocio:
- Debe existir C1 antes de registrar C2
- No se puede registrar C2 si ya existe
- El marbete debe estar en estado `IMPRESO`

---

### ═══════════════════════════════════════
### FASE 5: ACTUALIZACIÓN Y CORRECCIÓN
### ═══════════════════════════════════════

---

## 1️⃣1️⃣ PUT `/labels/counts/c1`
**Actualizar Primer Conteo**

### 📌 Propósito:
Corregir el primer conteo si hubo un error.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

### 📥 Request:
```json
{
  "folio": 1001,
  "newCountedValue": 96,
  "reason": "Error de captura, se contó incorrectamente"
}
```

### 📤 Response:
```json
{
  "id": 789,
  "folio": 1001,
  "userId": 10,
  "countNumber": 1,
  "countedValue": 96,
  "roleAtTime": "ALMACENISTA",
  "isFinal": false,
  "createdAt": "2025-12-29T14:30:00"
}
```

### 🎯 Cuándo usar:
- Para corregir errores de captura
- Cuando se detecta un conteo incorrecto
- Antes de finalizar el inventario

---

## 1️⃣2️⃣ PUT `/labels/counts/c2`
**Actualizar Segundo Conteo**

### 📌 Propósito:
Corregir el segundo conteo si hubo un error.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA (solo si él lo registró)
- AUXILIAR_DE_CONTEO (solo si él lo registró)

### 📥 Request:
```json
{
  "folio": 1001,
  "newCountedValue": 94,
  "reason": "Reconteo solicitado por supervisor"
}
```

### 📤 Response:
```json
{
  "id": 790,
  "folio": 1001,
  "userId": 11,
  "countNumber": 2,
  "countedValue": 94,
  "roleAtTime": "AUXILIAR_DE_CONTEO",
  "isFinal": false,
  "createdAt": "2025-12-29T15:00:00"
}
```

---

## 1️⃣3️⃣ POST `/labels/cancel`
**Cancelar un Marbete**

### 📌 Propósito:
Cancelar un marbete que no se puede contar (producto no encontrado, dañado, etc.).

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

### 📥 Request:
```json
{
  "folio": 1001,
  "reason": "Producto no encontrado en el almacén",
  "existenciasActuales": 0
}
```

### 📤 Response:
```
HTTP 200 OK
(Sin cuerpo)
```

### 🎯 Cuándo usar:
- Cuando un producto no se encuentra físicamente
- Cuando un marbete está dañado
- Cuando hay errores en la asignación

### ⚠️ Reglas de negocio:
- Solo se pueden cancelar marbetes `IMPRESOS`
- No se pueden cancelar si tienen conteos registrados
- Se registra en tabla `label_cancelled` para auditoría

---

### ═══════════════════════════════════════
### FASE 6: CONSULTAS Y ADMINISTRACIÓN
### ═══════════════════════════════════════

---

## 1️⃣4️⃣ GET `/labels/status`
**Consultar Estado de un Marbete**

### 📌 Propósito:
Ver el estado actual de un marbete específico.

### 🔐 Roles permitidos:
- Todos los roles autenticados

### 📥 Request:
```
GET /api/sigmav2/labels/status?folio=1001&periodId=1&warehouseId=14
```

### 📤 Response:
```json
{
  "folio": 1001,
  "estado": "IMPRESO",
  "productId": 123,
  "productName": "Producto A",
  "c1": 95,
  "c2": 93,
  "hasC1": true,
  "hasC2": true,
  "createdAt": "2025-12-29T10:00:00",
  "impresoAt": "2025-12-29T11:00:00"
}
```

---

## 1️⃣5️⃣ GET `/labels/product/{productId}`
**Obtener Marbetes de un Producto**

### 📌 Propósito:
Ver todos los marbetes asociados a un producto específico.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

### 📥 Request:
```
GET /api/sigmav2/labels/product/123?periodId=1&warehouseId=14
```

### 📤 Response:
```json
[
  {
    "folio": 1001,
    "estado": "IMPRESO",
    "c1": 95,
    "c2": 93,
    "impresoAt": "2025-12-29T11:00:00"
  },
  {
    "folio": 1002,
    "estado": "GENERADO",
    "c1": null,
    "c2": null,
    "impresoAt": null
  }
]
```

---

## 1️⃣6️⃣ GET `/labels/cancelled`
**Consultar Marbetes Cancelados**

### 📌 Propósito:
Ver lista de todos los marbetes cancelados.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

### 📥 Request:
```
GET /api/sigmav2/labels/cancelled?periodId=1&warehouseId=14
```

### 📤 Response:
```json
[
  {
    "folio": 1005,
    "productId": 125,
    "productName": "Producto C",
    "motivoCancelacion": "Producto no encontrado",
    "existenciasAlCancelar": 0,
    "existenciasActuales": 0,
    "canceladoAt": "2025-12-29T12:00:00",
    "canceladoBy": 10,
    "reactivado": false
  }
]
```

---

## 1️⃣7️⃣ POST `/labels/for-count/list`
**Listar Marbetes Disponibles para Conteo**

### 📌 Propósito:
Obtener lista de todos los marbetes que pueden ser contados.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

### 📥 Request:
```json
{
  "periodId": 1,
  "warehouseId": 14
}
```

### 📤 Response:
```json
[
  {
    "folio": 1001,
    "productCode": "PROD001",
    "productName": "Producto A",
    "estado": "IMPRESO",
    "existenciasSistema": 100,
    "c1": null,
    "c2": null,
    "canCount": true
  },
  {
    "folio": 1002,
    "productCode": "PROD002",
    "productName": "Producto B",
    "estado": "IMPRESO",
    "existenciasSistema": 50,
    "c1": 48,
    "c2": null,
    "canCount": true
  }
]
```

### 🎯 Cuándo usar:
- Para mostrar una lista de marbetes pendientes de contar
- Para interfaces de selección de folios
- Para dashboards de progreso

---

### ═══════════════════════════════════════
### FASE 7: REPORTES
### ═══════════════════════════════════════

---

## 1️⃣8️⃣ POST `/labels/reports/distribution`
**Reporte de Distribución de Marbetes**

### 📌 Propósito:
Ver distribución de marbetes por estado.

### 📥 Request:
```json
{
  "periodId": 1,
  "warehouseId": 14
}
```

### 📤 Response:
```json
[
  {
    "estado": "GENERADO",
    "cantidad": 10,
    "porcentaje": 10.0
  },
  {
    "estado": "IMPRESO",
    "cantidad": 80,
    "porcentaje": 80.0
  },
  {
    "estado": "CANCELADO",
    "cantidad": 10,
    "porcentaje": 10.0
  }
]
```

---

## 1️⃣9️⃣ POST `/labels/reports/with-differences`
**Reporte de Marbetes con Diferencias**

### 📌 Propósito:
Ver marbetes donde el conteo físico difiere del sistema.

### 📥 Request:
```json
{
  "periodId": 1,
  "warehouseId": 14
}
```

### 📤 Response:
```json
[
  {
    "folio": 1001,
    "productCode": "PROD001",
    "productName": "Producto A",
    "existenciasSistema": 100,
    "c1": 95,
    "c2": 93,
    "diferencia": -7,
    "porcentajeDiferencia": -7.0
  }
]
```

---

## 2️⃣0️⃣ POST `/labels/reports/pending`
**Reporte de Marbetes Pendientes**

### 📌 Propósito:
Ver marbetes que aún no han sido contados.

---

## 2️⃣1️⃣ POST `/labels/reports/cancelled`
**Reporte de Marbetes Cancelados**

### 📌 Propósito:
Ver detalles de todos los marbetes cancelados.

---

## 2️⃣2️⃣ POST `/labels/reports/comparative`
**Reporte Comparativo**

### 📌 Propósito:
Comparar existencias del sistema vs conteos físicos.

---

### ═══════════════════════════════════════
### FASE 8: GENERACIÓN DE ARCHIVO FINAL
### ═══════════════════════════════════════

---

## 2️⃣3️⃣ POST `/labels/generate-file`
**Generar Archivo TXT de Existencias**

### 📌 Propósito:
Generar el archivo de texto final con las existencias ajustadas para el sistema principal.

### 🔐 Roles permitidos:
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA

### 📥 Request:
```json
{
  "periodId": 1
}
```

### 📤 Response:
```json
{
  "fileName": "existencias_P1_20251229_143052.txt",
  "filePath": "/exports/existencias_P1_20251229_143052.txt",
  "totalRecords": 150,
  "generatedAt": "2025-12-29T14:30:52"
}
```

### 🎯 Cuándo usar:
- **Al final del proceso de inventario**
- Cuando todos los conteos están completos
- Para exportar las existencias ajustadas

### 📄 Formato del archivo:
```
PROD001|95
PROD002|48
PROD003|150
...
```

---

## 📊 Tabla Resumen de APIs

| # | Endpoint | Método | Propósito | Fase |
|---|----------|--------|-----------|------|
| 1 | `/labels/request` | POST | Solicitar folios | 1. Preparación |
| 2 | `/labels/generate` | POST | Generar marbetes | 1. Preparación |
| 3 | `/labels/generate/batch` | POST | Generar lote masivo | 1. Preparación |
| 4 | `/labels/pending-print-count` | POST | Contar pendientes | 2. Verificación |
| 5 | `/labels/debug/count` | GET | Total de marbetes | 2. Verificación |
| 6 | `/labels/summary` | POST | Resumen por producto | 2. Verificación |
| 7 | `/labels/print` | POST | **IMPRIMIR** | 3. Impresión |
| 8 | `/labels/for-count` | POST | Buscar para contar | 4. Conteo |
| 9 | `/labels/counts/c1` | POST | Registrar C1 | 4. Conteo |
| 10 | `/labels/counts/c2` | POST | Registrar C2 | 4. Conteo |
| 11 | `/labels/counts/c1` | PUT | Actualizar C1 | 5. Corrección |
| 12 | `/labels/counts/c2` | PUT | Actualizar C2 | 5. Corrección |
| 13 | `/labels/cancel` | POST | Cancelar marbete | 5. Corrección |
| 14 | `/labels/status` | GET | Estado de marbete | 6. Consulta |
| 15 | `/labels/product/{id}` | GET | Marbetes de producto | 6. Consulta |
| 16 | `/labels/cancelled` | GET | Marbetes cancelados | 6. Consulta |
| 17 | `/labels/for-count/list` | POST | Lista para contar | 6. Consulta |
| 18 | `/labels/reports/distribution` | POST | Distribución | 7. Reportes |
| 19 | `/labels/reports/with-differences` | POST | Diferencias | 7. Reportes |
| 20 | `/labels/reports/pending` | POST | Pendientes | 7. Reportes |
| 21 | `/labels/reports/cancelled` | POST | Cancelados | 7. Reportes |
| 22 | `/labels/reports/comparative` | POST | Comparativo | 7. Reportes |
| 23 | `/labels/generate-file` | POST | Archivo TXT | 8. Exportación |

---

## 🎯 Ejemplos de Flujos Completos

### Flujo 1: Proceso Normal (Sin Errores)

```javascript
// 1. Solicitar folios
await axios.post('/api/sigmav2/labels/request', {
  productId: 123,
  warehouseId: 14,
  periodId: 1,
  requestedLabels: 10
});

// 2. Generar marbetes
await axios.post('/api/sigmav2/labels/generate', {
  requestId: 456,
  periodId: 1,
  warehouseId: 14
});

// 3. Verificar pendientes
const count = await axios.post('/api/sigmav2/labels/pending-print-count', {
  periodId: 1,
  warehouseId: 14
});
// count.data.count = 10

// 4. Imprimir
const pdf = await axios.post('/api/sigmav2/labels/print', {
  periodId: 1,
  warehouseId: 14
}, { responseType: 'blob' });

// 5. Buscar marbete para contar
const marbete = await axios.post('/api/sigmav2/labels/for-count', {
  folio: 1001,
  periodId: 1,
  warehouseId: 14
});

// 6. Registrar conteo
await axios.post('/api/sigmav2/labels/counts/c1', {
  folio: 1001,
  countedValue: 95
});

// 7. Registrar segundo conteo
await axios.post('/api/sigmav2/labels/counts/c2', {
  folio: 1001,
  countedValue: 93
});
```

---

### Flujo 2: Proceso con Correcciones

```javascript
// 1-7: Igual que el flujo 1...

// 8. Corregir C1 (hubo error)
await axios.put('/api/sigmav2/labels/counts/c1', {
  folio: 1001,
  newCountedValue: 96,
  reason: "Error de captura"
});

// 9. Reporte de diferencias
const report = await axios.post('/api/sigmav2/labels/reports/with-differences', {
  periodId: 1,
  warehouseId: 14
});

// 10. Generar archivo final
const file = await axios.post('/api/sigmav2/labels/generate-file', {
  periodId: 1
});
```

---

## 🚨 Errores Comunes y Soluciones

### Error: "No hay marbetes pendientes de impresión"
**Causa:** `/labels/print` sin marbetes en estado GENERADO  
**Solución:** 
1. Verificar con `/labels/pending-print-count`
2. Si count=0, generar marbetes primero con `/labels/generate`

### Error: "Catálogos no cargados"
**Causa:** No se han importado los catálogos de inventario  
**Solución:** Cargar catálogos antes de generar marbetes

### Error: "Folio no encontrado"
**Causa:** El folio no existe para ese periodo/almacén  
**Solución:** Verificar con `/labels/status` que el folio existe

### Error: "No se puede registrar C1 porque ya existe C2"
**Causa:** Secuencia de conteos rota  
**Solución:** Validar secuencia antes de registrar

---

## 📚 Documentación Relacionada

- `SOLUCION-PROBLEMA-IMPRESION-PDF.md` - Solución de errores de impresión
- `ANALISIS-PROFUNDO-FUNCIONES-IMPRESION.md` - Análisis técnico detallado
- `RESUMEN-REFACTORIZACION-IMPRESION.md` - Cambios aplicados
- `GUIA-VISUAL-CAMBIOS-IMPRESION.md` - Comparación antes/después

---

**Documento generado:** 2025-12-29  
**Versión:** 1.0  
**Autor:** GitHub Copilot

