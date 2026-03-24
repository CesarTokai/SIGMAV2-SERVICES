# 🛠️ GUÍA PRÁCTICA DE IMPLEMENTACIÓN: QR/Scanner en SIGMAV2

**Documento:** Pasos concretos y ejemplos de código para implementar flujo móvil con QR  
**Nivel:** Técnico (Arquitecto/Líder de Desarrollo)  
**Fecha:** 23 de Marzo 2026

---

## 📑 ÍNDICE RÁPIDO

1. [Crear migraciones Flyway](#1-crear-migraciones-flyway)
2. [Extender modelo Label](#2-extender-modelo-label)
3. [Crear puertos en dominio](#3-crear-puertos-en-dominio)
4. [Implementar servicio aplicación](#4-implementar-servicio-aplicación)
5. [Crear adaptador JPA](#5-crear-adaptador-jpa)
6. [Generador QR](#6-generador-qr)
7. [Controlador REST](#7-controlador-rest)
8. [Mapper DTOs](#8-mapper-dtos)
9. [Integración JasperReports](#9-integración-jasperreports)
10. [App móvil base](#10-app-móvil-base)

---

## 1. CREAR MIGRACIONES FLYWAY

**Ubicación:** `src/main/resources/db/migration/`

### Migración 1: Agregar QR a labels

**Archivo:** `V1_3_0__Add_qr_code_to_labels.sql`

```sql
-- ========================================
-- MIGRACIÓN: Agregar soporte QR a marbetes
-- Versión: 1.3.0
-- Fecha: 2026-03-23
-- ========================================

-- 1. Agregar columna QR_CODE a tabla labels
ALTER TABLE labels 
ADD COLUMN qr_code VARCHAR(500) UNIQUE NOT NULL AFTER estado;

-- Crear índice para búsqueda O(1)
CREATE INDEX idx_labels_qr_code ON labels(qr_code);

-- 2. Agregar campos de auditoría móvil a label_count_events
ALTER TABLE label_count_events 
ADD COLUMN device_id VARCHAR(100) AFTER second_count_at;

ALTER TABLE label_count_events 
ADD COLUMN scan_timestamp DATETIME AFTER device_id;

-- Crear índice compuesto para auditoría
CREATE INDEX idx_label_count_device_scan 
  ON label_count_events(device_id, scan_timestamp);

-- 3. Agregar restricción única: no duplicar C1/C2 por folio
ALTER TABLE label_count_events 
ADD CONSTRAINT uk_label_count_number 
  UNIQUE (folio, count_number);

-- 4. Crear tabla para rastreo de dispositivos
CREATE TABLE IF NOT EXISTS mobile_devices (
  id_device VARCHAR(100) PRIMARY KEY,
  id_user BIGINT NOT NULL,
  device_name VARCHAR(200),
  device_type ENUM('WEB', 'MOBILE_ANDROID', 'MOBILE_IOS', 'PWA') NOT NULL,
  imei VARCHAR(15),
  last_activity DATETIME,
  is_active BOOLEAN DEFAULT TRUE,
  created_at DATETIME NOT NULL,
  FOREIGN KEY (id_user) REFERENCES users(id_user),
  INDEX idx_device_user (id_user, is_active)
);

-- 5. Crear tabla para auditoría de tokens por dispositivo
CREATE TABLE IF NOT EXISTS device_token_revocations (
  id_revocation BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_device VARCHAR(100) NOT NULL,
  token_jti VARCHAR(500),
  revoked_at DATETIME NOT NULL,
  reason VARCHAR(255),
  FOREIGN KEY (id_device) REFERENCES mobile_devices(id_device),
  INDEX idx_device_token (id_device, revoked_at)
);

-- 6. Inicializar valores por defecto - Generar QR para marbetes existentes
-- (Este script asume que ejecuta AFTER que se agrega la columna)
UPDATE labels 
SET qr_code = CONCAT('SIGMAV2-FOLIO-', folio, '-P', id_period, '-W', id_warehouse)
WHERE qr_code IS NULL;

-- Verificación
SELECT COUNT(*) as total_labels, 
       COUNT(qr_code) as labels_con_qr 
FROM labels;

-- Resultado esperado: Todos los marbetes deben tener QR
-- Si no: ERROR - investigar qué marbetes faltan QR

COMMIT;
```

**Validación post-migración:**

```sql
-- Ejecutar después de que Flyway aplique la migración
SELECT 
  COUNT(*) as total_marbetes,
  COUNT(DISTINCT qr_code) as qr_unicos,
  COUNT(folio) as folios,
  CASE 
    WHEN COUNT(*) = COUNT(DISTINCT qr_code) THEN '✓ OK: QRs únicos'
    ELSE '✗ ERROR: QRs duplicados'
  END as validacion
FROM labels;

-- Salida esperada:
-- total_marbetes: 10000
-- qr_unicos: 10000
-- folios: 10000
-- validacion: ✓ OK: QRs únicos
```

---

## 2. EXTENDER MODELO LABEL

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/domain/model/Label.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "labels")
public class Label {

    @Id
    private Long folio;

    @Column(name = "id_label_request")
    private Long labelRequestId;

    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "id_product", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State estado = State.GENERADO;

    // ==================== NUEVO ====================
    @Column(name = "qr_code", length = 500, unique = true, nullable = false)
    private String qrCode;
    // ================================================

    @Column(name = "impreso_at")
    private LocalDateTime impresoAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum State {
        GENERADO,
        IMPRESO,
        CANCELADO
    }

    // ==================== NUEVO ====================
    /**
     * Genera el código QR basado en folio + período + almacén
     * Garantiza unicidad global incluso en diferentes períodos
     * 
     * Formato: SIGMAV2-FOLIO-{folio}-P{periodId}-W{warehouseId}
     * Ejemplo: SIGMAV2-FOLIO-123-P16-W369
     */
    public void generateQrCode() {
        if (this.folio == null || this.periodId == null || this.warehouseId == null) {
            throw new IllegalArgumentException(
                "No se puede generar QR sin folio, periodId y warehouseId");
        }
        
        this.qrCode = String.format(
            "SIGMAV2-FOLIO-%d-P%d-W%d",
            this.folio,
            this.periodId,
            this.warehouseId
        );
    }

    /**
     * Valida que el QR sea válido
     */
    public boolean isValidQrCode() {
        return this.qrCode != null && 
               this.qrCode.startsWith("SIGMAV2-FOLIO-") &&
               this.qrCode.contains("-P") &&
               this.qrCode.contains("-W");
    }
    // ================================================
}
```

---

## 3. CREAR PUERTOS EN DOMINIO

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/domain/port/input/`

### Puerto: LabelScanService.java (NUEVO)

```java
package tokai.com.mx.SIGMAV2.modules.labels.domain.port.input;

import tokai.com.mx.SIGMAV2.modules.labels.domain.model.CountType;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelScanValidationResult;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountResult;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelMobileStatus;

/**
 * Puerto de entrada: Casos de uso para escaneo y conteo desde móvil
 * Implementado por: LabelScanApplicationService
 */
public interface LabelScanService {

    /**
     * Validar que un marbete puede ser contado
     * 
     * @param qrCode Código QR escaneado (ej: SIGMAV2-FOLIO-123-P16-W369)
     *               O número de folio ingresado manualmente (ej: 123)
     * @param countType Tipo de conteo: C1 o C2
     * @param currentUserId ID del usuario (del JWT token)
     * @param currentWarehouseId ID del almacén donde se realiza conteo
     * 
     * @return LabelScanValidationResult con estado y validaciones
     * 
     * @throws IllegalArgumentException si folio no existe
     * @throws IllegalStateException si período está cerrado
     * @throws AccessDeniedException si usuario no tiene acceso al almacén
     */
    LabelScanValidationResult validateLabelForCounting(
        String qrCode,
        CountType countType,
        Long currentUserId,
        Long currentWarehouseId
    );

    /**
     * Registrar conteo de marbete desde dispositivo móvil
     * 
     * @param folio Número de folio/marbete
     * @param countType C1 o C2
     * @param quantity Cantidad contada (ej: 95)
     * @param deviceId ID único del dispositivo (ej: UUID-MOB-001)
     * @param currentUserId ID del usuario que realiza conteo
     * @param currentWarehouseId ID del almacén
     * 
     * @return LabelCountResult confirmando registro
     * 
     * @throws IllegalArgumentException si cantidad es inválida
     * @throws DuplicateCountException si C1/C2 ya existe
     * @throws PeriodClosedException si período está cerrado
     */
    LabelCountResult registerMobileCount(
        Long folio,
        CountType countType,
        Integer quantity,
        String deviceId,
        Long currentUserId,
        Long currentWarehouseId
    );

    /**
     * Obtener estado actual de un marbete (para verificación en móvil)
     * 
     * @param qrCode Código QR del marbete
     * @param currentWarehouseId Almacén (para validar acceso)
     * 
     * @return LabelMobileStatus con información completa del marbete
     */
    LabelMobileStatus getLabelStatusByQrCode(
        String qrCode,
        Long currentWarehouseId
    );

    /**
     * Buscar marbete por número de folio ingresado manualmente
     * (fallback cuando el escaneo falla)
     * 
     * @param folioNumber Número de folio (ej: 123)
     * @param currentWarehouseId Almacén
     * @param countType C1 o C2 que se va a registrar
     * 
     * @return LabelScanValidationResult
     */
    LabelScanValidationResult findLabelByFolioNumber(
        Long folioNumber,
        Long currentWarehouseId,
        CountType countType
    );
}
```

### Enum: CountType.java (NUEVO)

```java
package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

/**
 * Tipos de conteo disponibles
 * C1: Primer conteo (conteo inicial)
 * C2: Segundo conteo (verificación/reconciliación)
 */
public enum CountType {
    C1("Primer Conteo"),
    C2("Segundo Conteo");

    private final String descripcion;

    CountType(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
```

### Modelos de Resultado: (DTOs de Dominio - SIN anotaciones JPA)

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/domain/model/LabelScanValidationResult.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Resultado de validación de marbete para conteo
 * Este es un modelo de dominio PURO (sin anotaciones JPA)
 */
@Getter
@Setter
@Builder
public class LabelScanValidationResult {
    
    private Long folio;
    private String qrCode;
    private Long productId;
    private String productDescription;
    private Long warehouseId;
    private String warehouseName;
    private Long periodId;
    
    // Datos teóricos
    private Integer theoreticalQuantity;
    
    // Conteos registrados
    private Integer c1Value;
    private String c1RegisteredBy;
    private LocalDateTime c1RegisteredAt;
    
    private Integer c2Value;
    private String c2RegisteredBy;
    private LocalDateTime c2RegisteredAt;
    
    // Estado del marbete
    private Label.State estado;
    private LocalDateTime impresoAt;
    
    // Validación
    private String validationStatus;  // "VALID_FOR_C1", "VALID_FOR_C2", "ALREADY_COUNTED", etc.
    private String errorMessage;
    private boolean valid;
    
    // Información para el usuario
    private String readableMessage; // "Puedes registrar C1", "C1 ya registrado hace 2 horas"
}
```

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/domain/model/LabelCountResult.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class LabelCountResult {
    private Long folio;
    private String qrCode;
    private CountType countType;
    private Integer quantity;
    private boolean success;
    private String message;
    private LocalDateTime registeredAt;
    private String registeredBy;
}
```

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/domain/model/LabelMobileStatus.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class LabelMobileStatus {
    private Long folio;
    private String qrCode;
    private String productDescription;
    private Long warehouseId;
    private String warehouseName;
    private String estado;
    
    @Getter
    @Builder
    public static class CountInfo {
        private boolean registered;
        private Integer quantity;
        private String registeredBy;
        private LocalDateTime registeredAt;
    }
    
    private CountInfo c1;
    private CountInfo c2;
    
    private Integer theoretical;
    private Integer variance;
    private boolean readyForC2;
    private String message;
}
```

---

## 4. IMPLEMENTAR SERVICIO APLICACIÓN

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/LabelScanApplicationService.java` (NUEVO)

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tokai.com.mx.SIGMAV2.modules.labels.domain.model.*;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.input.LabelScanService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.LabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.LabelCountRepository;
import tokai.com.mx.SIGMAV2.modules.periods.domain.port.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.output.WarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.port.output.InventoryRepository;
import tokai.com.mx.SIGMAV2.shared.audit.Auditable;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelScanApplicationService implements LabelScanService {

    private final LabelRepository labelRepository;
    private final LabelCountRepository labelCountRepository;
    private final PeriodRepository periodRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public LabelScanValidationResult validateLabelForCounting(
        String qrCode,
        CountType countType,
        Long currentUserId,
        Long currentWarehouseId
    ) {
        log.info("Validando marbete para conteo: qrCode={}, countType={}, userId={}, warehouseId={}",
            qrCode, countType, currentUserId, currentWarehouseId);

        // 1. Parsear QR o convertir número de folio a QR
        String parsedQrCode = parseQrCode(qrCode);
        
        // 2. Buscar marbete en BD
        Label label = labelRepository.findByQrCode(parsedQrCode)
            .orElseThrow(() -> new IllegalArgumentException(
                "Marbete no encontrado: " + qrCode));

        // 3. Validar que almacén coincide
        if (!label.getWarehouseId().equals(currentWarehouseId)) {
            throw new AccessDeniedException(
                "Marbete pertenece a almacén " + label.getWarehouseId() +
                ", pero estás contando en " + currentWarehouseId);
        }

        // 4. Validar que período está activo
        Period period = periodRepository.findById(label.getPeriodId())
            .orElseThrow(() -> new IllegalStateException("Período no encontrado"));
        
        if (!period.isActive()) {
            throw new IllegalStateException("Período " + period.getName() + " está cerrado");
        }

        // 5. Validar estado del marbete
        if (label.getEstado() == Label.State.CANCELADO) {
            throw new IllegalStateException("Marbete #" + label.getFolio() + " está cancelado");
        }

        if (label.getEstado() != Label.State.IMPRESO) {
            throw new IllegalStateException(
                "Marbete #" + label.getFolio() + " debe estar IMPRESO, actual: " + label.getEstado());
        }

        // 6. Buscar conteos existentes
        LabelCount existingCounts = labelCountRepository.findByFolio(label.getFolio())
            .orElse(null);

        // 7. Validar según tipo de conteo
        String validationStatus;
        String readableMessage;
        boolean isValid = true;

        if (countType == CountType.C1) {
            if (existingCounts != null && existingCounts.getOneCount() != null) {
                validationStatus = "ALREADY_COUNTED_C1";
                readableMessage = "C1 ya registrado: " + 
                    existingCounts.getOneCount() + " unidades";
                isValid = false;
            } else {
                validationStatus = "VALID_FOR_C1";
                readableMessage = "Marbete válido para registrar primer conteo (C1)";
            }
        } else { // C2
            if (existingCounts == null || existingCounts.getOneCount() == null) {
                validationStatus = "MISSING_C1";
                readableMessage = "Debe registrar C1 antes de C2";
                isValid = false;
            } else if (existingCounts.getSecondCount() != null) {
                validationStatus = "ALREADY_COUNTED_C2";
                readableMessage = "C2 ya registrado: " + 
                    existingCounts.getSecondCount() + " unidades";
                isValid = false;
            } else {
                validationStatus = "VALID_FOR_C2";
                readableMessage = "Marbete válido para registrar segundo conteo (C2)";
            }
        }

        // 8. Obtener cantidad teórica
        Integer theoreticalQuantity = inventoryRepository
            .findTheoricalQuantity(label.getProductId(), label.getWarehouseId(), label.getPeriodId())
            .orElse(0);

        // Construir resultado
        return LabelScanValidationResult.builder()
            .folio(label.getFolio())
            .qrCode(label.getQrCode())
            .productId(label.getProductId())
            .warehouseId(label.getWarehouseId())
            .periodId(label.getPeriodId())
            .theoreticalQuantity(theoreticalQuantity)
            .c1Value(existingCounts != null ? existingCounts.getOneCount() : null)
            .c1RegisteredAt(existingCounts != null ? existingCounts.getOneCountAt() : null)
            .c2Value(existingCounts != null ? existingCounts.getSecondCount() : null)
            .c2RegisteredAt(existingCounts != null ? existingCounts.getSecondCountAt() : null)
            .estado(label.getEstado())
            .impresoAt(label.getImpresoAt())
            .validationStatus(validationStatus)
            .valid(isValid)
            .readableMessage(readableMessage)
            .build();
    }

    @Override
    @Transactional
    @Auditable(action = "REGISTER_MOBILE_COUNT", resource = "LABEL")
    public LabelCountResult registerMobileCount(
        Long folio,
        CountType countType,
        Integer quantity,
        String deviceId,
        Long currentUserId,
        Long currentWarehouseId
    ) {
        log.info("Registrando conteo móvil: folio={}, countType={}, quantity={}, deviceId={}",
            folio, countType, quantity, deviceId);

        // 1. Validar cantidad
        if (quantity == null || quantity < 0 || quantity > 999999) {
            throw new IllegalArgumentException("Cantidad inválida: " + quantity);
        }

        // 2. Buscar marbete
        Label label = labelRepository.findById(folio)
            .orElseThrow(() -> new IllegalArgumentException("Folio no encontrado: " + folio));

        // 3. Validar almacén
        if (!label.getWarehouseId().equals(currentWarehouseId)) {
            throw new AccessDeniedException("Almacén no coincide");
        }

        // 4. Ejecutar validaciones previas
        LabelScanValidationResult validation = validateLabelForCounting(
            label.getQrCode(), countType, currentUserId, currentWarehouseId);

        if (!validation.isValid()) {
            throw new IllegalStateException(validation.getReadableMessage());
        }

        // 5. Obtener o crear LabelCount
        LabelCount labelCount = labelCountRepository.findByFolio(folio)
            .orElse(new LabelCount());
        labelCount.setFolio(folio);

        // 6. Registrar conteo
        LocalDateTime now = LocalDateTime.now();
        if (countType == CountType.C1) {
            labelCount.setOneCount(quantity.longValue());
            labelCount.setOneCountBy(currentUserId);
            labelCount.setOneCountAt(now);
        } else {
            labelCount.setSecondCount(quantity.longValue());
            labelCount.setSecondCountBy(currentUserId);
            labelCount.setSecondCountAt(now);
        }

        // 7. Guardar LabelCount
        labelCountRepository.save(labelCount);

        // 8. Crear evento de conteo (auditoría detallada)
        createLabelCountEvent(folio, countType, quantity, deviceId, currentUserId);

        log.info("Conteo registrado exitosamente: folio={}, countType={}", folio, countType);

        return LabelCountResult.builder()
            .folio(folio)
            .qrCode(label.getQrCode())
            .countType(countType)
            .quantity(quantity)
            .success(true)
            .message("Conteo registrado exitosamente")
            .registeredAt(now)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LabelMobileStatus getLabelStatusByQrCode(String qrCode, Long currentWarehouseId) {
        String parsedQrCode = parseQrCode(qrCode);
        
        Label label = labelRepository.findByQrCode(parsedQrCode)
            .orElseThrow(() -> new IllegalArgumentException("Marbete no encontrado"));

        if (!label.getWarehouseId().equals(currentWarehouseId)) {
            throw new AccessDeniedException("Almacén no coincide");
        }

        LabelCount counts = labelCountRepository.findByFolio(label.getFolio())
            .orElse(null);

        Integer theoretical = inventoryRepository
            .findTheoricalQuantity(label.getProductId(), label.getWarehouseId(), label.getPeriodId())
            .orElse(0);

        // Calcular varianza
        Integer variance = null;
        if (counts != null && counts.getSecondCount() != null) {
            variance = Math.toIntExact(counts.getSecondCount() - theoretical);
        }

        LabelMobileStatus.CountInfo c1 = LabelMobileStatus.CountInfo.builder()
            .registered(counts != null && counts.getOneCount() != null)
            .quantity(counts != null && counts.getOneCount() != null ? 
                counts.getOneCount().intValue() : null)
            .registeredAt(counts != null ? counts.getOneCountAt() : null)
            .build();

        LabelMobileStatus.CountInfo c2 = LabelMobileStatus.CountInfo.builder()
            .registered(counts != null && counts.getSecondCount() != null)
            .quantity(counts != null && counts.getSecondCount() != null ? 
                counts.getSecondCount().intValue() : null)
            .registeredAt(counts != null ? counts.getSecondCountAt() : null)
            .build();

        boolean readyForC2 = c1.isRegistered() && !c2.isRegistered();

        return LabelMobileStatus.builder()
            .folio(label.getFolio())
            .qrCode(label.getQrCode())
            .estado(label.getEstado().toString())
            .c1(c1)
            .c2(c2)
            .theoretical(theoretical)
            .variance(variance)
            .readyForC2(readyForC2)
            .message(generateStatusMessage(c1, c2, theoretical, variance))
            .build();
    }

    @Override
    public LabelScanValidationResult findLabelByFolioNumber(
        Long folioNumber,
        Long currentWarehouseId,
        CountType countType
    ) {
        Label label = labelRepository.findById(folioNumber)
            .orElseThrow(() -> new IllegalArgumentException("Folio no encontrado: " + folioNumber));

        if (!label.getWarehouseId().equals(currentWarehouseId)) {
            throw new AccessDeniedException("Folio pertenece a otro almacén");
        }

        // Reutilizar validación estándar
        return validateLabelForCounting(label.getQrCode(), countType, null, currentWarehouseId);
    }

    // ==================== HELPERS ====================

    private String parseQrCode(String input) {
        // Si es número puro, convertir a formato QR
        if (input.matches("\\d+")) {
            // Fallback: se maneja en controller con contexto de periodo/almacén
            return input;
        }
        return input;
    }

    private void createLabelCountEvent(Long folio, CountType countType, Integer quantity,
                                       String deviceId, Long userId) {
        // Implementar creación de evento en tabla label_count_events
        // con device_id y scan_timestamp para auditoría
    }

    private String generateStatusMessage(LabelMobileStatus.CountInfo c1, 
                                        LabelMobileStatus.CountInfo c2,
                                        Integer theoretical, Integer variance) {
        if (!c1.isRegistered()) {
            return "Pendiente: registrar primer conteo (C1)";
        }
        if (!c2.isRegistered()) {
            return "C1 registrado. Pendiente: segundo conteo (C2)";
        }
        if (variance != null && variance == 0) {
            return "✓ Conteos coinciden con teórico (" + theoretical + ")";
        }
        if (variance != null && Math.abs(variance) <= 5) {
            return "⚠ Varianza menor al 5% (" + variance + " unidades)";
        }
        return "✗ Varianza > 5% (" + variance + " unidades)";
    }
}

// Excepciones custom
class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}

class DuplicateCountException extends RuntimeException {
    public DuplicateCountException(String message) {
        super(message);
    }
}
```

---

## 5. CREAR ADAPTADOR JPA

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/infrastructure/persistence/` (ACTUALIZAR)

### Actualizar JpaLabelRepository

```java
package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;

import java.util.Optional;
import java.util.List;

@Repository
public interface JpaLabelRepository extends JpaRepository<Label, Long> {
    
    // ...existing methods...
    
    // NUEVO: Buscar por código QR
    @Query("SELECT l FROM Label l WHERE l.qrCode = :qrCode")
    Optional<Label> findByQrCode(@Param("qrCode") String qrCode);
    
    // NUEVO: Validar unicidad de QR
    @Query("SELECT COUNT(l) > 0 FROM Label l WHERE l.qrCode = :qrCode")
    boolean qrCodeExists(@Param("qrCode") String qrCode);
    
    // NUEVO: Búsqueda por folio + almacén (validación de acceso)
    @Query("SELECT l FROM Label l WHERE l.folio = :folio AND l.warehouseId = :warehouseId")
    Optional<Label> findByFolioAndWarehouse(@Param("folio") Long folio, 
                                            @Param("warehouseId") Long warehouseId);
    
    // NUEVO: Contar marbetes por almacén en período
    @Query("SELECT COUNT(l) FROM Label l WHERE l.periodId = :periodId AND l.warehouseId = :warehouseId")
    long countByPeriodAndWarehouse(@Param("periodId") Long periodId, 
                                   @Param("warehouseId") Long warehouseId);
}
```

### Crear Adaptador: LabelScanRepositoryAdapter.java

```java
package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.LabelScanRepository;

import java.util.Optional;

/**
 * Adaptador que implementa puerto de salida para escaneo
 * Encapsula lógica JPA para mantener dominio independiente
 */
@Component
@RequiredArgsConstructor
public class LabelScanRepositoryAdapter implements LabelScanRepository {
    
    private final JpaLabelRepository jpaLabelRepository;
    
    @Override
    public Optional<Label> findByQrCode(String qrCode) {
        return jpaLabelRepository.findByQrCode(qrCode);
    }
    
    @Override
    public Optional<Label> findByFolioAndWarehouse(Long folio, Long warehouseId) {
        return jpaLabelRepository.findByFolioAndWarehouse(folio, warehouseId);
    }
    
    @Override
    public boolean qrCodeExists(String qrCode) {
        return jpaLabelRepository.qrCodeExists(qrCode);
    }
    
    @Override
    public void updateQrCode(Long folio, String qrCode) {
        Optional<Label> label = jpaLabelRepository.findById(folio);
        if (label.isPresent()) {
            label.get().setQrCode(qrCode);
            jpaLabelRepository.save(label.get());
        }
    }
}
```

---

## 6. GENERADOR QR

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/infrastructure/qr/` (NUEVO)

### Dependencia Maven (agregar a pom.xml):

```xml
<!-- Generación de códigos QR -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

### Servicio: QrCodeGenerator.java

```java
package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Servicio para generar códigos QR para marbetes
 * Usa ZXing (Zebra Crossing) - librería estándar de código de barras
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QrCodeGenerator {
    
    @Value("${app.qr.directory:C:/Sistemas/SIGMA/QR-Codes}")
    private String qrDirectory;
    
    @Value("${app.qr.width:200}")
    private int qrWidth;
    
    @Value("${app.qr.height:200}")
    private int qrHeight;

    /**
     * Generar código QR como archivo PNG y retornar ruta
     * 
     * @param qrContent Contenido del QR (ej: SIGMAV2-FOLIO-123-P16-W369)
     * @param folio Número de folio (para nombre de archivo)
     * @return Ruta completa del archivo PNG generado
     */
    public String generateQrCode(String qrContent, Long folio) throws WriterException, IOException {
        try {
            // Crear directorio si no existe
            File directory = new File(qrDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generar matriz QR
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                qrContent, 
                BarcodeFormat.QR_CODE, 
                qrWidth, 
                qrHeight
            );

            // Convertir a imagen
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            // Guardar archivo
            String fileName = String.format("marbete_%d.png", folio);
            Path path = FileSystems.getDefault().getPath(qrDirectory, fileName);
            
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

            log.info("QR generado exitosamente: {}", path.toString());
            return path.toString();

        } catch (WriterException | IOException e) {
            log.error("Error generando QR para folio {}: {}", folio, e.getMessage());
            throw e;
        }
    }

    /**
     * Generar código QR como BufferedImage (para incrustar en JasperReports)
     */
    public BufferedImage generateQrCodeAsImage(String qrContent) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            qrContent, 
            BarcodeFormat.QR_CODE, 
            qrWidth, 
            qrHeight
        );
        
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
```

---

## 7. CONTROLADOR REST

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/adapter/web/LabelScanController.java` (NUEVO)

```java
package tokai.com.mx.SIGMAV2.modules.labels.adapter.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import tokai.com.mx.SIGMAV2.modules.labels.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelScanApplicationService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.CountType;
import tokai.com.mx.SIGMAV2.shared.audit.Auditable;
import tokai.com.mx.SIGMAV2.shared.security.JwtUtils;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/labels/scan")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AUXILIAR_DE_CONTEO', 'ALMACENISTA')")
public class LabelScanController {

    private final LabelScanApplicationService labelScanApplicationService;
    private final JwtUtils jwtUtils;
    private final LabelScanMapper labelScanMapper;

    /**
     * Validar marbete antes de registrar conteo
     * POST /api/sigmav2/labels/scan/validate
     */
    @PostMapping("/validate")
    @Auditable(action = "VALIDATE_LABEL_FOR_SCANNING", resource = "LABEL")
    public ResponseEntity<LabelScanValidationResponse> validateLabel(
        @Valid @RequestBody LabelScanValidationRequest request,
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam Long warehouseId
    ) {
        log.info("Validando marbete: qrCode={}, countType={}, warehouse={}",
            request.getQrCode(), request.getCountType(), warehouseId);

        Long userId = extractUserIdFromToken(userDetails);

        var result = labelScanApplicationService.validateLabelForCounting(
            request.getQrCode(),
            CountType.valueOf(request.getCountType()),
            userId,
            warehouseId
        );

        LabelScanValidationResponse response = labelScanMapper.toValidationResponse(result);
        return ResponseEntity.ok(response);
    }

    /**
     * Registrar conteo desde dispositivo móvil
     * POST /api/sigmav2/labels/scan/count
     */
    @PostMapping("/count")
    @Auditable(action = "REGISTER_MOBILE_COUNT", resource = "LABEL")
    public ResponseEntity<LabelCountResponse> registerCount(
        @Valid @RequestBody LabelScanCountRequest request,
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam Long warehouseId
    ) {
        log.info("Registrando conteo móvil: folio={}, countType={}, deviceId={}",
            request.getFolio(), request.getCountType(), request.getDeviceId());

        Long userId = extractUserIdFromToken(userDetails);

        var result = labelScanApplicationService.registerMobileCount(
            request.getFolio(),
            CountType.valueOf(request.getCountType()),
            request.getQuantity(),
            request.getDeviceId(),
            userId,
            warehouseId
        );

        LabelCountResponse response = labelScanMapper.toCountResponse(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtener estado actual de marbete
     * GET /api/sigmav2/labels/scan/status/{qrCode}
     */
    @GetMapping("/status/{qrCode}")
    public ResponseEntity<LabelMobileStatusResponse> getStatus(
        @PathVariable String qrCode,
        @RequestParam Long warehouseId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Obteniendo estado de marbete: qrCode={}, warehouse={}", qrCode, warehouseId);

        var result = labelScanApplicationService.getLabelStatusByQrCode(qrCode, warehouseId);
        LabelMobileStatusResponse response = labelScanMapper.toStatusResponse(result);
        return ResponseEntity.ok(response);
    }

    /**
     * Fallback: Buscar por número de folio ingresado manualmente
     * GET /api/sigmav2/labels/scan/folio/{folioNumber}
     */
    @GetMapping("/folio/{folioNumber}")
    public ResponseEntity<LabelScanValidationResponse> findByFolio(
        @PathVariable Long folioNumber,
        @RequestParam Long warehouseId,
        @RequestParam String countType,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Buscando marbete por folio: folio={}, countType={}", folioNumber, countType);

        var result = labelScanApplicationService.findLabelByFolioNumber(
            folioNumber,
            warehouseId,
            CountType.valueOf(countType)
        );

        LabelScanValidationResponse response = labelScanMapper.toValidationResponse(result);
        return ResponseEntity.ok(response);
    }

    // ==================== HELPERS ====================

    private Long extractUserIdFromToken(UserDetails userDetails) {
        // Implementar extracción de ID desde JWT
        // Ej: usando SecurityContextHolder o annotation personalizada
        return 1L; // TODO: implementar
    }
}
```

---

## 8. MAPPER DTOs

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/infrastructure/mapper/` (NUEVO)

### DTOs Request:

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/adapter/web/dto/LabelScanValidationRequest.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LabelScanValidationRequest {
    
    @NotBlank(message = "QR/Folio requerido")
    private String qrCode;  // Puede ser: SIGMAV2-FOLIO-123-P16-W369 o solo 123
    
    @NotBlank(message = "Tipo de conteo requerido")
    @Pattern(regexp = "C1|C2", message = "Debe ser C1 o C2")
    private String countType;
}
```

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/adapter/web/dto/LabelScanCountRequest.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.adapter.web.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LabelScanCountRequest {
    
    @NotNull(message = "Folio requerido")
    @Positive(message = "Folio debe ser positivo")
    private Long folio;
    
    @NotBlank(message = "Tipo de conteo requerido")
    @Pattern(regexp = "C1|C2")
    private String countType;
    
    @NotNull(message = "Cantidad requerida")
    @Min(value = 0, message = "Cantidad no puede ser negativa")
    @Max(value = 999999, message = "Cantidad máxima: 999,999")
    private Integer quantity;
    
    @NotBlank(message = "Device ID requerido")
    private String deviceId;  // UUID del dispositivo móvil
    
    private LocalDateTime scanTimestamp;  // Timestamp exacto del escaneo en móvil
}
```

### DTOs Response:

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/adapter/web/dto/LabelScanValidationResponse.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelScanValidationResponse {
    private Long folio;
    private String qrCode;
    private Long productId;
    private String productDescription;
    private Integer theoreticalQuantity;
    private Integer c1Value;
    private String c1RegisteredBy;
    private LocalDateTime c1RegisteredAt;
    private Integer c2Value;
    private String c2RegisteredBy;
    private LocalDateTime c2RegisteredAt;
    private String estado;
    private LocalDateTime impresoAt;
    private String validationStatus;  // VALID_FOR_C1, ALREADY_COUNTED_C1, etc.
    private String readableMessage;   // Mensaje para usuario
    private boolean valid;
}
```

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/adapter/web/dto/LabelCountResponse.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelCountResponse {
    private Long folio;
    private String qrCode;
    private String countType;
    private Integer quantity;
    private boolean success;
    private String message;
    private LocalDateTime registeredAt;
    private String registeredBy;
}
```

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/adapter/web/dto/LabelMobileStatusResponse.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelMobileStatusResponse {
    
    private Long folio;
    private String qrCode;
    private String productDescription;
    private String estado;
    
    private CountInfoDto c1;
    private CountInfoDto c2;
    
    private Integer theoretical;
    private Integer variance;
    private boolean readyForC2;
    private String message;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CountInfoDto {
        private boolean registered;
        private Integer quantity;
        private String registeredBy;
        private LocalDateTime registeredAt;
    }
}
```

### Mapper MapStruct:

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/infrastructure/mapper/LabelScanMapper.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tokai.com.mx.SIGMAV2.modules.labels.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.*;

@Mapper(componentModel = "spring")
public interface LabelScanMapper {
    
    @Mapping(source = "validationStatus", target = "validationStatus")
    LabelScanValidationResponse toValidationResponse(LabelScanValidationResult result);
    
    @Mapping(source = "countType", target = "countType")
    LabelCountResponse toCountResponse(LabelCountResult result);
    
    @Mapping(source = "c1", target = "c1")
    @Mapping(source = "c2", target = "c2")
    LabelMobileStatusResponse toStatusResponse(LabelMobileStatus status);
    
    LabelMobileStatusResponse.CountInfoDto toCountInfoDto(LabelMobileStatus.CountInfo countInfo);
}
```

---

## 9. INTEGRACIÓN JASPERREPORTS

**Ubicación:** `src/main/resources/reports/Carta_Tres_Cuadros.jrxml`

### Modificación de plantilla JRXML (para incluir QR):

```xml
<!-- AGREGAR dentro del elemento <detail> de la plantilla existente -->

<!-- Elemento QR: Código de barras 2D -->
<componentElement>
    <reportElement 
        x="10" 
        y="80" 
        width="80" 
        height="80" 
        uuid="qr-code-element"/>
    
    <c:barcode 
        xmlns:c="http://jasperreports.sourceforge.net/jasperreports/components"
        xsi:type="c:QRCode">
        
        <!-- Contenido QR: Codificar folio + período + almacén -->
        <c:codeExpression>
            <![CDATA[
                "SIGMAV2-FOLIO-" + $F{folio} + "-P" + $F{periodId} + "-W" + $F{warehouseId}
            ]]>
        </c:codeExpression>
        
        <!-- Propiedades del QR -->
        <c:errorCorrectionLevel>L</c:errorCorrectionLevel>  <!-- 30% correction -->
        <c:margin>2</c:margin>
        <c:moduleWidth>2</c:moduleWidth>
    </c:barcode>
</componentElement>

<!-- Etiqueta de referencia bajo QR -->
<textField>
    <reportElement x="10" y="165" width="80" height="10" uuid="folio-label"/>
    <textElement>
        <font size="8" isBold="true"/>
    </textElement>
    <textFieldExpression>
        <![CDATA["Folio: " + $F{folio}]]>
    </textFieldExpression>
</textField>
```

### Alternativa: Generar QR en servidor e insertar como imagen

Modificar servicio de generación:

```java
// En LabelGenerationService.generatePdf()
@Transactional
public byte[] generateLabelsPdf(Long periodId, Long warehouseId, Integer labelCount) {
    Map<String, Object> jasperParams = new HashMap<>();
    
    // ... parámetros existentes ...
    
    // NUEVO: Generar e insertar rutas QR
    List<Label> labels = labelRepository.findByPeriodAndWarehouse(periodId, warehouseId);
    
    for (Label label : labels) {
        String qrImagePath = qrCodeGenerator.generateQrCode(
            label.getQrCode(), 
            label.getFolio()
        );
        // Almacenar ruta para JasperReports
    }
    
    // Pasar colección de labels con rutas QR
    jasperParams.put("labels", labels);
    
    return generarPdfConJasper(jasperParams);
}
```

---

## 10. APP MÓVIL BASE

### Opción 1: PWA (Rapid Prototype - 1-2 semanas)

**Archivo:** `frontend/mobile-app/index.html`

```html
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SIGMAV2 - Conteo Marbetes</title>
    <script src="https://unpkg.com/@zxing/library@latest"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        
        .container { max-width: 100%; padding: 10px; }
        .header { background: #2c3e50; color: white; padding: 15px; text-align: center; }
        .scanner-box { border: 2px solid #3498db; padding: 20px; margin: 20px 0; }
        #qr-video { width: 100%; max-width: 400px; }
        .button { padding: 10px 20px; margin: 5px; border: none; border-radius: 5px; cursor: pointer; }
        .btn-primary { background: #3498db; color: white; }
        .btn-success { background: #27ae60; color: white; }
        .btn-danger { background: #e74c3c; color: white; }
        
        .form-group { margin: 15px 0; }
        input[type="text"], input[type="number"] { width: 100%; padding: 10px; margin: 5px 0; }
        
        .status-box { padding: 15px; margin: 10px 0; border-radius: 5px; }
        .status-valid { background: #d5f4e6; border-left: 4px solid #27ae60; }
        .status-error { background: #fadbd8; border-left: 4px solid #e74c3c; }
        .status-pending { background: #fef5e7; border-left: 4px solid #f39c12; }
    </style>
</head>
<body>
    <div class="header">
        <h1>📱 SIGMAV2 - Conteo de Marbetes</h1>
        <p id="user-info">Usuario: -</p>
    </div>

    <div class="container">
        
        <!-- 1. AUTENTICACIÓN -->
        <div id="login-section">
            <h2>Iniciar Sesión</h2>
            <div class="form-group">
                <input type="text" id="email" placeholder="Email" />
                <input type="password" id="password" placeholder="Contraseña" />
                <button class="button btn-primary" onclick="login()">Login</button>
            </div>
        </div>

        <!-- 2. SCANNER -->
        <div id="scanner-section" style="display:none;">
            <h2>📷 Escanear Marbete</h2>
            
            <div class="scanner-box">
                <video id="qr-video" controls="false"></video>
                <button class="button btn-primary" onclick="startScanning()">Iniciar Escaneo</button>
                <button class="button btn-danger" onclick="stopScanning()">Detener</button>
            </div>
            
            <h3>O ingresa manualmente:</h3>
            <div class="form-group">
                <input type="number" id="manual-folio" placeholder="Número de Folio (ej: 123)" />
                <button class="button btn-primary" onclick="validateByFolio()">Validar</button>
            </div>
            
            <div id="validation-result"></div>
        </div>

        <!-- 3. FORMULARIO DE CONTEO -->
        <div id="count-section" style="display:none;">
            <h2>📊 Registrar Conteo</h2>
            
            <div id="label-info" class="status-box status-pending"></div>
            
            <div class="form-group">
                <label>Tipo de Conteo:</label>
                <select id="count-type">
                    <option value="C1">Primer Conteo (C1)</option>
                    <option value="C2">Segundo Conteo (C2)</option>
                </select>
            </div>
            
            <div class="form-group">
                <label>Cantidad Contada:</label>
                <input type="number" id="count-quantity" min="0" max="999999" placeholder="0" />
            </div>
            
            <div class="form-group">
                <button class="button btn-success" onclick="registerCount()">✓ Guardar Conteo</button>
                <button class="button btn-danger" onclick="resetForm()">↶ Nuevo Marbete</button>
            </div>
        </div>
    </div>

    <script>
        const API_BASE = 'http://localhost:8080/api/sigmav2';
        let jwtToken = null;
        let currentLabel = null;

        // ========== AUTENTICACIÓN ==========
        async function login() {
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            
            try {
                const response = await fetch(`${API_BASE}/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });
                
                if (response.ok) {
                    const data = await response.json();
                    jwtToken = data.token;
                    document.getElementById('user-info').textContent = `Usuario: ${data.user.email}`;
                    document.getElementById('login-section').style.display = 'none';
                    document.getElementById('scanner-section').style.display = 'block';
                } else {
                    alert('Error: Credenciales inválidas');
                }
            } catch (error) {
                console.error('Error login:', error);
            }
        }

        // ========== SCANNER QR ==========
        let codeReader;
        
        async function startScanning() {
            const videoElement = document.getElementById('qr-video');
            const { BrowserQRCodeReader } = ZXing;
            
            codeReader = new BrowserQRCodeReader();
            
            try {
                const result = await codeReader.decodeFromVideoDevice(undefined, 'qr-video', 
                    (result, err) => {
                        if (result) {
                            const qrCode = result.text;
                            console.log('QR Escaneado:', qrCode);
                            validateLabel(qrCode);
                            stopScanning();
                        }
                    }
                );
            } catch (error) {
                console.error('Error scanning:', error);
                alert('Error al acceder a cámara');
            }
        }

        function stopScanning() {
            if (codeReader) {
                codeReader.reset();
            }
        }

        // ========== VALIDACIÓN ==========
        async function validateLabel(qrCode) {
            try {
                const response = await fetch(
                    `${API_BASE}/labels/scan/validate?warehouseId=1`, // Implementar warehouse context
                    {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${jwtToken}`
                        },
                        body: JSON.stringify({
                            qrCode: qrCode,
                            countType: 'C1'
                        })
                    }
                );

                const data = await response.json();
                currentLabel = data;

                if (data.valid) {
                    showLabelInfo(data);
                    document.getElementById('scanner-section').style.display = 'none';
                    document.getElementById('count-section').style.display = 'block';
                } else {
                    document.getElementById('validation-result').innerHTML = 
                        `<div class="status-box status-error">${data.readableMessage}</div>`;
                }
            } catch (error) {
                console.error('Error validación:', error);
            }
        }

        async function validateByFolio() {
            const folio = document.getElementById('manual-folio').value;
            // Llamar mismo endpoint pero con folio en lugar de QR
            const qrCode = folio; // Servidor convertirá a QR
            await validateLabel(qrCode);
        }

        function showLabelInfo(label) {
            const info = `
                <h3>✓ Marbete Válido</h3>
                <p><strong>Folio:</strong> ${label.folio}</p>
                <p><strong>Producto:</strong> ${label.productDescription}</p>
                <p><strong>Cantidad Teórica:</strong> ${label.theoreticalQuantity}</p>
                <p><strong>Estado:</strong> ${label.estado}</p>
                ${label.c1Value ? `<p><strong>C1 Registrado:</strong> ${label.c1Value} unidades</p>` : ''}
            `;
            document.getElementById('label-info').innerHTML = info;
            document.getElementById('label-info').className = 'status-box status-valid';
        }

        // ========== REGISTRAR CONTEO ==========
        async function registerCount() {
            const quantity = parseInt(document.getElementById('count-quantity').value);
            const countType = document.getElementById('count-type').value;
            
            if (!quantity || quantity < 0) {
                alert('Ingresa una cantidad válida');
                return;
            }

            try {
                const response = await fetch(
                    `${API_BASE}/labels/scan/count?warehouseId=1`,
                    {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${jwtToken}`
                        },
                        body: JSON.stringify({
                            folio: currentLabel.folio,
                            countType: countType,
                            quantity: quantity,
                            deviceId: getDeviceId(),
                            scanTimestamp: new Date().toISOString()
                        })
                    }
                );

                const data = await response.json();
                
                if (data.success) {
                    alert(`✓ Conteo ${countType} registrado: ${quantity} unidades`);
                    resetForm();
                } else {
                    alert(`✗ Error: ${data.message}`);
                }
            } catch (error) {
                console.error('Error registrando conteo:', error);
            }
        }

        function resetForm() {
            document.getElementById('count-quantity').value = '';
            document.getElementById('count-section').style.display = 'none';
            document.getElementById('scanner-section').style.display = 'block';
            currentLabel = null;
        }

        function getDeviceId() {
            // Generar UUID único del dispositivo
            let deviceId = localStorage.getItem('deviceId');
            if (!deviceId) {
                deviceId = 'DEV-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
                localStorage.setItem('deviceId', deviceId);
            }
            return deviceId;
        }
    </script>
</body>
</html>
```

---

## CHECKLIST DE IMPLEMENTACIÓN

- [ ] **Paso 1:** Crear y ejecutar migración Flyway V1_3_0
- [ ] **Paso 2:** Extender entidad Label con qrCode y generateQrCode()
- [ ] **Paso 3:** Crear puertos LabelScanService en dominio
- [ ] **Paso 4:** Implementar LabelScanApplicationService
- [ ] **Paso 5:** Crear adaptadores JPA + LabelScanRepositoryAdapter
- [ ] **Paso 6:** Implementar QrCodeGenerator con ZXing
- [ ] **Paso 7:** Crear LabelScanController con 4 endpoints
- [ ] **Paso 8:** Crear DTOs + Mapper MapStruct
- [ ] **Paso 9:** Modificar plantilla JasperReports para incluir QR
- [ ] **Paso 10:** Desarrollar PWA o app React Native
- [ ] **Paso 11:** Tests unitarios + integración
- [ ] **Paso 12:** Testing E2E con múltiples usuarios
- [ ] **Paso 13:** Deploy y capacitación

---

**Documento preparado:** 23 de Marzo 2026  
**Próximo:** Iniciar Fase 1 (Migraciones + Modelo)

