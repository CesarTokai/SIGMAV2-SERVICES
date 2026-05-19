# 🔐 Validaciones y Reglas de Negocio para APIs de QR/Scanner

> Aplicación de reglas de negocio del módulo de marbetes a endpoints de escaneo QR y generación de códigos
> Basado en: `REGLAS-NEGOCIO-VALIDACIONES-MARBETES.md`

---

## 📡 APIs de QR/Scanner

### 1. Generar PDF con QR Incrustados

#### Endpoint
```
POST /api/sigmav2/labels/print-with-qr
```

#### Request
```json
{
  "periodId": 16,
  "warehouseId": 369,
  "forceReprint": false
}
```

#### Validaciones Aplicadas
| # | Validación | Regla | Error |
|---|-----------|-------|-------|
| V1 | Rol requerido | `I8` | `PermissionDeniedException` |
| V2 | Acceso al almacén | `I9` | `PermissionDeniedException` |
| V3 | Marbetes no en estado CANCELADO | `I3` | `InvalidLabelStateException` |
| V4 | Máximo 500 marbetes | `I4` | `InvalidLabelStateException` |
| V5 | Marbetes en estado GENERADO o IMPRESO | `I1`, `RE1` | `InvalidLabelStateException` |
| V6 | PDF no vacío | `I7` | `InvalidLabelStateException` |

#### Respuesta Exitosa
```json
{
  "statusCode": 200,
  "message": "PDF generado con QR",
  "pdfBytes": "[bytes]",
  "totalMarbetes": 50,
  "qrGenerados": 50,
  "tamañoKB": 250
}
```

#### Respuestas de Error
- `400` → Validación fallida (marbete cancelado, límite superado)
- `403` → Sin permisos (rol, almacén)
- `404` → Almacén o período no existe

---

### 2. Generar PDF con QR para Marbetes Específicos

#### Endpoint
```
POST /api/sigmav2/labels/print-specific-with-qr
```

#### Request
```json
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [42, 43, 44, 45]
}
```

#### Validaciones Aplicadas
| # | Validación | Regla | Error |
|---|-----------|-------|-------|
| V1 | Folios requeridos | `RE1` | `InvalidLabelStateException` |
| V2 | Folios no vacíos | `RE1` | `InvalidLabelStateException` |
| V3 | Todos los folios existen | `I6` | `LabelNotFoundException` |
| V4 | Todos están en estado IMPRESO | `RE2` | `InvalidLabelStateException` |
| V5 | Ninguno está CANCELADO | `I3` | `InvalidLabelStateException` |
| V6 | Máximo 500 folios | `I4` | `InvalidLabelStateException` |
| V7 | Acceso al almacén | `I9` | `PermissionDeniedException` |
| V8 | PDF no vacío | `I7` | `InvalidLabelStateException` |

#### Respuesta Exitosa
```json
{
  "statusCode": 200,
  "message": "PDF generado para folios específicos con QR",
  "pdfBytes": "[bytes]",
  "foliosGenerados": 4,
  "qrGenerados": 4,
  "tamañoKB": 35
}
```

---

### 3. Validar QR/Folio para Escaneo Móvil ⚠️ (A Implementar)

#### Endpoint
```
POST /api/sigmav2/labels/scan/validate
```

#### Request
```json
{
  "qrCode": "42",
  "countType": "C1",
  "warehouseId": 369,
  "periodId": 16
}
```

#### Validaciones Aplicadas
| # | Validación | Regla | Error |
|---|-----------|-------|-------|
| V1 | QR/Folio no nulo | `C1-7` | `InvalidLabelStateException` |
| V2 | Folio existe en BD | `C1-7` | `LabelNotFoundException` |
| V3 | Folio pertenece al período correcto | `C1-5` | `InvalidLabelStateException` |
| V4 | Folio pertenece al almacén correcto (excepto AUX_CONTEO) | `C1-6` | `InvalidLabelStateException` |
| V5 | Marbete no está CANCELADO | `C1-2` | `InvalidLabelStateException` |
| V6 | Marbete está IMPRESO | `C1-1` | `InvalidLabelStateException` |
| V7 | **Si countType=C1**: C1 no registrado ya | `C1-3` | `DuplicateCountException` |
| V8 | **Si countType=C1**: C2 no existe sin C1 | `C1-4` | `CountSequenceException` |
| V9 | **Si countType=C2**: C1 existe | `C2-1` | `CountSequenceException` |
| V10 | **Si countType=C2**: C2 no registrado ya | `C2-4` | `DuplicateCountException` |
| V11 | Rol permitido | `C1-8`, `C2-6` | `PermissionDeniedException` |
| V12 | Usuario autenticado | General | `Unauthorized` |

#### Respuesta Exitosa (C1)
```json
{
  "valid": true,
  "folio": 42,
  "qrCode": "42",
  "productId": 15,
  "productName": "Laptop Dell Inspiron 15",
  "productCode": "WDGT-001",
  "warehouseId": 369,
  "warehouseName": "ALMACEN_A",
  "periodId": 16,
  "theoreticalQuantity": 100,
  "c1Registered": false,
  "c2Registered": false,
  "estado": "IMPRESO",
  "validationStatus": "VALID_FOR_C1",
  "message": "Marbete válido para registrar C1",
  "warnings": []
}
```

#### Respuesta Exitosa (C2 - Pendiente C2)
```json
{
  "valid": true,
  "folio": 42,
  "qrCode": "42",
  "productId": 15,
  "productName": "Laptop Dell Inspiron 15",
  "c1Registered": true,
  "c1Value": 100,
  "c2Registered": false,
  "estado": "IMPRESO",
  "validationStatus": "VALID_FOR_C2",
  "message": "Marbete listo para registrar C2",
  "warnings": ["Conteo C1 registrado en 2026-04-14 09:30:00"]
}
```

#### Respuestas de Error
```json
{
  "valid": false,
  "folio": 42,
  "validationStatus": "INVALID_CANCELLED",
  "message": "El marbete está CANCELADO",
  "errorCode": "LABEL_CANCELLED",
  "readableMessage": "No se puede escanear: marbete cancelado"
}
```

---

### 4. Registrar Conteo desde Móvil (C1 o C2) ⚠️ (A Implementar)

#### Endpoint
```
POST /api/sigmav2/labels/scan/count
```

#### Request
```json
{
  "folio": 42,
  "countType": "C1",
  "countedValue": 98,
  "warehouseId": 369,
  "periodId": 16,
  "deviceId": "device-uuid-123",
  "scanTimestamp": "2026-04-14T10:30:00Z"
}
```

#### Validaciones Aplicadas (además de las de validación QR)
| # | Validación | Regla | Error |
|---|-----------|-------|-------|
| V1 | **Todas las de validación QR** | `C1-1` a `C2-6` | Igual |
| V2 | Cantidad >= 0 | Negocio | `InvalidLabelStateException` |
| V3 | Cantidad es número (no string) | Input | `BadRequestException` |
| V4 | Timestamp válido | Auditoría | `InvalidLabelStateException` |
| V5 | Si countType=C1: registra C1 | `C1-3` | `DuplicateCountException` |
| V6 | Si countType=C2: registra C2 después de C1 | `C2-1` | `CountSequenceException` |

#### Respuesta Exitosa
```json
{
  "success": true,
  "message": "Conteo C1 registrado exitosamente",
  "folio": 42,
  "countType": "C1",
  "countedValue": 98,
  "registeredAt": "2026-04-14T10:30:15Z",
  "registeredByUserId": 5,
  "nextAction": "Proceder a registrar C2 cuando sea necesario"
}
```

---

### 5. Obtener Estado de Marbete por QR ⚠️ (A Implementar)

#### Endpoint
```
GET /api/sigmav2/labels/scan/status/{qrCode}?warehouseId=369
```

#### Validaciones Aplicadas
| # | Validación | Regla | Error |
|---|-----------|-------|-------|
| V1 | QR no nulo | General | `InvalidLabelStateException` |
| V2 | Folio existe | `C1-7` | `LabelNotFoundException` |
| V3 | Acceso al almacén | `C1-6` | `PermissionDeniedException` |
| V4 | Folio pertenece al almacén | `C1-6` | `InvalidLabelStateException` |

#### Respuesta Exitosa
```json
{
  "folio": 42,
  "qrCode": "42",
  "estado": "IMPRESO",
  "productName": "Laptop Dell Inspiron 15",
  "c1Registered": true,
  "c1Value": 98,
  "c1RegisteredAt": "2026-04-14T09:00:00Z",
  "c2Registered": false,
  "cancelado": false,
  "status": "PENDIENTE_C2",
  "message": "Conteo C1 registrado. Pendiente C2"
}
```

---

### 6. Buscar Marbete por Folio Manual ⚠️ (A Implementar)

#### Endpoint
```
GET /api/sigmav2/labels/scan/folio/{folioNumber}?warehouseId=369&countType=C1
```

#### Validaciones Aplicadas
| # | Validación | Regla | Error |
|---|-----------|-------|-------|
| V1 | folioNumber es número | Input | `BadRequestException` |
| V2 | folioNumber >= 0 | Negocio | `InvalidLabelStateException` |
| V3 | Folio existe | `C1-7` | `LabelNotFoundException` |
| V4 | Folio pertenece al almacén | `C1-6` | `InvalidLabelStateException` |
| V5 | Folio pertenece al período | `C1-5` | `InvalidLabelStateException` |
| V6 | **Todas las validaciones de `scan/validate`** | `C1-1` a `C2-6` | Igual |

#### Respuesta Exitosa
```json
{
  "valid": true,
  "folio": 42,
  "productName": "Laptop Dell Inspiron 15",
  "warehouseName": "ALMACEN_A",
  "estado": "IMPRESO",
  "c1Registered": false,
  "c2Registered": false,
  "validationStatus": "VALID_FOR_C1",
  "message": "Folio válido encontrado. Listo para escanear."
}
```

---

## 🛡️ Reglas de Negocio Específicas para QR

### Flujo de Generación de QR en Impresión
```
1. Validar acceso (V1, V2)
2. Obtener marbetes del estado correcto (V5)
3. Para CADA marbete:
   a) Validar NO está CANCELADO (V3)
   b) Llamar QRGeneratorService.generarQR(folio)
   c) Incrustar en MarbeteReportDTO
   d) Agrupar de 3 en 3 para PDF (formato Jasper)
4. Generar PDF con marbetes + QR
5. Registrar impresión en BD
6. Retornar bytes del PDF
```

### Flujo de Escaneo en Móvil
```
1. Usuario escanea QR (lee folio)
2. POST /scan/validate con folio + countType
3. Validaciones:
   - Folio existe
   - Estado IMPRESO
   - No CANCELADO
   - Secuencia C1 → C2
   - Rol permitido
4. Si válido: retorna datos del marbete + estado de conteos
5. Usuario ingresa cantidad
6. POST /scan/count registra conteo
7. Historial completo guardado en BD
```

---

## ⚠️ Casos de Error Comunes en QR

| Caso | Validación Fallida | Response |
|------|-------------------|----------|
| Escanea folio CANCELADO | `C1-2`, `C2-3` | "Marbete cancelado. No se puede escanear" |
| Intenta C2 sin C1 | `C2-1` | "Debe registrar C1 primero" |
| Intenta duplicar C1 | `C1-3` | "C1 ya fue registrado" |
| Folio pertenece otro período | `C1-5` | "Folio pertenece a otro período" |
| Folio pertenece otro almacén | `C1-6` | "Folio pertenece a otro almacén" |
| Sin acceso almacén (AUXILIAR) | `C1-6` | "Sin permisos en este almacén" |
| AUX_CONTEO con acceso completo | `C1-6` | ✅ Permitido (sin restricción) |

---

## 📝 Notas de Implementación

### DTOs Requeridos para Endpoints Mobile

```java
@Data
public class LabelScanValidationRequest {
    private String qrCode;           // "42" o "SIGMAV2-FOLIO-42-P16-W369"
    private String countType;        // "C1" o "C2"
    private Long warehouseId;
    private Long periodId;
}

@Data
public class LabelScanValidationResponse {
    private Boolean valid;
    private Long folio;
    private String qrCode;
    private Long productId;
    private String productName;
    private Integer theoreticalQuantity;
    private Boolean c1Registered;
    private Boolean c2Registered;
    private BigDecimal c1Value;
    private BigDecimal c2Value;
    private String estado;
    private String validationStatus;  // VALID_FOR_C1, VALID_FOR_C2, INVALID_CANCELLED, etc.
    private String message;
    private List<String> warnings;
}

@Data
public class LabelScanCountRequest {
    private Long folio;
    private String countType;           // "C1" o "C2"
    private BigDecimal countedValue;
    private Long warehouseId;
    private Long periodId;
    private String deviceId;
    private LocalDateTime scanTimestamp;
}

@Data
public class LabelScanCountResponse {
    private Boolean success;
    private String message;
    private Long folio;
    private String countType;
    private BigDecimal countedValue;
    private LocalDateTime registeredAt;
    private Long registeredByUserId;
    private String nextAction;
}
```

### Servicio de Validación para QR (pseudocódigo)

```java
@Service
public class LabelScanService {
    
    public LabelScanValidationResponse validateForScan(
        String qrCode, 
        String countType, 
        Long warehouseId,
        Long periodId,
        Long userId,
        String userRole) {
        
        // V1-V6: Validar folio existe y estado
        Label label = findAndValidateLabelExistsAndImpreso(qrCode, warehouseId, periodId, userId, userRole);
        
        // V7-V10: Validar secuencia C1/C2
        if ("C1".equals(countType)) {
            if (hasCountNumber(label.getFolio(), 1)) 
                throw new DuplicateCountException("C1 ya registrado");
            if (hasCountNumber(label.getFolio(), 2)) 
                throw new CountSequenceException("C2 existe sin C1");
        } else if ("C2".equals(countType)) {
            if (!hasCountNumber(label.getFolio(), 1))
                throw new CountSequenceException("C1 no existe");
            if (hasCountNumber(label.getFolio(), 2))
                throw new DuplicateCountException("C2 ya registrado");
        }
        
        // Construir respuesta con datos del marbete
        return buildValidationResponse(label, countType);
    }
}
```

---

*⚠️ Los endpoints marcados con (A Implementar) necesitan ser creados siguiendo esta especificación.*

