# 📋 PLAN REAL DE IMPLEMENTACIÓN - QR MÓVIL v2 SIMPLIFICADA

**Fecha:** 24 de Marzo 2026  
**Status:** LISTO PARA IMPLEMENTACIÓN  
**Base:** Revisión de código existente + feedback usuario

---

## 🎯 HALLAZGOS DEL ANÁLISIS

### ✅ YA EXISTE EN BACKEND

1. **Endpoint:** `GET /api/sigmav2/labels/by-folio/{folio}`
   - **DTO:** `LabelStatusResponseDTO`
   - **Campos:** folio, productId, nombreProducto, estado, impreso, etc.
   - ❌ **PROBLEMA:** No incluye información de conteos (C1, C2, teórico)

2. **Endpoint:** `GET /api/sigmav2/labels/for-count` (GET o POST)
   - **DTO:** `LabelForCountDTO`
   - **Campos:** ✅ Sí incluye `conteo1`, `conteo2`, `existQty`, `diferencia`
   - ✅ **MEJOR OPCIÓN:** Este es el que debe usarse en móvil

3. **Tabla:** `label_count_events` (existe)
   - Tiene: `idCountEvent`, `folio`, `userId`, `countNumber`, `countedValue`, `createdAt`
   - ❌ **FALTA:** Campos `device_id` y `scan_timestamp` para auditoría móvil

4. **Endpoints existentes:** C1 y C2
   - `POST /api/sigmav2/labels/counts/c1`
   - `POST /api/sigmav2/labels/counts/c2`
   - Pero no llevan metadata del dispositivo

---

## 🔧 LO QUE NECESITAMOS HACER

### PASO 1: AMPLIAR `label_count_events` (Migration Flyway)

```sql
-- V1_2_3__Add_mobile_audit_fields_to_label_count_events.sql

ALTER TABLE label_count_events ADD COLUMN device_id VARCHAR(255) DEFAULT NULL AFTER updated_by;
ALTER TABLE label_count_events ADD COLUMN scan_timestamp DATETIME DEFAULT NULL AFTER device_id;

-- Índices para búsquedas futuras
CREATE INDEX idx_label_count_events_device_id ON label_count_events(device_id);
CREATE INDEX idx_label_count_events_scan_timestamp ON label_count_events(scan_timestamp);
```

### PASO 2: ACTUALIZAR ENTIDAD `LabelCountEvent`

```java
// En LabelCountEvent.java - agregar campos:

@Column(name = "device_id")
private String deviceId;

@Column(name = "scan_timestamp")
private LocalDateTime scanTimestamp;
```

### PASO 3: CREAR DTO PARA MÓVIL (Reutilizar existente + extender)

```java
// NO crear nuevo, sino actualizar LabelForCountDTO

// LabelForCountDTO.java - agregar:
private Boolean readyForC2;          // ¿Puede registrar C2?
private String c1RegisteredBy;       // Usuario que registró C1
private String c2RegisteredBy;       // Usuario que registró C2
private LocalDateTime c1RegisteredAt;
private LocalDateTime c2RegisteredAt;
```

### PASO 4: CREAR NUEVO ENDPOINT ÚNICO

**Ruta:** `POST /api/sigmav2/labels/scan/count`

**Propósito:** Registrar C1/C2 con metadata móvil

```java
// En LabelsController.java - agregar:

@PostMapping("/scan/count")
@PreAuthorize("hasAnyRole('ALMACENISTA','AUXILIAR_DE_CONTEO')")
@Auditable(action = "REGISTER_MOBILE_COUNT", resource = "LABEL")
public ResponseEntity<MobileCountResponse> registerMobileCount(
    @Valid @RequestBody MobileCountRequest request
) {
    Long userId = getUserIdFromToken();
    MobileCountResponse response = labelService.registerMobileCountWithDeviceAudit(
        request.getFolio(),
        request.getCountType(),
        request.getQuantity(),
        request.getWarehouseId(),
        request.getPeriodId(),
        request.getDeviceId(),
        request.getScanTimestamp(),
        userId
    );
    return ResponseEntity.status(201).body(response);
}
```

### PASO 5: SERVICIO (Agregar método en LabelService)

```java
// En LabelApplicationService.java - agregar método:

@Transactional
public MobileCountResponse registerMobileCountWithDeviceAudit(
    Long folio,
    String countType,
    Integer quantity,
    Long warehouseId,
    Long periodId,
    String deviceId,
    LocalDateTime scanTimestamp,
    Long userId
) {
    // 1. Validar cantidad
    if (quantity == null || quantity < 0 || quantity > 999999) {
        throw new ValidationException("Cantidad inválida: " + quantity);
    }
    
    // 2. Obtener label y validar
    Label label = labelRepository.findById(folio)
        .orElseThrow(() -> new LabelNotFoundException(folio));
    
    if (!label.getEstado().equals(Label.State.IMPRESO)) {
        throw new ValidationException("Marbete debe estar IMPRESO");
    }
    
    if (!label.getWarehouseId().equals(warehouseId)) {
        throw new ValidationException("Almacén no coincide");
    }
    
    // 3. Obtener o crear LabelCount
    LabelCount counts = labelCountRepository.findByFolio(folio)
        .orElse(new LabelCount());
    counts.setFolio(folio);
    
    // 4. Validar según countType
    LocalDateTime now = LocalDateTime.now();
    if ("C1".equals(countType)) {
        if (counts.getOneCount() != null) {
            throw new ValidationException("C1 ya está registrado: " + counts.getOneCount());
        }
        counts.setOneCount((long) quantity);
        counts.setOneCountBy(userId);
        counts.setOneCountAt(now);
    } else if ("C2".equals(countType)) {
        if (counts.getOneCount() == null) {
            throw new ValidationException("Debe registrar C1 antes de C2");
        }
        if (counts.getSecondCount() != null) {
            throw new ValidationException("C2 ya está registrado: " + counts.getSecondCount());
        }
        counts.setSecondCount((long) quantity);
        counts.setSecondCountBy(userId);
        counts.setSecondCountAt(now);
    } else {
        throw new ValidationException("CountType debe ser C1 o C2");
    }
    
    // 5. Guardar LabelCount
    labelCountRepository.save(counts);
    
    // 6. Guardar evento con metadata móvil
    LabelCountEvent event = new LabelCountEvent();
    event.setFolio(folio);
    event.setUserId(userId);
    event.setCountNumber("C1".equals(countType) ? 1 : 2);
    event.setCountedValue(new BigDecimal(quantity));
    event.setRoleAtTime(getUserRole());  // ADMINISTRADOR, ALMACENISTA, etc.
    event.setCreatedAt(now);
    event.setDeviceId(deviceId);         // NUEVO ✅
    event.setScanTimestamp(scanTimestamp != null ? scanTimestamp : now);  // NUEVO ✅
    labelCountEventRepository.save(event);
    
    // 7. Calcular varianza
    Integer theoretical = inventoryRepository
        .findTheoricalQuantity(label.getProductId(), warehouseId, periodId)
        .orElse(0);
    
    Integer variance = null;
    if ("C2".equals(countType)) {
        variance = quantity - theoretical;
    }
    
    return MobileCountResponse.builder()
        .success(true)
        .folio(folio)
        .countType(countType)
        .quantity(quantity)
        .registeredAt(now)
        .variance(variance)
        .message("✓ " + countType + " registrado exitosamente")
        .build();
}
```

---

## 📱 FLUJO FINAL EN FLUTTER (SIMPLIFICADO)

```
[LOGIN]
  ↓ POST /auth/login
  ↓
[HOME SCREEN]
  ├─ GET /warehouses (dropdown)
  ├─ GET /periods/active (validar)
  ↓
[SCANNER]
  ├─ Usuario escanea QR o ingresa folio: "42"
  ↓
  ├─ GET /labels/for-count?folio=42&warehouseId=369&periodId=16
  ↓
[VALIDACIÓN MOSTRADA EN FLUTTER]
  ├─ Producto: Laptop Dell
  ├─ Teórico: 100
  ├─ C1: ✓ Registrado (95) | ⏳ No registrado
  ├─ C2: ⏳ No disponible (requiere C1)
  ├─ Estado: IMPRESO
  ↓
[USUARIO DECIDE: ¿Registrar C1 o C2?]
  ├─ Ingresar cantidad: 95
  ↓
  ├─ POST /labels/scan/count
  │   {
  │     "folio": 42,
  │     "countType": "C1",
  │     "quantity": 95,
  │     "warehouseId": 369,
  │     "periodId": 16,
  │     "deviceId": "device-uuid-12345",
  │     "scanTimestamp": "2026-03-24T14:35:22Z"
  │   }
  ↓
[CONFIRMACIÓN]
  ├─ ✓ C1 Registrado
  ├─ Varianza: -5 (95 vs 100 teórico)
  ├─ [🔄 Siguiente Marbete]
  ↓
[VOLVER A SCANNER]
```

---

## 📊 DTOs NECESARIOS

### REQUEST: `MobileCountRequest.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileCountRequest {
    
    @NotNull(message = "Folio requerido")
    @Positive(message = "Folio debe ser positivo")
    private Long folio;
    
    @NotBlank(message = "CountType requerido")
    @Pattern(regexp = "C1|C2", message = "Debe ser C1 o C2")
    private String countType;
    
    @NotNull(message = "Cantidad requerida")
    @Min(value = 0, message = "Cantidad debe ser >= 0")
    @Max(value = 999999, message = "Cantidad debe ser <= 999999")
    private Integer quantity;
    
    @NotNull(message = "WarehouseId requerido")
    private Long warehouseId;
    
    @NotNull(message = "PeriodId requerido")
    private Long periodId;
    
    @NotBlank(message = "DeviceId requerido")
    @Size(min = 5, max = 255, message = "DeviceId entre 5 y 255 caracteres")
    private String deviceId;
    
    @NotNull(message = "ScanTimestamp requerido")
    private LocalDateTime scanTimestamp;
}
```

### RESPONSE: `MobileCountResponse.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileCountResponse {
    
    private boolean success;
    private Long folio;
    private String countType;
    private Integer quantity;
    private LocalDateTime registeredAt;
    private Integer variance;          // null si es C1, -valor si es C2
    private String message;
}
```

---

## 🎯 RESUMEN: QUÉ IMPLEMENTAR

| Item | Status | Detalle |
|------|--------|---------|
| Migration SQL (device_id, scan_timestamp) | 🆕 | `V1_2_3__Add_mobile_audit_fields...sql` |
| Actualizar LabelCountEvent.java | 🔧 | Agregar 2 campos |
| Crear MobileCountRequest.java | 🆕 | DTO para request |
| Crear MobileCountResponse.java | 🆕 | DTO para response |
| Agregar método en LabelService | 🆕 | `registerMobileCountWithDeviceAudit()` |
| Agregar endpoint en LabelsController | 🆕 | `POST /scan/count` |
| ❌ NO crear endpoint validación | ✅ | Reutilizar `/for-count` existente |
| ❌ NO crear endpoint status | ✅ | Reutilizar `/for-count` existente |

---

## 📝 ORDEN DE EJECUCIÓN

1. **Crear migration SQL** → Ejecutar
2. **Actualizar LabelCountEvent** → Compilar
3. **Crear DTOs** → 2 archivos
4. **Extender LabelService** → 1 método
5. **Extender LabelsController** → 1 endpoint
6. **Test manual** → Verificar flujo completo

---

## 🧪 TEST MANUAL POST-IMPLEMENTACIÓN

```bash
# 1. Login
curl -X POST http://localhost:8080/api/sigmav2/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan@tokai.mx","password":"pass123"}'

# 2. Copiar token

# 3. Obtener marbete para conteo
curl -X GET "http://localhost:8080/api/sigmav2/labels/for-count?folio=42&warehouseId=369&periodId=16" \
  -H "Authorization: Bearer {token}"

# 4. Registrar C1 MÓVIL
curl -X POST http://localhost:8080/api/sigmav2/labels/scan/count \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "folio": 42,
    "countType": "C1",
    "quantity": 95,
    "warehouseId": 369,
    "periodId": 16,
    "deviceId": "device-uuid-12345",
    "scanTimestamp": "2026-03-24T14:35:22Z"
  }'

# 5. Verificar en BD
SELECT * FROM label_count_events WHERE folio = 42 AND count_number = 1;
```

---

## 💡 VENTAJAS DE ESTA SOLUCIÓN

✅ **Reutiliza endpoints existentes** (login, warehouses, periods, for-count)  
✅ **Solo 1 endpoint nuevo** (scan/count)  
✅ **Auditoría completa** (device_id + scan_timestamp)  
✅ **Sin redundancia** (no valida + status duplicados)  
✅ **Simple para Flutter** (4 pasos claros)  
✅ **Fácil mantenimiento** (cambios centralizados)  

---

## ❓ PREGUNTA SOBRE QR vs CÓDIGO DE BARRAS

En tu pregunta anterior:

> Antes que nada quiero preguntarte que es mejor utilizar qr o codigo de barras?

**RESPUESTA:**

### QR ✅ MEJOR PARA SIGMAV2
- **Capacidad:** 2953 bytes de datos (vs 20 caracteres en código de barras)
- **Información:** Puede codificar: `SIGMAV2-FOLIO-42-P16-W369-PRODUCTO-XYZ`
- **Redundancia:** 30% corrección de errores (sigue leyéndose si está dañado)
- **Escalabilidad:** Una sola imagen contiene TODO
- **Móvil:** Cámaras modernas escanean QR al instante
- **UX:** Usuario simplemente apunta (vs tener que posicionar en línea de código de barras)

### Código de Barras ❌ LIMITACIÓN
- Solo codifica número secuencial: "42"
- Después debe validar en BD
- Si se daña parte → no se lee
- Requiere posicionamiento preciso en scanner

**CONCLUSIÓN:** Para móvil con Flutter + cámara, **QR es la opción correcta**. Codifica todo el contexto del marbete en una imagen, haciendo el flujo más simple.

**Próximo paso:** ¿Implemento esto ahora o quieres que profundizamos en algún detalle de la arquitectura?


