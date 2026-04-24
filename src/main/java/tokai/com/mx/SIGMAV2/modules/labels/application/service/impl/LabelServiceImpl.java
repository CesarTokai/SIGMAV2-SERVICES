package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fachada principal del módulo de marbetes.
 * Orquesta y delega en servicios especializados:
 *   - LabelGenerationService  — generación de marbetes
 *   - LabelCountService       — conteos C1/C2
 *   - LabelReportService      — reportes y archivo TXT
 *   - LabelPrintService       — impresión/reimpresión PDF
 *   - LabelQueryService       — consultas read-only
 *   - LabelCancelService      — cancelación y reactivación
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelServiceImpl implements LabelService {

    private final LabelGenerationService labelGenerationService;
    private final LabelCountService labelCountService;
    private final LabelReportService labelReportService;
    private final LabelPrintService labelPrintService;
    private final LabelQueryService labelQueryService;
    private final LabelCancelService labelCancelService;

    // Solo para los métodos deprecated que aún tienen lógica inline
    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;

    // ═══════════════════════════════════════════════════════════════════════
    // DEPRECATED — mantener hasta que el frontend migre a generateBatchList
    // ═══════════════════════════════════════════════════════════════════════

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
    // GENERACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    @Override @Transactional
    public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
        labelGenerationService.generateBatchList(dto, userId, userRole);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // IMPRESIÓN
    // ═══════════════════════════════════════════════════════════════════════

    @Override @Transactional
    public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
        return labelPrintService.printLabels(dto, userId, userRole);
    }

    @Override @Transactional
    public byte[] extraordinaryReprint(PrintRequestDTO dto, Long userId, String userRole) {
        return labelPrintService.extraordinaryReprint(dto, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public byte[] getPrintedLabelPdf(Long folio, Long userId, String userRole) {
        return labelPrintService.getPrintedLabelPdf(folio, userId, userRole);
    }

    @Override @Transactional
    public byte[] reprintSimple(Long folio, Long userId, String userRole) {
        return labelPrintService.reprintSimple(folio, userId, userRole);
    }

    @Override @Transactional
    public byte[] printSelectedLabelsWithInfo(PrintSelectedLabelsRequestDTO request, Long userId, String userRole) {
        return labelPrintService.printSelectedLabelsWithInfo(request, userId, userRole);
    }

    @Override @Transactional
    public byte[] printSelectedLabelsAutoWarehouse(PrintSelectedLabelsAutoWarehouseDTO request, Long userId, String userRole) {
        return labelPrintService.printSelectedLabelsAutoWarehouse(request, userId, userRole);
    }

    @Override @Transactional
    public byte[] printSelectedLabelsWithQR(PrintSelectedLabelsRequestDTO request, Long userId, String userRole) {
        return labelPrintService.printSelectedLabelsWithQR(request, userId, userRole);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONTEO
    // ═══════════════════════════════════════════════════════════════════════

    @Override @Transactional
    public LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole) {
        return labelCountService.registerCountC1(dto, userId, userRole);
    }

    @Override @Transactional
    public LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole) {
        return labelCountService.registerCountC2(dto, userId, userRole);
    }

    @Override @Transactional
    public LabelCountEvent updateCountC1(UpdateCountDTO dto, Long userId, String userRole) {
        return labelCountService.updateCountC1(dto, userId, userRole);
    }

    @Override @Transactional
    public LabelCountEvent updateCountC2(UpdateCountDTO dto, Long userId, String userRole) {
        return labelCountService.updateCountC2(dto, userId, userRole);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONSULTAS
    // ═══════════════════════════════════════════════════════════════════════

    @Override @Transactional(readOnly = true)
    public PendingPrintCountResponseDTO getPendingPrintCount(PendingPrintCountRequestDTO dto, Long userId, String userRole) {
        return labelQueryService.getPendingPrintCount(dto, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public List<LabelSummaryResponseDTO> getLabelSummary(LabelSummaryRequestDTO dto, Long userId, String userRole) {
        return labelQueryService.getLabelSummary(dto, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public LabelStatusResponseDTO getLabelStatus(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        return labelQueryService.getLabelStatus(folio, periodId, warehouseId, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public long countLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        return labelQueryService.countLabelsByPeriodAndWarehouse(periodId, warehouseId);
    }

    @Override @Transactional(readOnly = true)
    public List<LabelDetailDTO> getLabelsByProduct(Long productId, Long periodId, Long warehouseId, Long userId, String userRole) {
        return labelQueryService.getLabelsByProduct(productId, periodId, warehouseId, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public LabelForCountDTO getLabelForCount(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        return labelQueryService.getLabelForCount(folio, periodId, warehouseId, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public List<LabelForCountDTO> getLabelsForCountList(Long periodId, Long warehouseId, Long userId, String userRole) {
        return labelQueryService.getLabelsForCountList(periodId, warehouseId, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public LabelFullDetailDTO getLabelFullDetail(Long folio, Long userId, String userRole) {
        return labelQueryService.getLabelFullDetail(folio, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public Page<LabelFullDetailDTO> getLabelFullDetailList(LabelListFilterDTO filter, Long userId, String userRole) {
        return labelQueryService.getLabelFullDetailList(filter, userId, userRole);
    }

    @Override @Transactional(readOnly = true)
    public List<LabelDetailForPrintDTO> getSelectedLabelsInfo(List<Long> folios, Long periodId, Long warehouseId, Long userId, String userRole) {
        return labelQueryService.getSelectedLabelsInfo(folios, periodId, warehouseId, userId, userRole);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CANCELACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    @Override @Transactional(readOnly = true)
    public List<LabelCancelledDTO> getCancelledLabels(Long periodId, Long warehouseId, Long userId, String userRole) {
        return labelCancelService.getCancelledLabels(periodId, warehouseId, userId, userRole);
    }

    @Override @Transactional
    public LabelCancelledDTO updateCancelledStock(UpdateCancelledStockDTO dto, Long userId, String userRole) {
        return labelCancelService.updateCancelledStock(dto, userId, userRole);
    }

    @Override @Transactional
    public void cancelLabel(CancelLabelRequestDTO dto, Long userId, String userRole) {
        labelCancelService.cancelLabel(dto, userId, userRole);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // REPORTES
    // ═══════════════════════════════════════════════════════════════════════

    @Override public List<DistributionReportDTO> getDistributionReport(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getDistributionReport(filter, userId, userRole); }
    @Override public List<LabelListReportDTO> getLabelListReport(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getLabelListReport(filter, userId, userRole); }
    @Override public List<PendingLabelsReportDTO> getPendingLabelsReport(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getPendingLabelsReport(filter, userId, userRole); }
    @Override public List<DifferencesReportDTO> getDifferencesReport(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getDifferencesReport(filter, userId, userRole); }
    @Override public List<CancelledLabelsReportDTO> getCancelledLabelsReport(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getCancelledLabelsReport(filter, userId, userRole); }
    @Override public List<ComparativeReportDTO> getComparativeReport(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getComparativeReport(filter, userId, userRole); }
    @Override public List<WarehouseDetailReportDTO> getWarehouseDetailReport(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getWarehouseDetailReport(filter, userId, userRole); }
    @Override public List<ProductDetailReportDTO> getProductDetailReport(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getProductDetailReport(filter, userId, userRole); }
    @Override public List<LabelWithCommentsReportDTO> getLabelListWithComments(ReportFilterDTO filter, Long userId, String userRole) { return labelReportService.getLabelListWithComments(filter, userId, userRole); }
    @Override public GenerateFileResponseDTO generateInventoryFile(Long periodId, Long userId, String userRole) { return labelReportService.generateInventoryFile(periodId, userId, userRole); }
}
