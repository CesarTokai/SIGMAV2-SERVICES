package tokai.com.mx.SIGMAV2.modules.labels.adapter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.AuthenticatedUserService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.JasperReportPdfService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sigmav2/labels")
@RequiredArgsConstructor
public class LabelsController {

    private static final Logger log = LoggerFactory.getLogger(LabelsController.class);

    private final LabelService labelService;
    private final AuthenticatedUserService authenticatedUserService;
    private final JasperReportPdfService jasperReportPdfService;
    
    private Long getUserIdFromToken() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return authenticatedUserService.getUserIdByEmail(email);
    }

    private String getUserRoleFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    // ...existing code...

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Void> requestLabels(@Valid @RequestBody LabelRequestDTO dto) {
        labelService.requestLabels(dto, getUserIdFromToken(), getUserRoleFromToken());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<GenerateBatchResponseDTO> generateBatch(@Valid @RequestBody GenerateBatchDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        log.info("Generando marbetes para usuario {} con rol {}", userId, userRole);
        return ResponseEntity.ok(labelService.generateBatch(dto, userId, userRole));
    }

    @PostMapping("/print")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> printLabels(@Valid @RequestBody PrintRequestDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        log.info("Imprimiendo marbetes: usuario={}, periodo={}, almacén={}", userId, dto.getPeriodId(), dto.getWarehouseId());
        byte[] pdfBytes = labelService.printLabels(dto, userId, userRole);
        return buildPdfResponse(pdfBytes, dto.getPeriodId(), dto.getWarehouseId(), "marbetes");
    }

    @PostMapping("/pending-print-count")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<PendingPrintCountResponseDTO> getPendingPrintCount(
            @Valid @RequestBody PendingPrintCountRequestDTO dto) {
        return ResponseEntity.ok(labelService.getPendingPrintCount(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/counts/c1")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC1(@Valid @RequestBody CountEventDTO dto) {
        return ResponseEntity.ok(labelService.registerCountC1(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/counts/c2")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC2(@Valid @RequestBody CountEventDTO dto) {
        return ResponseEntity.ok(labelService.registerCountC2(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PutMapping("/counts/c1")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> updateCountC1(@Valid @RequestBody UpdateCountDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("Actualizando C1: folio={}, usuario={}", dto.getFolio(), userId);
        return ResponseEntity.ok(labelService.updateCountC1(dto, userId, getUserRoleFromToken()));
    }

    @PutMapping("/counts/c2")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> updateCountC2(@Valid @RequestBody UpdateCountDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("Actualizando C2: folio={}, usuario={}", dto.getFolio(), userId);
        return ResponseEntity.ok(labelService.updateCountC2(dto, userId, getUserRoleFromToken()));
    }

    @PostMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<LabelSummaryResponseDTO>> getLabelSummary(@RequestBody LabelSummaryRequestDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("POST /summary: periodId={}, warehouseId={}", dto.getPeriodId(), dto.getWarehouseId());
        return ResponseEntity.ok(labelService.getLabelSummary(dto, userId, getUserRoleFromToken()));
    }

    @PostMapping("/generate/batch")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Void> generateBatchList(@Valid @RequestBody GenerateBatchListDTO dto) {
        labelService.generateBatchList(dto, getUserIdFromToken(), getUserRoleFromToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate-and-print")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> generateAndPrint(@Valid @RequestBody GenerateBatchListDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        log.info("🚀 /generate-and-print: usuario={}", userId);
        labelService.generateBatchList(dto, userId, userRole);
        PendingPrintCountRequestDTO countDto = new PendingPrintCountRequestDTO();
        countDto.setPeriodId(dto.getPeriodId());
        countDto.setWarehouseId(dto.getWarehouseId());
        var countResponse = labelService.getPendingPrintCount(countDto, userId, userRole);
        if (countResponse.getCount() == 0) {
            return ResponseEntity.badRequest().build();
        }
        PrintRequestDTO printDto = new PrintRequestDTO();
        printDto.setPeriodId(dto.getPeriodId());
        printDto.setWarehouseId(dto.getWarehouseId());
        byte[] pdfBytes = labelService.printLabels(printDto, userId, userRole);
        return buildPdfResponse(pdfBytes, dto.getPeriodId(), dto.getWarehouseId(), "marbetes");
    }

    @GetMapping("/status")
    public ResponseEntity<LabelStatusResponseDTO> getLabelStatus(
            @RequestParam Long folio, @RequestParam Long periodId, @RequestParam Long warehouseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userRole = auth != null && !auth.getAuthorities().isEmpty()
                ? auth.getAuthorities().iterator().next().getAuthority() : null;
        return ResponseEntity.ok(labelService.getLabelStatus(folio, periodId, warehouseId, getUserIdFromToken(), userRole));
    }

    @GetMapping("/debug/count")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Map<String, Object>> getLabelsCount(
            @RequestParam Long periodId, @RequestParam Long warehouseId) {
        Long userId = getUserIdFromToken();
        long count = labelService.countLabelsByPeriodAndWarehouse(periodId, warehouseId);
        return ResponseEntity.ok(Map.of(
                "periodId", periodId, "warehouseId", warehouseId,
                "totalLabels", count, "userId", userId));
    }

    @GetMapping("/cancelled")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<List<LabelCancelledDTO>> getCancelledLabels(
            @RequestParam Long periodId, @RequestParam Long warehouseId) {
        return ResponseEntity.ok(labelService.getCancelledLabels(periodId, warehouseId, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PutMapping("/cancelled/update-stock")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<LabelCancelledDTO> updateCancelledStock(@Valid @RequestBody UpdateCancelledStockDTO dto) {
        return ResponseEntity.ok(labelService.updateCancelledStock(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<LabelDetailDTO>> getLabelsByProduct(
            @PathVariable Long productId, @RequestParam Long periodId, @RequestParam Long warehouseId) {
        return ResponseEntity.ok(labelService.getLabelsByProduct(productId, periodId, warehouseId, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @GetMapping("/by-folio/{folio}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelStatusResponseDTO> getLabelByFolio(@PathVariable Long folio) {
        return ResponseEntity.ok(labelService.getLabelStatus(folio, null, null, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Void> cancelLabel(@Valid @RequestBody CancelLabelRequestDTO dto) {
        log.info("Cancelando folio={}", dto.getFolio());
        labelService.cancelLabel(dto, getUserIdFromToken(), getUserRoleFromToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/for-count")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelForCountDTO> getLabelForCount(
            @RequestParam Long folio, @RequestParam Long periodId, @RequestParam Long warehouseId) {
        return ResponseEntity.ok(labelService.getLabelForCount(folio, periodId, warehouseId, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/for-count")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelForCountDTO> getLabelForCountByBody(@Valid @RequestBody LabelForCountRequestDTO dto) {
        return ResponseEntity.ok(labelService.getLabelForCount(dto.getFolio(), dto.getPeriodId(), dto.getWarehouseId(), getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/for-count/list")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<LabelForCountDTO>> getLabelsForCountList(@Valid @RequestBody LabelCountListRequestDTO dto) {
        List<LabelForCountDTO> labels = labelService.getLabelsForCountList(dto.getPeriodId(), dto.getWarehouseId(), getUserIdFromToken(), getUserRoleFromToken());
        return ResponseEntity.ok(labels);
    }

    // ── Reportes JSON ────────────────────────────────────────────────────
    @PostMapping("/reports/distribution")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<DistributionReportDTO>> getDistributionReport(
            @Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getDistributionReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/list")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<LabelListReportDTO>> getLabelListReport(
            @Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getLabelListReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/pending")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<PendingLabelsReportDTO>> getPendingLabelsReport(
            @Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getPendingLabelsReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/with-differences")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<DifferencesReportDTO>> getDifferencesReport(
            @Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getDifferencesReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/cancelled")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<CancelledLabelsReportDTO>> getCancelledLabelsReport(
            @Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getCancelledLabelsReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/comparative")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<ComparativeReportDTO>> getComparativeReport(
            @Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getComparativeReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/warehouse-detail")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<WarehouseDetailReportDTO>> getWarehouseDetailReport(
            @Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getWarehouseDetailReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/product-detail")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<ProductDetailReportDTO>> getProductDetailReport(
            @Valid @RequestBody ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getProductDetailReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/generate-file")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> generateInventoryFile(@Valid @RequestBody GenerateFileRequestDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("Generando archivo TXT para periodo={}, usuario={}", dto.getPeriodId(), userId);
        GenerateFileResponseDTO response = labelService.generateInventoryFile(dto.getPeriodId(), userId, getUserRoleFromToken());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment()
                        .filename(response.getFileName()).build());
        if (response.getFileBytes() != null) {
            headers.setContentLength(response.getFileBytes().length);
        }
        return ResponseEntity.ok().headers(headers).body(response.getFileBytes());
    }

    @PostMapping("/for-extraordinary-reprint/list")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<List<Map<String, Object>>> getImpresosForExtraordinaryReprint(
            @Valid @RequestBody LabelCountListRequestDTO dto) {
        Long userId = getUserIdFromToken();
        List<LabelForCountDTO> labelsForCount = labelService.getLabelsForCountList(dto.getPeriodId(), dto.getWarehouseId(), userId, getUserRoleFromToken());
        List<Map<String, Object>> result = new ArrayList<>();
        for (LabelForCountDTO label : labelsForCount) {
            result.add(Map.of(
                    "folio", label.getFolio(),
                    "producto", label.getClaveProducto() + " - " + label.getDescripcionProducto(),
                    "claveProducto", label.getClaveProducto(),
                    "descripcionProducto", label.getDescripcionProducto(),
                    "almacen", label.getClaveAlmacen() + " - " + label.getNombreAlmacen(),
                    "estado", label.getEstado(),
                    "mensaje", label.getMensaje()
            ));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/extraordinary-reprint")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> extraordinaryReprint(@Valid @RequestBody PrintRequestDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("🔄 /extraordinary-reprint: usuario={}, folios={}", userId,
                dto.getFolios() != null ? dto.getFolios().size() : 0);
        byte[] pdfBytes = labelService.extraordinaryReprint(dto, userId, getUserRoleFromToken());
        return buildPdfResponse(pdfBytes, dto.getPeriodId(), dto.getWarehouseId(), "marbetes_REIMPRESION");
    }

    // ── PDFs de reportes ─────────────────────────────────────────────────
    @PostMapping("/reports/distribution/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getDistributionReportPdf(@Valid @RequestBody ReportFilterDTO filter) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        List<DistributionReportDTO> data = labelService.getDistributionReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("distribución", filter.getPeriodId(), filter.getWarehouseId());
        return buildPdfResponse(jasperReportPdfService.generateDistributionPdf(data), "distribucion_marbetes");
    }

    @PostMapping("/reports/distribution/pdf/all")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getAllDistributionReportPdf(@Valid @RequestBody AllWarehousesReportRequestDTO request) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(request.getPeriodId());
        filter.setWarehouseId(null);
        List<DistributionReportDTO> data = labelService.getDistributionReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("distribución de todos los almacenes", request.getPeriodId(), null);
        return buildPdfResponse(jasperReportPdfService.generateDistributionPdf(data), "distribucion_todos_almacenes_marbetes");
    }

    @PostMapping("/reports/list/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getLabelListReportPdf(@Valid @RequestBody ReportFilterDTO filter) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        List<LabelListReportDTO> data = labelService.getLabelListReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("listado de marbetes", filter.getPeriodId(), filter.getWarehouseId());
        return buildPdfResponse(jasperReportPdfService.generateListPdf(data), "listado_marbetes");
    }

    @PostMapping("/reports/list/all/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getAllLabelListReportPdf(@Valid @RequestBody AllWarehousesReportRequestDTO request) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(request.getPeriodId());
        filter.setWarehouseId(null);
        List<LabelListReportDTO> data = labelService.getLabelListReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("listado de todos los marbetes", request.getPeriodId(), null);
        return buildPdfResponse(jasperReportPdfService.generateListPdf(data), "listado_todos_marbetes");
    }

    @PostMapping("/reports/pending/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getPendingLabelsReportPdf(@Valid @RequestBody ReportFilterDTO filter) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        List<PendingLabelsReportDTO> data = labelService.getPendingLabelsReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("marbetes pendientes", filter.getPeriodId(), filter.getWarehouseId());
        return buildPdfResponse(jasperReportPdfService.generatePendingPdf(data), "pendientes_marbetes");
    }

    @PostMapping("/reports/pending/pdf/all")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getAllPendingLabelsReportPdf(@Valid @RequestBody AllWarehousesReportRequestDTO request) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(request.getPeriodId());
        filter.setWarehouseId(null);
        List<PendingLabelsReportDTO> data = labelService.getPendingLabelsReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("marbetes pendientes de todos los almacenes", request.getPeriodId(), null);
        return buildPdfResponse(jasperReportPdfService.generatePendingPdf(data), "pendientes_todos_almacenes_marbetes");
    }

    @PostMapping("/reports/with-differences/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getDifferencesReportPdf(@Valid @RequestBody ReportFilterDTO filter) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        List<DifferencesReportDTO> data = labelService.getDifferencesReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("marbetes con diferencias", filter.getPeriodId(), filter.getWarehouseId());
        return buildPdfResponse(jasperReportPdfService.generateDifferencesPdf(data), "diferencias_marbetes");
    }

    @PostMapping("/reports/with-differences/all/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getAllDifferencesReportPdf(@Valid @RequestBody AllWarehousesReportRequestDTO request) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(request.getPeriodId());
        filter.setWarehouseId(null);
        List<DifferencesReportDTO> data = labelService.getDifferencesReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("marbetes con diferencias", request.getPeriodId(), null);
        return buildPdfResponse(jasperReportPdfService.generateDifferencesPdf(data), "diferencias_todos_almacenes_marbetes");
    }

    @PostMapping("/reports/cancelled/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getCancelledLabelsReportPdf(@Valid @RequestBody ReportFilterDTO filter) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        List<CancelledLabelsReportDTO> data = labelService.getCancelledLabelsReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("marbetes cancelados", filter.getPeriodId(), filter.getWarehouseId());
        return buildPdfResponse(jasperReportPdfService.generateCancelledPdf(data), "cancelados_marbetes");
    }

    @PostMapping("/reports/comparative/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getComparativeReportPdf(@Valid @RequestBody ReportFilterDTO filter) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        List<ComparativeReportDTO> data = labelService.getComparativeReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("comparativo de inventario", filter.getPeriodId(), filter.getWarehouseId());
        return buildPdfResponse(jasperReportPdfService.generateComparativePdf(data), "comparativo_marbetes");
    }

    @PostMapping("/reports/comparative/all/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getAllComparativeReportPdf(@Valid @RequestBody AllWarehousesReportRequestDTO request) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(request.getPeriodId());
        filter.setWarehouseId(null);
        List<ComparativeReportDTO> data = labelService.getComparativeReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("comparativo de todos los almacenes", request.getPeriodId(), null);
        return buildPdfResponse(jasperReportPdfService.generateComparativePdf(data), "comparativo_todos_almacenes_marbetes");
    }

    @PostMapping("/reports/product-detail/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getProductDetailReportPdf(@Valid @RequestBody ReportFilterDTO filter) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        List<ProductDetailReportDTO> data = labelService.getProductDetailReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("detalle por producto", filter.getPeriodId(), filter.getWarehouseId());
        return buildPdfResponse(jasperReportPdfService.generateProductDetailPdf(data), "detalle_producto_marbetes");
    }

    @PostMapping("/reports/product-detail/all/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getAllProductsDetailReportPdf(@Valid @RequestBody AllWarehousesReportRequestDTO request) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(request.getPeriodId());
        filter.setWarehouseId(null);
        List<ProductDetailReportDTO> data = labelService.getProductDetailReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("detalle de todos los productos", request.getPeriodId(), null);
        return buildPdfResponse(jasperReportPdfService.generateProductDetailPdf(data), "detalle_todos_productos_marbetes");
    }

    @PostMapping("/reports/warehouse-detail/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getWarehouseDetailReportPdf(@Valid @RequestBody ReportFilterDTO filter) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        List<WarehouseDetailReportDTO> data = labelService.getWarehouseDetailReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("detalle por almacén", filter.getPeriodId(), filter.getWarehouseId());
        return buildPdfResponse(jasperReportPdfService.generateWarehouseDetailPdf(data), "detalle_almacen_marbetes");
    }

    @PostMapping("/reports/warehouse-detail/all/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getAllWarehousesDetailReportPdf(@Valid @RequestBody AllWarehousesReportRequestDTO request) {
        Long userId = getUserIdFromToken(); String userRole = getUserRoleFromToken();
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(request.getPeriodId());
        filter.setWarehouseId(null);
        List<WarehouseDetailReportDTO> data = labelService.getWarehouseDetailReport(filter, userId, userRole);
        if (data.isEmpty()) return noDataResponse("detalle de todos los almacenes", request.getPeriodId(), null);
        return buildPdfResponse(jasperReportPdfService.generateWarehouseDetailPdf(data), "detalle_todos_almacenes_marbetes");
    }

    private ResponseEntity<Map<String, Object>> noDataResponse(String reporte, Long periodId, Long warehouseId) {
        String almacen = warehouseId != null ? "almacén " + warehouseId : "todos los almacenes";
        log.warn("⚠️ Sin registros para reporte '{}' — periodo={}, almacén={}", reporte, periodId, almacen);
        return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "No se encontraron registros de " + reporte + " para el periodo " + periodId + " en " + almacen + ".",
                "periodId", periodId,
                "warehouseId", warehouseId != null ? warehouseId : "todos"
        ));
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfBytes, String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename  = String.format("%s_%s.pdf", prefix, timestamp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(pdfBytes.length);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfBytes, Long periodId, Long warehouseId, String prefix) {
        String safePeriodId = String.valueOf(periodId).replaceAll("[^0-9]", "");
        String safeWarehouseId = String.valueOf(warehouseId).replaceAll("[^0-9]", "");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("%s_P%s_A%s_%s.pdf", prefix, safePeriodId, safeWarehouseId, timestamp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(pdfBytes.length);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}

