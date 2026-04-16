# 💻 Guía de Implementación — APIs de QR con Validaciones

> Ejemplos prácticos de cómo implementar las APIs de escaneo QR aplicando todas las validaciones y reglas de negocio

---

## Estructura de Carpetas

```
modules/labels/
├── domain/
│   ├── model/
│   │   └── Label.java          (ya existe)
│   └── port/
│       └── input/
│           └── LabelScanService.java    (NUEVO - interfaz)
├── application/
│   ├── dto/
│   │   └── scan/
│   │       ├── LabelScanValidationRequest.java      (NUEVO)
│   │       ├── LabelScanValidationResponse.java     (NUEVO)
│   │       ├── LabelScanCountRequest.java           (NUEVO)
│   │       └── LabelScanCountResponse.java          (NUEVO)
│   └── service/
│       ├── LabelScanApplicationService.java         (NUEVO - implementa puerto)
│       ├── QRGeneratorService.java                  (ya existe)
│       └── MarbeteQRIntegrationService.java        (ya existe)
├── adapter/
│   ├── web/
│   │   ├── LabelScanMobileController.java           (NUEVO)
│   │   └── dto/
│   │       └── (DTOs para controller)
│   └── mapper/
│       └── LabelScanMapper.java                     (NUEVO)
└── infrastructure/
    └── persistence/
        └── JpaLabelScanRepository.java              (NUEVO)
```

---

## 1️⃣ Puerto de Dominio (Interfaz)

### `modules/labels/domain/port/input/LabelScanService.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.domain.port.input;

import tokai.com.mx.SIGMAV2.modules.labels.application.dto.scan.*;
import java.util.List;

/**
 * Puerto de entrada para operaciones de escaneo QR/folio en móvil.
 */
public interface LabelScanService {
    
    /**
     * Valida si un QR/folio es válido para registrar el conteo indicado
     * 
     * @param qrCode Código escaneado o folio manual (ej: "42")
     * @param countType "C1" o "C2"
     * @param warehouseId ID del almacén
     * @param periodId ID del período
     * @param userId ID del usuario autenticado
     * @param userRole Rol del usuario (ADMIN, AUXILIAR, ALMACENISTA, AUX_CONTEO)
     * @return Respuesta con validación + datos del marbete
     */
    LabelScanValidationResponse validateForScan(
        String qrCode,
        String countType,
        Long warehouseId,
        Long periodId,
        Long userId,
        String userRole
    );
    
    /**
     * Registra un conteo (C1 o C2) desde dispositivo móvil
     * 
     * @param request Datos del conteo + cantidad + folio
     * @param userId ID del usuario
     * @param userRole Rol del usuario
     * @return Confirmación del registro
     */
    LabelScanCountResponse registerCountFromScan(
        LabelScanCountRequest request,
        Long userId,
        String userRole
    );
    
    /**
     * Obtiene estado actual de un marbete por QR
     * 
     * @param qrCode Código QR o folio
     * @param warehouseId Almacén
     * @return Estado con conteos registrados
     */
    LabelMobileStatusResponse getLabelStatusByQrCode(
        String qrCode,
        Long warehouseId
    );
    
    /**
     * Lista marbetes pendientes de conteo para un almacén en un período
     * 
     * @param periodId Período
     * @param warehouseId Almacén
     * @param userId Usuario
     * @param userRole Rol
     * @return Lista de marbetes listos para contar
     */
    List<LabelMobileStatusResponse> getPendingLabelsForCounting(
        Long periodId,
        Long warehouseId,
        Long userId,
        String userRole
    );
}
```

---

## 2️⃣ DTOs para Escaneo

### `modules/labels/application/dto/scan/LabelScanValidationRequest.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.dto.scan;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelScanValidationRequest {
    
    @NotBlank(message = "El QR/folio es requerido")
    private String qrCode;                    // "42" o "SIGMAV2-FOLIO-42-P16-W369"
    
    @NotBlank(message = "Tipo de conteo es requerido")
    @Pattern(regexp = "^(C1|C2)$", message = "countType debe ser C1 o C2")
    private String countType;                 // "C1" o "C2"
    
    @NotNull(message = "warehouseId es requerido")
    private Long warehouseId;
    
    @NotNull(message = "periodId es requerido")
    private Long periodId;
}
```

### `modules/labels/application/dto/scan/LabelScanValidationResponse.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.dto.scan;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LabelScanValidationResponse {
    
    private Boolean valid;
    private Long folio;
    private String qrCode;
    
    // Datos del producto
    private Long productId;
    private String productCode;              // Clave del producto
    private String productName;
    private Integer theoreticalQuantity;
    
    // Datos del almacén
    private Long warehouseId;
    private String warehouseName;
    
    // Datos del período
    private Long periodId;
    
    // Estado del marbete
    private String estado;                  // GENERADO, IMPRESO, CANCELADO
    private Boolean cancelado;
    
    // Estado de conteos
    private Boolean c1Registered;
    private BigDecimal c1Value;
    private String c1RegisteredAt;
    
    private Boolean c2Registered;
    private BigDecimal c2Value;
    private String c2RegisteredAt;
    
    // Validación
    private String validationStatus;        // VALID_FOR_C1, VALID_FOR_C2, INVALID_CANCELLED, etc.
    private String message;                 // Mensaje amigable al usuario
    private String errorCode;               // Código para programación
    private String readableMessage;         // Mensaje para mostrar en la app
    
    private List<String> warnings;
}
```

### `modules/labels/application/dto/scan/LabelScanCountRequest.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.dto.scan;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelScanCountRequest {
    
    @NotNull(message = "Folio es requerido")
    @Min(value = 1, message = "Folio debe ser > 0")
    private Long folio;
    
    @NotBlank(message = "Tipo de conteo es requerido")
    private String countType;               // "C1" o "C2"
    
    @NotNull(message = "Cantidad es requerida")
    @Min(value = 0, message = "Cantidad no puede ser negativa")
    private BigDecimal countedValue;
    
    @NotNull(message = "warehouseId es requerido")
    private Long warehouseId;
    
    @NotNull(message = "periodId es requerido")
    private Long periodId;
    
    private String deviceId;                // ID del dispositivo móvil (opcional)
    private LocalDateTime scanTimestamp;    // Timestamp del escaneo
}
```

### `modules/labels/application/dto/scan/LabelScanCountResponse.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.dto.scan;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LabelScanCountResponse {
    
    private Boolean success;
    private String message;
    
    private Long folio;
    private String countType;
    private BigDecimal countedValue;
    
    private LocalDateTime registeredAt;
    private Long registeredByUserId;
    private String registeredByEmail;
    
    // Información útil para siguiente paso
    private String nextAction;              // "Proceder a C2", "Completado", etc.
    private Boolean countingComplete;       // true si C1 y C2 ya registrados
    private BigDecimal difference;          // C2 - C1 (si ambos registrados)
    
    private String errorCode;               // En caso de error
    private List<String> warnings;
}
```

### `modules/labels/application/dto/scan/LabelMobileStatusResponse.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.dto.scan;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class LabelMobileStatusResponse {
    
    private Long folio;
    private String qrCode;
    
    private String productCode;
    private String productName;
    
    private String warehouseName;
    
    // Estado
    private String estado;
    private Boolean cancelado;
    
    // Conteos
    private Boolean c1Registered;
    private BigDecimal c1Value;
    
    private Boolean c2Registered;
    private BigDecimal c2Value;
    
    private Boolean countingComplete;
    private BigDecimal difference;
    
    private String status;                  // "PENDIENTE_C1", "PENDIENTE_C2", "COMPLETO"
    private String message;
}
```

---

## 3️⃣ Servicio de Aplicación (Implementa Puerto)

### `modules/labels/application/service/LabelScanApplicationService.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.scan.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.*;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.input.LabelScanService;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelScanApplicationService implements LabelScanService {
    
    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;
    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final JpaLabelRepository jpaLabelRepository;
    private final JpaLabelCountEventRepository jpaLabelCountEventRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;
    
    // ═══════════════════════════════════════════════════════════════════════
    // Método Principal: VALIDACIÓN para escaneo
    // ═══════════════════════════════════════════════════════════════════════
    
    @Override
    @Transactional(readOnly = true)
    public LabelScanValidationResponse validateForScan(
            String qrCode,
            String countType,
            Long warehouseId,
            Long periodId,
            Long userId,
            String userRole) {
        
        log.info("🔍 Validando QR para escaneo: qrCode={}, countType={}, warehouse={}, period={}",
                qrCode, countType, warehouseId, periodId);
        
        try {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // V1-V2: Convertir QR a folio y validar formato
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Long folio = parseFolioFromQR(qrCode);
            if (folio == null || folio <= 0) {
                return buildErrorResponse(
                    false, folio, qrCode,
                    "INVALID_FORMAT", "Formato de QR inválido",
                    "El QR debe ser un número válido (ej: 42)"
                );
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // V3: Folio existe en BD
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            Optional<Label> optLabel = persistence.findByFolio(folio);
            if (optLabel.isEmpty()) {
                return buildErrorResponse(
                    false, folio, qrCode,
                    "LABEL_NOT_FOUND", "Marbete no encontrado",
                    "El folio " + folio + " no existe en el sistema"
                );
            }
            Label label = optLabel.get();
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // V4: Folio pertenece al período correcto
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            if (!label.getPeriodId().equals(periodId)) {
                return buildErrorResponse(
                    false, folio, qrCode,
                    "LABEL_PERIOD_MISMATCH", "Período incorrecto",
                    "El folio pertenece a otro período"
                );
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // V5: Folio pertenece al almacén correcto (excepto AUX_CONTEO)
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            String roleUpper = userRole != null ? userRole.toUpperCase() : "";
            if (!roleUpper.equals("AUXILIAR_DE_CONTEO") && !label.getWarehouseId().equals(warehouseId)) {
                return buildErrorResponse(
                    false, folio, qrCode,
                    "LABEL_WAREHOUSE_MISMATCH", "Almacén incorrecto",
                    "El folio pertenece a otro almacén"
                );
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // V6: Marbete NO debe estar CANCELADO
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            if (label.getEstado() == Label.State.CANCELADO) {
                return buildErrorResponse(
                    false, folio, qrCode,
                    "LABEL_CANCELLED", "Marbete cancelado",
                    "Este marbete está CANCELADO y no puede procesarse"
                );
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // V7: Marbete debe estar IMPRESO
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            if (label.getEstado() != Label.State.IMPRESO) {
                return buildErrorResponse(
                    false, folio, qrCode,
                    "LABEL_NOT_PRINTED", "Marbete no impreso",
                    "El marbete debe estar IMPRESO para registrar conteos"
                );
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // V8-V11: Validar secuencia de conteos según countType
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
            boolean hasC1 = events.stream().anyMatch(e -> e.getCountNumber() == 1);
            boolean hasC2 = events.stream().anyMatch(e -> e.getCountNumber() == 2);
            
            if ("C1".equals(countType)) {
                if (hasC1) {
                    return buildErrorResponse(
                        false, folio, qrCode,
                        "C1_ALREADY_REGISTERED", "C1 ya registrado",
                        "El conteo C1 ya fue registrado para este marbete"
                    );
                }
                if (hasC2 && !hasC1) {
                    return buildErrorResponse(
                        false, folio, qrCode,
                        "SEQUENCE_ERROR", "Secuencia inválida",
                        "Existe C2 sin C1. Contactar administrador"
                    );
                }
            } else if ("C2".equals(countType)) {
                if (!hasC1) {
                    return buildErrorResponse(
                        false, folio, qrCode,
                        "C1_MISSING", "C1 no registrado",
                        "Debe registrar C1 antes de C2"
                    );
                }
                if (hasC2) {
                    return buildErrorResponse(
                        false, folio, qrCode,
                        "C2_ALREADY_REGISTERED", "C2 ya registrado",
                        "El conteo C2 ya fue registrado para este marbete"
                    );
                }
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ✅ VALIDACIÓN EXITOSA — Construir respuesta
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            return buildSuccessResponse(label, folio, qrCode, countType, hasC1, hasC2);
            
        } catch (Exception e) {
            log.error("❌ Error en validación de QR: {}", e.getMessage(), e);
            return buildErrorResponse(
                false, null, qrCode,
                "ERROR", "Error en validación",
                "Error interno del servidor: " + e.getMessage()
            );
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Método: REGISTRAR CONTEO desde móvil
    // ═══════════════════════════════════════════════════════════════════════
    
    @Override
    @Transactional
    public LabelScanCountResponse registerCountFromScan(
            LabelScanCountRequest request,
            Long userId,
            String userRole) {
        
        log.info("📊 Registrando conteo desde escaneo: folio={}, countType={}, valor={}",
                request.getFolio(), request.getCountType(), request.getCountedValue());
        
        try {
            // 1. Validar primero con validateForScan()
            LabelScanValidationResponse validation = validateForScan(
                request.getFolio().toString(),
                request.getCountType(),
                request.getWarehouseId(),
                request.getPeriodId(),
                userId,
                userRole
            );
            
            if (!validation.getValid()) {
                return LabelScanCountResponse.builder()
                    .success(false)
                    .message(validation.getReadableMessage())
                    .errorCode(validation.getErrorCode())
                    .build();
            }
            
            // 2. Registrar el conteo
            int countNumber = "C1".equals(request.getCountType()) ? 1 : 2;
            LabelCountEvent.Role roleEnum = parseRole(userRole.toUpperCase(), LabelCountEvent.Role.AUXILIAR_DE_CONTEO);
            
            LabelCountEvent event = persistence.saveCountEvent(
                request.getFolio(),
                userId,
                countNumber,
                request.getCountedValue(),
                roleEnum,
                countNumber == 2  // isCompleting: true si es C2
            );
            
            log.info("✅ Conteo registrado: folio={}, C{}={}", 
                request.getFolio(), countNumber, request.getCountedValue());
            
            // 3. Construir respuesta
            return buildCountResponse(event, userId, request, validation);
            
        } catch (Exception e) {
            log.error("❌ Error al registrar conteo: {}", e.getMessage(), e);
            return LabelScanCountResponse.builder()
                .success(false)
                .message("Error al registrar: " + e.getMessage())
                .errorCode("ERROR")
                .build();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // Métodos Helper
    // ═══════════════════════════════════════════════════════════════════════
    
    private Long parseFolioFromQR(String qrCode) {
        try {
            // Puede ser: "42" o "SIGMAV2-FOLIO-42-P16-W369"
            if (qrCode.contains("-")) {
                String[] parts = qrCode.split("-");
                if (parts.length >= 3) return Long.parseLong(parts[2]);
            }
            return Long.parseLong(qrCode);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private LabelScanValidationResponse buildSuccessResponse(
            Label label, Long folio, String qrCode, String countType,
            boolean hasC1, boolean hasC2) {
        
        // Obtener datos del producto
        ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
        String productCode = product != null ? product.getCveArt() : "N/A";
        String productName = product != null ? product.getDescr() : "N/A";
        
        // Obtener datos del almacén
        WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
        String warehouseName = warehouse != null ? warehouse.getNameWarehouse() : "N/A";
        
        // Obtener existencias teóricas
        Integer existencias = 0;
        try {
            var stock = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                label.getProductId(), label.getWarehouseId(), label.getPeriodId());
            if (stock.isPresent() && stock.get().getExistQty() != null) {
                existencias = stock.get().getExistQty().intValue();
            }
        } catch (Exception e) {
            log.warn("No se pudieron obtener existencias: {}", e.getMessage());
        }
        
        // Obtener valores de conteos si existen
        BigDecimal c1Value = null, c2Value = null;
        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
        for (LabelCountEvent e : events) {
            if (e.getCountNumber() == 1) c1Value = e.getCountedValue();
            if (e.getCountNumber() == 2) c2Value = e.getCountedValue();
        }
        
        // Determinar status y mensaje
        String validationStatus = "C1".equals(countType) ? "VALID_FOR_C1" : "VALID_FOR_C2";
        String message;
        if ("C1".equals(countType)) {
            message = "Marbete válido. Proceda a registrar conteo C1";
        } else {
            message = "Marbete válido. Conteo C1 ya registrado. Proceda a C2";
        }
        
        return LabelScanValidationResponse.builder()
            .valid(true)
            .folio(folio)
            .qrCode(qrCode)
            .productId(label.getProductId())
            .productCode(productCode)
            .productName(productName)
            .theoreticalQuantity(existencias)
            .warehouseId(label.getWarehouseId())
            .warehouseName(warehouseName)
            .periodId(label.getPeriodId())
            .estado(label.getEstado().name())
            .cancelado(false)
            .c1Registered(hasC1)
            .c1Value(c1Value)
            .c2Registered(hasC2)
            .c2Value(c2Value)
            .validationStatus(validationStatus)
            .message(message)
            .readableMessage(message)
            .warnings(new ArrayList<>())
            .build();
    }
    
    private LabelScanValidationResponse buildErrorResponse(
            Boolean valid, Long folio, String qrCode,
            String errorCode, String message, String readableMessage) {
        
        return LabelScanValidationResponse.builder()
            .valid(valid)
            .folio(folio)
            .qrCode(qrCode)
            .errorCode(errorCode)
            .message(message)
            .readableMessage(readableMessage)
            .validationStatus("INVALID_" + errorCode)
            .build();
    }
    
    private LabelScanCountResponse buildCountResponse(
            LabelCountEvent event, Long userId,
            LabelScanCountRequest request,
            LabelScanValidationResponse validation) {
        
        String nextAction = "C1".equals(request.getCountType())
            ? "Proceder a registrar conteo C2"
            : "Conteo completo";
        
        return LabelScanCountResponse.builder()
            .success(true)
            .message("Conteo registrado exitosamente")
            .folio(request.getFolio())
            .countType(request.getCountType())
            .countedValue(request.getCountedValue())
            .registeredAt(event.getCreatedAt())
            .registeredByUserId(userId)
            .nextAction(nextAction)
            .countingComplete("C2".equals(request.getCountType()))
            .build();
    }
    
    private LabelCountEvent.Role parseRole(String roleUpper, LabelCountEvent.Role defaultRole) {
        try {
            return LabelCountEvent.Role.valueOf(roleUpper);
        } catch (Exception ex) {
            return defaultRole;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public LabelMobileStatusResponse getLabelStatusByQrCode(String qrCode, Long warehouseId) {
        // Implementar similar a buildSuccessResponse()
        // Retorna estado actual del marbete
        throw new UnsupportedOperationException("A implementar");
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LabelMobileStatusResponse> getPendingLabelsForCounting(
            Long periodId, Long warehouseId, Long userId, String userRole) {
        // Implementar lista de marbetes listos para contar
        throw new UnsupportedOperationException("A implementar");
    }
}
```

---

## 4️⃣ Controlador REST para Móvil

### `modules/labels/adapter/controller/LabelScanMobileController.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.adapter.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.scan.*;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.input.LabelScanService;
import tokai.com.mx.SIGMAV2.shared.audit.Auditable;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/sigmav2/labels/scan")
@RequiredArgsConstructor
@Slf4j
public class LabelScanMobileController {
    
    private final LabelScanService labelScanService;
    
    /**
     * Valida QR/folio para escaneo móvil
     * POST /api/sigmav2/labels/scan/validate
     */
    @PostMapping("/validate")
    @Auditable(action = "VALIDATE_LABEL_SCAN", resource = "LABEL")
    public ResponseEntity<LabelScanValidationResponse> validateLabel(
            @Valid @RequestBody LabelScanValidationRequest request) {
        
        log.info("🔍 Endpoint /validate: qrCode={}, countType={}", request.getQrCode(), request.getCountType());
        
        try {
            Long userId = getUserIdFromToken();
            String userRole = getUserRoleFromToken();
            
            LabelScanValidationResponse response = labelScanService.validateForScan(
                request.getQrCode(),
                request.getCountType(),
                request.getWarehouseId(),
                request.getPeriodId(),
                userId,
                userRole
            );
            
            return response.getValid() 
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                
        } catch (Exception e) {
            log.error("❌ Error en /validate: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                LabelScanValidationResponse.builder()
                    .valid(false)
                    .errorCode("ERROR")
                    .message("Error interno del servidor")
                    .build()
            );
        }
    }
    
    /**
     * Registra conteo desde móvil
     * POST /api/sigmav2/labels/scan/count
     */
    @PostMapping("/count")
    @Auditable(action = "REGISTER_COUNT_FROM_SCAN", resource = "LABEL")
    public ResponseEntity<LabelScanCountResponse> registerCount(
            @Valid @RequestBody LabelScanCountRequest request) {
        
        log.info("📊 Endpoint /count: folio={}, countType={}, value={}", 
            request.getFolio(), request.getCountType(), request.getCountedValue());
        
        try {
            Long userId = getUserIdFromToken();
            String userRole = getUserRoleFromToken();
            
            LabelScanCountResponse response = labelScanService.registerCountFromScan(
                request, userId, userRole
            );
            
            return response.getSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                
        } catch (Exception e) {
            log.error("❌ Error en /count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                LabelScanCountResponse.builder()
                    .success(false)
                    .message("Error al registrar conteo")
                    .errorCode("ERROR")
                    .build()
            );
        }
    }
    
    /**
     * Obtener estado por QR
     * GET /api/sigmav2/labels/scan/status/{qrCode}?warehouseId=369
     */
    @GetMapping("/status/{qrCode}")
    public ResponseEntity<LabelMobileStatusResponse> getStatus(
            @PathVariable String qrCode,
            @RequestParam Long warehouseId) {
        
        log.info("📋 Endpoint /status: qrCode={}, warehouse={}", qrCode, warehouseId);
        
        try {
            LabelMobileStatusResponse response = labelScanService.getLabelStatusByQrCode(qrCode, warehouseId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error en /status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────
    
    private Long getUserIdFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Implementar según tu forma de extraer userId del token JWT
        return 1L;  // Placeholder
    }
    
    private String getUserRoleFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Implementar según tu forma de extraer userRole del token JWT
        return "AUXILIAR_DE_CONTEO";  // Placeholder
    }
}
```

---

*Continúa con configuración de wiring en `UserModuleConfig.java`...*

