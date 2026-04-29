package tokai.com.mx.SIGMAV2.modules.labels.adapter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.AuthenticatedUserService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.JasperReportPdfService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.MarbeteQRIntegrationService;
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
    private final MarbeteQRIntegrationService marbeteQRIntegrationService;
    private final tokai.com.mx.SIGMAV2.modules.labels.application.service.CountHistoryQueryService countHistoryQueryService;
    
    private Long getUserIdFromToken() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return authenticatedUserService.getUserIdByEmail(email);
    }

    private String getUserEmailFromToken() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
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
        log.info("📥 CONTROLLER registerCountC1: folio={}, comment='{}' (length={}, isEmpty={})",
            dto.getFolio(), dto.getComment(),
            (dto.getComment() != null ? dto.getComment().length() : "null"),
            (dto.getComment() != null ? dto.getComment().isEmpty() : "null"));
        return ResponseEntity.ok(labelService.registerCountC1(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/counts/c2")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC2(@Valid @RequestBody CountEventDTO dto) {
        log.info("📥 CONTROLLER registerCountC2: folio={}, comment='{}' (length={}, isEmpty={})",
            dto.getFolio(), dto.getComment(),
            (dto.getComment() != null ? dto.getComment().length() : "null"),
            (dto.getComment() != null ? dto.getComment().isEmpty() : "null"));
        return ResponseEntity.ok(labelService.registerCountC2(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PutMapping("/counts/c1/update")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> updateCountC1(@Valid @RequestBody UpdateCountDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("📥 CONTROLLER updateCountC1: folio={}, comment='{}' (length={}, isEmpty={})",
            dto.getFolio(), dto.getComment(),
            (dto.getComment() != null ? dto.getComment().length() : "null"),
            (dto.getComment() != null ? dto.getComment().isEmpty() : "null"));
        return ResponseEntity.ok(labelService.updateCountC1(dto, userId, getUserRoleFromToken()));
    }

    @PutMapping("/counts/c2/update")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> updateCountC2(@Valid @RequestBody UpdateCountDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("📥 CONTROLLER updateCountC2: folio={}, comment='{}' (length={}, isEmpty={})",
            dto.getFolio(), dto.getComment(),
            (dto.getComment() != null ? dto.getComment().length() : "null"),
            (dto.getComment() != null ? dto.getComment().isEmpty() : "null"));
        return ResponseEntity.ok(labelService.updateCountC2(dto, userId, getUserRoleFromToken()));
    }

    @PostMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<LabelSummaryResponseDTO>> getLabelSummary(@RequestBody LabelSummaryRequestDTO dto) {
        Long userId = getUserIdFromToken();
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

    /**
     * 📊 GET /api/sigmav2/labels/reports/with-comments
     * Reporte de Marbetes con Comentarios de Conteos
     * Retorna lista de marbetes con:
     * - Información del producto y almacén
     * - Conteos C1 y C2 con sus comentarios
     * - Análisis de diferencias
     * 
     * Body:
     * {
     *   "periodId": 1,
     *   "warehouseId": 5
     * }
     * 
     * Response: List<LabelWithCommentsReportDTO>
     */
    @PostMapping("/reports/with-comments")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<LabelWithCommentsReportDTO>> getLabelListWithComments(
            @Valid @RequestBody ReportFilterDTO filter) {
        log.info("📊 Reporte de marbetes con comentarios - periodo={}, almacén={}", 
                filter.getPeriodId(), filter.getWarehouseId());
        List<LabelWithCommentsReportDTO> result = labelService.getLabelListWithComments(
                filter, getUserIdFromToken(), getUserRoleFromToken());
        log.info("✅ Reporte generado: {} marbetes", result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 📄 PDF de Marbetes con Comentarios - Almacén Específico
     * POST /api/sigmav2/labels/reports/with-comments/pdf
     * 
     * Genera un PDF con todos los marbetes que tienen comentarios en conteos C1 o C2
     * para un almacén específico en un período determinado.
     * 
     * Body:
     * {
     *   "periodId": 1,
     *   "warehouseId": 5
     * }
     */
    @PostMapping("/reports/with-comments/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<byte[]> generateLabelWithCommentsPdf(
            @Valid @RequestBody ReportFilterDTO filter) {
        log.info("📄 Generando PDF marbetes con comentarios - período={}, almacén={}", 
                filter.getPeriodId(), filter.getWarehouseId());
        
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        
        List<LabelWithCommentsReportDTO> data = labelService.getLabelListWithComments(
                filter, userId, userRole);
        
        if (data.isEmpty()) {
            log.warn("No hay marbetes con comentarios para generar PDF");
            return ResponseEntity.status(404).body(new byte[0]);
        }
        
        byte[] pdfBytes = jasperReportPdfService.generateLabelWithCommentsPdf(data);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment()
                        .filename("Reporte_Marbetes_Comentarios_" + filter.getPeriodId() + ".pdf")
                        .build());
        headers.setContentLength(pdfBytes.length);
        
        log.info("✓ PDF generado: {} KB", pdfBytes.length / 1024);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    /**
     * 📄 PDF de Marbetes con Comentarios - Todos los Almacenes
     * POST /api/sigmav2/labels/reports/with-comments/all/pdf
     * 
     * Genera un PDF con todos los marbetes que tienen comentarios en conteos C1 o C2
     * para TODOS los almacenes en un período determinado.
     * 
     * Body:
     * {
     *   "periodId": 1
     * }
     */
    @PostMapping("/reports/with-comments/all/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<byte[]> generateLabelWithCommentsAllWarehousesPdf(
            @Valid @RequestBody ReportFilterDTO filter) {
        log.info("📄 Generando PDF marbetes con comentarios - TODOS los almacenes, período={}", 
                filter.getPeriodId());
        
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        
        ReportFilterDTO allWarehousesFilter = new ReportFilterDTO();
        allWarehousesFilter.setPeriodId(filter.getPeriodId());
        allWarehousesFilter.setWarehouseId(null);
        
        List<LabelWithCommentsReportDTO> data = labelService.getLabelListWithComments(
                allWarehousesFilter, userId, userRole);
        
        if (data.isEmpty()) {
            log.warn("No hay marbetes con comentarios para generar PDF");
            return ResponseEntity.status(404).body(new byte[0]);
        }
        
        byte[] pdfBytes = jasperReportPdfService.generateLabelWithCommentsPdf(data);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment()
                        .filename("Reporte_Marbetes_Comentarios_TodosAlmacenes_" + filter.getPeriodId() + ".pdf")
                        .build());
        headers.setContentLength(pdfBytes.length);
        
        log.info("✓ PDF generado (todos los almacenes): {} KB", pdfBytes.length / 1024);
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
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

    // ═══════════════════════════════════════════════════════════════════
    // NUEVOS ENDPOINTS: GENERACIÓN DE QR EN MARBETES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 🎯 POST /labels/print-with-qr
     * Genera e imprime marbetes CON códigos QR incrustados en el PDF
     * 
     * Request: { "periodId": 1, "warehouseId": 5, "withQR": true }
     * Response: PDF con todos los marbetes + QR
     */
    @PostMapping("/print-with-qr")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> printLabelsWithQR(@Valid @RequestBody PrintRequestDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        
        log.info("🎯 /print-with-qr: Generando marbetes CON QR - usuario={}, periodo={}, almacén={}", 
                 userId, dto.getPeriodId(), dto.getWarehouseId());
        
        try {
            // Establecer flag withQR
            dto.setWithQR(true);
            
            // Llamar al método unified printLabels() que detecta la rama QR
            byte[] pdfBytes = labelService.printLabels(dto, userId, userRole);

            if (pdfBytes == null || pdfBytes.length == 0) {
                log.warn("⚠️ Error: PDF vacío generado para período={}, almacén={}",
                         dto.getPeriodId(), dto.getWarehouseId());
                return ResponseEntity.status(500).build();
            }

            log.info("✅ PDF con QR generado: {} bytes", pdfBytes.length);

            return buildPdfResponse(pdfBytes, dto.getPeriodId(), dto.getWarehouseId(), "marbetes_con_qr");
            
        } catch (Exception e) {
            log.error("❌ Error al generar PDF con QR: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 🎯 POST /labels/print-specific-with-qr
     * Imprime marbetes específicos CON QR
     * 
     * Request: { 
     *   "folios": [42, 43, 44],
     *   "periodId": 1,
     *   "warehouseId": 5 
     * }
     * Response: PDF solo con esos marbetes + QR
     */
    @PostMapping("/print-specific-with-qr")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> printSpecificLabelsWithQR(
            @RequestBody Map<String, Object> request) {
        
        Long userId = getUserIdFromToken();
        
            try {
            // Parsear request
            List<Long> folios = (List<Long>) request.get("folios");
            Long periodId = ((Number) request.get("periodId")).longValue();
            Long warehouseId = ((Number) request.get("warehouseId")).longValue();
            
            log.info("🎯 /print-specific-with-qr: {} marbetes - usuario={}", folios.size(), userId);
            
            // Generar QR solo para estos folios
            List<MarbeteReportDTO> marbetesConQR = marbeteQRIntegrationService
                .generarMarbetesEspecificosConQR(folios, periodId, warehouseId);

            if (marbetesConQR.isEmpty()) {
                log.warn("⚠️ No se encontraron los folios solicitados");
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = generarPDFConQR(marbetesConQR);
            
            log.info("✅ PDF específico con QR generado: {} marbetes", marbetesConQR.size());
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=marbetes_qr_especificos.pdf")
                    .body(pdfBytes);
            
        } catch (Exception e) {
            log.error("❌ Error al generar PDF específico con QR: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Helper: Genera PDF a partir de lista de DTOs con QR
     * Compila el JRXML on-the-fly si es necesario
     */
    private byte[] generarPDFConQR(List<MarbeteReportDTO> marbetes) {
        try {
            log.info("📄 Generando PDF con QR para {} marbetes", marbetes.size());
            
            if (marbetes == null || marbetes.isEmpty()) {
                log.warn("⚠️ Lista de marbetes vacía o null");
                return new byte[0];
            }
            
            // Validación de datos
            for (int i = 0; i < marbetes.size(); i++) {
                MarbeteReportDTO dto = marbetes.get(i);
                log.debug("Marbete grupo {}: nom1={}, clave1={}, qr1={}", 
                    i, dto.getNomMarbete1(), dto.getClave1(), dto.getQrImage1() != null ? "✅" : "❌");
            }
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            
            // Parámetros base
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("fecha", java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy")));
            
            try {
                String reportDir = new ClassPathResource("reports/").getURL().toString();
                params.put("REPORT_DIR", reportDir);
                log.debug("REPORT_DIR configurado: {}", reportDir);
            } catch (Exception ex) {
                log.warn("No se pudo resolver REPORT_DIR: {}", ex.getMessage());
                params.put("REPORT_DIR", "");
            }
            
            // Intenta cargar .jasper (compilado), si no existe compila el .jrxml
            net.sf.jasperreports.engine.JasperReport jasperReport = null;
            
            try {
                // Intentar cargar .jasper precompilado
                java.io.InputStream jasperStream = new ClassPathResource("reports/marbete_qr.jasper").getInputStream();
                jasperReport = (net.sf.jasperreports.engine.JasperReport) 
                    net.sf.jasperreports.engine.util.JRLoader.loadObject(jasperStream);
                log.info("✅ Plantilla .jasper cargada");
            } catch (java.io.FileNotFoundException e) {
                // Si no existe .jasper, compilar .jrxml
                log.info("📋 Compilando JRXML on-the-fly...");
                java.io.InputStream jrxmlStream = new ClassPathResource("reports/marbete_qr.jrxml").getInputStream();
                jasperReport = net.sf.jasperreports.engine.JasperCompileManager.compileReport(jrxmlStream);
                log.info("✅ JRXML compilado exitosamente");
            }
            
            if (jasperReport == null) {
                throw new RuntimeException("No se pudo cargar la plantilla de jasper");
            }
            
            // Data source
            net.sf.jasperreports.engine.data.JRBeanCollectionDataSource dataSource = 
                new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(marbetes);
            
            log.info("📊 DataSource creado con {} registros", marbetes.size());
            
            // Llenar reporte
            net.sf.jasperreports.engine.JasperPrint jasperPrint = 
                net.sf.jasperreports.engine.JasperFillManager.fillReport(jasperReport, params, dataSource);
            
            log.info("📑 Reporte llenado: {} páginas", jasperPrint.getPages().size());
            
            // Exportar a PDF
            net.sf.jasperreports.engine.export.JRPdfExporter exporter = 
                new net.sf.jasperreports.engine.export.JRPdfExporter();
            exporter.setExporterInput(
                new net.sf.jasperreports.export.SimpleExporterInput(jasperPrint)
            );
            exporter.setExporterOutput(
                new net.sf.jasperreports.export.SimpleOutputStreamExporterOutput(baos)
            );
            
            net.sf.jasperreports.export.SimplePdfExporterConfiguration config = 
                new net.sf.jasperreports.export.SimplePdfExporterConfiguration();
            config.setCompressed(false); // ⚠️ Desactivar compresión para debugging
            exporter.setConfiguration(config);
            exporter.exportReport();
            
            byte[] pdfBytes = baos.toByteArray();
            log.info("✅ PDF con QR generado exitosamente: {} bytes", pdfBytes.length);
            
            if (pdfBytes.length == 0) {
                log.error("❌ PDF generado pero está vacío (0 bytes)");
                throw new RuntimeException("PDF generado pero está vacío");
            }
            
            return pdfBytes;
            
        } catch (Exception e) {
            log.error("❌ Error generando PDF con QR: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF con QR: " + e.getMessage(), e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HISTORIAL DE CONTEOS — Endpoints para consultar registro de usuarios
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene el historial de conteos registrados por el usuario autenticado
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 20)
     * @return Página con el historial de conteos del usuario
     */
    @GetMapping("/history/my-counts")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getMyCountHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Long userId = getUserIdFromToken();
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            var historial = countHistoryQueryService.getCountHistoryByUserId(userId, pageable);
            log.info("Historial de conteos obtenido para usuario {}: {} registros", userId, historial.getTotalElements());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Historial de conteos del usuario",
                "data", historial.getContent(),
                "totalElements", historial.getTotalElements(),
                "totalPages", historial.getTotalPages(),
                "currentPage", historial.getNumber()
            ));
        } catch (Exception e) {
            log.error("Error al obtener historial de conteos del usuario", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error al obtener historial: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene el historial de conteos de un usuario específico (solo administrador)
     * @param userId ID del usuario
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 20)
     * @return Página con el historial de conteos del usuario
     */
    @GetMapping("/history/user/{userId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> getUserCountHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            var historial = countHistoryQueryService.getCountHistoryByUserId(userId, pageable);
            log.info("Historial de conteos obtenido para usuario {}: {} registros", userId, historial.getTotalElements());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Historial de conteos del usuario " + userId,
                "data", historial.getContent(),
                "totalElements", historial.getTotalElements(),
                "totalPages", historial.getTotalPages(),
                "currentPage", historial.getNumber()
            ));
        } catch (Exception e) {
            log.error("Error al obtener historial de conteos para usuario {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error al obtener historial: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene el historial de conteos de un almacén específico
     * @param warehouseId ID del almacén
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 20)
     * @return Página con el historial de conteos del almacén
     */
    @GetMapping("/history/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA')")
    public ResponseEntity<?> getWarehouseCountHistory(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            var historial = countHistoryQueryService.getCountHistoryByWarehouse(warehouseId, pageable);
            log.info("Historial de conteos obtenido para almacén {}: {} registros", warehouseId, historial.getTotalElements());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Historial de conteos del almacén " + warehouseId,
                "data", historial.getContent(),
                "totalElements", historial.getTotalElements(),
                "totalPages", historial.getTotalPages(),
                "currentPage", historial.getNumber()
            ));
        } catch (Exception e) {
            log.error("Error al obtener historial de conteos para almacén {}", warehouseId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error al obtener historial: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene el historial de conteos de un período específico
     * @param periodId ID del período
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 20)
     * @return Página con el historial de conteos del período
     */
    @GetMapping("/history/period/{periodId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getPeriodCountHistory(
            @PathVariable Long periodId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            var historial = countHistoryQueryService.getCountHistoryByPeriod(periodId, pageable);
            log.info("Historial de conteos obtenido para período {}: {} registros", periodId, historial.getTotalElements());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Historial de conteos del período " + periodId,
                "data", historial.getContent(),
                "totalElements", historial.getTotalElements(),
                "totalPages", historial.getTotalPages(),
                "currentPage", historial.getNumber()
            ));
        } catch (Exception e) {
            log.error("Error al obtener historial de conteos para período {}", periodId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error al obtener historial: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene el historial completo de un folio específico
     * @param folio Folio del marbete
     * @return Lista con el historial completo del folio (C1, C2, actualizaciones)
     */
    @GetMapping("/history/folio/{folio}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getFolioCountHistory(@PathVariable Long folio) {
        try {
            var historial = countHistoryQueryService.getCountHistoryByFolio(folio);
            log.info("Historial de conteos obtenido para folio {}: {} registros", folio, historial.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Historial completo del folio " + folio,
                "data", historial,
                "totalRecords", historial.size()
            ));
        } catch (Exception e) {
            log.error("Error al obtener historial de conteos para folio {}", folio, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error al obtener historial: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene el historial de conteos de un usuario en un período específico
     * @param userId ID del usuario
     * @param periodId ID del período
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 20)
     * @return Página con el historial filtrado
     */
    @GetMapping("/history/user/{userId}/period/{periodId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA')")
    public ResponseEntity<?> getUserPeriodCountHistory(
            @PathVariable Long userId,
            @PathVariable Long periodId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            var historial = countHistoryQueryService.getCountHistoryByUserAndPeriod(userId, periodId, pageable);
            Long totalConteos = countHistoryQueryService.countUserContosInPeriod(userId, periodId);
            
            log.info("Historial de conteos obtenido para usuario {} en período {}: {} registros", 
                    userId, periodId, historial.getTotalElements());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Historial de conteos del usuario " + userId + " en período " + periodId,
                "data", historial.getContent(),
                "totalElements", historial.getTotalElements(),
                "totalPages", historial.getTotalPages(),
                "currentPage", historial.getNumber(),
                "totalConteosRegistrados", totalConteos
            ));
        } catch (Exception e) {
            log.error("Error al obtener historial de conteos para usuario {} en período {}", userId, periodId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error al obtener historial: " + e.getMessage()
            ));
        }
    }

    /**
     * Obtiene TODOS los conteos registrados de todos los almacenes
     * Endpoint para ver el historial completo de conteos de toda la organización
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 20)
     * @return Página con todos los conteos ordenados por fecha descendente (más recientes primero)
     */
    @GetMapping("/history/all")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR')")
    public ResponseEntity<?> getAllCountsHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            var historial = countHistoryQueryService.getAllCounts(pageable);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Historial completo de TODOS los conteos registrados en la organización",
                "data", historial.getContent(),
                "totalElements", historial.getTotalElements(),
                "totalPages", historial.getTotalPages(),
                "currentPage", historial.getNumber(),
                "pageSize", size
            ));
        } catch (Exception e) {
            log.error("Error al obtener historial completo de conteos", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error al obtener historial: " + e.getMessage()
            ));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NUEVOS ENDPOINTS: CONSULTAR Y REIMPRIMIR SIMPLE MARBETES IMPRESOS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * 🔍 GET /labels/{folio}/pdf
     * Obtiene el PDF de un marbete ya impreso para visualizar/descargar
     * Solo muestra PDF sin cambiar estados
     * 
     * @param folio ID del folio del marbete
     * @param periodId ID del peri
     *                 eodo
     * @return PDF del marbete
     */
    @GetMapping("/{folio}/pdf")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> getPrintedLabelPdf(@PathVariable Long folio, @RequestParam Long periodId) {
        Long userId = getUserIdFromToken();
        log.info("📄 /labels/{folio}/pdf: Obteniendo PDF - folio={}, periodo={}, usuario={}", folio, periodId, userId);
        
        try {
            byte[] pdfBytes = labelService.getPrintedLabelPdf(folio, periodId, userId, getUserRoleFromToken());
            
            String filename = String.format("marbete_folio_%d.pdf", folio);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment()
                            .filename(filename).build());
            headers.setContentLength(pdfBytes.length);
            
            log.info("✅ PDF obtenido exitosamente: folio={}, tamaño={} bytes", folio, pdfBytes.length);
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener PDF del folio {}: {}", folio, e.getMessage());
            return ResponseEntity.status(404).build();
        }
    }

    /**
     * 🔄 POST /labels/{folio}/reprint-simple
     * Reimprimir un marbete que ya está impreso
     * SOLO actualiza timestamp de reimpresión, NO cambia estado
     * 
     * Request: (sin body)
     * Response: PDF reimprimido
     * 
     * @param folio ID del folio del marbete
     * @param periodId ID del periodo
     * @return PDF reimprimido
     */
    @PostMapping("/{folio}/reprint-simple")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> reprintSimple(@PathVariable Long folio, @RequestParam Long periodId) {
        Long userId = getUserIdFromToken();
        log.info("🔄 /labels/{folio}/reprint-simple: Reimprimiendo - folio={}, periodo={}, usuario={}", folio, periodId, userId);
        
        try {
            byte[] pdfBytes = labelService.reprintSimple(folio, periodId, userId, getUserRoleFromToken());
            
            String filename = String.format("marbete_folio_%d_REIMPRESION_%d.pdf", folio, System.currentTimeMillis());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment()
                            .filename(filename).build());
            headers.setContentLength(pdfBytes.length);
            
            log.info("✅ Reimpresión completada: folio={}, tamaño={} bytes", folio, pdfBytes.length);
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
            
        } catch (Exception e) {
            log.error("❌ Error al reimprimir folio {}: {}", folio, e.getMessage());
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * 📋 GET /labels/{folio}/full-info
     * Obtiene TODA la información de un marbete en un solo endpoint
     * Incluye: datos del marbete, usuario creador, producto, almacén, período,
     * existencias, conteos (C1 y C2), historial de conteos, impresiones,
     * reimpresiones, cancelaciones, reactivaciones, solicitud de folios, etc.
     * 
     * @param folio ID del folio del marbete
     * @param periodId ID del periodo (requerido para buscar el marbete)
     * @return DTO con TODA la información del marbete
     */
    @GetMapping("/{folio}/full-info")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getLabelFullInfo(@PathVariable Long folio, @RequestParam Long periodId) {
        Long userId = getUserIdFromToken();
        log.info("📋 /labels/{folio}/full-info: Obteniendo información completa - folio={}, periodo={}, usuario={}", folio, periodId, userId);
        
        try {
            LabelFullDetailDTO fullDetail = labelService.getLabelFullDetail(folio, periodId, userId, getUserRoleFromToken());
            
            log.info("✅ Información completa obtenida: folio={}, estado={}, conteos={}/{}", 
                    folio, fullDetail.getEstado(),
                    fullDetail.getConteo1Valor() != null ? "C1" : "",
                    fullDetail.getConteo2Valor() != null ? "C2" : "");
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Información completa del marbete folio " + folio,
                    "data", fullDetail,
                    "timestamp", LocalDateTime.now()
            ));
            
        } catch (LabelNotFoundException e) {
            log.warn("❌ Marbete no encontrado: folio={}", folio);
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Marbete no encontrado",
                    "message", e.getMessage(),
                    "folio", folio
            ));
        } catch (Exception e) {
            log.error("❌ Error al obtener información completa del folio {}: {}", folio, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error interno del servidor",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 📋 GET /labels/selected-info
     * Consulta información de marbetes específicos (por folios)
     * El usuario proporciona los folios y recibe la información de cada uno
     * 
     * Query Parameters:
     *   - folios: Lista de folios separados por coma (ej: 1,2,3)
     *   - periodId: ID del período
     *   - warehouseId: ID del almacén (OPCIONAL - si no se proporciona, se autodetecta)
     * 
     * Response: [
     *   {
     *     "folio": 1,
     *     "claveProducto": "PROD-001",
     *     "nombreProducto": "Producto 1",
     *     "warehouseId": 5,        ← AHORA SIEMPRE RETORNA
     *     "claveAlmacen": "ALM-01",
     *     "nombreAlmacen": "Almacén 1",
     *     "conteo1Valor": 100,
     *     "conteo2Valor": 105,
     *     "diferencia": 5,
     *     "statusConteo": "COMPLETO",
     *     "mensaje": "Marbete 1 de 3"
     *   },
     *   ...
     * ]
     */
    @GetMapping("/selected-info")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getSelectedLabelsInfo(
            @RequestParam String folios,
            @RequestParam Long periodId,
            @RequestParam(required = false) Long warehouseId) {
        
        Long userId = getUserIdFromToken();
        log.info("📋 /labels/selected-info: Consultando información - usuario={}, folios={}, warehouseId={}", 
                userId, folios, warehouseId);
        
        try {
            // Parsear folios (pueden venir como "1,2,3")
            java.util.List<Long> folioList = java.util.Arrays.stream(folios.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());
            
            // Si no se proporciona warehouseId, pasar null para que el servicio lo autodetecte
            java.util.List<LabelDetailForPrintDTO> results = labelService.getSelectedLabelsInfo(
                    folioList, periodId, warehouseId, userId, getUserRoleFromToken());
            
            log.info("✅ Información consultada: {} marbetes", results.size());
            
            // Detectar almacenes únicos encontrados
            java.util.Set<Long> warehouseIds = new java.util.HashSet<>();
            java.util.Map<Long, String> warehouseNames = new java.util.HashMap<>();
            for (LabelDetailForPrintDTO result : results) {
                if (result.getWarehouseId() != null) {
                    warehouseIds.add(result.getWarehouseId());
                    if (result.getNombreAlmacen() != null) {
                        warehouseNames.put(result.getWarehouseId(), result.getNombreAlmacen());
                    }
                }
            }
            
            // Respuesta mejorada con información de almacén
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Información de marbetes consultada");
            response.put("data", results);
            response.put("total", results.size());
            
            // Si todos los marbetes están en un mismo almacén, indicarlo
            if (warehouseIds.size() == 1) {
                Long detectedWarehouse = warehouseIds.iterator().next();
                response.put("autoDetectedWarehouse", detectedWarehouse);
                response.put("autoDetectedWarehouseName", warehouseNames.getOrDefault(detectedWarehouse, ""));
            } else if (!warehouseIds.isEmpty()) {
                // Múltiples almacenes - retornar mapa de almacenes encontrados
                response.put("warehouses", warehouseNames);
            }
            
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("❌ Error en parámetros: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Parámetros inválidos",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error al consultar información: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error interno del servidor",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 🖨️ POST /labels/print-selected
     * Imprime marbetes específicos con su información completa
     * 
     * Request: {
     *   "folios": [1, 2, 3],
     *   "periodId": 1,
     *   "warehouseId": 5,
     *   "infoType": "BASICA"
     * }
     * 
     * Response: PDF binario con los marbetes imprimidos
     */
    @PostMapping("/print-selected")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> printSelectedLabels(
            @RequestBody PrintSelectedLabelsRequestDTO request) {
        
        Long userId = getUserIdFromToken();
        log.info("🖨️ /labels/print-selected: Imprimiendo {} marbetes - usuario={}", 
                request.getFolios().size(), userId);
        
        byte[] pdfBytes = labelService.printSelectedLabelsWithInfo(request, userId, getUserRoleFromToken());
        
        String filename = String.format("marbetes_seleccionados_%d.pdf", System.currentTimeMillis());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                org.springframework.http.ContentDisposition.attachment()
                        .filename(filename).build());
        headers.setContentLength(pdfBytes.length);
        
        log.info("✅ Impresión completada: {} marbetes, {} KB", 
                request.getFolios().size(), pdfBytes.length / 1024);
        
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    /**
     * 🖨️ POST /labels/print-selected-auto
     * Imprime marbetes específicos SIN requerir warehouseId
     * Detecta automáticamente el almacén de cada marbete
     * PERFECTO para imprimir marbetes de múltiples almacenes en una sola solicitud
     * 
     * Request: {
     *   "folios": [1, 142, 200],
     *   "periodId": 1,
     *   "infoType": "BASICA"
     * }
     * 
     * Response: PDF binario con los marbetes imprimidos (múltiples almacenes si es necesario)
     */
    @PostMapping("/print-selected-auto")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> printSelectedLabelsAutoWarehouse(
            @Valid @RequestBody PrintSelectedLabelsAutoWarehouseDTO request) {
        
        Long userId = getUserIdFromToken();
        log.info("🖨️ /labels/print-selected-auto: Imprimiendo {} marbetes (autodetección) - usuario={}", 
                request.getFolios().size(), userId);
        
        try {
            byte[] pdfBytes = labelService.printSelectedLabelsAutoWarehouse(request, userId, getUserRoleFromToken());
            
            String filename = String.format("marbetes_seleccionados_%d.pdf", System.currentTimeMillis());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment()
                            .filename(filename).build());
            headers.setContentLength(pdfBytes.length);
            
            log.info("✅ Impresión completada: {} marbetes, {} KB", 
                    request.getFolios().size(), pdfBytes.length / 1024);
            
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
            
        } catch (IllegalArgumentException e) {
            log.warn("❌ Error en parámetros: {}", e.getMessage());
            return ResponseEntity.status(400).build();
        } catch (Exception e) {
            log.error("❌ Error al imprimir marbetes: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 🎯 POST /labels/print-selected-with-qr
     * Imprime marbetes específicos CON QR (similar a print-selected pero con códigos QR)
     * 
     * Request: {
     *   "folios": [1, 142, 200],
     *   "periodId": 1,
     *   "warehouseId": 40
     * }
     * 
     * Response: PDF binario con los marbetes y sus códigos QR incluidos
     */
    @PostMapping("/print-selected-with-qr")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> printSelectedLabelsWithQR(
            @RequestBody PrintSelectedLabelsRequestDTO request) {
        
        Long userId = getUserIdFromToken();
        log.info("🎯 /labels/print-selected-with-qr: Imprimiendo {} marbetes CON QR - usuario={}", 
                request.getFolios().size(), userId);
        
        try {
            byte[] pdfBytes = labelService.printSelectedLabelsWithQR(request, userId, getUserRoleFromToken());
            
            String filename = String.format("marbetes_qr_seleccionados_%d.pdf", System.currentTimeMillis());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition.attachment()
                            .filename(filename).build());
            headers.setContentLength(pdfBytes.length);
            
            log.info("✅ Impresión CON QR completada: {} marbetes, {} KB", 
                    request.getFolios().size(), pdfBytes.length / 1024);
            
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
            
        } catch (IllegalArgumentException e) {
            log.warn("❌ Error en parámetros: {}", e.getMessage());
            return ResponseEntity.status(400).build();
        } catch (Exception e) {
            log.error("❌ Error al imprimir marbetes con QR: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/full-list")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<?> getLabelFullList(
            @RequestParam(required = false) Long periodId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Boolean impreso,
            @RequestParam(required = false) Boolean conteoCompleto,
            @RequestParam(required = false) Boolean cancelado,
            @RequestParam(required = false) String searchText,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "folio") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection
    ) {
        Long userId = getUserIdFromToken();
        log.info("📊 /labels/full-list: Obteniendo lista completa - usuario={}, filtros: periodo={}, almacen={}, estado={}", 
                userId, periodId, warehouseId, estado);
        
        try {
            // Construir DTO con los parámetros
            LabelListFilterDTO filter = LabelListFilterDTO.builder()
                    .periodId(periodId)
                    .warehouseId(warehouseId)
                    .productId(productId)
                    .estado(estado)
                    .impreso(impreso)
                    .conteoCompleto(conteoCompleto)
                    .cancelado(cancelado)
                    .searchText(searchText)
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();
            
            // Validar parámetros de paginación
            if (filter.getPage() < 0) filter.setPage(0);
            if (filter.getSize() <= 0) filter.setSize(20);
            if (filter.getSize() > 100) filter.setSize(100); // Máximo 100 por página
            
            org.springframework.data.domain.Page<LabelFullDetailDTO> result = 
                    labelService.getLabelFullDetailList(filter, userId, getUserRoleFromToken());
            
            log.info("✅ Lista obtenida: {} totales, {} en página {}/{}", 
                    result.getTotalElements(), result.getContent().size(), 
                    filter.getPage() + 1, result.getTotalPages());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lista de marbetes con información completa",
                    "data", result.getContent(),
                    "pagination", Map.of(
                            "totalElements", result.getTotalElements(),
                            "totalPages", result.getTotalPages(),
                            "currentPage", result.getNumber(),
                            "pageSize", result.getSize(),
                            "hasNext", result.hasNext(),
                            "hasPrevious", result.hasPrevious()
                    ),
                    "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("❌ Error al obtener lista de marbetes: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Error interno del servidor",
                    "message", e.getMessage()
            ));
        }
    }
}

