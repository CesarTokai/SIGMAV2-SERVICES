# Ejemplos de Uso - API de Impresión de Marbetes

Este documento proporciona ejemplos prácticos de cómo usar la API de impresión de marbetes.

---

## 1. Impresión Normal (Marbetes Recién Generados)

### Escenario
Usuario genera 50 marbetes para un producto y desea imprimirlos inmediatamente.

### Request
```http
POST /api/labels/print
Content-Type: application/json
Authorization: Bearer {token}

{
  "periodId": 1,
  "warehouseId": 250,
  "startFolio": 1001,
  "endFolio": 1050
}
```

### Response Exitosa
```json
{
  "idLabelPrint": 1,
  "periodId": 1,
  "warehouseId": 250,
  "folioInicial": 1001,
  "folioFinal": 1050,
  "cantidadImpresa": 50,
  "printedBy": 12,
  "printedAt": "2025-12-02T09:15:00"
}
```

---

## 2. Impresión Extraordinaria (Reimpresión)

### Escenario
Usuario necesita reimprimir algunos marbetes que ya fueron impresos anteriormente.

### Request
```http
POST /api/labels/print
Content-Type: application/json
Authorization: Bearer {token}

{
  "periodId": 1,
  "warehouseId": 250,
  "startFolio": 1010,
  "endFolio": 1020
}
```

### Response Exitosa
```json
{
  "idLabelPrint": 2,
  "periodId": 1,
  "warehouseId": 250,
  "folioInicial": 1010,
  "folioFinal": 1020,
  "cantidadImpresa": 11,
  "printedBy": 12,
  "printedAt": "2025-12-02T10:30:00"
}
```

### Nota
✅ El sistema permite reimprimir folios ya impresos (Impresión Extraordinaria).
✅ Los marbetes mantienen su estado IMPRESO pero se actualiza la fecha de impresión.
✅ Se crea un nuevo registro en `label_prints` para auditoría.

---

## 3. Impresión de un Solo Folio

### Escenario
Usuario necesita imprimir solo un marbete específico.

### Request
```http
POST /api/labels/print
Content-Type: application/json
Authorization: Bearer {token}

{
  "periodId": 1,
  "warehouseId": 250,
  "startFolio": 1025,
  "endFolio": 1025
}
```

### Response Exitosa
```json
{
  "idLabelPrint": 3,
  "periodId": 1,
  "warehouseId": 250,
  "folioInicial": 1025,
  "folioFinal": 1025,
  "cantidadImpresa": 1,
  "printedBy": 12,
  "printedAt": "2025-12-02T11:00:00"
}
```

---

## 4. Administrador Imprime en Cualquier Almacén

### Escenario
Usuario con rol ADMINISTRADOR imprime marbetes en un almacén diferente al asignado.

### Request
```http
POST /api/labels/print
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "periodId": 1,
  "warehouseId": 300,  // Almacén diferente
  "startFolio": 2001,
  "endFolio": 2050
}
```

### Response Exitosa
```json
{
  "idLabelPrint": 4,
  "periodId": 1,
  "warehouseId": 300,
  "folioInicial": 2001,
  "folioFinal": 2050,
  "cantidadImpresa": 50,
  "printedBy": 1,
  "printedAt": "2025-12-02T12:00:00"
}
```

### Nota
✅ Usuarios con rol **ADMINISTRADOR** o **AUXILIAR** pueden imprimir en cualquier almacén.
❌ Usuarios con otros roles solo pueden imprimir en su almacén asignado.

---

## 5. Errores Comunes y Soluciones

### Error 1: Catálogos No Cargados
```json
{
  "error": "CatalogNotLoadedException",
  "message": "No se pueden imprimir marbetes porque no se han cargado los catálogos de inventario y multialmacén para el periodo y almacén seleccionados. Por favor, importe los datos antes de continuar.",
  "status": 400
}
```

**Solución:**
1. Verificar que se hayan importado los datos de inventario
2. Verificar que se hayan importado los datos de multialmacén
3. Verificar que los datos sean del periodo y almacén correctos

---

### Error 2: Rango de Folios Inválido
```json
{
  "error": "InvalidLabelStateException",
  "message": "El folio inicial no puede ser mayor que el folio final.",
  "status": 400
}
```

**Solución:**
- Asegurarse de que `startFolio <= endFolio`
- Verificar los valores antes de enviar el request

---

### Error 3: Folios Faltantes
```json
{
  "error": "InvalidLabelStateException",
  "message": "No es posible imprimir marbetes no generados. Folios faltantes: 1005, 1007, 1010",
  "status": 400
}
```

**Solución:**
1. Verificar que todos los folios del rango hayan sido generados
2. Revisar el listado de marbetes generados
3. Generar los folios faltantes o ajustar el rango

---

### Error 4: Marbete Cancelado
```json
{
  "error": "InvalidLabelStateException",
  "message": "No es posible imprimir marbetes cancelados. Folio: 1015",
  "status": 400
}
```

**Solución:**
- No es posible imprimir marbetes cancelados
- Ajustar el rango para excluir folios cancelados
- Contactar al administrador si el marbete fue cancelado por error

---

### Error 5: Folio No Pertenece al Periodo/Almacén
```json
{
  "error": "InvalidLabelStateException",
  "message": "El folio 1020 no pertenece al periodo/almacén seleccionado.",
  "status": 400
}
```

**Solución:**
- Verificar que el folio corresponda al periodo y almacén correctos
- Consultar el listado de marbetes para verificar pertenencia

---

### Error 6: Usuario Sin Acceso
```json
{
  "error": "PermissionDeniedException",
  "message": "No tiene acceso al almacén especificado.",
  "status": 403
}
```

**Solución:**
- Usuario debe tener asignado el almacén o tener rol ADMINISTRADOR/AUXILIAR
- Contactar al administrador para solicitar acceso al almacén

---

### Error 7: Rango Muy Grande
```json
{
  "error": "InvalidLabelStateException",
  "message": "Máximo 500 folios por lote.",
  "status": 400
}
```

**Solución:**
- Dividir la impresión en múltiples requests
- Máximo 500 folios por operación

---

## 6. Verificación del Estado de Impresión

### Consultar Listado de Marbetes
```http
GET /api/labels?periodId=1&warehouseId=250&page=0&size=100
Authorization: Bearer {token}
```

### Response
```json
{
  "content": [
    {
      "folio": 1001,
      "productId": 123,
      "estado": "IMPRESO",
      "impresoAt": "2025-12-02T09:15:00",
      "printedBy": 12
    },
    {
      "folio": 1002,
      "productId": 123,
      "estado": "IMPRESO",
      "impresoAt": "2025-12-02T09:15:00",
      "printedBy": 12
    },
    {
      "folio": 1003,
      "productId": 124,
      "estado": "GENERADO",
      "impresoAt": null,
      "printedBy": null
    }
  ],
  "totalElements": 50,
  "totalPages": 1
}
```

---

## 7. Flujo Completo de Trabajo

### Paso 1: Solicitar Folios
```http
POST /api/labels/request
{
  "productId": 123,
  "warehouseId": 250,
  "periodId": 1,
  "requestedLabels": 50
}
```

### Paso 2: Generar Marbetes
```http
POST /api/labels/generate
{
  "productId": 123,
  "warehouseId": 250,
  "periodId": 1,
  "labelsToGenerate": 50
}
```

### Paso 3: Verificar Catálogos (Opcional)
```http
GET /api/inventory/stock?warehouseId=250&periodId=1
```

### Paso 4: Imprimir Marbetes
```http
POST /api/labels/print
{
  "periodId": 1,
  "warehouseId": 250,
  "startFolio": 1001,
  "endFolio": 1050
}
```

### Paso 5: Verificar Impresión
```http
GET /api/labels/prints?periodId=1&warehouseId=250
```

---

## 8. Testing con cURL

### Impresión Normal
```bash
curl -X POST http://localhost:8080/api/labels/print \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "periodId": 1,
    "warehouseId": 250,
    "startFolio": 1001,
    "endFolio": 1050
  }'
```

### Reimpresión
```bash
curl -X POST http://localhost:8080/api/labels/print \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "periodId": 1,
    "warehouseId": 250,
    "startFolio": 1010,
    "endFolio": 1020
  }'
```

---

## 9. Testing con PowerShell

### Impresión Normal
```powershell
$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer YOUR_TOKEN"
}

$body = @{
    periodId = 1
    warehouseId = 250
    startFolio = 1001
    endFolio = 1050
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/labels/print" `
    -Method POST `
    -Headers $headers `
    -Body $body
```

---

## 10. Mejores Prácticas

### ✅ DO's
- Verificar que los catálogos estén cargados antes de imprimir
- Usar rangos de folios consecutivos
- Validar el estado de los marbetes antes de reimprimir
- Mantener registro de las impresiones para auditoría
- Dividir impresiones grandes en lotes de máximo 500 folios

### ❌ DON'Ts
- No intentar imprimir más de 500 folios a la vez
- No intentar imprimir folios cancelados
- No intentar imprimir folios que no existen
- No ignorar mensajes de error relacionados con catálogos
- No intentar acceder a almacenes sin permisos

---

## 11. Monitoreo y Logs

### Logs de Impresión Exitosa
```
[INFO] Iniciando impresión de marbetes: periodId=1, warehouseId=250, startFolio=1001, endFolio=1050, userId=12, userRole=ALMACENISTA
[INFO] Usuario 12 tiene rol ALMACENISTA - validando acceso al almacén
[INFO] Intentando imprimir 50 folio(s) desde 1001 hasta 1050
[INFO] Impresión exitosa: 50 folio(s) impresos del 1001 al 1050
```

### Logs de Error
```
[ERROR] Error de validación en impresión: No es posible imprimir marbetes no generados. Folios faltantes: 1005, 1007, 1010
```

---

## Fecha de Actualización
2 de diciembre de 2025

