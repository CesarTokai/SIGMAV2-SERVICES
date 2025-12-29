# Ejemplos de Respuestas: API de Impresión Automática

**Propósito:** Documentar las respuestas exitosas y de error de la nueva API de impresión para facilitar el desarrollo frontend y debugging.

---

## 📤 Endpoint

```
POST /api/sigmav2/labels/print
```

**Autenticación:** Bearer Token (JWT)

**Content-Type:** application/json

---

## ✅ Respuestas Exitosas

### Caso 1: Impresión Automática Exitosa

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369
}
```

**Response:**
- **Status:** 200 OK
- **Content-Type:** application/pdf
- **Headers:**
  ```
  Content-Disposition: attachment; filename="marbetes_P16_A369_20251216_120530.pdf"
  Content-Length: 245632
  ```
- **Body:** Binary PDF data

**Descripción:**
- Se generó PDF con todos los marbetes pendientes
- Los marbetes se marcaron como IMPRESOS
- Se registró la impresión en `label_print`

---

### Caso 2: Impresión por Producto Exitosa

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369,
  "productId": 123
}
```

**Response:**
- **Status:** 200 OK
- **Content-Type:** application/pdf
- **Headers:**
  ```
  Content-Disposition: attachment; filename="marbetes_P16_A369_20251216_120645.pdf"
  Content-Length: 45120
  ```
- **Body:** Binary PDF data

**Descripción:**
- PDF contiene solo marbetes del producto 123
- Menos folios que impresión completa

---

### Caso 3: Reimpresión Selectiva Exitosa

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [25, 26, 27],
  "forceReprint": true
}
```

**Response:**
- **Status:** 200 OK
- **Content-Type:** application/pdf
- **Headers:**
  ```
  Content-Disposition: attachment; filename="marbetes_P16_A369_20251216_120755.pdf"
  Content-Length: 12480
  ```
- **Body:** Binary PDF data

**Descripción:**
- PDF contiene exactamente 3 marbetes (folios 25, 26, 27)
- Se registró como reimpresión

---

## ❌ Respuestas de Error

### Error 1: No Hay Marbetes Pendientes

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369
}
```

**Response:**
- **Status:** 400 Bad Request
- **Content-Type:** application/json
- **Body:**
  ```json
  {
    "success": false,
    "message": "No hay marbetes pendientes de impresión para el periodo y almacén especificados",
    "error": "INVALID_LABEL_STATE",
    "timestamp": "2025-12-16T12:10:30.123456"
  }
  ```

**Causa:**
- Todos los marbetes ya están impresos
- No se han generado marbetes

**Acción Frontend:**
```javascript
if (error.message.includes('No hay marbetes pendientes')) {
  // Mostrar mensaje amigable
  alert('Todos los marbetes ya están impresos. ¿Desea reimprimir?');
  // Ofrecer opción de reimpresión
}
```

---

### Error 2: Reimprimir sin Autorización

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [25],
  "forceReprint": false
}
```

**Response:**
- **Status:** 400 Bad Request
- **Content-Type:** application/json
- **Body:**
  ```json
  {
    "success": false,
    "message": "El folio 25 ya está IMPRESO. Use forceReprint=true para reimprimir",
    "error": "INVALID_LABEL_STATE",
    "timestamp": "2025-12-16T12:15:45.789012"
  }
  ```

**Causa:**
- Intentó reimprimir folio ya impreso
- Flag `forceReprint` no está en `true`

**Acción Frontend:**
```javascript
if (error.message.includes('Use forceReprint=true')) {
  // Pedir confirmación al usuario
  if (confirm('Este folio ya está impreso. ¿Desea reimprimirlo?')) {
    // Reintentar con forceReprint: true
    reimprimir(folios, true);
  }
}
```

---

### Error 3: Folio Cancelado

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [50]
}
```

**Response:**
- **Status:** 400 Bad Request
- **Content-Type:** application/json
- **Body:**
  ```json
  {
    "success": false,
    "message": "El folio 50 está CANCELADO y no se puede imprimir",
    "error": "INVALID_LABEL_STATE",
    "timestamp": "2025-12-16T12:20:15.345678"
  }
  ```

**Causa:**
- El folio fue cancelado previamente
- Marbetes cancelados no se pueden imprimir

**Acción Frontend:**
```javascript
if (error.message.includes('CANCELADO')) {
  alert('No se puede imprimir este folio porque ha sido cancelado.');
  // No ofrecer opción de reimprimir
}
```

---

### Error 4: Folio No Encontrado

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [999]
}
```

**Response:**
- **Status:** 404 Not Found
- **Content-Type:** application/json
- **Body:**
  ```json
  {
    "success": false,
    "message": "Folio 999 no encontrado para periodo 16 y almacén 369",
    "error": "LABEL_NOT_FOUND",
    "timestamp": "2025-12-16T12:25:30.567890"
  }
  ```

**Causa:**
- El folio no existe
- Período o almacén incorrectos

**Acción Frontend:**
```javascript
if (error.error === 'LABEL_NOT_FOUND') {
  alert('El folio especificado no existe. Verifique el número.');
}
```

---

### Error 5: Sin Acceso al Almacén

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 999
}
```

**Response:**
- **Status:** 403 Forbidden
- **Content-Type:** application/json
- **Body:**
  ```json
  {
    "success": false,
    "message": "No tiene acceso al almacén especificado",
    "error": "PERMISSION_DENIED",
    "timestamp": "2025-12-16T12:30:45.123456"
  }
  ```

**Causa:**
- Usuario no tiene permisos para ese almacén
- Solo para roles ALMACENISTA (limitado a su almacén)

**Acción Frontend:**
```javascript
if (error.error === 'PERMISSION_DENIED') {
  alert('No tiene permisos para imprimir en este almacén.');
  // Redirigir a selección de almacén
}
```

---

### Error 6: Catálogos No Cargados

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369
}
```

**Response:**
- **Status:** 400 Bad Request
- **Content-Type:** application/json
- **Body:**
  ```json
  {
    "success": false,
    "message": "No se pueden imprimir marbetes porque no se han cargado los catálogos de inventario y multialmacén para el periodo y almacén seleccionados. Por favor, importe los datos antes de continuar.",
    "error": "CATALOG_NOT_LOADED",
    "timestamp": "2025-12-16T12:35:00.789012"
  }
  ```

**Causa:**
- No se han importado datos de inventario
- Falta ejecutar carga de catálogos

**Acción Frontend:**
```javascript
if (error.error === 'CATALOG_NOT_LOADED') {
  alert('Primero debe importar los catálogos de inventario.');
  // Redirigir a módulo de importación
}
```

---

### Error 7: Token Inválido

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369
}
```

**Headers:**
```
Authorization: Bearer token_invalido_o_expirado
```

**Response:**
- **Status:** 401 Unauthorized
- **Content-Type:** application/json
- **Body:**
  ```json
  {
    "success": false,
    "message": "Token inválido o expirado",
    "error": "UNAUTHORIZED",
    "timestamp": "2025-12-16T12:40:15.345678"
  }
  ```

**Causa:**
- Token JWT expirado
- Token inválido o manipulado

**Acción Frontend:**
```javascript
if (response.status === 401) {
  // Eliminar token
  localStorage.removeItem('token');
  // Redirigir a login
  window.location.href = '/login';
}
```

---

### Error 8: Validación de Campos

**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": null,
  "warehouseId": 369
}
```

**Response:**
- **Status:** 400 Bad Request
- **Content-Type:** application/json
- **Body:**
  ```json
  {
    "success": false,
    "message": "Errores de validación",
    "errors": [
      {
        "field": "periodId",
        "message": "El periodo es obligatorio"
      }
    ],
    "error": "VALIDATION_ERROR",
    "timestamp": "2025-12-16T12:45:30.567890"
  }
  ```

**Causa:**
- Campos obligatorios faltantes
- Valores inválidos

**Acción Frontend:**
```javascript
if (error.errors) {
  // Mostrar errores por campo
  error.errors.forEach(err => {
    showFieldError(err.field, err.message);
  });
}
```

---

## 🔍 Debugging Tips

### Verificar Estado de Marbetes

**Antes de imprimir, consultar:**
```http
POST /api/sigmav2/labels/for-count/list
{
  "periodId": "16",
  "warehouseId": "369"
}
```

**Respuesta incluye:**
```json
[
  {
    "folio": 1,
    "estado": "GENERADO",    // ← Pendiente de imprimir
    "impreso": false,
    ...
  },
  {
    "folio": 2,
    "estado": "IMPRESO",     // ← Ya impreso
    "impreso": true,
    ...
  },
  {
    "folio": 3,
    "estado": "CANCELADO",   // ← Cancelado
    "cancelado": true,
    ...
  }
]
```

### Estados Posibles de Marbetes

| Estado | Descripción | Se puede imprimir |
|--------|-------------|-------------------|
| **GENERADO** | Marbete generado, pendiente de impresión | ✅ Sí (automático) |
| **IMPRESO** | Marbete ya impreso | ⚠️ Solo con forceReprint |
| **CANCELADO** | Marbete cancelado | ❌ No |

---

## 📝 Ejemplo Completo de Flujo

### Frontend: Función de Impresión

```javascript
async function imprimirMarbetesPendientes(periodId, warehouseId) {
  try {
    // 1. Mostrar loading
    showLoading('Generando PDF...');

    // 2. Hacer request
    const response = await fetch('/api/sigmav2/labels/print', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${getToken()}`
      },
      body: JSON.stringify({
        periodId: periodId,
        warehouseId: warehouseId
      })
    });

    // 3. Manejar respuesta
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    // 4. Descargar PDF
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `marbetes_${periodId}_${warehouseId}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);

    // 5. Mostrar éxito
    hideLoading();
    showSuccess('PDF generado exitosamente');

    // 6. Actualizar lista de marbetes
    await refreshMarbetesList();

  } catch (error) {
    hideLoading();

    // Manejar errores específicos
    if (error.message.includes('No hay marbetes pendientes')) {
      // Ofrecer reimpresión
      const reprint = confirm(
        'Todos los marbetes ya están impresos.\n' +
        '¿Desea reimprimir algunos folios específicos?'
      );
      if (reprint) {
        showReprintDialog(periodId, warehouseId);
      }
    } else if (error.message.includes('catálogos')) {
      // Error de catálogos
      showError('Primero debe importar los catálogos de inventario');
      redirectToImportacion();
    } else if (error.message.includes('acceso')) {
      // Sin permisos
      showError('No tiene permisos para este almacén');
    } else {
      // Error genérico
      showError('Error al imprimir: ' + error.message);
    }
  }
}
```

---

## 🧪 Testing con cURL

### Impresión Automática

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "periodId": 16,
    "warehouseId": 369
  }' \
  --output marbetes.pdf
```

### Reimpresión con Folios Específicos

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "periodId": 16,
    "warehouseId": 369,
    "folios": [25, 26, 27],
    "forceReprint": true
  }' \
  --output reimpresion.pdf
```

### Verificar Error (sin forceReprint)

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/print \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "periodId": 16,
    "warehouseId": 369,
    "folios": [25],
    "forceReprint": false
  }'
```

---

## 📊 Logs del Servidor

### Log de Impresión Exitosa

```
2025-12-16T12:50:00.123 INFO  [LabelsController] Endpoint /print llamado por usuario 1 con rol ADMINISTRADOR
2025-12-16T12:50:00.125 INFO  [LabelServiceImpl] Iniciando impresión de marbetes: periodId=16, warehouseId=369, userId=1, userRole=ADMINISTRADOR
2025-12-16T12:50:00.127 INFO  [LabelServiceImpl] Modo automático: Imprimiendo todos los marbetes pendientes
2025-12-16T12:50:00.132 INFO  [LabelServiceImpl] Encontrados 25 marbetes pendientes de impresión
2025-12-16T12:50:00.145 INFO  [LabelServiceImpl] Impresión registrada exitosamente: 25 folio(s) del 1 al 25
2025-12-16T12:50:00.187 INFO  [LabelServiceImpl] Generando PDF con 25 marbetes...
2025-12-16T12:50:01.234 INFO  [LabelServiceImpl] PDF generado exitosamente: 245 KB
2025-12-16T12:50:01.236 INFO  [LabelsController] Retornando PDF de 245 KB
```

### Log de Error (No hay pendientes)

```
2025-12-16T12:55:00.123 INFO  [LabelsController] Endpoint /print llamado por usuario 1 con rol ADMINISTRADOR
2025-12-16T12:55:00.125 INFO  [LabelServiceImpl] Iniciando impresión de marbetes: periodId=16, warehouseId=369, userId=1, userRole=ADMINISTRADOR
2025-12-16T12:55:00.127 INFO  [LabelServiceImpl] Modo automático: Imprimiendo todos los marbetes pendientes
2025-12-16T12:55:00.130 ERROR [LabelServiceImpl] No hay marbetes pendientes de impresión
```

---

**Última actualización:** 2025-12-16

