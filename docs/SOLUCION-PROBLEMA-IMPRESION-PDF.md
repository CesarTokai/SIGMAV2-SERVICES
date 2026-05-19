# 🔧 Solución al Problema de Impresión de Marbetes

**Fecha:** 2025-12-29  
**Problema:** API `/labels/print` no devuelve PDF ni muestra mensajes de error

---

## 📋 Diagnóstico del Problema

### Síntomas:
- API `/labels/print` no devuelve PDF
- Frontend muestra: "Unexpected Request Failure - attempted to output null or undefined value"
- API `/labels/pending-print-count` devuelve `count: 0`

### Causa Raíz:
**No hay marbetes pendientes de impresión** para el periodo y almacén especificados.

---

## 🔍 Análisis Técnico

### 1. El Flujo de la API:

```
Frontend envía: {periodId: 1, warehouseId: 14}
        ↓
Backend consulta marbetes en estado GENERADO
        ↓
Si count = 0 → No hay marbetes pendientes
        ↓
labelService.printLabels() lanza InvalidLabelStateException
        ↓
Controller SIN manejo de excepciones → retorna null
        ↓
Frontend recibe null → Error: "attempted to output null or undefined"
```

### 2. El Problema en el Código Original:

**LabelsController.java (ANTES):**
```java
@PostMapping("/print")
public ResponseEntity<byte[]> printLabels(@RequestBody PrintRequestDTO dto) {
    Long userId = getUserIdFromToken();
    String userRole = getUserRoleFromToken();
    
    // ❌ SIN TRY-CATCH - Si labelService lanza excepción, retorna null
    byte[] pdfBytes = labelService.printLabels(dto, userId, userRole);
    
    // Configurar headers...
    return ResponseEntity.ok().body(pdfBytes);
}
```

**LabelServiceImpl.java:**
```java
public byte[] printLabels(PrintRequestDTO dto, ...) {
    // ...validaciones...
    
    List<Label> labelsToProcess = getPendingLabels(dto);
    
    if (labelsToProcess.isEmpty()) {
        // ❌ Lanza excepción que no es capturada por el controller
        throw new InvalidLabelStateException(
            "No hay marbetes pendientes de impresión...");
    }
    
    // ...genera PDF...
}
```

---

## ✅ Solución Implementada

### Cambio en LabelsController.java:

```java
@PostMapping("/print")
public ResponseEntity<?> printLabels(@RequestBody PrintRequestDTO dto) {
    Long userId = getUserIdFromToken();
    String userRole = getUserRoleFromToken();
    
    log.info("Endpoint /print llamado por usuario {} con rol {}", userId, userRole);
    
    try {
        // ✅ Ahora captura excepciones
        byte[] pdfBytes = labelService.printLabels(dto, userId, userRole);
        
        // Validar que el PDF se generó correctamente
        if (pdfBytes == null || pdfBytes.length == 0) {
            log.error("El servicio retornó un PDF vacío o null");
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "No se pudo generar el PDF",
                    "message", "El PDF generado está vacío. Verifique que existan marbetes pendientes."
                ));
        }
        
        // Configurar headers y retornar PDF
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
            
    } catch (InvalidLabelStateException e) {
        // ✅ Captura "No hay marbetes pendientes"
        log.warn("Error de estado al intentar imprimir: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", "Estado inválido",
                "message", e.getMessage()
            ));
            
    } catch (LabelNotFoundException e) {
        // ✅ Captura "Folios no encontrados"
        log.warn("Folios no encontrados: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", "Folios no encontrados",
                "message", e.getMessage()
            ));
            
    } catch (CatalogNotLoadedException e) {
        // ✅ Captura "Catálogos no cargados"
        log.warn("Catálogos no cargados: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", "Catálogos no cargados",
                "message", e.getMessage()
            ));
            
    } catch (PermissionDeniedException e) {
        // ✅ Captura "Permiso denegado"
        log.warn("Permiso denegado: {}", e.getMessage());
        return ResponseEntity.status(403)
            .body(Map.of(
                "error", "Permiso denegado",
                "message", e.getMessage()
            ));
            
    } catch (Exception e) {
        // ✅ Captura cualquier otro error
        log.error("Error inesperado al generar PDF de marbetes", e);
        return ResponseEntity.status(500)
            .body(Map.of(
                "error", "Error interno del servidor",
                "message", "Error al generar el PDF de marbetes: " + e.getMessage()
            ));
    }
}
```

---

## 🎯 Beneficios de la Solución

### ANTES:
- ❌ Excepción no capturada → retorna `null`
- ❌ Frontend muestra error genérico
- ❌ Usuario no sabe qué pasó

### AHORA:
- ✅ Todas las excepciones capturadas
- ✅ Respuestas JSON estructuradas con error y mensaje
- ✅ Frontend puede mostrar mensaje claro al usuario
- ✅ Logs detallados en backend

---

## 📊 Respuestas de la API

### Caso 1: No hay marbetes pendientes
**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 1,
  "warehouseId": 14
}
```

**Response:** `400 Bad Request`
```json
{
  "error": "Estado inválido",
  "message": "No hay marbetes pendientes de impresión para el periodo y almacén especificados"
}
```

### Caso 2: Folios no encontrados
**Request:**
```json
POST /api/sigmav2/labels/print
{
  "periodId": 1,
  "warehouseId": 14,
  "folios": [999, 1000]
}
```

**Response:** `400 Bad Request`
```json
{
  "error": "Folios no encontrados",
  "message": "Los siguientes folios no existen para periodo 1 y almacén 14: [999, 1000]"
}
```

### Caso 3: Catálogos no cargados
**Response:** `400 Bad Request`
```json
{
  "error": "Catálogos no cargados",
  "message": "No se pueden imprimir marbetes porque no se han cargado los catálogos de inventario y multialmacén..."
}
```

### Caso 4: Éxito (hay marbetes pendientes)
**Response:** `200 OK`
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="marbetes_P1_A14_20251229_143052.pdf"

[BINARY PDF DATA]
```

---

## 🔧 Pasos para Resolver el Problema del Usuario

### Paso 1: Verificar si hay marbetes pendientes
```bash
# Consultar endpoint de conteo
POST /api/sigmav2/labels/pending-print-count
{
  "periodId": 1,
  "warehouseId": 14
}

# Si count = 0, no hay marbetes pendientes
```

### Paso 2: Generar marbetes si no existen
```bash
# Primero solicitar folios
POST /api/sigmav2/labels/request
{
  "productId": 123,
  "warehouseId": 14,
  "periodId": 1,
  "requestedLabels": 10
}

# Luego generar los marbetes
POST /api/sigmav2/labels/generate
{
  "requestId": [ID_de_la_solicitud],
  "periodId": 1,
  "warehouseId": 14
}
```

### Paso 3: Ahora sí imprimir
```bash
POST /api/sigmav2/labels/print
{
  "periodId": 1,
  "warehouseId": 14
}
```

---

## 🗄️ Consultas SQL para Diagnóstico

### Verificar si hay marbetes para el periodo/almacén:
```sql
SELECT 
    estado,
    COUNT(*) as cantidad
FROM labels
WHERE period_id = 1 
  AND warehouse_id = 14
GROUP BY estado;
```

**Resultado esperado:**
```
estado      | cantidad
------------|----------
GENERADO    |   0     ← No hay pendientes
IMPRESO     |  150
CANCELADO   |   5
```

### Verificar solicitudes de folios:
```sql
SELECT 
    lr.id,
    lr.product_id,
    lr.requested_labels,
    lr.folios_generados,
    p.descr as producto
FROM label_requests lr
LEFT JOIN products p ON p.id_product = lr.product_id
WHERE lr.period_id = 1 
  AND lr.warehouse_id = 14;
```

---

## 📝 Frontend - Manejo Recomendado

### Actualizar el código del frontend para mostrar errores:

```javascript
async function imprimirMarbetes(periodId, warehouseId) {
    try {
        const response = await axios.post('/api/sigmav2/labels/print', {
            periodId,
            warehouseId
        }, {
            responseType: 'blob'  // Importante para PDFs
        });
        
        // Si es exitoso, descargar el PDF
        const blob = new Blob([response.data], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `marbetes_P${periodId}_A${warehouseId}.pdf`;
        link.click();
        
    } catch (error) {
        // ✅ Manejar errores estructurados
        if (error.response) {
            const errorData = error.response.data;
            
            // Si la respuesta es un Blob (error en formato JSON dentro de blob)
            if (errorData instanceof Blob) {
                const text = await errorData.text();
                const json = JSON.parse(text);
                mostrarError(json.message || json.error);
            } else {
                // Respuesta JSON directa
                mostrarError(errorData.message || errorData.error);
            }
        } else {
            mostrarError('Error de conexión con el servidor');
        }
    }
}

function mostrarError(mensaje) {
    // Mostrar al usuario de forma amigable
    alert(`Error: ${mensaje}`);
    // O usar un componente de notificaciones más elegante
}
```

---

## ✅ Checklist de Verificación

- [x] ✅ Manejo de excepciones agregado al controlador
- [x] ✅ Respuestas JSON estructuradas
- [x] ✅ Logs detallados en backend
- [ ] ⬜ Frontend actualizado para mostrar errores
- [ ] ⬜ Verificar que existan marbetes pendientes en BD
- [ ] ⬜ Generar marbetes si no existen
- [ ] ⬜ Probar API después de los cambios

---

## 🎯 Próximos Pasos

1. **Compilar el backend** con los cambios aplicados
2. **Verificar en la base de datos** si hay marbetes en estado GENERADO
3. **Si no hay marbetes**, ejecutar el proceso de generación
4. **Actualizar el frontend** para manejar respuestas de error
5. **Probar la impresión** nuevamente

---

**Documento generado:** 2025-12-29  
**Problema resuelto:** Manejo de excepciones en endpoint de impresión  
**Estado:** ✅ SOLUCIONADO (falta compilar y probar)

