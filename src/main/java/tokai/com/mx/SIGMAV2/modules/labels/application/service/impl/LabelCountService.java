package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCountDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.*;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.CountHistoryService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Servicio especializado en el registro y actualización de conteos (C1 y C2).
 * Extraído de LabelServiceImpl para cumplir con el Principio de Responsabilidad Única.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelCountService {

    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;
    private final JpaLabelCountEventRepository jpaLabelCountEventRepository;
    private final JpaPeriodRepository jpaPeriodRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final CountHistoryService countHistoryService;

    @Transactional
    public LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole) {
        validateRole(userRole, "registrar C1",
                "ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO");

        String roleUpper = userRole.toUpperCase();
        Label label = findAndValidateLabelForCount(dto.getFolio(), dto.getPeriodId(), dto.getWarehouseId(), userId, roleUpper);

        if (persistence.hasCountNumber(dto.getFolio(), 1)) {
            throw new DuplicateCountException(
                    String.format("El conteo C1 ya fue registrado para el folio %d.", dto.getFolio()));
        }
        if (persistence.hasCountNumber(dto.getFolio(), 2)) {
            throw new CountSequenceException(
                    String.format("No se puede registrar C1 porque ya existe un conteo C2 para el folio %d.", dto.getFolio()));
        }

        LabelCountEvent.Role roleEnum = parseRole(roleUpper, LabelCountEvent.Role.AUXILIAR);
        LabelCountEvent result = persistence.saveCountEvent(dto.getFolio(), userId, 1, dto.getCountedValue(), roleEnum, false);
        
        // Registrar en historial con periodId y warehouseId del marbete
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        countHistoryService.recordCountRegistration(userId, email, dto.getFolio(), 1, dto.getCountedValue().intValue(), userRole, label.getWarehouseId(), label.getPeriodId());
        
        return result;
    }

    @Transactional
    public LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole) {
        validateRole(userRole, "registrar C2",
                "ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO");

        String roleUpper = userRole.toUpperCase();
        Label label = findAndValidateLabelForCount(dto.getFolio(), dto.getPeriodId(), dto.getWarehouseId(), userId, roleUpper);

        if (!persistence.hasCountNumber(dto.getFolio(), 1)) {
            throw new CountSequenceException(
                    String.format("No se puede registrar C2 porque no existe C1 previo para el folio %d.", dto.getFolio()));
        }
        if (persistence.hasCountNumber(dto.getFolio(), 2)) {
            throw new DuplicateCountException(
                    String.format("El conteo C2 ya fue registrado para el folio %d.", dto.getFolio()));
        }

        LabelCountEvent.Role roleEnum = parseRole(roleUpper, LabelCountEvent.Role.AUXILIAR_DE_CONTEO);
        LabelCountEvent result = persistence.saveCountEvent(dto.getFolio(), userId, 2, dto.getCountedValue(), roleEnum, true);
        
        // Registrar en historial con periodId y warehouseId del marbete
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        countHistoryService.recordCountRegistration(userId, email, dto.getFolio(), 2, dto.getCountedValue().intValue(), userRole, label.getWarehouseId(), label.getPeriodId());
        
        return result;
    }

    @Transactional
    public LabelCountEvent updateCountC1(UpdateCountDTO dto, Long userId, String userRole) {
        log.info("Actualizando conteo C1 para folio {}", dto.getFolio());
        validateRole(userRole, "actualizar C1",
                "ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO");

        String roleUpper = userRole.toUpperCase();
        Label label = findLabelForUpdate(dto.getFolio(), userId, roleUpper);

        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(dto.getFolio());
        LabelCountEvent eventC1 = events.stream()
                .filter(e -> e.getCountNumber() == 1)
                .findFirst()
                .orElseThrow(() -> new LabelNotFoundException("No existe un conteo C1 para actualizar en folio " + dto.getFolio()));

        BigDecimal valorAnterior = eventC1.getCountedValue();
        eventC1.setPreviousValue(valorAnterior);
        eventC1.setCountedValue(dto.getCountedValue());
        eventC1.setUpdatedAt(LocalDateTime.now());
        eventC1.setUpdatedBy(userId);

        LabelCountEvent updated = jpaLabelCountEventRepository.save(eventC1);
        
        // Registrar actualización en historial con periodId y warehouseId correctos
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        countHistoryService.recordCountUpdate(
            userId,
            email,
            dto.getFolio(),
            1,
            dto.getCountedValue().intValue(),
            valorAnterior.intValue(),
            userRole,
            label.getWarehouseId(),
            label.getPeriodId()
        );
        
        log.info("Conteo C1 actualizado: folio={}, anterior={}, nuevo={}, by={}",
                dto.getFolio(), valorAnterior, dto.getCountedValue(), userId);
        return updated;
    }

    @Transactional
    public LabelCountEvent updateCountC2(UpdateCountDTO dto, Long userId, String userRole) {
        log.info("Actualizando conteo C2 para folio {}", dto.getFolio());
        validateRole(userRole, "actualizar C2",
                "ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO");

        String roleUpper = userRole.toUpperCase();
        Label label = findLabelForUpdate(dto.getFolio(), userId, roleUpper);

        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(dto.getFolio());
        LabelCountEvent eventC2 = events.stream()
                .filter(e -> e.getCountNumber() == 2)
                .findFirst()
                .orElseThrow(() -> new LabelNotFoundException("No existe un conteo C2 para actualizar en folio " + dto.getFolio()));

        BigDecimal oldValue = eventC2.getCountedValue();
        eventC2.setPreviousValue(oldValue);
        eventC2.setCountedValue(dto.getCountedValue());
        eventC2.setUpdatedAt(LocalDateTime.now());
        eventC2.setUpdatedBy(userId);

        LabelCountEvent updated = jpaLabelCountEventRepository.save(eventC2);
        
        // Registrar actualización en historial con periodId y warehouseId correctos
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        countHistoryService.recordCountUpdate(
            userId,
            email,
            dto.getFolio(),
            2,
            dto.getCountedValue().intValue(),
            oldValue.intValue(),
            userRole,
            label.getWarehouseId(),
            label.getPeriodId()
        );
        
        log.info("Conteo C2 actualizado: folio={}, anterior={}, nuevo={}, by={}",
                dto.getFolio(), oldValue, dto.getCountedValue(), userId);
        return updated;
    }

    // ── helpers privados ────────────────────────────────────────────────────

    private void validateRole(String userRole, String operation, String... allowedRoles) {
        if (userRole == null) {
            throw new PermissionDeniedException("Rol de usuario requerido para " + operation);
        }
        String roleUpper = userRole.toUpperCase();
        for (String allowed : allowedRoles) {
            if (allowed.equals(roleUpper)) return;
        }
        throw new PermissionDeniedException("No tiene permiso para " + operation +
                ". Solo " + String.join(", ", allowedRoles));
    }

    private Label findAndValidateLabelForCount(Long folio, Long periodId, Long warehouseId,
                                               Long userId, String roleUpper) {
        Optional<Label> optLabel = persistence.findByFolio(folio);
        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException(String.format("El folio %d no existe en el sistema", folio));
        }
        Label label = optLabel.get();

        if (periodId != null && !label.getPeriodId().equals(periodId)) {
            throw new InvalidLabelStateException(String.format(
                    "El folio %d pertenece al periodo '%s' (ID: %d), pero está consultando el periodo ID: %d.",
                    folio, getPeriodName(label.getPeriodId()), label.getPeriodId(), periodId));
        }
        if (warehouseId != null && !label.getWarehouseId().equals(warehouseId)) {
            throw new InvalidLabelStateException(String.format(
                    "El folio %d pertenece al almacén '%s' (ID: %d), pero está consultando almacén ID: %d.",
                    folio, getWarehouseName(label.getWarehouseId()), label.getWarehouseId(), warehouseId));
        }

        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), roleUpper);
        }
        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException(
                    String.format("No se puede registrar conteo: el folio %d está CANCELADO.", folio));
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException(String.format(
                    "No se puede registrar conteo: el folio %d no está IMPRESO. Estado actual: %s",
                    folio, label.getEstado()));
        }
        return label;
    }

    private Label findLabelForUpdate(Long folio, Long userId, String roleUpper) {
        Optional<Label> optLabel = persistence.findByFolio(folio);
        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException("El folio " + folio + " no existe");
        }
        Label label = optLabel.get();

        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), roleUpper);
        }
        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException("No se puede actualizar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException("No se puede actualizar conteo: el marbete no está IMPRESO.");
        }
        return label;
    }

    private LabelCountEvent.Role parseRole(String roleUpper, LabelCountEvent.Role defaultRole) {
        try {
            return LabelCountEvent.Role.valueOf(roleUpper);
        } catch (Exception ex) {
            return defaultRole;
        }
    }

    private String getPeriodName(Long periodId) {
        try {
            return jpaPeriodRepository.findById(periodId)
                    .map(p -> {
                        if (p.getDate() != null) {
                            String f = p.getDate().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "ES")));
                            return f.substring(0, 1).toUpperCase() + f.substring(1);
                        }
                        return "Periodo " + periodId;
                    })
                    .orElse("Periodo " + periodId);
        } catch (Exception e) {
            return "Periodo " + periodId;
        }
    }

    private String getWarehouseName(Long warehouseId) {
        try {
            return warehouseRepository.findById(warehouseId)
                    .map(w -> w.getNameWarehouse() != null ? w.getNameWarehouse() : "Almacén " + warehouseId)
                    .orElse("Almacén " + warehouseId);
        } catch (Exception e) {
            return "Almacén " + warehouseId;
        }
    }
}
