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
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelGenerationBatch;
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
    private final tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelPrintRepository jpaLabelPrintRepository;

    @Override
    @Transactional
    public void requestLabels(LabelRequestDTO dto, Long userId, String userRole) {
        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        // REGLA DE NEGOCIO: Validar que la cantidad sea numérica entera (ya validado por DTO)
        // El DTO debe tener validación @Min(0) o similar

        // Buscar si ya existe una solicitud para este producto/almacén/periodo
        Optional<LabelRequest> existingRequest = persistence.findByProductWarehousePeriod(
            dto.getProductId(),
            dto.getWarehouseId(),
            dto.getPeriodId()
        );

        // REGLA DE NEGOCIO: Si la cantidad es 0, significa que ya no desea generar folios
        if (dto.getRequestedLabels() == 0) {
            if (existingRequest.isPresent()) {
                LabelRequest req = existingRequest.get();

                // Solo permitir eliminar/cancelar si NO se han generado folios aún
                if (req.getFoliosGenerados() == 0) {
                    persistence.delete(req);
                    log.info("Solicitud cancelada (cantidad=0) para producto {} en almacén {} periodo {}",
                        dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
                } else {
                    throw new InvalidLabelStateException(
                        "No se puede cancelar la solicitud porque ya se generaron " +
                        req.getFoliosGenerados() + " folios. Debe imprimirlos primero.");
                }
            }
            // Si no existe solicitud y la cantidad es 0, no hacer nada
            return;
        }

        // REGLA DE NEGOCIO: No permitir solicitar si existen marbetes GENERADOS sin imprimir
        // Esta validación solo aplica para solicitudes NUEVAS o al INCREMENTAR la cantidad
        if (existingRequest.isPresent()) {
            LabelRequest existing = existingRequest.get();

            // Si ya se generaron folios, verificar que no haya sin imprimir
            if (existing.getFoliosGenerados() > 0) {
                boolean hasUnprinted = persistence.existsGeneratedUnprintedForProductWarehousePeriod(
                    dto.getProductId(),
                    dto.getWarehouseId(),
                    dto.getPeriodId()
                );
                if (hasUnprinted) {
                    throw new InvalidLabelStateException(
                        "Existen marbetes GENERADOS sin imprimir para este producto/almacén/periodo. " +
                        "Por favor imprima los marbetes existentes antes de solicitar más.");
                }
            }

            // REGLA DE NEGOCIO: Mientras no haya ejecutado "Generar marbetes",
            // podrá cambiar la cantidad las veces que desee
            log.info("Actualizando solicitud existente de {} a {} folios para producto {} en almacén {} periodo {}",
                existing.getRequestedLabels(), dto.getRequestedLabels(),
                dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

            existing.setRequestedLabels(dto.getRequestedLabels());
            persistence.save(existing);

        } else {
            // CREAR nueva solicitud
            log.info("Creando nueva solicitud de {} folios para producto {} en almacén {} periodo {}",
                dto.getRequestedLabels(), dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

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

        // REGLA DE NEGOCIO CUMPLIDA: Los datos se guardan automáticamente en BD,
        // puede cambiar de módulo sin temor a perder el dato
    }

    @Override
    @Transactional
    public GenerateBatchResponseDTO generateBatch(GenerateBatchDTO dto, Long userId, String userRole) {
        log.info("=== INICIO generateBatch ===");
        log.info("DTO recibido: productId={}, warehouseId={}, periodId={}, labelsToGenerate={}",
            dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId(), dto.getLabelsToGenerate());
        log.info("Usuario: userId={}, userRole={}", userId, userRole);

        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
        log.info("Acceso al almacén validado correctamente");

        // Buscar solicitud existente
        Optional<LabelRequest> opt = persistence.findByProductWarehousePeriod(dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
        if (opt.isEmpty()) {
            log.error("No se encontró solicitud para producto={}, warehouse={}, period={}",
                dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
            throw new LabelNotFoundException("No existe una solicitud para el producto/almacén/periodo.");
        }
        LabelRequest req = opt.get();
        log.info("Solicitud encontrada: id={}, requestedLabels={}, foliosGenerados={}",
            req.getIdLabelRequest(), req.getRequestedLabels(), req.getFoliosGenerados());

        int remaining = req.getRequestedLabels() - req.getFoliosGenerados();
        if (remaining <= 0) {
            log.error("No hay folios restantes para generar. Solicitados={}, Generados={}",
                req.getRequestedLabels(), req.getFoliosGenerados());
            throw new InvalidLabelStateException("No hay folios solicitados para generar.");
        }
        int toGenerate = Math.min(remaining, dto.getLabelsToGenerate());
        log.info("Se generarán {} marbetes (restantes={}, solicitados en lote={})",
            toGenerate, remaining, dto.getLabelsToGenerate());

        // NUEVA REGLA DE NEGOCIO: Verificar existencias del producto
        log.info("Verificando existencias del producto {} en almacén {} periodo {}",
            dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

        Integer existencias = 0;
        try {
            var stockOpt = inventoryStockRepository
                .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

            if (stockOpt.isPresent()) {
                existencias = stockOpt.get().getExistQty() != null ?
                    stockOpt.get().getExistQty().intValue() : 0;
            }
            log.info("Existencias encontradas: {}", existencias);
        } catch (Exception e) {
            log.warn("No se pudieron obtener existencias: {}", e.getMessage());
        }

        // Allocación de rango de folios (transaccional)
        long[] range = persistence.allocateFolioRange(dto.getPeriodId(), toGenerate);
        long primer = range[0];
        long ultimo = range[1];
        log.info("Rango de folios asignado: {} a {}", primer, ultimo);

        // Guardar marbetes individuales con validación de existencias
        log.info("Guardando {} marbetes en la base de datos...", toGenerate);

        int generadosConExistencias = 0;
        int generadosSinExistencias = 0;

        if (existencias > 0) {
            // Producto CON existencias - generar normalmente
            persistence.saveLabelsBatch(req.getIdLabelRequest(), dto.getPeriodId(),
                dto.getWarehouseId(), dto.getProductId(), primer, ultimo, userId);
            generadosConExistencias = toGenerate;
            log.info("Marbetes guardados exitosamente con estado GENERADO");
        } else {
            // Producto SIN existencias - crear como CANCELADO
            persistence.saveLabelsBatchAsCancelled(req.getIdLabelRequest(), dto.getPeriodId(),
                dto.getWarehouseId(), dto.getProductId(), primer, ultimo, userId, existencias);
            generadosSinExistencias = toGenerate;
            log.warn("Marbetes guardados con estado CANCELADO por falta de existencias");
        }

        // Registrar lote
        LabelGenerationBatch batch = new LabelGenerationBatch();
        batch.setLabelRequestId(req.getIdLabelRequest());
        batch.setPeriodId(dto.getPeriodId());
        batch.setWarehouseId(dto.getWarehouseId());
        batch.setPrimerFolio(primer);
        batch.setUltimoFolio(ultimo);
        batch.setTotalGenerados(toGenerate);
        batch.setGeneradoPor(userId);
        batch.setGeneradoAt(LocalDateTime.now());

        persistence.saveGenerationBatch(batch);
        log.info("Lote de generación registrado: batchId será asignado por BD");

        // Actualizar la solicitud
        int nuevosFoliosGenerados = req.getFoliosGenerados() + toGenerate;
        req.setFoliosGenerados(nuevosFoliosGenerados);
        persistence.save(req);
        log.info("Solicitud actualizada: foliosGenerados={}/{}", nuevosFoliosGenerados, req.getRequestedLabels());
        log.info("=== FIN generateBatch EXITOSO ===");

        // Construir respuesta con detalles
        String mensaje = String.format(
            "Generación completada: %d marbete(s) total. " +
            "%d con existencias (GENERADOS), %d sin existencias (CANCELADOS)",
            toGenerate, generadosConExistencias, generadosSinExistencias
        );

        return tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchResponseDTO.builder()
            .totalGenerados(toGenerate)
            .generadosConExistencias(generadosConExistencias)
            .generadosSinExistencias(generadosSinExistencias)
            .primerFolio(primer)
            .ultimoFolio(ultimo)
            .mensaje(mensaje)
            .build();
    }

    @Override
    @Transactional
    public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
        log.info("Iniciando impresión de marbetes: periodId={}, warehouseId={}, startFolio={}, endFolio={}, userId={}, userRole={}",
            dto.getPeriodId(), dto.getWarehouseId(), dto.getStartFolio(), dto.getEndFolio(), userId, userRole);

        // REGLA DE NEGOCIO: Esta operación delimita el contexto según usuario y almacén asignado,
        // PERO si el usuario tiene rol "ADMINISTRADOR" o "AUXILIAR", puede cambiar de almacén
        if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR"))) {
            log.info("Usuario {} tiene rol {} - puede imprimir en cualquier almacén", userId, userRole);
            // Los administradores y auxiliares pueden imprimir en cualquier almacén sin validación restrictiva
        } else {
            // Para otros roles, validar acceso estricto al almacén
            warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
        }

        // REGLA DE NEGOCIO: Se podrán imprimir marbetes siempre y cuando se hayan importado datos
        // de los catálogos de inventario y multialmacén
        boolean hasInventoryData = inventoryStockRepository.existsByWarehouseIdWarehouseAndPeriodId(
            dto.getWarehouseId(), dto.getPeriodId());

        if (!hasInventoryData) {
            throw new tokai.com.mx.SIGMAV2.modules.labels.application.exception.CatalogNotLoadedException(
                "No se pueden imprimir marbetes porque no se han cargado los catálogos de inventario " +
                "y multialmacén para el periodo y almacén seleccionados. " +
                "Por favor, importe los datos antes de continuar.");
        }

        // REGLA DE NEGOCIO: Validar que el rango de folios sea válido
        if (dto.getStartFolio() > dto.getEndFolio()) {
            throw new InvalidLabelStateException(
                "El folio inicial no puede ser mayor que el folio final.");
        }

        // REGLA DE NEGOCIO: Verificar que los folios solicitados existan y estén generados
        long foliosCount = dto.getEndFolio() - dto.getStartFolio() + 1;
        log.info("Intentando imprimir {} folio(s) desde {} hasta {}",
            foliosCount, dto.getStartFolio(), dto.getEndFolio());

        // Obtener los marbetes del rango especificado
        List<Label> labelsToProcess = persistence.findByFolioRange(
            dto.getPeriodId(),
            dto.getWarehouseId(),
            dto.getStartFolio(),
            dto.getEndFolio()
        );

        if (labelsToProcess.isEmpty()) {
            throw new InvalidLabelStateException(
                "No se encontraron marbetes en el rango especificado");
        }

        // Validar que no haya marbetes CANCELADOS
        long cancelledCount = labelsToProcess.stream()
            .filter(l -> l.getEstado() == Label.State.CANCELADO)
            .count();

        if (cancelledCount > 0) {
            throw new InvalidLabelStateException(
                String.format("No se pueden imprimir los marbetes: %d folios están CANCELADOS", cancelledCount));
        }

        // REGLA DE NEGOCIO: Soportar dos escenarios de impresión:
        // 1. Impresión normal: Impresión inmediata de marbetes recién generados (GENERADOS)
        // 2. Impresión extraordinaria: Reimpresión de marbetes previamente impresos (IMPRESOS)

        try {
            // Marcar como impresos y registrar en la tabla label_print
            LabelPrint result = persistence.printLabelsRange(
                dto.getPeriodId(),
                dto.getWarehouseId(),
                dto.getStartFolio(),
                dto.getEndFolio(),
                userId
            );

            log.info("Impresión registrada exitosamente: {} folio(s) del {} al {}",
                result.getCantidadImpresa(), result.getFolioInicial(), result.getFolioFinal());

            // Generar el PDF con JasperReports
            log.info("Generando PDF con {} marbetes...", labelsToProcess.size());
            byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labelsToProcess);

            log.info("PDF generado exitosamente: {} KB", pdfBytes.length / 1024);
            return pdfBytes;

        } catch (IllegalArgumentException e) {
            log.error("Error de validación en impresión: {}", e.getMessage());
            throw new InvalidLabelStateException(e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Error de estado en impresión: {}", e.getMessage());
            throw new InvalidLabelStateException(e.getMessage());
        }
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
            throw new LabelNotFoundException("El folio no existe");
        }
        Label label = optLabel.get();

        // Validar acceso al almacén del marbete
        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException("No se puede registrar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException("No se puede registrar conteo: el marbete no está IMPRESO.");
        }

        // No permitir registrar C1 si ya existe C1
        if (persistence.hasCountNumber(dto.getFolio(), 1)) {
            throw new DuplicateCountException("El conteo C1 ya fue registrado para este folio.");
        }
        // No permitir registrar C1 si ya existe C2 (secuencia rota)
        if (persistence.hasCountNumber(dto.getFolio(), 2)) {
            throw new CountSequenceException("No se puede registrar C1 porque ya existe un conteo C2 para este folio.");
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
            throw new LabelNotFoundException("El folio no existe");
        }
        Label label = optLabel.get();

        // Validar acceso al almacén del marbete
        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException("No se puede registrar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException("No se puede registrar conteo: el marbete no está IMPRESO.");
        }

        // Debe existir C1 antes de C2
        if (!persistence.hasCountNumber(dto.getFolio(), 1)) {
            throw new CountSequenceException("No se puede registrar C2 porque no existe un conteo C1 previo.");
        }

        // No permitir duplicar C2
        if (persistence.hasCountNumber(dto.getFolio(), 2)) {
            throw new DuplicateCountException("El conteo C2 ya fue registrado para este folio.");
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
        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

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
        log.info("Actualizando conteo C2 para folio {}", dto.getFolio());

        if (userRole == null) {
            throw new PermissionDeniedException("Role de usuario requerido para actualizar C2");
        }

        String roleUpper = userRole.toUpperCase();
        // Para C2, permitir actualización a ADMINISTRADOR y AUXILIAR_DE_CONTEO
        boolean allowed = roleUpper.equals("ADMINISTRADOR") || roleUpper.equals("AUXILIAR_DE_CONTEO");
        if (!allowed) {
            throw new PermissionDeniedException("No tiene permiso para actualizar C2. Solo ADMINISTRADOR o AUXILIAR_DE_CONTEO pueden actualizar el segundo conteo.");
        }

        // Verificar que el marbete exista
        Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException("El folio no existe");
        }
        Label label = optLabel.get();

        // Validar acceso al almacén del marbete
        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException("No se puede actualizar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException("No se puede actualizar conteo: el marbete no está IMPRESO.");
        }

        // Buscar el evento de conteo C2 existente
        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(dto.getFolio());
        LabelCountEvent eventC2 = events.stream()
            .filter(e -> e.getCountNumber() == 2)
            .findFirst()
            .orElseThrow(() -> new LabelNotFoundException("No existe un conteo C2 para actualizar"));

        // Actualizar el valor
        eventC2.setCountedValue(dto.getCountedValue());

        LabelCountEvent updated = jpaLabelCountEventRepository.save(eventC2);
        log.info("Conteo C2 actualizado exitosamente para folio {}", dto.getFolio());

        return updated;
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
                    .collect(Collectors.toList());

                Long primerFolio = null;
                Long ultimoFolio = null;
                List<Long> foliosList = new ArrayList<>();

                if (!productLabels.isEmpty()) {
                    primerFolio = productLabels.get(0).getFolio();
                    ultimoFolio = productLabels.get(productLabels.size() - 1).getFolio();
                    foliosList = productLabels.stream()
                        .map(Label::getFolio)
                        .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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

    @Override
    @Transactional
    public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
        // Validar acceso al almacén una sola vez
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
        for (ProductBatchDTO product : dto.getProducts()) {
            try {
                GenerateBatchDTO single = new GenerateBatchDTO();
                single.setProductId(product.getProductId());
                single.setWarehouseId(dto.getWarehouseId());
                single.setPeriodId(dto.getPeriodId());
                single.setLabelsToGenerate(product.getLabelsToGenerate());
                this.generateBatch(single, userId, userRole);
            } catch (Exception e) {
                log.error("Error generando marbetes para producto {}: {}", product.getProductId(), e.getMessage());
                // Aquí podrías recolectar errores individuales si quieres devolver un resumen
            }
        }
    }

    @Override
    public tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelStatusResponseDTO getLabelStatus(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        var builder = tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelStatusResponseDTO.builder();
        builder.folio(folio).periodId(periodId).warehouseId(warehouseId);
        String mensaje = "";
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
        Integer existencias = 0;
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
        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelDetailDTO> dtos = labels.stream()
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
            .collect(Collectors.toList());

        return dtos;
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
                int existencias = stockOpt.get().getExistQty() != null ?
                    stockOpt.get().getExistQty().intValue() : 0;
                cancelled.setExistenciasAlCancelar(existencias);
                cancelled.setExistenciasActuales(existencias);
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

        // Buscar el marbete por folio
        Label label = jpaLabelRepository.findById(folio)
            .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

        // Validar que pertenece al periodo y almacén especificado
        if (!label.getPeriodId().equals(periodId) || !label.getWarehouseId().equals(warehouseId)) {
            throw new InvalidLabelStateException("El marbete no pertenece al periodo/almacén especificado");
        }

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
        String mensaje = "";
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
            .collect(Collectors.toList());

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
                String mensaje = "";
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
        return result.stream().sorted(Comparator.comparing(DistributionReportDTO::getClaveAlmacen)).collect(Collectors.toList());
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
        List<Long> folios = labels.stream().map(Label::getFolio).collect(Collectors.toList());
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
          .collect(Collectors.toList());

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
            .collect(Collectors.toList());

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
        return result.stream().sorted(Comparator.comparing(PendingLabelsReportDTO::getNumeroMarbete)).collect(Collectors.toList());
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
            .collect(Collectors.toList());

        List<DifferencesReportDTO> result = new ArrayList<>();

        for (Label label : labels) {
            List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());

            java.math.BigDecimal conteo1 = null;
            java.math.BigDecimal conteo2 = null;

            for (LabelCountEvent event : events) {
                if (event.getCountNumber() == 1) conteo1 = event.getCountedValue();
                if (event.getCountNumber() == 2) conteo2 = event.getCountedValue();
            }

            // Si ambos conteos existen y son diferentes
            if (conteo1 != null && conteo2 != null && conteo1.compareTo(conteo2) != 0) {
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
        return result.stream().sorted(Comparator.comparing(DifferencesReportDTO::getNumeroMarbete)).collect(Collectors.toList());
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
          .collect(Collectors.toList());

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
            .collect(Collectors.toList());

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
            .collect(Collectors.toList());
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
          .collect(Collectors.toList());

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
            .collect(Collectors.toList());

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
          .collect(Collectors.toList());

        log.info("Reporte de producto con detalle generado con {} registros", result.size());
        return result;
    }
}

