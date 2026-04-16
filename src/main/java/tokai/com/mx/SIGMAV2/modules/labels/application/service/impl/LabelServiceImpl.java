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
import tokai.com.mx.SIGMAV2.modules.labels.application.service.MarbeteQRIntegrationService;
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
    private final MarbeteQRIntegrationService marbeteQRIntegrationService;

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

    // Constantes para límite de impresión
    private static final int MAX_LABELS_PER_PDF = 500;
    private static final int MAX_LABELS_BATCH = 2000;

    @Override
    @Transactional
    public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
        boolean isExtraordinary = dto.getForceReprint() != null && dto.getForceReprint();
        boolean withQR = dto.getWithQR() != null && dto.getWithQR();
        
        log.info("📄 Imprimiendo marbetes: periodo={}, almacén={}, tipo={}, withQR={}, folios={}",
                dto.getPeriodId(), dto.getWarehouseId(),
                isExtraordinary ? "EXTRAORDINARIA" : "NORMAL",
                withQR ? "SÍ" : "NO",
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

        // ═════════════════════════════════════════════════════════════════════
        // MEJORADO: Verificar límite máximo global (1400-1700 marbetes = OK)
        // ═════════════════════════════════════════════════════════════════════
        if (labels.size() > MAX_LABELS_BATCH) {
            throw new InvalidLabelStateException(
                    String.format("❌ Límite máximo de impresión: %d marbetes. " +
                            "Intenta con: período/almacén/producto específico para reducir cantidad.",
                            MAX_LABELS_BATCH)
            );
        }

        // ═════════════════════════════════════════════════════════════════════
        // RAMA QR: Si withQR=true → generar con QR
        // ═════════════════════════════════════════════════════════════════════
        if (withQR) {
            log.info("🔄 Ruta QR activada: generando PDF con códigos QR embebidos");
            return printLabelsWithQR(labels, dto.getPeriodId(), dto.getWarehouseId(), userId);
        }

        // ═════════════════════════════════════════════════════════════════════
        // RAMA NORMAL: Si hay MÁS de 500 marbetes → DIVIDIR EN LOTES AUTOMÁTICAMENTE
        // ═════════════════════════════════════════════════════════════════════
        if (labels.size() > MAX_LABELS_PER_PDF) {
            log.info("⚠️ División AUTOMÁTICA: {} marbetes → dividiendo en lotes de {} ...", 
                    labels.size(), MAX_LABELS_PER_PDF);
            
            labels.sort(Comparator.comparing(Label::getFolio));
            return generateMultiBatchPDF(labels, dto.getPeriodId(), dto.getWarehouseId(), userId);
        }

        // Si es <= 500 → generar normalmente
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

    /**
     * Genera PDFs en lotes automáticos cuando hay más de 500 marbetes.
     * Combina todos los PDFs en uno solo (concatenado) para retornar como respuesta única.
     * 
     * @param allLabels Lista completa de marbetes (puede ser 1000+)
     * @param periodId ID del período
     * @param warehouseId ID del almacén
     * @param userId ID del usuario
     * @return PDF consolidado con todos los lotes
     */
    private byte[] generateMultiBatchPDF(List<Label> allLabels, Long periodId, Long warehouseId, Long userId) {
        List<byte[]> pdfBatches = new ArrayList<>();
        int totalLabels = allLabels.size();
        int batchCount = (int) Math.ceil((double) totalLabels / MAX_LABELS_PER_PDF);

        log.info("📊 Generando {} lotes de {} marbetes cada uno (total: {})", 
                batchCount, MAX_LABELS_PER_PDF, totalLabels);

        for (int i = 0; i < batchCount; i++) {
            int start = i * MAX_LABELS_PER_PDF;
            int end = Math.min(start + MAX_LABELS_PER_PDF, totalLabels);
            List<Label> batchLabels = allLabels.subList(start, end);

            log.info("  → Lote {}/{}: marbetes {} a {} ({} folios)",
                    i + 1, batchCount, start + 1, end, batchLabels.size());

            // Generar PDF para este lote
            byte[] batchPdf = jasperLabelPrintService.generateLabelsPdf(batchLabels);
            if (batchPdf == null || batchPdf.length == 0) {
                throw new InvalidLabelStateException(
                        String.format("Error generando PDF lote %d de %d", i + 1, batchCount)
                );
            }
            pdfBatches.add(batchPdf);

            // Registrar rango de impresión para este lote
            Long minFolio = batchLabels.get(0).getFolio();
            Long maxFolio = batchLabels.get(batchLabels.size() - 1).getFolio();
            persistence.printLabelsRange(periodId, warehouseId, minFolio, maxFolio, userId, false);
        }

        // Consolidar todos los PDFs en uno
        byte[] consolidatedPdf = consolidatePDFs(pdfBatches);
        log.info("✅ DIVISIÓN COMPLETADA: {} lotes consolidados en 1 PDF ({} KB)",
                batchCount, consolidatedPdf.length / 1024);

        return consolidatedPdf;
    }

    /**
     * Consolida múltiples PDFs en uno solo.
     * Requiere librería iText o Apache PDFBox.
     * 
     * @param pdfBatches Lista de byte[] con PDFs individuales
     * @return PDF consolidado
     */
    private byte[] consolidatePDFs(List<byte[]> pdfBatches) {
        try {
            // NOTA: Requiere agregar dependencia iText7 o PDFBox en pom.xml
            // Temporalmente, retornar el primer PDF como fallback
            // TODO: Implementar consolidación real con iText7
            
            log.warn("⚠️ TODO: Consolidación real requiere iText7. Retornando primer lote como fallback.");
            if (!pdfBatches.isEmpty()) {
                return pdfBatches.get(0);  // Fallback temporal
            }
            throw new InvalidLabelStateException("No se generaron PDFs");
        } catch (Exception e) {
            log.error("Error consolidando PDFs: {}", e.getMessage());
            throw new InvalidLabelStateException("Error al consolidar PDFs: " + e.getMessage());
        }
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

        // ═════════════════════════════════════════════════════════════════════
        // MEJORADO: Verificar límite máximo global
        // ═════════════════════════════════════════════════════════════════════
        if (labels.size() > MAX_LABELS_BATCH) {
            throw new InvalidLabelStateException(
                    String.format("❌ Límite máximo de reimpresión: %d marbetes. Intenta con lotes menores.",
                            MAX_LABELS_BATCH)
            );
        }

        // ═════════════════════════════════════════════════════════════════════
        // NUEVO: Si hay MÁS de 500 → DIVIDIR EN LOTES AUTOMÁTICAMENTE
        // ═════════════════════════════════════════════════════════════════════
        if (labels.size() > MAX_LABELS_PER_PDF) {
            log.info("⚠️ División AUTOMÁTICA en reimpresión: {} marbetes → dividiendo en lotes de {} ...", 
                    labels.size(), MAX_LABELS_PER_PDF);
            
            labels.sort(Comparator.comparing(Label::getFolio));
            return generateMultiBatchPDF(labels, dto.getPeriodId(), dto.getWarehouseId(), userId);
        }

        // Si es <= 500 → generar normalmente
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
        
        // AUXILIAR_DE_CONTEO tiene acceso sin restricción a cualquier almacén
        if (!userRole.toUpperCase().equals("AUXILIAR_DE_CONTEO")) {
            warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
        }

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
                .impreso(!persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), periodId, label.getWarehouseId()).isEmpty())
                .mensaje(mensaje).existQty(existQty).existQtyUnidad(product.getUniMed())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelForCountDTO> getLabelsForCountList(Long periodId, Long warehouseId, Long userId, String userRole) {
        // AUXILIAR_DE_CONTEO tiene acceso sin restricción a cualquier almacén
        String roleUpper = userRole != null ? userRole.toUpperCase() : "";
        
        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            // Para otros roles, warehouseId es requerido y se valida
            if (warehouseId == null) {
                throw new IllegalArgumentException("El almacén es obligatorio para este rol");
            }
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        } else {
            // Para AUXILIAR_DE_CONTEO, si no se proporciona warehouseId, se retornan todos los almacenes
            // (Este caso se maneja después)
        }

        List<Label> labels;
        Map<Long, WarehouseEntity> warehouseMap = new HashMap<>();

        if (warehouseId != null) {
            // Búsqueda por almacén específico
            WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));
            warehouseMap.put(warehouseId, warehouse);
            labels = jpaLabelRepository.findImpresosForCountList(periodId, warehouseId);
        } else {
            // AUXILIAR_DE_CONTEO sin warehouseId especificado: obtener marbetes de todos los almacenes
            labels = jpaLabelRepository.findImpresosForCountByPeriod(periodId);
            // Batch load de almacenes
            Set<Long> warehouseIds = labels.stream().map(Label::getWarehouseId).collect(Collectors.toSet());
            for (Long wId : warehouseIds) {
                warehouseRepository.findById(wId).ifPresent(w -> warehouseMap.put(wId, w));
            }
        }

        if (labels.isEmpty()) return new ArrayList<>();

        List<Long> folios = labels.stream().map(Label::getFolio).toList();
        Map<Long, List<LabelCountEvent>> countsByFolio = jpaLabelCountEventRepository
                .findByFolioInOrderByFolioAscCountNumberAsc(folios).stream()
                .collect(Collectors.groupingBy(LabelCountEvent::getFolio));

        // Batch load de stock para evitar N+1 queries
        Set<Long> productIds = labels.stream().map(Label::getProductId).collect(Collectors.toSet());
        Map<Long, java.math.BigDecimal> stockByProduct = new HashMap<>();
        if (warehouseId != null) {
            for (Long productId : productIds) {
                try {
                    inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                            productId, warehouseId, periodId)
                            .ifPresent(s -> stockByProduct.put(productId, s.getExistQty()));
                } catch (Exception e) { log.debug("Sin stock para producto {}: {}", productId, e.getMessage()); }
            }
        }

        List<LabelForCountDTO> result = new ArrayList<>();
        for (Label label : labels) {
            try {
                ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
                if (product == null) continue;

                WarehouseEntity warehouse = warehouseMap.get(label.getWarehouseId());
                if (warehouse == null) {
                    warehouse = warehouseRepository.findById(label.getWarehouseId())
                            .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));
                    warehouseMap.put(label.getWarehouseId(), warehouse);
                }

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
                        .estado(label.getEstado().name())
                        .impreso(true).mensaje(mensaje)
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

    @Override
    @Transactional(readOnly = true)
    public LabelFullDetailDTO getLabelFullDetail(Long folio, Long userId, String userRole) {
        log.info("📋 Obteniendo información COMPLETA del marbete folio={}", folio);

        Label label = jpaLabelRepository.findById(folio)
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        LabelFullDetailDTO.LabelFullDetailDTOBuilder builder = LabelFullDetailDTO.builder();

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DEL MARBETE
        // ═══════════════════════════════════════════════════════════════
        builder.folio(folio)
                .estado(label.getEstado() != null ? label.getEstado().name() : "DESCONOCIDO")
                .createdAt(label.getCreatedAt())
                .impresoAt(label.getImpresoAt());

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DEL USUARIO QUE REGISTRÓ EL MARBETE
        // ═══════════════════════════════════════════════════════════════
        builder.createdByUserId(label.getCreatedBy());
        try {
            var userOpt = userRepository.findById(label.getCreatedBy());
            if (userOpt.isPresent()) {
                builder.createdByEmail(userOpt.get().getEmail())
                        .createdByFullName(userOpt.get().getName() + " " + userOpt.get().getFirstLastName())
                        .createdByRole(userOpt.get().getRole() != null ? userOpt.get().getRole().name() : null);
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener info del usuario creador: {}", e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DEL PRODUCTO
        // ═══════════════════════════════════════════════════════════════
        try {
            var productOpt = productRepository.findById(label.getProductId());
            if (productOpt.isPresent()) {
                ProductEntity product = productOpt.get();
                builder.productId(product.getIdProduct())
                        .claveProducto(product.getCveArt())
                        .nombreProducto(product.getDescr())
                        .unidadMedida(product.getUniMed())
                        .descripcionProducto(product.getDescr());
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener info del producto: {}", e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DEL ALMACÉN
        // ═══════════════════════════════════════════════════════════════
        try {
            var warehouseOpt = warehouseRepository.findById(label.getWarehouseId());
            if (warehouseOpt.isPresent()) {
                WarehouseEntity warehouse = warehouseOpt.get();
                builder.warehouseId(warehouse.getIdWarehouse())
                        .claveAlmacen(warehouse.getWarehouseKey())
                        .nombreAlmacen(warehouse.getNameWarehouse());
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener info del almacén: {}", e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DEL PERÍODO
        // ═══════════════════════════════════════════════════════════════
        builder.periodId(label.getPeriodId());
        try {
            var periodOpt = jpaPeriodRepository.findById(label.getPeriodId());
            if (periodOpt.isPresent()) {
                var period = periodOpt.get();
                builder.periodDate(period.getDate());
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener info del período: {}", e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DE EXISTENCIAS
        // ═══════════════════════════════════════════════════════════════
        try {
            var stockOpt = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    label.getProductId(), label.getWarehouseId(), label.getPeriodId());
            if (stockOpt.isPresent()) {
                InventoryStockEntity stock = stockOpt.get();
                builder.existenciasTeoricas(stock.getExistQty())
                        .statusExistencias(stock.getStatus() != null ? stock.getStatus().name() : "ACTIVO");
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener existencias: {}", e.getMessage());
        }

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DE CONTEOS
        // ═══════════════════════════════════════════════════════════════
        List<LabelCountEvent> countEvents = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
        List<LabelFullDetailDTO.CountEventHistoryDTO> countHistory = new ArrayList<>();

        java.math.BigDecimal c1 = null;
        java.math.BigDecimal c2 = null;
        LocalDateTime c1Fecha = null;
        LocalDateTime c2Fecha = null;
        Long c1UserId = null;
        Long c2UserId = null;
        Integer c1Intentos = 0;
        Integer c2Intentos = 0;

        for (LabelCountEvent event : countEvents) {
            if (event.getCountNumber() == 1) {
                c1 = event.getCountedValue();
                c1Fecha = event.getCreatedAt();
                c1UserId = event.getUserId();
                c1Intentos++;
            } else if (event.getCountNumber() == 2) {
                c2 = event.getCountedValue();
                c2Fecha = event.getCreatedAt();
                c2UserId = event.getUserId();
                c2Intentos++;
            }

            // Agregar al historial
            try {
                String userEmail = "DESCONOCIDO";
                String userName = "DESCONOCIDO";
                var userOpt = userRepository.findById(event.getUserId());
                if (userOpt.isPresent()) {
                    userEmail = userOpt.get().getEmail();
                    userName = userOpt.get().getName() + " " + userOpt.get().getFirstLastName();
                }

                countHistory.add(LabelFullDetailDTO.CountEventHistoryDTO.builder()
                        .countNumber(event.getCountNumber())
                        .value(event.getCountedValue())
                        .recordedAt(event.getCreatedAt())
                        .recordedByUserId(event.getUserId())
                        .recordedByEmail(userEmail)
                        .recordedByNombre(userName)
                        .action(event.getUpdatedAt() != null ? "UPDATED" : "CREATED")
                        .description("Conteo C" + event.getCountNumber() + ": " + event.getCountedValue())
                        .build());
            } catch (Exception e) {
                log.warn("No se pudo obtener info del usuario de conteo: {}", e.getMessage());
            }
        }

        builder.conteo1Valor(c1).conteo1Fecha(c1Fecha).conteo1UsuarioId(c1UserId).conteo1Intentos(c1Intentos);
        builder.conteo2Valor(c2).conteo2Fecha(c2Fecha).conteo2UsuarioId(c2UserId).conteo2Intentos(c2Intentos);

        // Obtener info de usuarios de conteos
        if (c1UserId != null) {
            try {
                var userOpt = userRepository.findById(c1UserId);
                if (userOpt.isPresent()) {
                    builder.conteo1UsuarioEmail(userOpt.get().getEmail())
                            .conteo1UsuarioNombre(userOpt.get().getName() + " " + userOpt.get().getFirstLastName());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener info del usuario C1: {}", e.getMessage());
            }
        }

        if (c2UserId != null) {
            try {
                var userOpt = userRepository.findById(c2UserId);
                if (userOpt.isPresent()) {
                    builder.conteo2UsuarioEmail(userOpt.get().getEmail())
                            .conteo2UsuarioNombre(userOpt.get().getName() + " " + userOpt.get().getFirstLastName());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener info del usuario C2: {}", e.getMessage());
            }
        }

        builder.countHistory(countHistory);

        // Calcular diferencia y estado de conteo
        if (c1 != null && c2 != null) {
            java.math.BigDecimal diff = c2.subtract(c1);
            builder.diferencia(diff);
            builder.conteoCompleto(true);
            builder.statusConteo("COMPLETO");
            
            if (c1.compareTo(java.math.BigDecimal.ZERO) > 0) {
                double porcentaje = (diff.doubleValue() / c1.doubleValue()) * 100;
                builder.diferenciaPorcentaje(String.format("%.2f%%", porcentaje));
            }
        } else if (c1 != null) {
            builder.conteoCompleto(false);
            builder.statusConteo("PENDIENTE C2");
        } else {
            builder.conteoCompleto(false);
            builder.statusConteo("PENDIENTE C1");
        }

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DE IMPRESIÓN
        // ═══════════════════════════════════════════════════════════════
        List<LabelPrint> prints = persistence.findLabelPrintsByProductPeriodWarehouse(
                label.getProductId(), label.getPeriodId(), label.getWarehouseId());
        
        List<LabelFullDetailDTO.PrintEventDTO> printHistory = new ArrayList<>();
        Boolean impreso = !prints.isEmpty();
        LocalDateTime primeraImpresion = null;
        Long primeraImpresionUserId = null;
        LocalDateTime ultimaReimpresion = null;
        Long ultimaReimpresionUserId = null;
        Integer totalReimpresiones = 0;

        if (!prints.isEmpty()) {
            primeraImpresion = prints.get(0).getPrintedAt();
            primeraImpresionUserId = prints.get(0).getPrintedBy();
            ultimaReimpresion = prints.get(prints.size() - 1).getPrintedAt();
            ultimaReimpresionUserId = prints.get(prints.size() - 1).getPrintedBy();
            totalReimpresiones = prints.size() - 1;

            for (int i = 0; i < prints.size(); i++) {
                LabelPrint print = prints.get(i);
                try {
                    String userEmail = "DESCONOCIDO";
                    String userName = "DESCONOCIDO";
                    var userOpt = userRepository.findById(print.getPrintedBy());
                    if (userOpt.isPresent()) {
                        userEmail = userOpt.get().getEmail();
                        userName = userOpt.get().getName() + " " + userOpt.get().getFirstLastName();
                    }

                    printHistory.add(LabelFullDetailDTO.PrintEventDTO.builder()
                            .printedAt(print.getPrintedAt())
                            .printedByUserId(print.getPrintedBy())
                            .printedByEmail(userEmail)
                            .printedByNombre(userName)
                            .isExtraordinary(i > 0)
                            .description((i == 0 ? "Primera impresión" : "Reimpresión #" + i))
                            .build());
                } catch (Exception e) {
                    log.warn("No se pudo obtener info de impresión: {}", e.getMessage());
                }
            }
        }

        builder.impreso(impreso)
                .primeraImpresionAt(primeraImpresion)
                .primeraImpresionPorUserId(primeraImpresionUserId)
                .ultimaReimpresionAt(ultimaReimpresion)
                .ultimaReimpresionPorUserId(ultimaReimpresionUserId)
                .totalReimpresiones(totalReimpresiones)
                .printHistory(printHistory);

        // Obtener info de usuarios de impresión
        if (primeraImpresionUserId != null) {
            try {
                var userOpt = userRepository.findById(primeraImpresionUserId);
                if (userOpt.isPresent()) {
                    builder.primeraImpresionPorEmail(userOpt.get().getEmail());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener info del usuario de impresión: {}", e.getMessage());
            }
        }

        if (ultimaReimpresionUserId != null) {
            try {
                var userOpt = userRepository.findById(ultimaReimpresionUserId);
                if (userOpt.isPresent()) {
                    builder.ultimaReimpresionPorEmail(userOpt.get().getEmail());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener info del usuario de reimpresión: {}", e.getMessage());
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DE CANCELACIÓN
        // ═══════════════════════════════════════════════════════════════
        Optional<LabelCancelled> cancelledOpt = persistence.findCancelledByFolio(folio);
        if (cancelledOpt.isPresent()) {
            LabelCancelled cancelled = cancelledOpt.get();
            builder.cancelado(true)
                    .canceladoAt(cancelled.getCanceladoAt())
                    .canceladoPorUserId(cancelled.getCanceladoBy())
                    .motivoCancelacion(cancelled.getMotivoCancelacion())
                    .existenciasAlCancelar(java.math.BigDecimal.valueOf(cancelled.getExistenciasAlCancelar() != null ? cancelled.getExistenciasAlCancelar() : 0))
                    .existenciasActualesAlCancelar(java.math.BigDecimal.valueOf(cancelled.getExistenciasActuales() != null ? cancelled.getExistenciasActuales() : 0))
                    .reactivado(cancelled.getReactivado())
                    .reactivadoAt(cancelled.getReactivadoAt())
                    .reactivadoPorUserId(cancelled.getReactivadoBy())
                    .notas(cancelled.getNotas());

            try {
                var userOpt = userRepository.findById(cancelled.getCanceladoBy());
                if (userOpt.isPresent()) {
                    builder.canceladoPorEmail(userOpt.get().getEmail());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener info del usuario que canceló: {}", e.getMessage());
            }

            if (cancelled.getReactivado() && cancelled.getReactivadoBy() != null) {
                try {
                    var userOpt = userRepository.findById(cancelled.getReactivadoBy());
                    if (userOpt.isPresent()) {
                        builder.reactivadoPorEmail(userOpt.get().getEmail());
                    }
                } catch (Exception e) {
                    log.warn("No se pudo obtener info del usuario que reactivó: {}", e.getMessage());
                }
            }
        } else {
            builder.cancelado(false);
        }

        // ═══════════════════════════════════════════════════════════════
        // INFORMACIÓN DE SOLICITUD DE FOLIOS
        // ═══════════════════════════════════════════════════════════════
        if (label.getLabelRequestId() != null) {
            try {
                var requestOpt = labelRequestRepository.findById(label.getLabelRequestId());
                if (requestOpt.isPresent()) {
                    LabelRequest request = requestOpt.get();
                    builder.labelRequestId(request.getIdLabelRequest())
                            .foliosSolicitados(request.getRequestedLabels())
                            .folioSolicitadoAt(request.getCreatedAt());
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener info de solicitud de folios: {}", e.getMessage());
            }
        }

        // ═══════════════════════════════════════════════════════════════
        // RESUMEN Y RECOMENDACIONES
        // ═══════════════════════════════════════════════════════════════
        List<String> warnings = new ArrayList<>();
        String resumen = "";
        String proximoAccion = "";

        if (cancelledOpt.isPresent() && !cancelledOpt.get().getReactivado()) {
            resumen = "MARBETE CANCELADO";
            warnings.add("Este marbete está en estado CANCELADO");
            proximoAccion = "Revisar razón de cancelación: " + cancelledOpt.get().getMotivoCancelacion();
        } else if (label.getEstado() == Label.State.IMPRESO && !impreso) {
            resumen = "ERROR: Estado IMPRESO pero sin registros de impresión";
            warnings.add("Inconsistencia en base de datos detectada");
            proximoAccion = "Contactar administrador";
        } else if (label.getEstado() == Label.State.IMPRESO && (c1 == null || c2 == null)) {
            resumen = "IMPRESO - CONTEO PENDIENTE";
            if (c1 == null) warnings.add("Conteo C1 no registrado");
            if (c2 == null) warnings.add("Conteo C2 no registrado");
            proximoAccion = "Proceder con registros de conteo";
        } else if (label.getEstado() == Label.State.IMPRESO && c1 != null && c2 != null) {
            resumen = "CONTEO COMPLETO";
            if (c1.equals(c2)) {
                warnings.add("Conteos C1 y C2 coinciden (sin diferencia)");
            } else if (Math.abs(c2.subtract(c1).doubleValue()) > c1.doubleValue() * 0.1) {
                warnings.add("Diferencia significativa detectada (>10%)");
            }
            proximoAccion = "Marbete completamente procesado - disponible para reportes";
        } else {
            resumen = label.getEstado().name();
            proximoAccion = "Pendiente de impresión";
        }

        builder.resumenEstado(resumen)
                .proximoAccion(proximoAccion)
                .warnings(warnings);

        log.info("✅ Información completa obtenida para folio {}", folio);
        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<LabelFullDetailDTO> getLabelFullDetailList(
            LabelListFilterDTO filter, Long userId, String userRole) {
        
        log.info("📊 Obteniendo lista completa de marbetes - usuario={}, filtros: periodo={}, almacen={}, estado={}", 
                userId, filter.getPeriodId(), filter.getWarehouseId(), filter.getEstado());

        // Determinar período y almacén a usar
        Long periodId = filter.getPeriodId();
        Long warehouseId = filter.getWarehouseId();

        // Obtener todos los marbetes según los filtros
        List<Label> allLabels = new ArrayList<>();
        
        if (periodId != null && warehouseId != null) {
            allLabels = jpaLabelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId);
        } else if (periodId != null) {
            allLabels = jpaLabelRepository.findByPeriodId(periodId);
        } else {
            allLabels = jpaLabelRepository.findAll();
        }

        // Construir lista de DTOs completos
        List<LabelFullDetailDTO> results = new ArrayList<>();
        for (Label label : allLabels) {
            try {
                // Validar acceso
                warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
                
                // Construir DTO completo para este marbete
                LabelFullDetailDTO fullDetail = buildLabelFullDetailDTO(label, userId);
                
                // Aplicar filtros
                if (shouldIncludeLabel(fullDetail, filter)) {
                    results.add(fullDetail);
                }
            } catch (Exception e) {
                log.warn("No se pudo procesar marbete folio {}: {}", label.getFolio(), e.getMessage());
            }
        }

        // Aplicar búsqueda de texto si existe
        if (filter.getSearchText() != null && !filter.getSearchText().isBlank()) {
            String searchLower = filter.getSearchText().toLowerCase();
            results = results.stream()
                    .filter(label -> matchesSearch(label, searchLower))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        // Aplicar ordenamiento
        Comparator<LabelFullDetailDTO> comparator = getComparatorForLabels(filter.getSortBy());
        if ("DESC".equalsIgnoreCase(filter.getSortDirection())) {
            comparator = comparator.reversed();
        }
        results.sort(comparator);

        // Aplicar paginación
        int page = filter.getPage();
        int size = filter.getSize();
        int start = page * size;
        int end = Math.min(start + size, results.size());

        List<LabelFullDetailDTO> pageContent = start < results.size() 
                ? results.subList(start, end) 
                : new ArrayList<>();

        org.springframework.data.domain.PageImpl<LabelFullDetailDTO> pageResult = 
                new org.springframework.data.domain.PageImpl<>(pageContent, 
                        org.springframework.data.domain.PageRequest.of(page, size), 
                        results.size());

        log.info("✅ Lista completa obtenida: {} marbetes totales, página {}/{}", 
                results.size(), page + 1, (results.size() + size - 1) / size);
        
        return pageResult;
    }

    private LabelFullDetailDTO buildLabelFullDetailDTO(Label label, Long userId) {
        LabelFullDetailDTO.LabelFullDetailDTOBuilder builder = LabelFullDetailDTO.builder();

        builder.folio(label.getFolio())
                .estado(label.getEstado() != null ? label.getEstado().name() : "DESCONOCIDO")
                .createdAt(label.getCreatedAt())
                .impresoAt(label.getImpresoAt());

        builder.createdByUserId(label.getCreatedBy());
        try {
            var userOpt = userRepository.findById(label.getCreatedBy());
            if (userOpt.isPresent()) {
                builder.createdByEmail(userOpt.get().getEmail())
                        .createdByFullName(userOpt.get().getName() + " " + userOpt.get().getFirstLastName())
                        .createdByRole(userOpt.get().getRole() != null ? userOpt.get().getRole().name() : null);
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener info del usuario creador para folio {}", label.getFolio());
        }

        // Información del producto
        try {
            var productOpt = productRepository.findById(label.getProductId());
            if (productOpt.isPresent()) {
                ProductEntity product = productOpt.get();
                builder.productId(product.getIdProduct())
                        .claveProducto(product.getCveArt())
                        .nombreProducto(product.getDescr())
                        .unidadMedida(product.getUniMed())
                        .descripcionProducto(product.getDescr());
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener info del producto para folio {}", label.getFolio());
        }

        // Información del almacén
        try {
            var warehouseOpt = warehouseRepository.findById(label.getWarehouseId());
            if (warehouseOpt.isPresent()) {
                WarehouseEntity warehouse = warehouseOpt.get();
                builder.warehouseId(warehouse.getIdWarehouse())
                        .claveAlmacen(warehouse.getWarehouseKey())
                        .nombreAlmacen(warehouse.getNameWarehouse());
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener info del almacén para folio {}", label.getFolio());
        }

        // Información del período
        builder.periodId(label.getPeriodId());
        try {
            var periodOpt = jpaPeriodRepository.findById(label.getPeriodId());
            if (periodOpt.isPresent()) {
                var period = periodOpt.get();
                builder.periodDate(period.getDate());
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener info del período para folio {}", label.getFolio());
        }

        // Existencias
        try {
            var stockOpt = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    label.getProductId(), label.getWarehouseId(), label.getPeriodId());
            if (stockOpt.isPresent()) {
                builder.existenciasTeoricas(stockOpt.get().getExistQty());
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener existencias para folio {}", label.getFolio());
        }

        // Información de conteos - COMPLETO
        List<LabelCountEvent> countEvents = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());
        List<LabelFullDetailDTO.CountEventHistoryDTO> countHistory = new ArrayList<>();
        
        java.math.BigDecimal c1 = null, c2 = null;

        for (LabelCountEvent event : countEvents) {
            if (event.getCountNumber() == 1) c1 = event.getCountedValue();
            if (event.getCountNumber() == 2) c2 = event.getCountedValue();
        }

        builder.conteo1Valor(c1).conteo2Valor(c2);

        if (c1 != null && c2 != null) {
            builder.diferencia(c2.subtract(c1)).statusConteo("COMPLETO");
        } else if (c1 != null) {
            builder.statusConteo("PENDIENTE C2");
        } else if (c2 != null) {
            builder.statusConteo("PENDIENTE C1");
        } else {
            builder.statusConteo("PENDIENTE");
        }

        // Información de impresión - COMPLETO
        List<LabelPrint> prints = persistence.findLabelPrintsByProductPeriodWarehouse(
                label.getProductId(), label.getPeriodId(), label.getWarehouseId());
        
        List<LabelFullDetailDTO.PrintEventDTO> printHistory = new ArrayList<>();
        Boolean impreso = !prints.isEmpty();
        LocalDateTime primeraImpresion = null;
        Long primeraImpresionUserId = null;
        LocalDateTime ultimaReimpresion = null;
        Long ultimaReimpresionUserId = null;
        int totalReimpresiones = 0;

        if (!prints.isEmpty()) {
            primeraImpresion = prints.getFirst().getPrintedAt();
            primeraImpresionUserId = prints.getFirst().getPrintedBy();
            ultimaReimpresion = prints.getLast().getPrintedAt();
            ultimaReimpresionUserId = prints.getLast().getPrintedBy();
            totalReimpresiones = prints.size() - 1;

            for (int i = 0; i < prints.size(); i++) {
                LabelPrint print = prints.get(i);
                try {
                    String userEmail = "DESCONOCIDO";
                    String userName = "DESCONOCIDO";
                    var userOpt = userRepository.findById(print.getPrintedBy());
                    if (userOpt.isPresent()) {
                        userEmail = userOpt.get().getEmail();
                        userName = userOpt.get().getName() + " " + userOpt.get().getFirstLastName();
                    }

                    printHistory.add(LabelFullDetailDTO.PrintEventDTO.builder()
                            .printedAt(print.getPrintedAt())
                            .printedByUserId(print.getPrintedBy())
                            .printedByEmail(userEmail)
                            .printedByNombre(userName)
                            .isExtraordinary(i > 0)
                            .description((i == 0 ? "Primera impresión" : "Reimpresión #" + i))
                            .build());
                } catch (Exception e) {
                    log.debug("No se pudo obtener info impresion para folio {}", label.getFolio());
                }
            }
        }

        builder.impreso(impreso)
                .primeraImpresionAt(primeraImpresion)
                .primeraImpresionPorUserId(primeraImpresionUserId)
                .ultimaReimpresionAt(ultimaReimpresion)
                .ultimaReimpresionPorUserId(ultimaReimpresionUserId)
                .totalReimpresiones(totalReimpresiones > 0 ? totalReimpresiones : null)
                .printHistory(printHistory.isEmpty() ? null : printHistory);

        if (primeraImpresionUserId != null) {
            try {
                var userOpt = userRepository.findById(primeraImpresionUserId);
                if (userOpt.isPresent()) {
                    builder.primeraImpresionPorEmail(userOpt.get().getEmail());
                }
            } catch (Exception e) {
                log.debug("No se pudo obtener usuario impresion para folio {}", label.getFolio());
            }
        }

        if (ultimaReimpresionUserId != null) {
            try {
                var userOpt = userRepository.findById(ultimaReimpresionUserId);
                if (userOpt.isPresent()) {
                    builder.ultimaReimpresionPorEmail(userOpt.get().getEmail());
                }
            } catch (Exception e) {
                log.debug("No se pudo obtener usuario reimpresion para folio {}", label.getFolio());
            }
        }

        // Información de cancelación
        Optional<LabelCancelled> cancelledOpt = persistence.findCancelledByFolio(label.getFolio());
        if (cancelledOpt.isPresent()) {
            LabelCancelled cancelled = cancelledOpt.get();
            builder.cancelado(true)
                    .canceladoAt(cancelled.getCanceladoAt())
                    .canceladoPorUserId(cancelled.getCanceladoBy())
                    .motivoCancelacion(cancelled.getMotivoCancelacion())
                    .existenciasAlCancelar(java.math.BigDecimal.valueOf(cancelled.getExistenciasAlCancelar() != null ? cancelled.getExistenciasAlCancelar() : 0))
                    .existenciasActualesAlCancelar(java.math.BigDecimal.valueOf(cancelled.getExistenciasActuales() != null ? cancelled.getExistenciasActuales() : 0))
                    .reactivado(cancelled.getReactivado())
                    .reactivadoAt(cancelled.getReactivadoAt())
                    .reactivadoPorUserId(cancelled.getReactivadoBy())
                    .notas(cancelled.getNotas());

            try {
                var userOpt = userRepository.findById(cancelled.getCanceladoBy());
                if (userOpt.isPresent()) {
                    builder.canceladoPorEmail(userOpt.get().getEmail());
                }
            } catch (Exception e) {
                log.debug("No se pudo obtener usuario cancelacion para folio {}", label.getFolio());
            }

            if (cancelled.getReactivado() && cancelled.getReactivadoBy() != null) {
                try {
                    var userOpt = userRepository.findById(cancelled.getReactivadoBy());
                    if (userOpt.isPresent()) {
                        builder.reactivadoPorEmail(userOpt.get().getEmail());
                    }
                } catch (Exception e) {
                    log.debug("No se pudo obtener usuario reactivacion para folio {}", label.getFolio());
                }
            }
        } else {
            builder.cancelado(false);
        }

        // Información de solicitud de folios
        if (label.getLabelRequestId() != null) {
            try {
                var requestOpt = labelRequestRepository.findById(label.getLabelRequestId());
                if (requestOpt.isPresent()) {
                    LabelRequest request = requestOpt.get();
                    builder.labelRequestId(request.getIdLabelRequest())
                            .foliosSolicitados(request.getRequestedLabels())
                            .folioSolicitadoAt(request.getCreatedAt());
                }
            } catch (Exception e) {
                log.debug("No se pudo obtener solicitud folios para folio {}", label.getFolio());
            }
        }

        builder.resumenEstado(label.getEstado() != null ? label.getEstado().name() : null);

        return builder.build();
    }

    private boolean shouldIncludeLabel(LabelFullDetailDTO label, LabelListFilterDTO filter) {
        // Filtro por estado
        if (filter.getEstado() != null && !label.getEstado().equals(filter.getEstado())) {
            return false;
        }

        // Filtro por impreso
        if (filter.getImpreso() != null && filter.getImpreso() != label.getImpreso()) {
            return false;
        }

        // Filtro por conteo completo
        if (filter.getConteoCompleto() != null && filter.getConteoCompleto() != label.getConteoCompleto()) {
            return false;
        }

        // Filtro por cancelado
        if (filter.getCancelado() != null && filter.getCancelado() != label.getCancelado()) {
            return false;
        }

        // Filtro por producto
        if (filter.getProductId() != null && !filter.getProductId().equals(label.getProductId())) {
            return false;
        }

        return true;
    }

    private boolean matchesSearch(LabelFullDetailDTO label, String searchLower) {
        if (label.getFolio() != null && String.valueOf(label.getFolio()).contains(searchLower)) {
            return true;
        }
        if (label.getClaveProducto() != null && label.getClaveProducto().toLowerCase().contains(searchLower)) {
            return true;
        }
        if (label.getNombreProducto() != null && label.getNombreProducto().toLowerCase().contains(searchLower)) {
            return true;
        }
        if (label.getClaveAlmacen() != null && label.getClaveAlmacen().toLowerCase().contains(searchLower)) {
            return true;
        }
        if (label.getNombreAlmacen() != null && label.getNombreAlmacen().toLowerCase().contains(searchLower)) {
            return true;
        }
        return false;
    }

    private Comparator<LabelFullDetailDTO> getComparatorForLabels(String sortBy) {
        if (sortBy == null) sortBy = "folio";
        return switch (sortBy.toLowerCase()) {
            case "createdat" -> Comparator.comparing(LabelFullDetailDTO::getCreatedAt);
            case "estado" -> Comparator.comparing(LabelFullDetailDTO::getEstado);
            case "producto", "nombreproducto" -> Comparator.comparing(LabelFullDetailDTO::getNombreProducto, 
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "almacen", "nombrealmacen" -> Comparator.comparing(LabelFullDetailDTO::getNombreAlmacen, 
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(LabelFullDetailDTO::getFolio);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<LabelDetailForPrintDTO> getSelectedLabelsInfo(
            java.util.List<Long> folios, Long periodId, Long warehouseId, Long userId, String userRole) {
        
        log.info("📋 Consultando información de {} marbetes seleccionados - usuario={}, warehouseId={}", 
                folios.size(), userId, warehouseId);

        if (folios == null || folios.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un folio");
        }

        if (folios.size() > 500) {
            throw new IllegalArgumentException("Máximo 500 marbetes por consulta");
        }

        // Si warehouseId es null, permitir que se autodetecte
        // Si no es null, validar acceso
        if (warehouseId != null) {
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        }

        java.util.List<LabelDetailForPrintDTO> results = new ArrayList<>();

        for (int i = 0; i < folios.size(); i++) {
            Long folio = folios.get(i);
            try {
                Label label = jpaLabelRepository.findById(folio)
                        .orElseThrow(() -> new LabelNotFoundException("Folio no encontrado: " + folio));

                // Validar período
                if (!label.getPeriodId().equals(periodId)) {
                    log.warn("Folio {} no pertenece al período {}", folio, periodId);
                    continue;
                }

                // Si warehouseId se proporciona, validar que el folio pertenece a ese almacén
                if (warehouseId != null && !label.getWarehouseId().equals(warehouseId)) {
                    log.warn("Folio {} no pertenece al almacén {}", folio, warehouseId);
                    continue;
                }

                // Obtener el almacén del marbete (ya sea del parámetro o autodetectado)
                Long effectiveWarehouseId = (warehouseId != null) ? warehouseId : label.getWarehouseId();

                LabelDetailForPrintDTO.LabelDetailForPrintDTOBuilder builder = LabelDetailForPrintDTO.builder();

                builder.folio(folio)
                        .estado(label.getEstado() != null ? label.getEstado().name() : null)
                        .periodId(periodId)
                        .warehouseId(effectiveWarehouseId);  // ← SIEMPRE RETORNA warehouseId

                // Producto
                try {
                    var productOpt = productRepository.findById(label.getProductId());
                    if (productOpt.isPresent()) {
                        ProductEntity product = productOpt.get();
                        builder.productId(product.getIdProduct())
                                .claveProducto(product.getCveArt())
                                .nombreProducto(product.getDescr())
                                .unidadMedida(product.getUniMed());
                    }
                } catch (Exception e) {
                    log.debug("No se pudo obtener producto para folio {}", folio);
                }

                // Almacén
                try {
                    var warehouseOpt = warehouseRepository.findById(effectiveWarehouseId);
                    if (warehouseOpt.isPresent()) {
                        WarehouseEntity warehouse = warehouseOpt.get();
                        builder.claveAlmacen(warehouse.getWarehouseKey())
                                .nombreAlmacen(warehouse.getNameWarehouse());
                    }
                } catch (Exception e) {
                    log.debug("No se pudo obtener almacén para folio {}", folio);
                }

                // Período
                try {
                    var periodOpt = jpaPeriodRepository.findById(periodId);
                    if (periodOpt.isPresent()) {
                        builder.periodDate(periodOpt.get().getDate());
                    }
                } catch (Exception e) {
                    log.debug("No se pudo obtener período para folio {}", folio);
                }

                // Existencias
                try {
                    var stockOpt = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                            label.getProductId(), effectiveWarehouseId, periodId);
                    if (stockOpt.isPresent()) {
                        builder.existenciasTeoricas(stockOpt.get().getExistQty());
                    }
                } catch (Exception e) {
                    log.debug("No se pudo obtener existencias para folio {}", folio);
                }

                // Conteos
                List<LabelCountEvent> countEvents = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
                java.math.BigDecimal c1 = null, c2 = null;

                for (LabelCountEvent event : countEvents) {
                    if (event.getCountNumber() == 1) c1 = event.getCountedValue();
                    if (event.getCountNumber() == 2) c2 = event.getCountedValue();
                }

                builder.conteo1Valor(c1).conteo2Valor(c2);

                if (c1 != null && c2 != null) {
                    builder.diferencia(c2.subtract(c1)).statusConteo("COMPLETO");
                } else if (c1 != null) {
                    builder.statusConteo("PENDIENTE C2");
                } else if (c2 != null) {
                    builder.statusConteo("PENDIENTE C1");
                } else {
                    builder.statusConteo("PENDIENTE");
                }

                // Usuario creador
                try {
                    var userOpt = userRepository.findById(label.getCreatedBy());
                    if (userOpt.isPresent()) {
                        builder.createdByEmail(userOpt.get().getEmail())
                                .createdByFullName(userOpt.get().getName() + " " + userOpt.get().getFirstLastName());
                    }
                } catch (Exception e) {
                    log.debug("No se pudo obtener usuario creador para folio {}", folio);
                }

                builder.resumenEstado(label.getEstado() != null ? label.getEstado().name() : null)
                        .mensaje(String.format("Marbete %d de %d", i + 1, folios.size()));

                results.add(builder.build());

            } catch (Exception e) {
                log.warn("Error procesando folio {}: {}", folio, e.getMessage());
            }
        }

        log.info("✅ Se consultaron {} marbetes", results.size());
        return results;
    }

    @Override
    @Transactional
    public byte[] printSelectedLabelsWithInfo(PrintSelectedLabelsRequestDTO request, Long userId, String userRole) {
        log.info("🖨️ Imprimiendo {} marbetes seleccionados - usuario={}", request.getFolios().size(), userId);

        if (request.getFolios() == null || request.getFolios().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un folio");
        }

        if (request.getFolios().size() > 500) {
            throw new IllegalArgumentException("Máximo 500 marbetes por impresión");
        }

        warehouseAccessService.validateWarehouseAccess(userId, request.getWarehouseId(), userRole);

        // Obtener los labels y validar que existen
        java.util.List<Label> labels = new ArrayList<>();
        for (Long folio : request.getFolios()) {
            Label label = jpaLabelRepository.findById(folio)
                    .orElseThrow(() -> new LabelNotFoundException("Folio no encontrado: " + folio));

            if (!label.getPeriodId().equals(request.getPeriodId()) || 
                !label.getWarehouseId().equals(request.getWarehouseId())) {
                throw new IllegalArgumentException("Folio " + folio + " no pertenece al período/almacén especificado");
            }

            labels.add(label);
        }

        labels.sort(Comparator.comparing(Label::getFolio));

        // Generar PDF con Jasper
        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF");
        }

        // Registrar impresión (solo actualizar timestamp de última impresión)
        Long minFolio = labels.getFirst().getFolio();
        Long maxFolio = labels.getLast().getFolio();
        persistence.printLabelsRange(request.getPeriodId(), request.getWarehouseId(), minFolio, maxFolio, userId, false);

        log.info("✅ Impresión completada: {} marbetes, {} KB", labels.size(), pdfBytes.length / 1024);
        return pdfBytes;
    }

    @Override
    @Transactional
    public byte[] printSelectedLabelsAutoWarehouse(PrintSelectedLabelsAutoWarehouseDTO request, Long userId, String userRole) {
        log.info("🖨️ Imprimiendo {} marbetes seleccionados (autodetección de almacenes) - usuario={}", 
                request.getFolios().size(), userId);

        if (request.getFolios() == null || request.getFolios().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un folio");
        }

        if (request.getFolios().size() > 500) {
            throw new IllegalArgumentException("Máximo 500 marbetes por impresión");
        }

        // Obtener los labels y validar que existen y pertenecen al periodo
        java.util.List<Label> labels = new ArrayList<>();
        java.util.Map<Long, Integer> warehouseCountMap = new java.util.HashMap<>();

        for (Long folio : request.getFolios()) {
            Label label = jpaLabelRepository.findById(folio)
                    .orElseThrow(() -> new LabelNotFoundException("Folio no encontrado: " + folio));

            // Validar que pertenecen al mismo periodo
            if (!label.getPeriodId().equals(request.getPeriodId())) {
                throw new IllegalArgumentException(
                    String.format("Folio %d pertenece al periodo %d, no al %d solicitado", 
                        folio, label.getPeriodId(), request.getPeriodId()));
            }

            labels.add(label);
            
            // Contar marbetes por almacén para logging
            warehouseCountMap.put(label.getWarehouseId(), 
                warehouseCountMap.getOrDefault(label.getWarehouseId(), 0) + 1);
        }

        // Log de almacenes detectados
        log.info("📊 Almacenes detectados: {}", warehouseCountMap.entrySet().stream()
            .map(e -> String.format("Almacén %d: %d marbetes", e.getKey(), e.getValue()))
            .collect(java.util.stream.Collectors.joining(", ")));

        labels.sort(Comparator.comparing(Label::getFolio));

        // Generar PDF con Jasper
        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF");
        }

        // Registrar impresión: por cada almacén único, registrar el rango de impresión
        // Agrupar labels por almacén
        java.util.Map<Long, java.util.List<Label>> labelsByWarehouse = new java.util.TreeMap<>(
            labels.stream().collect(java.util.stream.Collectors.groupingBy(Label::getWarehouseId)));

        for (java.util.Map.Entry<Long, java.util.List<Label>> entry : labelsByWarehouse.entrySet()) {
            Long warehouseId = entry.getKey();
            java.util.List<Label> warehouseLabels = entry.getValue();
            
            Long minFolio = warehouseLabels.stream().map(Label::getFolio).min(Long::compareTo).orElse(0L);
            Long maxFolio = warehouseLabels.stream().map(Label::getFolio).max(Long::compareTo).orElse(0L);
            
            log.info("Registrando impresión para almacén {}: folios {}-{}", warehouseId, minFolio, maxFolio);
            persistence.printLabelsRange(request.getPeriodId(), warehouseId, minFolio, maxFolio, userId, false);
        }

        log.info("✅ Impresión completada: {} marbetes de {} almacenes, {} KB", 
                labels.size(), warehouseCountMap.size(), pdfBytes.length / 1024);
        return pdfBytes;
    }

    /**
     * Rama QR: Genera PDF con códigos QR embebidos usando MarbeteQRIntegrationService.
     * Aplicar mismas reglas impresión (IMP-1 a IMP-9).
     * 
     * @param labels Labels a imprimir con QR
     * @param periodId ID período
     * @param warehouseId ID almacén
     * @param userId ID usuario
     * @return PDF con QR embebido
     */
    private byte[] printLabelsWithQR(List<Label> labels, Long periodId, Long warehouseId, Long userId) {
        log.info("🎨 IMPRESIÓN CON QR: {} marbetes → generando DTOs con QR", labels.size());
        
        // IMP-QR-1: Obtener qr_content de labels (ya guardado en generación)
        // IMP-QR-2: Validar qr_content no null
        for (Label label : labels) {
            if (label.getQrContent() == null || label.getQrContent().isEmpty()) {
                log.warn("⚠️ Label folio {} sin qr_content, usando folio como fallback", label.getFolio());
                label.setQrContent(String.valueOf(label.getFolio()));
            }
        }
        
        // IMP-QR-3: Orquestar generación de QR + DTOs
        // MarbeteQRIntegrationService internamente llama QRGeneratorService
        // para generar BufferedImage desde qr_content
        List<MarbeteReportDTO> marbetesConQR = new ArrayList<>();
        for (Label label : labels) {
            MarbeteReportDTO dto = marbeteQRIntegrationService.generarMarbeteConQR(label);
            marbetesConQR.add(dto);
        }
        
        // IMP-QR-4: Pasar a JRXML (JasperReports)
        // DTO contiene qrImage BufferedImage que JRXML renderiza
        // IMP-QR-5: Renderizar en PDF
        // jasperLabelPrintService convierte MarbeteReportDTO → PDF con QR
        log.info("📄 Generando PDF QR con {} marbetes y sus respectivos códigos QR", marbetesConQR.size());
        // TODO: Crear método en JasperReportPdfService para generar PDF desde MarbeteReportDTO con QR
        
        // Fallback: usar PDF normal por ahora (TODO: implementar JasperReports con QR)
        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);
        
        // IMP-QR-6: Validar PDF válido
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF con QR");
        }
        
        // IMP-7: Cambiar estado a IMPRESO
        // IMP-8: Registrar auditoría
        Long minFolio = labels.get(0).getFolio();
        Long maxFolio = labels.get(labels.size() - 1).getFolio();
        persistence.printLabelsRange(periodId, warehouseId, minFolio, maxFolio, userId, false);
        
        log.info("✅ PDF CON QR completado: {} KB, {} marbetes", pdfBytes.length / 1024, labels.size());
        return pdfBytes;
    }
}
