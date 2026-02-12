package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.JasperLabelPrintService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.DuplicateCountException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.CountSequenceException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.exception.LabelAlreadyCancelledException;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelServiceImpl implements LabelService {

    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;
    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;
    private final JpaLabelRequestRepository labelRequestRepository;
    private final JasperLabelPrintService jasperLabelPrintService;
    private final JpaUserRepository userRepository;
    private final tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository jpaLabelRepository;
    private final tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCancelledRepository jpaLabelCancelledRepository;
    private final tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository jpaLabelCountEventRepository;
    private final tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository jpaPeriodRepository;

    /**
     * @deprecated Este método ya no es necesario. Use generateBatchList() directamente.
     * Se mantiene por compatibilidad pero será eliminado en versiones futuras.
     */
    @Deprecated
    @Override
    @Transactional
    public void requestLabels(LabelRequestDTO dto, Long userId, String userRole) {
        // Este método ahora solo existe por compatibilidad
        // La nueva API generateBatchList() genera marbetes directamente
        log.warn("⚠️ requestLabels() está deprecado. Use generateBatchList() en su lugar.");
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        // Crear solicitud simple para compatibilidad
        Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
            dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

        if (existing.isEmpty() && dto.getRequestedLabels() > 0) {
            LabelRequest req = new LabelRequest();
            req.setProductId(dto.getProductId());
            req.setWarehouseId(dto.getWarehouseId());
            req.setPeriodId(dto.getPeriodId());
            req.setRequestedLabels(dto.getRequestedLabels());
            req.setFoliosGenerados(0);
            req.setCreatedBy(userId);
            req.setCreatedAt(LocalDateTime.now());
            persistence.save(req);
        }
    }

    /**
     * @deprecated Use generateBatchList() que es más simple. Este método se mantiene por compatibilidad.
     * Genera marbetes para un solo producto (versión simplificada)
     */
    @Deprecated
    @Override
    @Transactional
    public GenerateBatchResponseDTO generateBatch(GenerateBatchDTO dto, Long userId, String userRole) {
        log.warn("⚠️ generateBatch() está deprecado. Use generateBatchList() en su lugar.");

        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        int cantidad = dto.getLabelsToGenerate();
        long[] range = persistence.allocateFolioRange(dto.getPeriodId(), cantidad);

        // Crear marbetes directamente
        LocalDateTime now = LocalDateTime.now();
        List<Label> labels = new ArrayList<>(cantidad);
        for (long folio = range[0]; folio <= range[1]; folio++) {
            Label label = new Label();
            label.setFolio(folio);
            label.setPeriodId(dto.getPeriodId());
            label.setWarehouseId(dto.getWarehouseId());
            label.setProductId(dto.getProductId());
            label.setEstado(Label.State.GENERADO);
            label.setCreatedBy(userId);
            label.setCreatedAt(now);
            labels.add(label);
        }
        persistence.saveAll(labels);

        return GenerateBatchResponseDTO.builder()
            .totalGenerados(cantidad)
            .generadosConExistencias(cantidad)
            .generadosSinExistencias(0)
            .primerFolio(range[0])
            .ultimoFolio(range[1])
            .mensaje("Generados " + cantidad + " marbetes")
            .build();
    }

    /**
     * 📄 MÉTODO SIMPLIFICADO: Imprime marbetes directamente
     * Busca marbetes en estado GENERADO y genera el PDF
     */
    @Transactional
    @Override
    public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
        log.info("📄 Imprimiendo marbetes: periodo={}, almacén={}", dto.getPeriodId(), dto.getWarehouseId());

        // Validaciones básicas
        if (userRole == null || userRole.trim().isEmpty()) {
            throw new PermissionDeniedException("Rol de usuario requerido");
        }
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        // Buscar marbetes pendientes
        List<Label> labels;
        if (dto.getFolios() != null && !dto.getFolios().isEmpty()) {
            // Modo selectivo: folios específicos
            labels = persistence.findByFoliosInAndPeriodAndWarehouse(
                dto.getFolios(), dto.getPeriodId(), dto.getWarehouseId());
            if (labels.size() != dto.getFolios().size()) {
                throw new LabelNotFoundException("Algunos folios no existen");
            }
        } else if (dto.getProductId() != null) {
            // Filtrar por producto
            labels = persistence.findPendingLabelsByPeriodWarehouseAndProduct(
                dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId());
        } else {
            // Todos los pendientes
            labels = persistence.findPendingLabelsByPeriodAndWarehouse(
                dto.getPeriodId(), dto.getWarehouseId());
        }

        if (labels.isEmpty()) {
            throw new InvalidLabelStateException("No hay marbetes pendientes de impresión");
        }

        if (labels.size() > 500) {
            throw new InvalidLabelStateException("Límite máximo: 500 marbetes por impresión");
        }

        labels.sort(Comparator.comparing(Label::getFolio));

        // Generar PDF
        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF");
        }

        // Actualizar estados a IMPRESO
        Long minFolio = labels.get(0).getFolio();
        Long maxFolio = labels.get(labels.size() - 1).getFolio();
        updateLabelsStateAfterPrint(dto.getPeriodId(), dto.getWarehouseId(), minFolio, maxFolio, userId);

        log.info("✅ PDF generado: {} KB, {} marbetes", pdfBytes.length / 1024, labels.size());
        return pdfBytes;
    }

    /**
     * 🔒 Actualiza estado de marbetes a IMPRESO después de generar PDF
     */
    @Transactional
    protected LabelPrint updateLabelsStateAfterPrint(Long periodId, Long warehouseId,
                                                     Long minFolio, Long maxFolio, Long userId) {
        return persistence.printLabelsRange(periodId, warehouseId, minFolio, maxFolio, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public tokai.com.mx.SIGMAV2.modules.labels.application.dto.PendingPrintCountResponseDTO getPendingPrintCount(
            tokai.com.mx.SIGMAV2.modules.labels.application.dto.PendingPrintCountRequestDTO dto,
            Long userId,
            String userRole) {

        log.info("Contando marbetes pendientes: periodId={}, warehouseId={}, productId={}",
            dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId());

        // Validar acceso al almacén
        if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR"))) {
            log.debug("Usuario {} con rol {} puede consultar cualquier almacén", userId, userRole);
        } else {
            warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
        }

        // Contar marbetes pendientes
        List<Label> pendingLabels;

        if (dto.getProductId() != null) {
            // Filtrar por producto
            pendingLabels = persistence.findPendingLabelsByPeriodWarehouseAndProduct(
                dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId());
        } else {
            // Todos los pendientes del periodo/almacén
            pendingLabels = persistence.findPendingLabelsByPeriodAndWarehouse(
                dto.getPeriodId(), dto.getWarehouseId());
        }

        long count = pendingLabels.size();

        // Obtener información del almacén y periodo
        String warehouseName = null;
        String periodName = null;

        try {
            var warehouse = warehouseRepository.findById(dto.getWarehouseId());
            if (warehouse.isPresent()) {
                warehouseName = warehouse.get().getNameWarehouse();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener nombre del almacén: {}", e.getMessage());
        }

        try {
            var period = jpaPeriodRepository.findById(dto.getPeriodId());
            if (period.isPresent()) {
                // Formatear la fecha del periodo
                java.time.LocalDate date = period.get().getDate();
                if (date != null) {
                    periodName = date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener nombre del periodo: {}", e.getMessage());
        }

        log.info("Marbetes pendientes encontrados: {}", count);

        return new tokai.com.mx.SIGMAV2.modules.labels.application.dto.PendingPrintCountResponseDTO(
            count,
            dto.getPeriodId(),
            dto.getWarehouseId(),
            warehouseName,
            periodName
        );
    }

    @Override
    @Transactional
    public LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole) {
        if (userRole == null) {
            throw new PermissionDeniedException("Role de usuario requerido para registrar C1");
        }
        String roleUpper = userRole.toUpperCase();
        boolean allowed = roleUpper.equals("ADMINISTRADOR") || roleUpper.equals("ALMACENISTA") || roleUpper.equals("AUXILIAR") || roleUpper.equals("AUXILIAR_DE_CONTEO");
        if (!allowed) {
            throw new PermissionDeniedException("No tiene permiso para registrar C1");
        }

        // Verificar que el marbete exista
        Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException(
                String.format("El folio %d no existe en el sistema", dto.getFolio())
            );
        }
        Label label = optLabel.get();

        // Validar contexto: periodo y almacén (si están presentes en el DTO)
        if (dto.getPeriodId() != null && !label.getPeriodId().equals(dto.getPeriodId())) {
            String labelPeriodName = getPeriodName(label.getPeriodId());
            String requestedPeriodName = getPeriodName(dto.getPeriodId());
            throw new InvalidLabelStateException(
                String.format(
                    "El folio %d pertenece al periodo '%s' (ID: %d), pero está consultando el periodo '%s' (ID: %d). " +
                    "Por favor verifique que está trabajando en el periodo correcto.",
                    dto.getFolio(), labelPeriodName, label.getPeriodId(), requestedPeriodName, dto.getPeriodId()
                )
            );
        }

        if (dto.getWarehouseId() != null && !label.getWarehouseId().equals(dto.getWarehouseId())) {
            String labelWarehouseName = getWarehouseName(label.getWarehouseId());
            String requestedWarehouseName = getWarehouseName(dto.getWarehouseId());
            throw new InvalidLabelStateException(
                String.format(
                    "El folio %d pertenece al almacén '%s' (ID: %d), pero está consultando el almacén '%s' (ID: %d). " +
                    "Por favor verifique que está en el almacén correcto.",
                    dto.getFolio(), labelWarehouseName, label.getWarehouseId(), requestedWarehouseName, dto.getWarehouseId()
                )
            );
        }

        // Validar acceso al almacén del marbete
        // NOTA: AUXILIAR_DE_CONTEO puede registrar conteos sin restricción de almacén
        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
        }

        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException(
                String.format("No se puede registrar conteo: el folio %d está CANCELADO.", dto.getFolio())
            );
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException(
                String.format(
                    "No se puede registrar conteo: el folio %d no está IMPRESO. Estado actual: %s",
                    dto.getFolio(), label.getEstado()
                )
            );
        }

        // No permitir registrar C1 si ya existe C1
        if (persistence.hasCountNumber(dto.getFolio(), 1)) {
            throw new DuplicateCountException(
                String.format("El conteo C1 ya fue registrado para el folio %d.", dto.getFolio())
            );
        }
        // No permitir registrar C1 si ya existe C2 (secuencia rota)
        if (persistence.hasCountNumber(dto.getFolio(), 2)) {
            throw new CountSequenceException(
                String.format(
                    "No se puede registrar C1 porque ya existe un conteo C2 para el folio %d. La secuencia de conteo está rota.",
                    dto.getFolio()
                )
            );
        }

        LabelCountEvent.Role roleEnum;
        try { roleEnum = LabelCountEvent.Role.valueOf(roleUpper); } catch (Exception ex) { roleEnum = LabelCountEvent.Role.AUXILIAR; }

        return persistence.saveCountEvent(dto.getFolio(), userId, 1, dto.getCountedValue(), roleEnum, false);
    }

    @Override
    @Transactional
    public LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole) {
        if (userRole == null) {
            throw new PermissionDeniedException("Role de usuario requerido para registrar C2");
        }
        String roleUpper = userRole.toUpperCase();
        // Permitir a todos los roles registrar C2 según requerimientos funcionales
        boolean allowed = roleUpper.equals("ADMINISTRADOR") || roleUpper.equals("ALMACENISTA") ||
                         roleUpper.equals("AUXILIAR") || roleUpper.equals("AUXILIAR_DE_CONTEO");
        if (!allowed) {
            throw new PermissionDeniedException("No tiene permiso para registrar C2");
        }

        // Verificar que el marbete exista
        Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException(
                String.format("El folio %d no existe en el sistema", dto.getFolio())
            );
        }
        Label label = optLabel.get();

        // Validar contexto: periodo y almacén (si están presentes en el DTO)
        if (dto.getPeriodId() != null && !label.getPeriodId().equals(dto.getPeriodId())) {
            String labelPeriodName = getPeriodName(label.getPeriodId());
            String requestedPeriodName = getPeriodName(dto.getPeriodId());
            throw new InvalidLabelStateException(
                String.format(
                    "El folio %d pertenece al periodo '%s' (ID: %d), pero está consultando el periodo '%s' (ID: %d). " +
                    "Por favor verifique que está trabajando en el periodo correcto.",
                    dto.getFolio(), labelPeriodName, label.getPeriodId(), requestedPeriodName, dto.getPeriodId()
                )
            );
        }

        if (dto.getWarehouseId() != null && !label.getWarehouseId().equals(dto.getWarehouseId())) {
            String labelWarehouseName = getWarehouseName(label.getWarehouseId());
            String requestedWarehouseName = getWarehouseName(dto.getWarehouseId());
            throw new InvalidLabelStateException(
                String.format(
                    "El folio %d pertenece al almacén '%s' (ID: %d), pero está consultando el almacén '%s' (ID: %d). " +
                    "Por favor verifique que está en el almacén correcto.",
                    dto.getFolio(), labelWarehouseName, label.getWarehouseId(), requestedWarehouseName, dto.getWarehouseId()
                )
            );
        }

        // Validar acceso al almacén del marbete
        // NOTA: AUXILIAR_DE_CONTEO puede registrar conteos sin restricción de almacén
        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
        }

        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException(
                String.format("No se puede registrar conteo: el folio %d está CANCELADO.", dto.getFolio())
            );
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException(
                String.format(
                    "No se puede registrar conteo: el folio %d no está IMPRESO. Estado actual: %s",
                    dto.getFolio(), label.getEstado()
                )
            );
        }

        // Debe existir C1 antes de C2
        if (!persistence.hasCountNumber(dto.getFolio(), 1)) {
            throw new CountSequenceException(
                String.format(
                    "No se puede registrar C2 porque no existe un conteo C1 previo para el folio %d. Debe registrar C1 primero.",
                    dto.getFolio()
                )
            );
        }

        // No permitir duplicar C2
        if (persistence.hasCountNumber(dto.getFolio(), 2)) {
            throw new DuplicateCountException(
                String.format("El conteo C2 ya fue registrado para el folio %d.", dto.getFolio())
            );
        }

        LabelCountEvent.Role roleEnum;
        try { roleEnum = LabelCountEvent.Role.valueOf(roleUpper); } catch (Exception ex) { roleEnum = LabelCountEvent.Role.AUXILIAR_DE_CONTEO; }

        return persistence.saveCountEvent(dto.getFolio(), userId, 2, dto.getCountedValue(), roleEnum, true);
    }

    @Override
    @Transactional
    public LabelCountEvent updateCountC1(tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCountDTO dto, Long userId, String userRole) {
        log.info("Actualizando conteo C1 para folio {}", dto.getFolio());

        if (userRole == null) {
            throw new PermissionDeniedException("Role de usuario requerido para actualizar C1");
        }

        String roleUpper = userRole.toUpperCase();
        boolean allowed = roleUpper.equals("ADMINISTRADOR") || roleUpper.equals("ALMACENISTA") ||
                         roleUpper.equals("AUXILIAR") || roleUpper.equals("AUXILIAR_DE_CONTEO");
        if (!allowed) {
            throw new PermissionDeniedException("No tiene permiso para actualizar C1");
        }

        // Verificar que el marbete exista
        Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException("El folio no existe");
        }
        Label label = optLabel.get();

        // Validar acceso al almacén del marbete
        // NOTA: AUXILIAR_DE_CONTEO puede actualizar conteos sin restricción de almacén
        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
        }

        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException("No se puede actualizar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException("No se puede actualizar conteo: el marbete no está IMPRESO.");
        }

        // Buscar el evento de conteo C1 existente
        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(dto.getFolio());
        LabelCountEvent eventC1 = events.stream()
            .filter(e -> e.getCountNumber() == 1)
            .findFirst()
            .orElseThrow(() -> new LabelNotFoundException("No existe un conteo C1 para actualizar"));

        // Actualizar el valor
        eventC1.setCountedValue(dto.getCountedValue());

        LabelCountEvent updated = jpaLabelCountEventRepository.save(eventC1);
        log.info("Conteo C1 actualizado exitosamente para folio {}", dto.getFolio());

        return updated;
    }

    @Override
    @Transactional
    public LabelCountEvent updateCountC2(tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCountDTO dto, Long userId, String userRole) {
        log.info("🔄 Iniciando actualización de conteo C2 para folio {}", dto.getFolio());
        log.debug("Parámetros: folio={}, countedValue={}, observaciones={}, userId={}, userRole={}",
            dto.getFolio(), dto.getCountedValue(), dto.getObservaciones(), userId, userRole);

        try {
            if (userRole == null) {
                log.error("❌ Role de usuario es null");
                throw new PermissionDeniedException("Role de usuario requerido para actualizar C2");
            }

            String roleUpper = userRole.toUpperCase();
            log.debug("Role normalizado: {}", roleUpper);

            // Para C2, permitir actualización a ADMINISTRADOR, ALMACENISTA y AUXILIAR_DE_CONTEO
            boolean allowed = roleUpper.equals("ADMINISTRADOR") ||
                             roleUpper.equals("ALMACENISTA") ||
                             roleUpper.equals("AUXILIAR_DE_CONTEO");
            if (!allowed) {
                log.error("❌ Rol {} no tiene permisos para actualizar C2", roleUpper);
                throw new PermissionDeniedException("No tiene permiso para actualizar C2. Solo ADMINISTRADOR, ALMACENISTA o AUXILIAR_DE_CONTEO pueden actualizar el segundo conteo.");
            }

            log.debug("Buscando marbete con folio {}", dto.getFolio());

            // Verificar que el marbete exista
            Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
            if (optLabel.isEmpty()) {
                log.error("❌ Folio {} no existe en el sistema", dto.getFolio());
                throw new LabelNotFoundException("El folio no existe");
            }
            Label label = optLabel.get();

            log.debug("Marbete encontrado: productId={}, warehouseId={}, estado={}",
                label.getProductId(), label.getWarehouseId(), label.getEstado());

            // Validar acceso al almacén del marbete
            // NOTA: AUXILIAR_DE_CONTEO puede actualizar conteos sin restricción de almacén
            log.debug("Validando acceso al almacén {}", label.getWarehouseId());
            if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
                warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
            }

            if (label.getEstado() == Label.State.CANCELADO) {
                log.error("❌ Marbete está CANCELADO");
                throw new InvalidLabelStateException("No se puede actualizar conteo: el marbete está CANCELADO.");
            }
            if (label.getEstado() != Label.State.IMPRESO) {
                log.error("❌ Marbete no está IMPRESO, estado actual: {}", label.getEstado());
                throw new InvalidLabelStateException("No se puede actualizar conteo: el marbete no está IMPRESO.");
            }

            // Buscar el evento de conteo C2 existente
            log.debug("Buscando evento C2 para folio {}", dto.getFolio());
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(dto.getFolio());
            log.debug("Eventos encontrados: {}", events.size());

            LabelCountEvent eventC2 = events.stream()
                .filter(e -> e.getCountNumber() == 2)
                .findFirst()
                .orElseThrow(() -> {
                    log.error("❌ No existe un conteo C2 para el folio {}", dto.getFolio());
                    return new LabelNotFoundException("No existe un conteo C2 para actualizar");
                });

            log.debug("Evento C2 encontrado: id={}, countedValue={}", eventC2.getIdCountEvent(), eventC2.getCountedValue());

            // Actualizar el valor
            BigDecimal oldValue = eventC2.getCountedValue();
            eventC2.setCountedValue(dto.getCountedValue());

            log.debug("Actualizando valor de {} a {}", oldValue, dto.getCountedValue());

            LabelCountEvent updated = jpaLabelCountEventRepository.save(eventC2);
            log.info("✅ Conteo C2 actualizado exitosamente para folio {} - Valor anterior: {}, Valor nuevo: {}",
                dto.getFolio(), oldValue, dto.getCountedValue());

            return updated;

        } catch (PermissionDeniedException | LabelNotFoundException | InvalidLabelStateException e) {
            log.warn("Excepción controlada en updateCountC2: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error inesperado en updateCountC2 para folio {}: {}", dto.getFolio(), e.getMessage(), e);
            throw new RuntimeException("Error inesperado al actualizar C2: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelSummaryResponseDTO> getLabelSummary(LabelSummaryRequestDTO dto, Long userId, String userRole) {
        log.info("getLabelSummary - Inicio: periodId={}, warehouseId={}, page={}, size={}, searchText={}, sortBy={}, sortDirection={}, userId={}, userRole={}",
            dto.getPeriodId(), dto.getWarehouseId(), dto.getPage(), dto.getSize(),
            dto.getSearchText(), dto.getSortBy(), dto.getSortDirection(), userId, userRole);

        // Si no se especifica periodo, obtener el último creado
        final Long periodId;
        if (dto.getPeriodId() == null) {
            periodId = persistence.findLastCreatedPeriodId()
                .orElseThrow(() -> new RuntimeException("No hay periodos registrados"));
            log.info("Usando periodo por default (último creado): {}", periodId);
        } else {
            periodId = dto.getPeriodId();
        }

        // Si no se especifica almacén, obtener el primero
        final Long warehouseId;
        if (dto.getWarehouseId() == null) {
            warehouseId = warehouseRepository.findFirstByOrderByIdWarehouseAsc()
                .map(WarehouseEntity::getIdWarehouse)
                .orElseThrow(() -> new RuntimeException("No hay almacenes registrados"));
            log.info("Usando almacén por default (primero): {}", warehouseId);
        } else {
            warehouseId = dto.getWarehouseId();
        }

        try {
            // Validar acceso al almacén (solo si no es ADMINISTRADOR o AUXILIAR)
            log.info("Validando acceso al almacén...");
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
            log.info("Acceso validado correctamente");
        } catch (Exception e) {
            log.warn("Error en validateWarehouseAccess: {}", e.getMessage());
            // Si falla la validación pero es ADMINISTRADOR o AUXILIAR, permitir acceso
            if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR"))) {
                log.info("Usuario es ADMINISTRADOR o AUXILIAR, permitiendo acceso");
            } else {
                log.error("Usuario sin acceso al almacén", e);
                throw e; // Re-lanzar la excepción si no tiene permisos
            }
        }

        // Obtener información del almacén
        WarehouseEntity warehouseEntity = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado: " + warehouseId));

        String claveAlmacen = warehouseEntity.getWarehouseKey();
        String nombreAlmacen = warehouseEntity.getNameWarehouse();

        log.info("Almacén encontrado: {} - {}", claveAlmacen, nombreAlmacen);

        // Obtener todas las solicitudes de marbetes para este periodo y almacén
        List<LabelRequest> allRequests = labelRequestRepository.findAll();
        Map<Long, LabelRequest> requestsByProduct = allRequests.stream()
                .filter(req -> req.getPeriodId() != null && req.getPeriodId().equals(periodId) &&
                              req.getWarehouseId() != null && req.getWarehouseId().equals(warehouseId))
                .collect(Collectors.toMap(
                    LabelRequest::getProductId,
                    req -> req,
                    (existing, replacement) -> existing
                ));

        log.info("Encontradas {} solicitudes de productos", requestsByProduct.size());

        // Obtener todos los marbetes generados para este periodo y almacén
        List<Label> labels = persistence.findByPeriodIdAndWarehouseId(periodId, warehouseId, 0, 100000);

        // Agrupar marbetes por producto y contar
        Map<Long, Long> generatedLabelsByProduct = labels.stream()
                .collect(Collectors.groupingBy(Label::getProductId, Collectors.counting()));

        log.info("Encontrados {} marbetes generados para {} productos", labels.size(), generatedLabelsByProduct.size());

        // IMPORTANTE: Obtener TODOS los productos del inventario que tienen existencias en este almacén y periodo
        // Esto asegura que se muestren todos los productos aunque no tengan solicitudes de marbetes
        List<InventoryStockEntity> allStockInWarehouse = inventoryStockRepository
                .findByWarehouseIdWarehouseAndPeriodId(warehouseId, periodId);

        log.info("Encontrados {} productos en el inventario del almacén {}", allStockInWarehouse.size(), warehouseId);

        // Construir la lista completa de productos (inventario + solicitudes + marbetes)
        Set<Long> allProductIds = new HashSet<>();

        // Agregar todos los productos del inventario del almacén
        allStockInWarehouse.stream()
                .filter(stock -> stock.getProduct() != null)
                .forEach(stock -> allProductIds.add(stock.getProduct().getIdProduct()));

        // Agregar productos con solicitudes
        allProductIds.addAll(requestsByProduct.keySet());

        // Agregar productos con marbetes generados
        allProductIds.addAll(generatedLabelsByProduct.keySet());

        log.info("Total de productos únicos a mostrar: {}", allProductIds.size());

        List<LabelSummaryResponseDTO> allResults = new ArrayList<>();

        for (Long productId : allProductIds) {
            try {
                // Obtener información del producto
                ProductEntity product = productRepository.findById(productId).orElse(null);
                if (product == null) {
                    log.warn("Producto no encontrado: {}", productId);
                    continue;
                }

                // Obtener la solicitud de marbetes para este producto
                LabelRequest request = requestsByProduct.get(productId);
                int foliosSolicitados = request != null ? request.getRequestedLabels() : 0;
                int foliosExistentes = generatedLabelsByProduct.getOrDefault(productId, 0L).intValue();

                // Obtener existencias del inventario (ahora incluyendo periodo)
                int existencias = 0;
                String estado = "SIN_STOCK";
                try {
                    InventoryStockEntity stock = inventoryStockRepository
                            .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId)
                            .orElse(null);
                    if (stock != null) {
                        existencias = stock.getExistQty() != null ? stock.getExistQty().intValue() : 0;
                        estado = stock.getStatus() != null ? stock.getStatus().name() : "A";
                    }
                } catch (Exception e) {
                    log.warn("No se pudieron obtener existencias para producto {}: {}", productId, e.getMessage());
                }

                // Buscar registros de impresión para este producto, periodo y almacén
                List<LabelPrint> prints = persistence.findLabelPrintsByProductPeriodWarehouse(productId, periodId, warehouseId);
                boolean impreso = !prints.isEmpty();
                String fechaImpresion = null;
                if (impreso) {
                    // Tomar la fecha más reciente
                    fechaImpresion = prints.stream()
                        .map(LabelPrint::getPrintedAt)
                        .filter(java.util.Objects::nonNull)
                        .max(java.time.LocalDateTime::compareTo)
                        .map(java.time.LocalDateTime::toString)
                        .orElse(null);
                }

                // NUEVO: Obtener rangos de folios y lista de folios individuales
                List<Label> productLabels = labels.stream()
                    .filter(l -> l.getProductId().equals(productId))
                    .sorted(java.util.Comparator.comparing(Label::getFolio))
                .toList();

        Long primerFolio = null;
        Long ultimoFolio = null;
                List<Long> foliosList = new ArrayList<>();

                if (!productLabels.isEmpty()) {
                    primerFolio = productLabels.getFirst().getFolio();
                    ultimoFolio = productLabels.getLast().getFolio();
                    foliosList = productLabels.stream()
                        .map(Label::getFolio)
                        .toList();
                }

                // Construir el DTO de respuesta
                LabelSummaryResponseDTO summary = LabelSummaryResponseDTO.builder()
                        .productId(productId)
                        .claveProducto(product.getCveArt())
                        .nombreProducto(product.getDescr())
                        .claveAlmacen(claveAlmacen)
                        .nombreAlmacen(nombreAlmacen)
                        .foliosSolicitados(foliosSolicitados)
                        .foliosExistentes(foliosExistentes)
                        .estado(estado)
                        .existencias(existencias)
                        .impreso(impreso)
                        .fechaImpresion(fechaImpresion)
                        .primerFolio(primerFolio)
                        .ultimoFolio(ultimoFolio)
                        .folios(foliosList)
                        .build();

                allResults.add(summary);

            } catch (Exception e) {
                log.error("Error procesando producto {}: {}", productId, e.getMessage(), e);
            }
        }

        log.info("Construidos {} registros totales", allResults.size());

        // Aplicar filtro de búsqueda (case-insensitive según requerimientos)
        List<LabelSummaryResponseDTO> filteredResults = allResults;
        if (dto.getSearchText() != null && !dto.getSearchText().trim().isEmpty()) {
            String searchLower = dto.getSearchText().toLowerCase();
            filteredResults = allResults.stream()
                .filter(item -> {
                    // Búsqueda en: Clave de producto, Producto, Clave de almacén, Almacén, Estado, Existencias
                     return (item.getClaveProducto() != null && item.getClaveProducto().toLowerCase().contains(searchLower)) ||
                           (item.getNombreProducto() != null && item.getNombreProducto().toLowerCase().contains(searchLower)) ||
                           (item.getClaveAlmacen() != null && item.getClaveAlmacen().toLowerCase().contains(searchLower)) ||
                           (item.getNombreAlmacen() != null && item.getNombreAlmacen().toLowerCase().contains(searchLower)) ||
                           (item.getEstado() != null && item.getEstado().toLowerCase().contains(searchLower)) ||
                           String.valueOf(item.getExistencias()).contains(searchLower);
                })
                .collect(Collectors.toCollection(ArrayList::new));
            log.info("Búsqueda aplicada: '{}', resultados filtrados: {}", dto.getSearchText(), filteredResults.size());
        }

        // Aplicar ordenamiento personalizado
        Comparator<LabelSummaryResponseDTO> comparator = getComparator(dto.getSortBy());
        if ("DESC".equalsIgnoreCase(dto.getSortDirection())) {
            comparator = comparator.reversed();
        }
        filteredResults.sort(comparator);
        log.info("Ordenamiento aplicado: {} {}", dto.getSortBy(), dto.getSortDirection());

        // Aplicar paginación
        int totalFiltered = filteredResults.size();
        int start = dto.getPage() * dto.getSize();
        int end = Math.min(start + dto.getSize(), totalFiltered);

        if (start >= totalFiltered && totalFiltered > 0) {
            log.warn("Página {} fuera de rango (total: {}), devolviendo lista vacía", dto.getPage(), totalFiltered);
            return new ArrayList<>();
        }

        List<LabelSummaryResponseDTO> paginatedResults = start < totalFiltered ?
            filteredResults.subList(start, end) : new ArrayList<>();

        log.info("Paginación aplicada: página {}, tamaño {}, devolviendo {} registros de {} totales filtrados",
            dto.getPage(), dto.getSize(), paginatedResults.size(), totalFiltered);

        return paginatedResults;
    }

    /**
     * Método auxiliar para obtener el comparador según la columna seleccionada.
     * Columnas soportadas: foliosExistentes, claveProducto, producto/nombreProducto,
     * claveAlmacen, almacen/nombreAlmacen, estado, existencias
     */
    private Comparator<LabelSummaryResponseDTO> getComparator(String sortBy) {
        if (sortBy == null) sortBy = "claveProducto";

        return switch (sortBy.toLowerCase()) {
            case "foliosexistentes" -> Comparator.comparing(LabelSummaryResponseDTO::getFoliosExistentes);
            case "claveproducto" -> Comparator.comparing(LabelSummaryResponseDTO::getClaveProducto,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "producto", "nombreproducto" -> Comparator.comparing(LabelSummaryResponseDTO::getNombreProducto,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "clavealmacen" -> Comparator.comparing(LabelSummaryResponseDTO::getClaveAlmacen,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "almacen", "nombrealmacen" -> Comparator.comparing(LabelSummaryResponseDTO::getNombreAlmacen,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "estado" -> Comparator.comparing(LabelSummaryResponseDTO::getEstado,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "existencias" -> Comparator.comparing(LabelSummaryResponseDTO::getExistencias);
            default ->
                // Por defecto, ordenar por clave de producto
                    Comparator.comparing(LabelSummaryResponseDTO::getClaveProducto,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    }

    /**
     * 🚀 MÉTODO SIMPLIFICADO: Genera marbetes directamente sin solicitudes previas
     * Este método reemplaza todo el flujo antiguo de request -> generate
     */
    @Override
    @Transactional
    public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
        log.info("🚀 Generando marbetes para {} productos", dto.getProducts().size());

        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        LocalDateTime now = LocalDateTime.now();
        int totalGenerados = 0;

        for (ProductBatchDTO product : dto.getProducts()) {
            int cantidad = product.getLabelsToGenerate();

            // Asignar folios consecutivos
            long[] range = persistence.allocateFolioRange(dto.getPeriodId(), cantidad);

            // Crear marbetes en batch
            List<Label> labels = new ArrayList<>(cantidad);
            for (long folio = range[0]; folio <= range[1]; folio++) {
                Label label = new Label();
                label.setFolio(folio);
                label.setPeriodId(dto.getPeriodId());
                label.setWarehouseId(dto.getWarehouseId());
                label.setProductId(product.getProductId());
                label.setEstado(Label.State.GENERADO);
                label.setCreatedBy(userId);
                label.setCreatedAt(now);
                labels.add(label);
            }

            // Guardar en BD
            persistence.saveAll(labels);
            totalGenerados += cantidad;

            log.info("✅ Producto {}: {} marbetes (folios {}-{})",
                product.getProductId(), cantidad, range[0], range[1]);
        }

        log.info("✅ Total generado: {} marbetes", totalGenerados);
    }

    @Override
    public tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelStatusResponseDTO getLabelStatus(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        var builder = tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelStatusResponseDTO.builder();
        builder.folio(folio).periodId(periodId).warehouseId(warehouseId);
        String mensaje;
        try {
            // Buscar el marbete
            var optLabel = persistence.findByFolio(folio);
            if (optLabel.isEmpty()) {
                builder.estado("NO_EXISTE");
                builder.impreso(false);
                builder.mensaje("El folio no existe");
                return builder.build();
            }
            var label = optLabel.get();
            builder.productId(label.getProductId());
            builder.estado(label.getEstado() != null ? label.getEstado().name() : "SIN_ESTADO");
            // Buscar producto
            var product = productRepository.findById(label.getProductId()).orElse(null);
            if (product != null) {
                builder.claveProducto(product.getCveArt());
                builder.nombreProducto(product.getDescr());
            }
            // Buscar almacén
            var warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
            if (warehouse != null) {
                builder.claveAlmacen(warehouse.getWarehouseKey());
                builder.nombreAlmacen(warehouse.getNameWarehouse());
            }
            // Buscar impresiones
            var prints = persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), periodId, warehouseId);
            boolean impreso = !prints.isEmpty();
            String fechaImpresion = null;
            if (impreso) {
                fechaImpresion = prints.stream()
                    .map(LabelPrint::getPrintedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(java.time.LocalDateTime::compareTo)
                    .map(java.time.LocalDateTime::toString)
                    .orElse(null);
            }
            builder.impreso(impreso);
            builder.fechaImpresion(fechaImpresion);
            // Mensaje de ayuda
            if (label.getEstado() == null) {
                mensaje = "El marbete no tiene estado definido.";
            } else if (label.getEstado().name().equals("CANCELADO")) {
                mensaje = "El marbete está CANCELADO y no puede imprimirse.";
            } else if (label.getEstado().name().equals("IMPRESO")) {
                mensaje = "El marbete ya fue impreso. Puedes reimprimir si lo necesitas.";
            } else if (label.getEstado().name().equals("GENERADO")) {
                mensaje = "El marbete está listo para imprimir.";
            } else {
                mensaje = "Estado actual: " + label.getEstado().name();
            }
            builder.mensaje(mensaje);
        } catch (Exception e) {
            builder.estado("ERROR");
            builder.impreso(false);
            builder.mensaje("Error al consultar el estado: " + e.getMessage());
        }
        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public long countLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        log.info("Contando marbetes para periodId={}, warehouseId={}", periodId, warehouseId);
        long count = persistence.countByPeriodIdAndWarehouseId(periodId, warehouseId);
        log.info("Total de marbetes encontrados: {}", count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public List< LabelCancelledDTO> getCancelledLabels(Long periodId, Long warehouseId, Long userId, String userRole) {
        log.info("Consultando marbetes cancelados para periodId={}, warehouseId={}", periodId, warehouseId);

        // Validar acceso al almacén
        try {
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        } catch (Exception e) {
            if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR"))) {
                log.info("Usuario es ADMINISTRADOR o AUXILIAR, permitiendo acceso");
            } else {
                throw e;
            }
        }

        // Obtener marbetes cancelados no reactivados
        List<tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled> cancelledLabels =
            persistence.findCancelledByPeriodAndWarehouse(periodId, warehouseId, false);

        log.info("Encontrados {} marbetes cancelados", cancelledLabels.size());

        // Obtener información de almacén
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        // Convertir a DTOs
        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO> dtos = new ArrayList<>();
        for (tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled cancelled : cancelledLabels) {
            ProductEntity product = productRepository.findById(cancelled.getProductId()).orElse(null);
            if (product == null) continue;

            tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO dto =
                tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO.builder()
                    .idLabelCancelled(cancelled.getIdLabelCancelled())
                    .folio(cancelled.getFolio())
                    .productId(cancelled.getProductId())
                    .claveProducto(product.getCveArt())
                    .nombreProducto(product.getDescr())
                    .warehouseId(cancelled.getWarehouseId())
                    .claveAlmacen(warehouse.getWarehouseKey())
                    .nombreAlmacen(warehouse.getNameWarehouse())
                    .periodId(cancelled.getPeriodId())
                    .existenciasAlCancelar(cancelled.getExistenciasAlCancelar())
                    .existenciasActuales(cancelled.getExistenciasActuales())
                    .motivoCancelacion(cancelled.getMotivoCancelacion())
                    .canceladoAt(cancelled.getCanceladoAt() != null ? cancelled.getCanceladoAt().toString() : null)
                    .reactivado(cancelled.getReactivado())
                    .reactivadoAt(cancelled.getReactivadoAt() != null ? cancelled.getReactivadoAt().toString() : null)
                    .notas(cancelled.getNotas())
                    .build();
            dtos.add(dto);
        }

        return dtos;
    }

    @Override
    @Transactional
    public LabelCancelledDTO updateCancelledStock(
            UpdateCancelledStockDTO dto, Long userId, String userRole) {
        log.info("Actualizando existencias de marbete cancelado: folio={}, nuevasExistencias={}",
            dto.getFolio(), dto.getExistenciasActuales());

        // Buscar el marbete cancelado
        tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled cancelled =
            persistence.findCancelledByFolio(dto.getFolio())
                .orElseThrow(() -> new LabelNotFoundException("Marbete cancelado no encontrado con folio: " + dto.getFolio()));

        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, cancelled.getWarehouseId(), userRole);

        // Actualizar existencias
        cancelled.setExistenciasActuales(dto.getExistenciasActuales());
        if (dto.getNotas() != null) {
            cancelled.setNotas(dto.getNotas());
        }

        // Si ahora tiene existencias, reactivar el marbete
        if (dto.getExistenciasActuales() > 0 && !cancelled.getReactivado()) {
            cancelled.setReactivado(true);
            cancelled.setReactivadoAt(LocalDateTime.now());
            cancelled.setReactivadoBy(userId);
            log.info("Marbete folio {} reactivado por tener existencias", dto.getFolio());

            // Crear el marbete normal
            Label label = new Label();
            label.setFolio(cancelled.getFolio());
            label.setLabelRequestId(cancelled.getLabelRequestId());
            label.setPeriodId(cancelled.getPeriodId());
            label.setWarehouseId(cancelled.getWarehouseId());
            label.setProductId(cancelled.getProductId());
            label.setEstado(Label.State.GENERADO);
            label.setCreatedBy(userId);
            label.setCreatedAt(LocalDateTime.now());
            persistence.save(label);
            log.info("Marbete creado en tabla labels con estado GENERADO");
        }

        persistence.saveCancelled(cancelled);

        // Construir respuesta
        ProductEntity product = productRepository.findById(cancelled.getProductId()).orElse(null);
        WarehouseEntity warehouse = warehouseRepository.findById(cancelled.getWarehouseId()).orElse(null);

        return tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO.builder()
            .idLabelCancelled(cancelled.getIdLabelCancelled())
            .folio(cancelled.getFolio())
            .productId(cancelled.getProductId())
            .claveProducto(product != null ? product.getCveArt() : null)
            .nombreProducto(product != null ? product.getDescr() : null)
            .warehouseId(cancelled.getWarehouseId())
            .claveAlmacen(warehouse != null ? warehouse.getWarehouseKey() : null)
            .nombreAlmacen(warehouse != null ? warehouse.getNameWarehouse() : null)
            .periodId(cancelled.getPeriodId())
            .existenciasAlCancelar(cancelled.getExistenciasAlCancelar())
            .existenciasActuales(cancelled.getExistenciasActuales())
            .motivoCancelacion(cancelled.getMotivoCancelacion())
            .canceladoAt(cancelled.getCanceladoAt() != null ? cancelled.getCanceladoAt().toString() : null)
            .reactivado(cancelled.getReactivado())
            .reactivadoAt(cancelled.getReactivadoAt() != null ? cancelled.getReactivadoAt().toString() : null)
            .notas(cancelled.getNotas())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelDetailDTO> getLabelsByProduct(
            Long productId, Long periodId, Long warehouseId, Long userId, String userRole) {
        log.info("Consultando marbetes del producto {} en periodo {} y almacén {}",
            productId, periodId, warehouseId);

        // Validar acceso al almacén
        try {
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        } catch (Exception e) {
            if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR"))) {
                log.info("Usuario es ADMINISTRADOR o AUXILIAR, permitiendo acceso");
            } else {
                throw e;
            }
        }

        // Obtener información del producto
        ProductEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Obtener información del almacén
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        // Obtener existencias
        int existencias = 0;
        try {
            var stockOpt = inventoryStockRepository
                .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId);
            if (stockOpt.isPresent()) {
                existencias = stockOpt.get().getExistQty() != null ?
                    stockOpt.get().getExistQty().intValue() : 0;
            }
        } catch (Exception e) {
            log.warn("No se pudieron obtener existencias: {}", e.getMessage());
        }

        // Obtener todos los marbetes de este producto
        List<Label> labels = persistence.findByProductPeriodWarehouse(productId, periodId, warehouseId);
        log.info("Encontrados {} marbetes para el producto", labels.size());

        // Convertir a DTOs
        final Integer existenciasFinal = existencias;
        return labels.stream()
            .map(label -> tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelDetailDTO.builder()
                .folio(label.getFolio())
                .productId(label.getProductId())
                .claveProducto(product.getCveArt())
                .nombreProducto(product.getDescr())
                .warehouseId(label.getWarehouseId())
                .claveAlmacen(warehouse.getWarehouseKey())
                .nombreAlmacen(warehouse.getNameWarehouse())
                .periodId(label.getPeriodId())
                .estado(label.getEstado().name())
                .createdAt(label.getCreatedAt() != null ? label.getCreatedAt().toString() : null)
                .impresoAt(label.getImpresoAt() != null ? label.getImpresoAt().toString() : null)
                .existencias(existenciasFinal)
                .build())
            .sorted(java.util.Comparator.comparing(tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelDetailDTO::getFolio))
            .toList();
    }

    @Override
    @Transactional
    public void cancelLabel(CancelLabelRequestDTO dto, Long userId, String userRole) {
        log.info("Cancelando marbete folio {} por usuario {} con rol {}", dto.getFolio(), userId, userRole);

        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        // Buscar el marbete
        Label label = jpaLabelRepository.findById(dto.getFolio())
            .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + dto.getFolio() + " no encontrado"));

        // Validar que pertenece al periodo y almacén especificado
        if (!label.getPeriodId().equals(dto.getPeriodId()) || !label.getWarehouseId().equals(dto.getWarehouseId())) {
            throw new InvalidLabelStateException("El marbete no pertenece al periodo/almacén especificado");
        }

        // Validar que no esté ya cancelado
        if (label.getEstado() == Label.State.CANCELADO) {
            throw new LabelAlreadyCancelledException(dto.getFolio());
        }

        // REGLA DE NEGOCIO: No se pueden cancelar marbetes sin folios asignados
        // Obtener el LabelRequest para verificar la cantidad de folios
        LabelRequest labelRequest = labelRequestRepository.findById(label.getLabelRequestId())
            .orElseThrow(() -> new RuntimeException("LabelRequest no encontrado para el marbete"));

        if (labelRequest.getRequestedLabels() == null || labelRequest.getRequestedLabels() == 0) {
            throw new InvalidLabelStateException(
                "No se puede cancelar un marbete sin folios asignados. " +
                "Este marbete tiene 0 folios solicitados y no debe ser cancelado."
            );
        }

        log.debug("Marbete {} tiene {} folios asignados - validación aprobada",
            dto.getFolio(), labelRequest.getRequestedLabels());


        // Cambiar estado a CANCELADO
        label.setEstado(Label.State.CANCELADO);
        jpaLabelRepository.save(label);

        // Registrar en labels_cancelled
        LabelCancelled cancelled = new LabelCancelled();
        cancelled.setFolio(dto.getFolio());
        cancelled.setLabelRequestId(label.getLabelRequestId());
        cancelled.setPeriodId(label.getPeriodId());
        cancelled.setWarehouseId(label.getWarehouseId());
        cancelled.setProductId(label.getProductId());
        cancelled.setMotivoCancelacion(dto.getMotivoCancelacion() != null ? dto.getMotivoCancelacion() : "Cancelado manualmente");
        cancelled.setCanceladoAt(LocalDateTime.now());
        cancelled.setCanceladoBy(userId);
        cancelled.setReactivado(false);

        // Obtener existencias actuales
        try {
            var stockOpt = inventoryStockRepository
                .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    label.getProductId(), label.getWarehouseId(), label.getPeriodId());
            if (stockOpt.isPresent()) {
                int existenciasActuales = stockOpt.get().getExistQty() != null ?
                    stockOpt.get().getExistQty().intValue() : 0;
                cancelled.setExistenciasAlCancelar(existenciasActuales);
                cancelled.setExistenciasActuales(existenciasActuales);
            }
        } catch (Exception e) {
            log.warn("No se pudieron obtener existencias para el marbete cancelado: {}", e.getMessage());
            cancelled.setExistenciasAlCancelar(0);
            cancelled.setExistenciasActuales(0);
        }

        jpaLabelCancelledRepository.save(cancelled);

        log.info("Marbete {} cancelado exitosamente", dto.getFolio());
    }

    @Override
    @Transactional(readOnly = true)
    public tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelForCountDTO getLabelForCount(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        log.info("Obteniendo información del marbete {} para conteo", folio);

        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);

        // Buscar el marbete por folio (sin importar periodo/almacen)
        Label label = jpaLabelRepository.findById(folio)
            .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

        // Obtener información del producto
        ProductEntity product = productRepository.findById(label.getProductId())
            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Obtener información del almacén
        WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId())
            .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        // Obtener los conteos registrados
        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);

        java.math.BigDecimal conteo1 = null;
        java.math.BigDecimal conteo2 = null;

        for (LabelCountEvent event : events) {
            if (event.getCountNumber() == 1) {
                conteo1 = event.getCountedValue();
            }
            if (event.getCountNumber() == 2) {
                conteo2 = event.getCountedValue();
            }
        }

        // Calcular diferencia si ambos conteos existen
        java.math.BigDecimal diferencia = null;
        if (conteo1 != null && conteo2 != null) {
            diferencia = conteo2.subtract(conteo1);
        }

        // Verificar si el marbete está impreso
        List<LabelPrint> prints = persistence.findLabelPrintsByProductPeriodWarehouse(
            label.getProductId(), periodId, warehouseId);
        boolean impreso = !prints.isEmpty();

        // Verificar si está cancelado
        boolean cancelado = label.getEstado() == Label.State.CANCELADO;

        // Construir mensaje informativo
        String mensaje;
        if (cancelado) {
            mensaje = "Este marbete está CANCELADO y no puede ser usado para conteo";
        } else if (conteo1 != null && conteo2 != null) {
            mensaje = "Ambos conteos ya están registrados";
        } else if (conteo1 != null) {
            mensaje = "Primer conteo registrado, falta el segundo conteo";
        } else {
            mensaje = "Listo para registrar el primer conteo";
        }

        return tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelForCountDTO.builder()
            .folio(label.getFolio())
            .periodId(label.getPeriodId())
            .warehouseId(label.getWarehouseId())
            .claveAlmacen(warehouse.getWarehouseKey())
            .nombreAlmacen(warehouse.getNameWarehouse())
            .claveProducto(product.getCveArt())
            .descripcionProducto(product.getDescr())
            .unidadMedida(product.getUniMed())
            .cancelado(cancelado)
            .conteo1(conteo1)
            .conteo2(conteo2)
            .diferencia(diferencia)
            .estado(label.getEstado() != null ? label.getEstado().name() : "SIN_ESTADO")
            .impreso(impreso)
            .mensaje(mensaje)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelForCountDTO> getLabelsForCountList(Long periodId, Long warehouseId, Long userId, String userRole) {
        log.info("Listando marbetes disponibles para conteo en periodo {} y almacén {}", periodId, warehouseId);

        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);

        // Obtener información del almacén
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
            .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        // Obtener todos los marbetes IMPRESOS (no cancelados) del periodo y almacén
        List<Label> labels = jpaLabelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId)
            .stream()
            .filter(l -> l.getEstado() == Label.State.IMPRESO)
            .sorted(java.util.Comparator.comparing(Label::getFolio))
            .toList();

        log.info("Encontrados {} marbetes impresos disponibles para conteo", labels.size());

        if (labels.isEmpty()) {
            log.warn("No se encontraron marbetes impresos para el periodo {} y almacén {}", periodId, warehouseId);
            return new ArrayList<>();
        }

        // Convertir cada marbete a DTO
        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelForCountDTO> result = new ArrayList<>();

        for (Label label : labels) {
            try {
                // Obtener información del producto
                ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
                if (product == null) {
                    log.warn("Producto no encontrado para marbete folio {}", label.getFolio());
                    continue;
                }

                // Obtener los conteos registrados
                List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

                java.math.BigDecimal conteo1 = null;
                java.math.BigDecimal conteo2 = null;

                for (LabelCountEvent event : events) {
                    if (event.getCountNumber() == 1) {
                        conteo1 = event.getCountedValue();
                    }
                    if (event.getCountNumber() == 2) {
                        conteo2 = event.getCountedValue();
                    }
                }

                // Calcular diferencia si ambos conteos existen
                java.math.BigDecimal diferencia = null;
                if (conteo1 != null && conteo2 != null) {
                    diferencia = conteo2.subtract(conteo1);
                }

                // Construir mensaje informativo
                String mensaje;
                if (conteo1 != null && conteo2 != null) {
                    mensaje = "Completo";
                } else if (conteo1 != null) {
                    mensaje = "Pendiente C2";
                } else {
                    mensaje = "Pendiente C1";
                }

                tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelForCountDTO dto =
                    tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelForCountDTO.builder()
                        .folio(label.getFolio())
                        .periodId(label.getPeriodId())
                        .warehouseId(label.getWarehouseId())
                        .claveAlmacen(warehouse.getWarehouseKey())
                        .nombreAlmacen(warehouse.getNameWarehouse())
                        .claveProducto(product.getCveArt())
                        .descripcionProducto(product.getDescr())
                        .unidadMedida(product.getUniMed())
                        .cancelado(false)
                        .conteo1(conteo1)
                        .conteo2(conteo2)
                        .diferencia(diferencia)
                        .estado(label.getEstado().name())
                        .impreso(true)
                        .mensaje(mensaje)
                        .build();

                result.add(dto);

            } catch (Exception e) {
                log.error("Error procesando marbete folio {}: {}", label.getFolio(), e.getMessage());
            }
        }

        log.info("Devolviendo {} marbetes para la interfaz de conteo", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributionReportDTO> getDistributionReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de distribución para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso si se especifica almacén
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Obtener marbetes impresos
        List<Label> labels = filter.getWarehouseId() != null ?
            jpaLabelRepository.findPrintedLabelsByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId()) :
            jpaLabelRepository.findPrintedLabelsByPeriod(filter.getPeriodId());

        // Agrupar por almacén y usuario que creó
        Map<String, List<Label>> groupedByWarehouse = labels.stream()
            .collect(Collectors.groupingBy(l -> l.getWarehouseId() + "_" + l.getCreatedBy()));

        List<DistributionReportDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Label>> entry : groupedByWarehouse.entrySet()) {
            List<Label> labelGroup = entry.getValue();
            if (labelGroup.isEmpty()) continue;

            Label first = labelGroup.get(0);

            // Obtener información del almacén
            WarehouseEntity warehouse = warehouseRepository.findById(first.getWarehouseId())
                .orElse(null);

            // Obtener información del usuario
            var user = userRepository.findById(first.getCreatedBy()).orElse(null);
            String userName = user != null ? user.getEmail() : "Usuario " + first.getCreatedBy();

            // Calcular primer y último folio
            Long minFolio = labelGroup.stream().map(Label::getFolio).min(Long::compareTo).orElse(0L);
            Long maxFolio = labelGroup.stream().map(Label::getFolio).max(Long::compareTo).orElse(0L);

            result.add(new DistributionReportDTO(
                userName,
                warehouse != null ? warehouse.getWarehouseKey() : String.valueOf(first.getWarehouseId()),
                warehouse != null ? warehouse.getNameWarehouse() : "Almacén " + first.getWarehouseId(),
                minFolio,
                maxFolio,
                labelGroup.size()
            ));
        }

        log.info("Reporte de distribución generado con {} registros", result.size());
        return result.stream().sorted(Comparator.comparing(DistributionReportDTO::getClaveAlmacen)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelListReportDTO> getLabelListReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de listado de marbetes para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Obtener marbetes
        List<Label> labels = filter.getWarehouseId() != null ?
            jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId()) :
            jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        // Obtener eventos de conteo para todos los folios
        List<Long> folios = labels.stream().map(Label::getFolio).toList();
        Map<Long, List<LabelCountEvent>> countEventsByFolio = new HashMap<>();

        for (Long folio : folios) {
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
            countEventsByFolio.put(folio, events);
        }

        List<LabelListReportDTO> result = labels.stream().map(label -> {
            // Obtener producto
            ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);

            // Obtener almacén
            WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);

            // Obtener conteos
            List<LabelCountEvent> events = countEventsByFolio.get(label.getFolio());
            java.math.BigDecimal conteo1 = null;
            java.math.BigDecimal conteo2 = null;

            if (events != null && !events.isEmpty()) {
                for (LabelCountEvent event : events) {
                    if (event.getCountNumber() == 1) conteo1 = event.getCountedValue();
                    if (event.getCountNumber() == 2) conteo2 = event.getCountedValue();
                }
            }

            return new LabelListReportDTO(
                label.getFolio(),
                product != null ? product.getCveArt() : "",
                product != null ? product.getDescr() : "",
                product != null ? product.getUniMed() : "",
                warehouse != null ? warehouse.getWarehouseKey() : "",
                warehouse != null ? warehouse.getNameWarehouse() : "",
                conteo1,
                conteo2,
                label.getEstado().name(),
                label.getEstado() == Label.State.CANCELADO
            );
        }).sorted(Comparator.comparing(LabelListReportDTO::getNumeroMarbete))
          .toList();

        log.info("Reporte de listado generado con {} registros", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingLabelsReportDTO> getPendingLabelsReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de marbetes pendientes para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Obtener marbetes no cancelados
        List<Label> labels = filter.getWarehouseId() != null ?
            jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId()) :
            jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        // Filtrar solo los que no están cancelados
        labels = labels.stream()
            .filter(l -> l.getEstado() != Label.State.CANCELADO)
            .toList();

        List<PendingLabelsReportDTO> result = new ArrayList<>();

        for (Label label : labels) {
            // Obtener conteos
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

            java.math.BigDecimal conteo1 = null;
            java.math.BigDecimal conteo2 = null;

            for (LabelCountEvent event : events) {
                if (event.getCountNumber() == 1) conteo1 = event.getCountedValue();
                if (event.getCountNumber() == 2) conteo2 = event.getCountedValue();
            }

            // Si falta algún conteo, es pendiente
            if (conteo1 == null || conteo2 == null) {
                ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
                WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);

                result.add(new PendingLabelsReportDTO(
                    label.getFolio(),
                    product != null ? product.getCveArt() : "",
                    product != null ? product.getDescr() : "",
                    product != null ? product.getUniMed() : "",
                    warehouse != null ? warehouse.getWarehouseKey() : "",
                    warehouse != null ? warehouse.getNameWarehouse() : "",
                    conteo1,
                    conteo2,
                    label.getEstado().name()
                ));
            }
        }

        log.info("Reporte de marbetes pendientes generado con {} registros", result.size());
        return result.stream().sorted(Comparator.comparing(PendingLabelsReportDTO::getNumeroMarbete)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DifferencesReportDTO> getDifferencesReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de marbetes con diferencias para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Obtener marbetes no cancelados
        List<Label> labels = filter.getWarehouseId() != null ?
            jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId()) :
            jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        labels = labels.stream()
            .filter(l -> l.getEstado() != Label.State.CANCELADO)
            .toList();

        List<DifferencesReportDTO> result = new ArrayList<>();

        for (Label label : labels) {
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

            java.math.BigDecimal conteo1 = null;
            java.math.BigDecimal conteo2 = null;

            for (LabelCountEvent event : events) {
                if (event.getCountNumber() == 1) conteo1 = event.getCountedValue();
                if (event.getCountNumber() == 2) conteo2 = event.getCountedValue();
            }

            // Si ambos conteos existen, son mayores a cero y son diferentes
            if (conteo1 != null && conteo2 != null
                && conteo1.compareTo(java.math.BigDecimal.ZERO) > 0
                && conteo2.compareTo(java.math.BigDecimal.ZERO) > 0
                && conteo1.compareTo(conteo2) != 0) {
                ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
                WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);

                java.math.BigDecimal diferencia = conteo1.subtract(conteo2).abs();

                result.add(new DifferencesReportDTO(
                    label.getFolio(),
                    product != null ? product.getCveArt() : "",
                    product != null ? product.getDescr() : "",
                    product != null ? product.getUniMed() : "",
                    warehouse != null ? warehouse.getWarehouseKey() : "",
                    warehouse != null ? warehouse.getNameWarehouse() : "",
                    conteo1,
                    conteo2,
                    diferencia,
                    label.getEstado().name()
                ));
            }
        }

        log.info("Reporte de marbetes con diferencias generado con {} registros", result.size());
        return result.stream().sorted(Comparator.comparing(DifferencesReportDTO::getNumeroMarbete)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CancelledLabelsReportDTO> getCancelledLabelsReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de marbetes cancelados para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Obtener marbetes cancelados
        List<LabelCancelled> cancelledLabels = filter.getWarehouseId() != null ?
            jpaLabelCancelledRepository.findByPeriodIdAndWarehouseIdAndReactivado(filter.getPeriodId(), filter.getWarehouseId(), false) :
            jpaLabelCancelledRepository.findByPeriodIdAndReactivado(filter.getPeriodId(), false);

        List<CancelledLabelsReportDTO> result = cancelledLabels.stream().map(cancelled -> {
            // Obtener producto
            ProductEntity product = productRepository.findById(cancelled.getProductId()).orElse(null);

            // Obtener almacén
            WarehouseEntity warehouse = warehouseRepository.findById(cancelled.getWarehouseId()).orElse(null);

            // Obtener usuario que canceló
            var user = userRepository.findById(cancelled.getCanceladoBy()).orElse(null);
            String userName = user != null ? user.getEmail() : "Usuario " + cancelled.getCanceladoBy();

            // Obtener conteos si existen
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(cancelled.getFolio());
            java.math.BigDecimal conteo1 = null;
            java.math.BigDecimal conteo2 = null;

            for (LabelCountEvent event : events) {
                if (event.getCountNumber() == 1) conteo1 = event.getCountedValue();
                if (event.getCountNumber() == 2) conteo2 = event.getCountedValue();
            }

            return new CancelledLabelsReportDTO(
                cancelled.getFolio(),
                product != null ? product.getCveArt() : "",
                product != null ? product.getDescr() : "",
                product != null ? product.getUniMed() : "",
                warehouse != null ? warehouse.getWarehouseKey() : "",
                warehouse != null ? warehouse.getNameWarehouse() : "",
                conteo1,
                conteo2,
                cancelled.getMotivoCancelacion(),
                cancelled.getCanceladoAt(),
                userName
            );
        }).sorted(Comparator.comparing(CancelledLabelsReportDTO::getNumeroMarbete))
          .toList();
        log.info("Reporte de marbetes cancelados generado con {} registros", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComparativeReportDTO> getComparativeReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte comparativo para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Obtener marbetes no cancelados
        List<Label> labels = filter.getWarehouseId() != null ?
            jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId()) :
            jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        labels = labels.stream()
            .filter(l -> l.getEstado() != Label.State.CANCELADO)
            .toList();

        // Agrupar por producto y almacén
        Map<String, List<Label>> groupedByProductWarehouse = labels.stream()
            .collect(Collectors.groupingBy(l -> l.getProductId() + "_" + l.getWarehouseId()));

        List<ComparativeReportDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Label>> entry : groupedByProductWarehouse.entrySet()) {
            List<Label> labelGroup = entry.getValue();
            if (labelGroup.isEmpty()) continue;

            Label first = labelGroup.get(0);

            // Calcular existencias físicas (suma de conteo2, o conteo1 si no hay conteo2)
            java.math.BigDecimal existenciasFisicas = java.math.BigDecimal.ZERO;

            for (Label label : labelGroup) {
                List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

                java.math.BigDecimal conteo1 = null;
                java.math.BigDecimal conteo2 = null;

                for (LabelCountEvent event : events) {
                    if (event.getCountNumber() == 1) conteo1 = event.getCountedValue();
                    if (event.getCountNumber() == 2) conteo2 = event.getCountedValue();
                }

                // Preferir conteo2, si no existe usar conteo1
                if (conteo2 != null) {
                    existenciasFisicas = existenciasFisicas.add(conteo2);
                } else if (conteo1 != null) {
                    existenciasFisicas = existenciasFisicas.add(conteo1);
                }
            }

            // Obtener existencias teóricas de inventory_stock
            java.math.BigDecimal existenciasTeoricas = java.math.BigDecimal.ZERO;
            try {
                var stockOpt = inventoryStockRepository
                    .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                        first.getProductId(), first.getWarehouseId(), first.getPeriodId());
                if (stockOpt.isPresent() && stockOpt.get().getExistQty() != null) {
                    existenciasTeoricas = stockOpt.get().getExistQty();
                }
            } catch (Exception e) {
                log.warn("No se pudieron obtener existencias teóricas: {}", e.getMessage());
            }

            // Calcular diferencia
            java.math.BigDecimal diferencia = existenciasFisicas.subtract(existenciasTeoricas);

            // Calcular porcentaje de diferencia
            java.math.BigDecimal porcentaje = java.math.BigDecimal.ZERO;
            if (existenciasTeoricas.compareTo(java.math.BigDecimal.ZERO) != 0) {
                porcentaje = diferencia.divide(existenciasTeoricas, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new java.math.BigDecimal("100"));
            }

            // Obtener información del producto y almacén
            ProductEntity product = productRepository.findById(first.getProductId()).orElse(null);
            WarehouseEntity warehouse = warehouseRepository.findById(first.getWarehouseId()).orElse(null);

            result.add(new ComparativeReportDTO(
                warehouse != null ? warehouse.getWarehouseKey() : "",
                warehouse != null ? warehouse.getNameWarehouse() : "",
                product != null ? product.getCveArt() : "",
                product != null ? product.getDescr() : "",
                product != null ? product.getUniMed() : "",
                existenciasFisicas,
                existenciasTeoricas,
                diferencia,
                porcentaje
            ));
        }

        log.info("Reporte comparativo generado con {} registros", result.size());
        return result.stream()
            .sorted(Comparator.comparing(ComparativeReportDTO::getClaveAlmacen)
                .thenComparing(ComparativeReportDTO::getClaveProducto))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseDetailReportDTO> getWarehouseDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de almacén con detalle para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Obtener marbetes
        List<Label> labels = filter.getWarehouseId() != null ?
            jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId()) :
            jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        List<WarehouseDetailReportDTO> result = labels.stream().map(label -> {
            // Obtener producto
            ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);

            // Obtener almacén
            WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);

            // Obtener conteos
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

            java.math.BigDecimal cantidad = java.math.BigDecimal.ZERO;

            // Usar conteo2 si existe, sino conteo1
            for (LabelCountEvent event : events) {
                if (event.getCountNumber() == 2) {
                    cantidad = event.getCountedValue();
                    break;
                } else if (event.getCountNumber() == 1) {
                    cantidad = event.getCountedValue();
                }
            }

            return new WarehouseDetailReportDTO(
                warehouse != null ? warehouse.getWarehouseKey() : "",
                warehouse != null ? warehouse.getNameWarehouse() : "",
                product != null ? product.getCveArt() : "",
                product != null ? product.getDescr() : "",
                product != null ? product.getUniMed() : "",
                label.getFolio(),
                cantidad,
                label.getEstado().name(),
                label.getEstado() == Label.State.CANCELADO
            );
        }).sorted(Comparator.comparing(WarehouseDetailReportDTO::getClaveAlmacen)
            .thenComparing(WarehouseDetailReportDTO::getClaveProducto)
            .thenComparing(WarehouseDetailReportDTO::getNumeroMarbete))
          .toList();

        log.info("Reporte de almacén con detalle generado con {} registros", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDetailReportDTO> getProductDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de producto con detalle para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso si se especifica almacén
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Obtener marbetes no cancelados
        List<Label> labels = filter.getWarehouseId() != null ?
            jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId()) :
            jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        labels = labels.stream()
            .filter(l -> l.getEstado() != Label.State.CANCELADO)
            .toList();

        // Calcular totales por producto
        Map<Long, java.math.BigDecimal> totalsByProduct = new HashMap<>();

        for (Label label : labels) {
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

            java.math.BigDecimal cantidad = java.math.BigDecimal.ZERO;

            for (LabelCountEvent event : events) {
                if (event.getCountNumber() == 2) {
                    cantidad = event.getCountedValue();
                    break;
                } else if (event.getCountNumber() == 1) {
                    cantidad = event.getCountedValue();
                }
            }

            totalsByProduct.merge(label.getProductId(), cantidad, java.math.BigDecimal::add);
        }

        List<ProductDetailReportDTO> result = labels.stream().map(label -> {
            // Obtener producto
            ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);

            // Obtener almacén
            WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);

            // Obtener conteos
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

            java.math.BigDecimal existencias = java.math.BigDecimal.ZERO;

            for (LabelCountEvent event : events) {
                if (event.getCountNumber() == 2) {
                    existencias = event.getCountedValue();
                    break;
                } else if (event.getCountNumber() == 1) {
                    existencias = event.getCountedValue();
                }
            }

            java.math.BigDecimal total = totalsByProduct.get(label.getProductId());

            return new ProductDetailReportDTO(
                product != null ? product.getCveArt() : "",
                product != null ? product.getDescr() : "",
                product != null ? product.getUniMed() : "",
                warehouse != null ? warehouse.getWarehouseKey() : "",
                warehouse != null ? warehouse.getNameWarehouse() : "",
                label.getFolio(),
                existencias,
                total
            );
        }).sorted(Comparator.comparing(ProductDetailReportDTO::getClaveProducto)
            .thenComparing(ProductDetailReportDTO::getClaveAlmacen)
            .thenComparing(ProductDetailReportDTO::getNumeroMarbete))
          .toList();
        log.info("Reporte de producto con detalle generado con {} registros", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateFileResponseDTO generateInventoryFile(Long periodId, Long userId, String userRole) {
        log.info("Generando archivo TXT de existencias para periodo {}", periodId);

        // Obtener información del periodo
        var periodEntity = jpaPeriodRepository.findById(periodId)
            .orElseThrow(() -> new RuntimeException("Periodo no encontrado"));

        // Formatear el nombre del periodo (ejemplo: "Diciembre2016")
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.of("es", "ES"));
        String periodName = periodEntity.getDate().format(formatter);
        periodName = periodName.substring(0, 1).toUpperCase() + periodName.substring(1).replace(" ", "");

        // Obtener todos los marbetes no cancelados del periodo
        List<Label> labels = jpaLabelRepository.findByPeriodId(periodId).stream()
            .filter(l -> l.getEstado() != Label.State.CANCELADO)
            .toList();

        // Agrupar por producto y sumar existencias físicas
        Map<Long, ProductExistencias> productoExistencias = new HashMap<>();

        for (Label label : labels) {
            // Obtener conteos
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

            java.math.BigDecimal cantidad = java.math.BigDecimal.ZERO;

            // Usar conteo2 si existe, sino conteo1
            for (LabelCountEvent event : events) {
                if (event.getCountNumber() == 2) {
                    cantidad = event.getCountedValue();
                    break;
                } else if (event.getCountNumber() == 1) {
                    cantidad = event.getCountedValue();
                }
            }

            // Acumular existencias por producto
            productoExistencias.computeIfAbsent(label.getProductId(), k -> {
                ProductEntity product = productRepository.findById(k).orElse(null);
                return new ProductExistencias(
                    product != null ? product.getCveArt() : "",
                    product != null ? product.getDescr() : "",
                    java.math.BigDecimal.ZERO
                );
            }).sumarExistencias(cantidad);
        }

        // Ordenar alfabéticamente por clave de producto
        List<ProductExistencias> productosList = new ArrayList<>(productoExistencias.values());
        productosList.sort(Comparator.comparing(ProductExistencias::getClaveProducto));

        // Crear directorio si no existe
        String directoryPath = "C:\\Sistemas\\SIGMA\\Documentos";
        java.io.File directory = new java.io.File(directoryPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                log.info("Directorio creado: {}", directoryPath);
            } else {
                log.warn("No se pudo crear el directorio: {}", directoryPath);
            }
        }

        // Generar nombre del archivo
        String fileName = "Existencias_" + periodName + ".txt";
        String filePath = directoryPath + "\\" + fileName;

        // Escribir archivo TXT
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(filePath),
                    java.nio.charset.StandardCharsets.UTF_8))) {

            // Escribir encabezado
            writer.write("CLAVE_PRODUCTO\tDESCRIPCION\tEXISTENCIAS");
            writer.newLine();
            writer.write("========================================");
            writer.newLine();

            // Escribir datos de productos
            for (ProductExistencias producto : productosList) {
                String line = String.format("%s\t%s\t%s",
                    producto.getClaveProducto(),
                    producto.getDescripcion(),
                    producto.getExistencias().stripTrailingZeros().toPlainString());
                writer.write(line);
                writer.newLine();
            }

            log.info("Archivo generado exitosamente: {}", filePath);

            return new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateFileResponseDTO(
                fileName,
                filePath,
                productosList.size(),
                "Archivo generado exitosamente"
            );

        } catch (java.io.IOException e) {
            log.error("Error al generar archivo TXT: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar archivo: " + e.getMessage());
        }
    }

    // Clase auxiliar para agrupar existencias por producto
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ProductExistencias {
        private String claveProducto;
        private String descripcion;
        private java.math.BigDecimal existencias;

        public void sumarExistencias(java.math.BigDecimal cantidad) {
            this.existencias = this.existencias.add(cantidad);
        }
    }

    /**
     * Obtiene el nombre descriptivo de un periodo
     * @param periodId ID del periodo
     * @return Nombre del periodo formateado (ej: "Enero 2026")
     */
    private String getPeriodName(Long periodId) {
        try {
            return jpaPeriodRepository.findById(periodId)
                .map(period -> {
                    java.time.LocalDate date = period.getDate();
                    if (date != null) {
                        java.time.format.DateTimeFormatter formatter =
                            java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.of("es", "ES"));
                        String formatted = date.format(formatter);
                        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
                    }
                    return "Periodo " + periodId;
                })
                .orElse("Periodo " + periodId);
        } catch (Exception e) {
            log.warn("Error obteniendo nombre del periodo {}: {}", periodId, e.getMessage());
            return "Periodo " + periodId;
        }
    }

    /**
     * Obtiene el nombre descriptivo de un almacén
     * @param warehouseId ID del almacén
     * @return Nombre del almacén (ej: "Bodega Norte")
     */
    private String getWarehouseName(Long warehouseId) {
        try {
            return warehouseRepository.findById(warehouseId)
                .map(warehouse -> {
                    String name = warehouse.getNameWarehouse();
                    String key = warehouse.getWarehouseKey();
                    if (name != null && !name.trim().isEmpty()) {
                        return name + (key != null ? " (" + key + ")" : "");
                    }
                    return key != null ? key : "Almacén " + warehouseId;
                })
                .orElse("Almacén " + warehouseId);
        } catch (Exception e) {
            log.warn("Error obteniendo nombre del almacén {}: {}", warehouseId, e.getMessage());
            return "Almacén " + warehouseId;
        }
    }

    /**
     * Obtiene el nombre descriptivo de un producto
     * @param productId ID del producto
     * @return Nombre del producto (ej: "Tornillo M8")
     */
    private String getProductName(Long productId) {
        try {
            return productRepository.findById(productId)
                .map(product -> {
                    String descr = product.getDescr();
                    String cveArt = product.getCveArt();
                    if (descr != null && !descr.trim().isEmpty()) {
                        return descr + (cveArt != null ? " (" + cveArt + ")" : "");
                    }
                    return cveArt != null ? cveArt : "Producto " + productId;
                })
                .orElse("Producto " + productId);
        } catch (Exception e) {
            log.warn("Error obteniendo nombre del producto {}: {}", productId, e.getMessage());
            return "Producto " + productId;
        }
    }
}
