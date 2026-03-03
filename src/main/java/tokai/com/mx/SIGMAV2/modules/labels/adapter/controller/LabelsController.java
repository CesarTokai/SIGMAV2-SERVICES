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
import tokai.com.mx.SIGMAV2.modules.labels.application.service.AuthenticatedUserService;
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

    /** Extrae el ID del usuario autenticado desde el token JWT. */
    private Long getUserIdFromToken() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return authenticatedUserService.getUserIdByEmail(email);
    }

    /** Extrae el rol del usuario autenticado desde el token JWT. */
    private String getUserRoleFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    // ── Solicitar folios (deprecado) ─────────────────────────────────────
    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Void> requestLabels(@Valid @RequestBody LabelRequestDTO dto) {
        labelService.requestLabels(dto, getUserIdFromToken(), getUserRoleFromToken());
        return ResponseEntity.status(201).build();
    }

    // ── Generar marbetes (deprecado) ─────────────────────────────────────
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<GenerateBatchResponseDTO> generateBatch(@Valid @RequestBody GenerateBatchDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        log.info("Generando marbetes para usuario {} con rol {}", userId, userRole);
        return ResponseEntity.ok(labelService.generateBatch(dto, userId, userRole));
    }

    // ── Imprimir marbetes ────────────────────────────────────────────────
    @PostMapping("/print")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> printLabels(@Valid @RequestBody PrintRequestDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        log.info("Imprimiendo marbetes: usuario={}, periodo={}, almacén={}", userId, dto.getPeriodId(), dto.getWarehouseId());
        byte[] pdfBytes = labelService.printLabels(dto, userId, userRole);
        return buildPdfResponse(pdfBytes, dto.getPeriodId(), dto.getWarehouseId(), "marbetes");
    }

    // ── Contar marbetes pendientes ───────────────────────────────────────
    @PostMapping("/pending-print-count")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<PendingPrintCountResponseDTO> getPendingPrintCount(
            @Valid @RequestBody PendingPrintCountRequestDTO dto) {
        return ResponseEntity.ok(labelService.getPendingPrintCount(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    // ── Registrar Conteo C1 ──────────────────────────────────────────────
    @PostMapping("/counts/c1")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC1(@Valid @RequestBody CountEventDTO dto) {
        return ResponseEntity.ok(labelService.registerCountC1(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    // ── Registrar Conteo C2 ──────────────────────────────────────────────
    @PostMapping("/counts/c2")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC2(@Valid @RequestBody CountEventDTO dto) {
        return ResponseEntity.ok(labelService.registerCountC2(dto, getUserIdFromToken(), getUserRoleFromToken()));
    }

    // ── Actualizar Conteo C1 ─────────────────────────────────────────────
    @PutMapping("/counts/c1")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> updateCountC1(@Valid @RequestBody UpdateCountDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("Actualizando C1: folio={}, usuario={}", dto.getFolio(), userId);
        return ResponseEntity.ok(labelService.updateCountC1(dto, userId, getUserRoleFromToken()));
    }

    // ── Actualizar Conteo C2 ─────────────────────────────────────────────
    @PutMapping("/counts/c2")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> updateCountC2(@Valid @RequestBody UpdateCountDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("Actualizando C2: folio={}, usuario={}", dto.getFolio(), userId);
        return ResponseEntity.ok(labelService.updateCountC2(dto, userId, getUserRoleFromToken()));
    }

    // ── Resumen de marbetes ──────────────────────────────────────────────
    @PostMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<LabelSummaryResponseDTO>> getLabelSummary(@RequestBody LabelSummaryRequestDTO dto) {
        Long userId = getUserIdFromToken();
        log.info("POST /summary: periodId={}, warehouseId={}", dto.getPeriodId(), dto.getWarehouseId());
        return ResponseEntity.ok(labelService.getLabelSummary(dto, userId, getUserRoleFromToken()));
    }

    // ── Generar marbetes para lista de productos ─────────────────────────
    @PostMapping("/generate/batch")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Void> generateBatchList(@Valid @RequestBody GenerateBatchListDTO dto) {
        labelService.generateBatchList(dto, getUserIdFromToken(), getUserRoleFromToken());
        return ResponseEntity.ok().build();
    }

    // ── Generar e Imprimir en un solo paso ───────────────────────────────
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

    // ── Estado de marbete ────────────────────────────────────────────────
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

    // ── Marbetes cancelados ──────────────────────────────────────────────
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

    // ── Detalle por producto ─────────────────────────────────────────────
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

    // ── Cancelar marbete ─────────────────────────────────────────────────
    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<Void> cancelLabel(@Valid @RequestBody CancelLabelRequestDTO dto) {
        log.info("Cancelando folio={}", dto.getFolio());
        labelService.cancelLabel(dto, getUserIdFromToken(), getUserRoleFromToken());
        return ResponseEntity.ok().build();
    }

    // ── Consulta para conteo ─────────────────────────────────────────────
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

    // ── Reportes ─────────────────────────────────────────────────────────
    @PostMapping("/reports/distribution")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.DistributionReportDTO>> getDistributionReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getDistributionReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/list")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.LabelListReportDTO>> getLabelListReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getLabelListReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/pending")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.PendingLabelsReportDTO>> getPendingLabelsReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getPendingLabelsReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/with-differences")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.DifferencesReportDTO>> getDifferencesReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getDifferencesReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/cancelled")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.CancelledLabelsReportDTO>> getCancelledLabelsReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getCancelledLabelsReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/comparative")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ComparativeReportDTO>> getComparativeReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getComparativeReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/warehouse-detail")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.WarehouseDetailReportDTO>> getWarehouseDetailReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getWarehouseDetailReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    @PostMapping("/reports/product-detail")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ProductDetailReportDTO>> getProductDetailReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        return ResponseEntity.ok(labelService.getProductDetailReport(filter, getUserIdFromToken(), getUserRoleFromToken()));
    }

    // ── Generar archivo TXT (descarga directa) ───────────────────────────
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

    // ── Reimpresión Extraordinaria ───────────────────────────────────────
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

    // ── Helper: construir ResponseEntity PDF ────────────────────────────
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
