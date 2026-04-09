package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.JasperLabelPrintService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.exception.LabelAlreadyCancelledException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.*;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orquestador principal del módulo de marbetes.
 * Delega en servicios especializados: LabelGenerationService, LabelCountService, LabelReportService.
 *
 * Responsabilidades restantes:
 *  - Impresión / reimpresión extraordinaria
 *  - Consulta de estado y resumen (summary)
 *  - Cancelación y reactivación
 *  - Consulta para conteo (getLabelForCount / getLabelsForCountList)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelServiceImpl implements LabelService {

    // ── servicios especializados ──────────────────────────────────────────
    private final LabelGenerationService labelGenerationService;
    private final LabelCountService labelCountService;
    private final LabelReportService labelReportService;

    // ── infraestructura necesaria para responsabilidades restantes ────────
    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;
    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;
    private final JpaLabelRequestRepository labelRequestRepository;
    private final JasperLabelPrintService jasperLabelPrintService;
    private final JpaUserRepository userRepository;
    private final JpaLabelRepository jpaLabelRepository;
    private final JpaLabelCancelledRepository jpaLabelCancelledRepository;
    private final JpaLabelCountEventRepository jpaLabelCountEventRepository;
    private final JpaPeriodRepository jpaPeriodRepository;

    @Value("${app.labels.inventory-file.directory:C:\\\\Sistemas\\\\SIGMA\\\\Documentos}")
    private String inventoryFileDirectory;

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS DEPRECATED — Mantenidos solo por compatibilidad de API
    // ═══════════════════════════════════════════════════════════════════════

    /** @deprecated Use generateBatchList(). */
    @Deprecated(forRemoval = true)
    @Override
    @Transactional
    public void requestLabels(LabelRequestDTO dto, Long userId, String userRole) {
        log.warn("⚠️ requestLabels() está deprecado. Use generateBatchList() en su lugar.");
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
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

    /** @deprecated Use generateBatchList(). */
    @Deprecated(forRemoval = true)
    @Override
    @Transactional
    public GenerateBatchResponseDTO generateBatch(GenerateBatchDTO dto, Long userId, String userRole) {
        log.warn("⚠️ generateBatch() está deprecado. Use generateBatchList() en su lugar.");
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
                dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
        LabelRequest labelRequest;
        if (existing.isPresent()) {
            labelRequest = existing.get();
        } else {
            labelRequest = new LabelRequest();
            labelRequest.setProductId(dto.getProductId());
            labelRequest.setWarehouseId(dto.getWarehouseId());
            labelRequest.setPeriodId(dto.getPeriodId());
            labelRequest.setRequestedLabels(dto.getLabelsToGenerate());
            labelRequest.setFoliosGenerados(0);
            labelRequest.setCreatedBy(userId);
            labelRequest.setCreatedAt(LocalDateTime.now());
            labelRequest = persistence.save(labelRequest);
        }

        int cantidad = dto.getLabelsToGenerate();
        long[] range = persistence.allocateFolioRange(dto.getPeriodId(), cantidad);
        LocalDateTime now = LocalDateTime.now();
        List<Label> labels = new ArrayList<>(cantidad);
        for (long folio = range[0]; folio <= range[1]; folio++) {
            Label label = new Label();
            label.setFolio(folio);
            label.setLabelRequestId(labelRequest.getIdLabelRequest());
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
                .totalGenerados(cantidad).generadosConExistencias(cantidad).generadosSinExistencias(0)
                .primerFolio(range[0]).ultimoFolio(range[1]).mensaje("Generados " + cantidad + " marbetes")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GENERACIÓN — delega en LabelGenerationService
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
        labelGenerationService.generateBatchList(dto, userId, userRole);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // IMPRESIÓN
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
        boolean isExtraordinary = dto.getForceReprint() != null && dto.getForceReprint();
        log.info("📄 Imprimiendo marbetes: periodo={}, almacén={}, tipo={}, folios={}",
                dto.getPeriodId(), dto.getWarehouseId(),
                isExtraordinary ? "EXTRAORDINARIA" : "NORMAL",
                dto.getFolios() != null ? dto.getFolios().size() : "TODOS");

        if (userRole == null || userRole.trim().isEmpty()) {
            throw new PermissionDeniedException("Rol de usuario requerido");
        }
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        List<Label> labels;
        if (isExtraordinary) {
            if (dto.getFolios() == null || dto.getFolios().isEmpty()) {
                throw new InvalidLabelStateException(
                        "Reimpresión extraordinaria requiere folios específicos.");
            }
            labels = persistence.findImpresosForReimpresion(dto.getPeriodId(), dto.getWarehouseId(), dto.getFolios());
        } else {
            if (dto.getFolios() != null && !dto.getFolios().isEmpty()) {
                labels = persistence.findByFoliosInAndPeriodAndWarehouse(
                        dto.getFolios(), dto.getPeriodId(), dto.getWarehouseId());
                if (labels.size() != dto.getFolios().size()) {
                    throw new LabelNotFoundException("Algunos folios no existen");
                }
            } else if (dto.getProductId() != null) {
                labels = persistence.findPendingLabelsByPeriodWarehouseAndProduct(
                        dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId());
            } else {
                labels = persistence.findPendingLabelsByPeriodAndWarehouse(
                        dto.getPeriodId(), dto.getWarehouseId());
            }
            if (labels.isEmpty()) {
                throw new InvalidLabelStateException("No hay marbetes pendientes de impresión");
            }
        }

        if (labels.size() > 500) {
            throw new InvalidLabelStateException("Límite máximo: 500 marbetes por impresión");
        }
        labels.sort(Comparator.comparing(Label::getFolio));

        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF");
        }

        Long minFolio = labels.get(0).getFolio();
        Long maxFolio = labels.get(labels.size() - 1).getFolio();
        persistence.printLabelsRange(dto.getPeriodId(), dto.getWarehouseId(), minFolio, maxFolio, userId, false);

        return pdfBytes;
    }

    @Override
    @Transactional
    public byte[] extraordinaryReprint(PrintRequestDTO dto, Long userId, String userRole) {
        log.info("🔄 REIMPRESIÓN EXTRAORDINARIA: periodo={}, almacén={}, folios={}",
                dto.getPeriodId(), dto.getWarehouseId(),
                dto.getFolios() != null ? dto.getFolios().size() : 0);

        if (userRole == null || userRole.trim().isEmpty()) {
            throw new PermissionDeniedException("Rol de usuario requerido");
        }
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        if (dto.getFolios() == null || dto.getFolios().isEmpty()) {
            throw new InvalidLabelStateException("Reimpresión extraordinaria requiere lista de folios específicos.");
        }

        List<Label> labels = persistence.findImpresosForReimpresion(
                dto.getPeriodId(), dto.getWarehouseId(), dto.getFolios());

        if (labels.size() > 500) {
            throw new InvalidLabelStateException("Límite máximo: 500 marbetes por reimpresión extraordinaria");
        }
        labels.sort(Comparator.comparing(Label::getFolio));

        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF de reimpresión: archivo vacío");
        }

        Long minFolio = labels.get(0).getFolio();
        Long maxFolio = labels.get(labels.size() - 1).getFolio();
        persistence.printLabelsRange(dto.getPeriodId(), dto.getWarehouseId(), minFolio, maxFolio, userId, true);

        log.info("✅ REIMPRESIÓN EXTRAORDINARIA completada: {} KB, {} marbetes", pdfBytes.length / 1024, labels.size());
        return pdfBytes;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTEO — delega en LabelCountService
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole) {
        return labelCountService.registerCountC1(dto, userId, userRole);
    }

    @Override
    @Transactional
    public LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole) {
        return labelCountService.registerCountC2(dto, userId, userRole);
    }

    @Override
    @Transactional
    public LabelCountEvent updateCountC1(UpdateCountDTO dto, Long userId, String userRole) {
        return labelCountService.updateCountC1(dto, userId, userRole);
    }

    @Override
    @Transactional
    public LabelCountEvent updateCountC2(UpdateCountDTO dto, Long userId, String userRole) {
        return labelCountService.updateCountC2(dto, userId, userRole);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTEO PENDIENTE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PendingPrintCountResponseDTO getPendingPrintCount(PendingPrintCountRequestDTO dto, Long userId, String userRole) {

        if (!warehouseAccessService.hasFullAccess(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
        }

        List<Label> pendingLabels = dto.getProductId() != null
                ? persistence.findPendingLabelsByPeriodWarehouseAndProduct(dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId())
                : persistence.findPendingLabelsByPeriodAndWarehouse(dto.getPeriodId(), dto.getWarehouseId());

        String warehouseName = null;
        String periodName = null;
        try {
            warehouseName = warehouseRepository.findById(dto.getWarehouseId())
                    .map(WarehouseEntity::getNameWarehouse).orElse(null);
        } catch (Exception e) { log.warn("No se pudo obtener nombre del almacén: {}", e.getMessage()); }
        try {
            var period = jpaPeriodRepository.findById(dto.getPeriodId());
            if (period.isPresent() && period.get().getDate() != null) {
                periodName = period.get().getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        } catch (Exception e) { log.warn("No se pudo obtener nombre del periodo: {}", e.getMessage()); }

        return new PendingPrintCountResponseDTO((long) pendingLabels.size(), dto.getPeriodId(), dto.getWarehouseId(), warehouseName, periodName);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RESUMEN (SUMMARY) con paginación — se mantiene aquí por su complejidad
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<LabelSummaryResponseDTO> getLabelSummary(LabelSummaryRequestDTO dto, Long userId, String userRole) {

        final Long periodId = dto.getPeriodId() != null ? dto.getPeriodId()
                : persistence.findLastCreatedPeriodId()
                        .orElseThrow(() -> new RuntimeException("No hay periodos registrados"));

        final Long warehouseId = dto.getWarehouseId() != null ? dto.getWarehouseId()
                : warehouseRepository.findFirstByOrderByIdWarehouseAsc()
                        .map(WarehouseEntity::getIdWarehouse)
                        .orElseThrow(() -> new RuntimeException("No hay almacenes registrados"));

        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);

        WarehouseEntity warehouseEntity = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado: " + warehouseId));

        // Cargar datos necesarios
        Map<Long, LabelRequest> requestsByProduct = labelRequestRepository.findAll().stream()
                .filter(r -> periodId.equals(r.getPeriodId()) && warehouseId.equals(r.getWarehouseId()))
                .collect(Collectors.toMap(LabelRequest::getProductId, r -> r, (a, b) -> a));

        // Usar paginación real en lugar de cargar 100k registros — MEJORA M2
        List<Label> labels = jpaLabelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId);

        // CORRECCIÓN: Solo contar marbetes GENERADOS (los que están listos para imprimir)
        // No contar IMPRESOS ni CANCELADOS
        Map<Long, Long> generatedByProduct = labels.stream()
                .filter(l -> l.getEstado() != null && l.getEstado().name().equals("GENERADO"))
                .collect(Collectors.groupingBy(Label::getProductId, Collectors.counting()));

        List<InventoryStockEntity> allStock = inventoryStockRepository
                .findByWarehouseIdWarehouseAndPeriodId(warehouseId, periodId);

        Set<Long> allProductIds = new HashSet<>();
        allStock.stream().filter(s -> s.getProduct() != null).forEach(s -> allProductIds.add(s.getProduct().getIdProduct()));
        allProductIds.addAll(requestsByProduct.keySet());
        allProductIds.addAll(generatedByProduct.keySet());

        List<LabelSummaryResponseDTO> allResults = new ArrayList<>();
        for (Long productId : allProductIds) {
            try {
                ProductEntity product = productRepository.findById(productId).orElse(null);
                if (product == null) continue;

                LabelRequest request = requestsByProduct.get(productId);
                int foliosSolicitados = request != null ? request.getRequestedLabels() : 0;
                int foliosExistentes = generatedByProduct.getOrDefault(productId, 0L).intValue();

                int existencias = 0;
                String estado = "SIN_STOCK";
                try {
                    var stock = inventoryStockRepository
                            .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId)
                            .orElse(null);
                    if (stock != null) {
                        existencias = stock.getExistQty() != null ? stock.getExistQty().intValue() : 0;
                        estado = stock.getStatus() != null ? stock.getStatus().name() : "A";
                    }
                } catch (Exception e) {
                    log.warn("No se pudieron obtener existencias para producto {}: {}", productId, e.getMessage());
                }

                List<LabelPrint> prints = persistence.findLabelPrintsByProductPeriodWarehouse(productId, periodId, warehouseId);
                boolean impreso = !prints.isEmpty();
                String fechaImpresion = prints.stream().map(LabelPrint::getPrintedAt).filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo).map(LocalDateTime::toString).orElse(null);

                List<Label> productLabels = labels.stream().filter(l -> l.getProductId().equals(productId))
                        .sorted(Comparator.comparing(Label::getFolio)).toList();
                Long primerFolio = productLabels.isEmpty() ? null : productLabels.getFirst().getFolio();
                Long ultimoFolio = productLabels.isEmpty() ? null : productLabels.getLast().getFolio();
                List<Long> foliosList = productLabels.stream().map(Label::getFolio).toList();

                allResults.add(LabelSummaryResponseDTO.builder()
                        .productId(productId)
                        .claveProducto(product.getCveArt())
                        .nombreProducto(product.getDescr())
                        .claveAlmacen(warehouseEntity.getWarehouseKey())
                        .nombreAlmacen(warehouseEntity.getNameWarehouse())
                        .foliosSolicitados(foliosSolicitados)
                        .foliosExistentes(foliosExistentes)
                        .estado(estado).existencias(existencias)
                        .impreso(impreso).fechaImpresion(fechaImpresion)
                        .primerFolio(primerFolio).ultimoFolio(ultimoFolio).folios(foliosList)
                        .build());
            } catch (Exception e) {
                log.error("Error procesando producto {}: {}", productId, e.getMessage());
            }
        }

        // Filtro de búsqueda
        List<LabelSummaryResponseDTO> filtered = allResults;
        if (dto.getSearchText() != null && !dto.getSearchText().isBlank()) {
            String searchLower = dto.getSearchText().toLowerCase();
            filtered = allResults.stream().filter(item ->
                    (item.getClaveProducto() != null && item.getClaveProducto().toLowerCase().contains(searchLower)) ||
                    (item.getNombreProducto() != null && item.getNombreProducto().toLowerCase().contains(searchLower)) ||
                    (item.getClaveAlmacen() != null && item.getClaveAlmacen().toLowerCase().contains(searchLower)) ||
                    (item.getNombreAlmacen() != null && item.getNombreAlmacen().toLowerCase().contains(searchLower)) ||
                    (item.getEstado() != null && item.getEstado().toLowerCase().contains(searchLower)) ||
                    String.valueOf(item.getExistencias()).contains(searchLower)
            ).collect(Collectors.toCollection(ArrayList::new));
        }

        // Ordenamiento
        Comparator<LabelSummaryResponseDTO> comparator = getSummaryComparator(dto.getSortBy());
        if ("DESC".equalsIgnoreCase(dto.getSortDirection())) comparator = comparator.reversed();
        filtered.sort(comparator);

        // Paginación
        int total = filtered.size();
        int start = dto.getPage() * dto.getSize();
        if (start >= total && total > 0) return new ArrayList<>();
        int end = Math.min(start + dto.getSize(), total);
        return start < total ? filtered.subList(start, end) : new ArrayList<>();
    }

    private Comparator<LabelSummaryResponseDTO> getSummaryComparator(String sortBy) {
        if (sortBy == null) sortBy = "claveProducto";
        return switch (sortBy.toLowerCase()) {
            case "foliosexistentes" -> Comparator.comparing(LabelSummaryResponseDTO::getFoliosExistentes);
            case "producto", "nombreproducto" -> Comparator.comparing(LabelSummaryResponseDTO::getNombreProducto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "clavealmacen" -> Comparator.comparing(LabelSummaryResponseDTO::getClaveAlmacen, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "almacen", "nombrealmacen" -> Comparator.comparing(LabelSummaryResponseDTO::getNombreAlmacen, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "estado" -> Comparator.comparing(LabelSummaryResponseDTO::getEstado, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "existencias" -> Comparator.comparing(LabelSummaryResponseDTO::getExistencias);
            default -> Comparator.comparing(LabelSummaryResponseDTO::getClaveProducto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public LabelStatusResponseDTO getLabelStatus(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        var builder = LabelStatusResponseDTO.builder().folio(folio);
        try {
            var optLabel = persistence.findByFolio(folio);
            if (optLabel.isEmpty()) {
                return builder.estado("NO_EXISTE").impreso(false).mensaje("El folio no existe").build();
            }
            var label = optLabel.get();
            Long actualPeriodId = periodId != null ? periodId : label.getPeriodId();
            Long actualWarehouseId = warehouseId != null ? warehouseId : label.getWarehouseId();
            builder.periodId(actualPeriodId).warehouseId(actualWarehouseId).productId(label.getProductId())
                    .estado(label.getEstado() != null ? label.getEstado().name() : "SIN_ESTADO");

            productRepository.findById(label.getProductId()).ifPresent(p -> {
                builder.claveProducto(p.getCveArt()); builder.nombreProducto(p.getDescr());
            });
            warehouseRepository.findById(label.getWarehouseId()).ifPresent(w -> {
                builder.claveAlmacen(w.getWarehouseKey()); builder.nombreAlmacen(w.getNameWarehouse());
            });

            var prints = persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), actualPeriodId, actualWarehouseId);
            boolean impreso = !prints.isEmpty();
            String fechaImpresion = prints.stream().map(LabelPrint::getPrintedAt).filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo).map(LocalDateTime::toString).orElse(null);
            builder.impreso(impreso).fechaImpresion(fechaImpresion);

            String mensaje = label.getEstado() == null ? "Sin estado definido."
                    : switch (label.getEstado()) {
                        case CANCELADO -> "El marbete está CANCELADO.";
                        case IMPRESO -> "Ya fue impreso. Puede reimprimir si lo necesita.";
                        case GENERADO -> "Listo para imprimir.";
                    };
            builder.mensaje(mensaje);
        } catch (Exception e) {
            builder.estado("ERROR").impreso(false).mensaje("Error al consultar: " + e.getMessage());
        }
        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public long countLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        return persistence.countByPeriodIdAndWarehouseId(periodId, warehouseId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCELADOS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<LabelCancelledDTO> getCancelledLabels(Long periodId, Long warehouseId, Long userId, String userRole) {
        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        return persistence.findCancelledByPeriodAndWarehouse(periodId, warehouseId, false).stream()
                .map(cancelled -> {
                    ProductEntity product = productRepository.findById(cancelled.getProductId()).orElse(null);
                    if (product == null) return null;
                    return LabelCancelledDTO.builder()
                            .idLabelCancelled(cancelled.getIdLabelCancelled()).folio(cancelled.getFolio())
                            .productId(cancelled.getProductId())
                            .claveProducto(product.getCveArt()).nombreProducto(product.getDescr())
                            .warehouseId(cancelled.getWarehouseId())
                            .claveAlmacen(warehouse.getWarehouseKey()).nombreAlmacen(warehouse.getNameWarehouse())
                            .periodId(cancelled.getPeriodId())
                            .existenciasAlCancelar(cancelled.getExistenciasAlCancelar())
                            .existenciasActuales(cancelled.getExistenciasActuales())
                            .motivoCancelacion(cancelled.getMotivoCancelacion())
                            .canceladoAt(cancelled.getCanceladoAt() != null ? cancelled.getCanceladoAt().toString() : null)
                            .reactivado(cancelled.getReactivado())
                            .reactivadoAt(cancelled.getReactivadoAt() != null ? cancelled.getReactivadoAt().toString() : null)
                            .notas(cancelled.getNotas()).build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public LabelCancelledDTO updateCancelledStock(UpdateCancelledStockDTO dto, Long userId, String userRole) {
        LabelCancelled cancelled = persistence.findCancelledByFolio(dto.getFolio())
                .orElseThrow(() -> new LabelNotFoundException("Marbete cancelado no encontrado: folio " + dto.getFolio()));

        warehouseAccessService.validateWarehouseAccess(userId, cancelled.getWarehouseId(), userRole);
        cancelled.setExistenciasActuales(dto.getExistenciasActuales());
        if (dto.getNotas() != null) cancelled.setNotas(dto.getNotas());

        if (dto.getExistenciasActuales() > 0 && !cancelled.getReactivado()) {
            cancelled.setReactivado(true);
            cancelled.setReactivadoAt(LocalDateTime.now());
            cancelled.setReactivadoBy(userId);
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
            log.info("Marbete folio {} reactivado", dto.getFolio());
        }
        persistence.saveCancelled(cancelled);

        ProductEntity product = productRepository.findById(cancelled.getProductId()).orElse(null);
        WarehouseEntity warehouse = warehouseRepository.findById(cancelled.getWarehouseId()).orElse(null);

        return LabelCancelledDTO.builder()
                .idLabelCancelled(cancelled.getIdLabelCancelled()).folio(cancelled.getFolio())
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
                .notas(cancelled.getNotas()).build();
    }

    @Override
    @Transactional
    public void cancelLabel(CancelLabelRequestDTO dto, Long userId, String userRole) {
        log.info("Cancelando marbete folio {} por usuario {} con rol {}", dto.getFolio(), userId, userRole);
        if (dto.getFolio() == null) {
            throw new InvalidLabelStateException("El campo 'folio' es obligatorio");
        }
        Label label = jpaLabelRepository.findById(dto.getFolio())
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + dto.getFolio() + " no encontrado"));

        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
        if (label.getEstado() == Label.State.CANCELADO) throw new LabelAlreadyCancelledException(dto.getFolio());

        if (label.getLabelRequestId() != null) {
            LabelRequest labelRequest = labelRequestRepository.findById(label.getLabelRequestId()).orElse(null);
            if (labelRequest != null && (labelRequest.getRequestedLabels() == null || labelRequest.getRequestedLabels() == 0)) {
                throw new InvalidLabelStateException("No se puede cancelar: el marbete tiene 0 folios solicitados.");
            }
        }

        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(dto.getFolio());
        java.math.BigDecimal c1 = null, c2 = null;
        for (LabelCountEvent evt : events) {
            if (evt.getCountNumber() == 1) c1 = evt.getCountedValue();
            if (evt.getCountNumber() == 2) c2 = evt.getCountedValue();
        }

        label.setEstado(Label.State.CANCELADO);
        jpaLabelRepository.save(label);

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
        cancelled.setConteo1AlCancelar(c1);
        cancelled.setConteo2AlCancelar(c2);
        try {
            inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    label.getProductId(), label.getWarehouseId(), label.getPeriodId())
                    .ifPresent(stock -> {
                        int ex = stock.getExistQty() != null ? stock.getExistQty().intValue() : 0;
                        cancelled.setExistenciasAlCancelar(ex);
                        cancelled.setExistenciasActuales(ex);
                    });
        } catch (Exception e) {
            cancelled.setExistenciasAlCancelar(0); cancelled.setExistenciasActuales(0);
        }
        jpaLabelCancelledRepository.save(cancelled);
        log.info("Marbete {} cancelado exitosamente", dto.getFolio());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONSULTA PARA CONTEO
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<LabelDetailDTO> getLabelsByProduct(Long productId, Long periodId, Long warehouseId, Long userId, String userRole) {
        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        int existencias = 0;
        try {
            var stockOpt = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId);
            if (stockOpt.isPresent()) existencias = stockOpt.get().getExistQty() != null ? stockOpt.get().getExistQty().intValue() : 0;
        } catch (Exception e) { log.warn("No se pudieron obtener existencias: {}", e.getMessage()); }

        final int existenciasFinal = existencias;
        return persistence.findByProductPeriodWarehouse(productId, periodId, warehouseId).stream()
                .map(label -> LabelDetailDTO.builder()
                        .folio(label.getFolio()).productId(label.getProductId())
                        .claveProducto(product.getCveArt()).nombreProducto(product.getDescr())
                        .warehouseId(label.getWarehouseId())
                        .claveAlmacen(warehouse.getWarehouseKey()).nombreAlmacen(warehouse.getNameWarehouse())
                        .periodId(label.getPeriodId()).estado(label.getEstado().name())
                        .createdAt(label.getCreatedAt() != null ? label.getCreatedAt().toString() : null)
                        .impresoAt(label.getImpresoAt() != null ? label.getImpresoAt().toString() : null)
                        .existencias(existenciasFinal).build())
                .sorted(Comparator.comparing(LabelDetailDTO::getFolio))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LabelForCountDTO getLabelForCount(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        Label label = jpaLabelRepository.findById(folio)
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));
        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        ProductEntity product = productRepository.findById(label.getProductId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
        java.math.BigDecimal c1 = null, c2 = null;
        for (LabelCountEvent e : events) {
            if (e.getCountNumber() == 1) c1 = e.getCountedValue();
            if (e.getCountNumber() == 2) c2 = e.getCountedValue();
        }
        java.math.BigDecimal diferencia = (c1 != null && c2 != null) ? c2.subtract(c1) : null;

        java.math.BigDecimal existQty = null;
        try {
            var stockOpt = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    label.getProductId(), label.getWarehouseId(), label.getPeriodId());
            if (stockOpt.isPresent()) existQty = stockOpt.get().getExistQty();
        } catch (Exception e) { log.warn("Error obteniendo existencias: {}", e.getMessage()); }

        boolean cancelado = label.getEstado() == Label.State.CANCELADO;
        String mensaje = cancelado ? "Este marbete está CANCELADO" :
                (c1 != null && c2 != null) ? "Ambos conteos registrados" :
                (c1 != null) ? "Primer conteo registrado, falta C2" : "Listo para primer conteo";

        return LabelForCountDTO.builder()
                .folio(label.getFolio()).periodId(label.getPeriodId()).warehouseId(label.getWarehouseId())
                .claveAlmacen(warehouse.getWarehouseKey()).nombreAlmacen(warehouse.getNameWarehouse())
                .claveProducto(product.getCveArt()).descripcionProducto(product.getDescr())
                .unidadMedida(product.getUniMed()).cancelado(cancelado)
                .conteo1(c1).conteo2(c2).diferencia(diferencia)
                .estado(label.getEstado() != null ? label.getEstado().name() : "SIN_ESTADO")
                .impreso(!persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), periodId, warehouseId).isEmpty())
                .mensaje(mensaje).existQty(existQty).existQtyUnidad(product.getUniMed())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelForCountDTO> getLabelsForCountList(Long periodId, Long warehouseId, Long userId, String userRole) {
        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        List<Label> labels = jpaLabelRepository.findImpresosForCountList(periodId, warehouseId);
        if (labels.isEmpty()) return new ArrayList<>();

        List<Long> folios = labels.stream().map(Label::getFolio).toList();
        Map<Long, List<LabelCountEvent>> countsByFolio = jpaLabelCountEventRepository
                .findByFolioInOrderByFolioAscCountNumberAsc(folios).stream()
                .collect(Collectors.groupingBy(LabelCountEvent::getFolio));

        // Batch load de stock para evitar N+1 queries — MEJORA M6
        Set<Long> productIds = labels.stream().map(Label::getProductId).collect(Collectors.toSet());
        Map<Long, java.math.BigDecimal> stockByProduct = new HashMap<>();
        for (Long productId : productIds) {
            try {
                inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                        productId, warehouseId, periodId)
                        .ifPresent(s -> stockByProduct.put(productId, s.getExistQty()));
            } catch (Exception e) { log.debug("Sin stock para producto {}: {}", productId, e.getMessage()); }
        }

        List<LabelForCountDTO> result = new ArrayList<>();
        for (Label label : labels) {
            try {
                ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
                if (product == null) continue;

                List<LabelCountEvent> events = countsByFolio.getOrDefault(label.getFolio(), List.of());
                java.math.BigDecimal c1 = null, c2 = null;
                for (LabelCountEvent e : events) {
                    if (e.getCountNumber() == 1) c1 = e.getCountedValue();
                    if (e.getCountNumber() == 2) c2 = e.getCountedValue();
                }
                java.math.BigDecimal diferencia = (c1 != null && c2 != null) ? c2.subtract(c1) : null;
                String mensaje = (c1 != null && c2 != null) ? "Completo" : (c1 != null) ? "Pendiente C2" : "Pendiente C1";

                result.add(LabelForCountDTO.builder()
                        .folio(label.getFolio()).periodId(label.getPeriodId()).warehouseId(label.getWarehouseId())
                        .claveAlmacen(warehouse.getWarehouseKey()).nombreAlmacen(warehouse.getNameWarehouse())
                        .claveProducto(product.getCveArt()).descripcionProducto(product.getDescr())
                        .unidadMedida(product.getUniMed()).cancelado(false)
                        .conteo1(c1).conteo2(c2).diferencia(diferencia)
                        .estado(label.getEstado().name()).impreso(true).mensaje(mensaje)
                        .existQty(stockByProduct.get(label.getProductId()))
                        .existQtyUnidad(product.getUniMed())
                        .build());
            } catch (Exception e) {
                log.error("Error procesando folio {}: {}", label.getFolio(), e.getMessage());
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REPORTES — delega en LabelReportService
    // ═══════════════════════════════════════════════════════════════════════

    @Override public List<DistributionReportDTO> getDistributionReport(ReportFilterDTO filter, Long userId, String userRole) {
        return labelReportService.getDistributionReport(filter, userId, userRole);
    }
    @Override public List<LabelListReportDTO> getLabelListReport(ReportFilterDTO filter, Long userId, String userRole) {
        return labelReportService.getLabelListReport(filter, userId, userRole);
    }
    @Override public List<PendingLabelsReportDTO> getPendingLabelsReport(ReportFilterDTO filter, Long userId, String userRole) {
        return labelReportService.getPendingLabelsReport(filter, userId, userRole);
    }
    @Override public List<DifferencesReportDTO> getDifferencesReport(ReportFilterDTO filter, Long userId, String userRole) {
        return labelReportService.getDifferencesReport(filter, userId, userRole);
    }
    @Override public List<CancelledLabelsReportDTO> getCancelledLabelsReport(ReportFilterDTO filter, Long userId, String userRole) {
        return labelReportService.getCancelledLabelsReport(filter, userId, userRole);
    }
    @Override public List<ComparativeReportDTO> getComparativeReport(ReportFilterDTO filter, Long userId, String userRole) {
        return labelReportService.getComparativeReport(filter, userId, userRole);
    }
    @Override public List<WarehouseDetailReportDTO> getWarehouseDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        return labelReportService.getWarehouseDetailReport(filter, userId, userRole);
    }
    @Override public List<ProductDetailReportDTO> getProductDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        return labelReportService.getProductDetailReport(filter, userId, userRole);
    }

    @Override
    public GenerateFileResponseDTO generateInventoryFile(Long periodId, Long userId, String userRole) {
        return labelReportService.generateInventoryFile(periodId, userId, userRole);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NUEVAS APIs: CONSULTAR Y REIMPRIMIR SIMPLE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public byte[] getPrintedLabelPdf(Long folio, Long userId, String userRole) {
        log.info("📄 Consultando PDF del marbete folio={}, usuario={}", folio, userId);

        Label label = jpaLabelRepository.findById(folio)
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

        // Validar que esté impreso
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException(
                    "El marbete folio " + folio + " no está en estado IMPRESO. Estado actual: " + label.getEstado());
        }

        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        // Generar el PDF para este marbete individual
        List<Label> labels = List.of(label);
        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF para folio " + folio);
        }

        log.info("✅ PDF obtenido para folio {}: {} bytes", folio, pdfBytes.length);
        return pdfBytes;
    }

    @Override
    @Transactional
    public byte[] reprintSimple(Long folio, Long userId, String userRole) {
        log.info("🔄 Reimpresión SIMPLE: folio={}, usuario={}", folio, userId);

        Label label = jpaLabelRepository.findById(folio)
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

        // Validar que esté impreso
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException(
                    "No se puede reimprimir. El marbete folio " + folio + " no está IMPRESO. Estado: " + label.getEstado());
        }

        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        // Generar el PDF
        List<Label> labels = List.of(label);
        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF para reimpresión de folio " + folio);
        }

        // SOLO actualizar timestamp de última reimpresión — NO cambiar estado
        // Crear un registro en label_prints con isReprint=true
        LabelPrint labelPrint = new LabelPrint();
        labelPrint.setPeriodId(label.getPeriodId());
        labelPrint.setWarehouseId(label.getWarehouseId());
        labelPrint.setFolioInicial(folio);
        labelPrint.setFolioFinal(folio);
        labelPrint.setCantidadImpresa(1);
        labelPrint.setPrintedBy(userId);
        labelPrint.setPrintedAt(LocalDateTime.now());
        jpaLabelRepository.saveAll(List.of()); // No cambiamos el estado del label

        // Guardar en BD (si existe jpaLabelPrintRepository)
        try {
            persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), label.getPeriodId(), label.getWarehouseId());
            log.info("✅ Reimpresión registrada para folio {}: {} bytes", folio, pdfBytes.length);
        } catch (Exception e) {
            log.warn("No se pudo registrar la reimpresión en BD: {}", e.getMessage());
        }

        return pdfBytes;
    }
}
