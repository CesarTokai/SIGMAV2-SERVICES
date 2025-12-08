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
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.PrintRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelSummaryRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelSummaryResponseDTO;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/sigmav2/labels")
@RequiredArgsConstructor
public class LabelsController {

    private static final Logger log = LoggerFactory.getLogger(LabelsController.class);

    private final LabelService labelService;
    private final JpaUserRepository userRepository;

    /**
     * Extrae el ID del usuario autenticado desde el token JWT
     */
    private Long getUserIdFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // El token contiene el email
        log.debug("Obteniendo ID de usuario para email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email))
                .getId();
    }

    /**
     * Extrae el rol del usuario autenticado desde el token JWT
     */
    private String getUserRoleFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    // Solicitar folios (crear LabelRequest)
    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Void> requestLabels(@Valid @RequestBody LabelRequestDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        labelService.requestLabels(dto, userId, userRole);
        return ResponseEntity.status(201).build();
    }

    // Generar marbetes a partir de una solicitud
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchResponseDTO> generateBatch(@Valid @RequestBody GenerateBatchDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        log.info("Generando marbetes para usuario {} con rol {}", userId, userRole);

        tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchResponseDTO response =
            labelService.generateBatch(dto, userId, userRole);

        log.info("Generación completada: {} total, {} con existencias, {} sin existencias",
            response.getTotalGenerados(), response.getGeneradosConExistencias(), response.getGeneradosSinExistencias());

        return ResponseEntity.ok(response);
    }

    // Imprimir / Reimprimir rango de marbetes
    @PostMapping("/print")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<byte[]> printLabels(@Valid @RequestBody PrintRequestDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Endpoint /print llamado por usuario {} con rol {}", userId, userRole);

        // Generar el PDF
        byte[] pdfBytes = labelService.printLabels(dto, userId, userRole);

        // Construir nombre del archivo
        String filename = String.format("marbetes_%d_%d.pdf", dto.getStartFolio(), dto.getEndFolio());

        // Configurar headers para descarga del PDF
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        log.info("Retornando PDF de {} KB", pdfBytes.length / 1024);

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
    }

    // Registrar Conteo C1
    @PostMapping("/counts/c1")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC1(@Valid @RequestBody CountEventDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        LabelCountEvent ev = labelService.registerCountC1(dto, userId, userRole);
        return ResponseEntity.ok(ev);
    }

    // Registrar Conteo C2
    @PostMapping("/counts/c2")
    @PreAuthorize("hasRole('AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC2(@Valid @RequestBody CountEventDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        LabelCountEvent ev = labelService.registerCountC2(dto, userId, userRole);
        return ResponseEntity.ok(ev);
    }

    // Resumen de marbetes por periodo y almacén - ENDPOINT DE PRUEBA
    @PostMapping(value = "/summary-test", consumes = "*/*")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<String> getLabelSummaryTest(@RequestBody(required = false) String rawBody) {
        log.info("POST /summary-test - Raw body received: {}", rawBody);
        log.info("Headers: {}", org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes());
        return ResponseEntity.ok("Petición recibida correctamente. Body: " + rawBody);
    }

    // Resumen de marbetes por periodo y almacén
    @PostMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<LabelSummaryResponseDTO>> getLabelSummary(@RequestBody LabelSummaryRequestDTO dto) {
        log.info("POST /summary - Request received: periodId={}, warehouseId={}", dto.getPeriodId(), dto.getWarehouseId());
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        log.info("User authenticated: userId={}, userRole={}", userId, userRole);
        List<LabelSummaryResponseDTO> summary = labelService.getLabelSummary(dto, userId, userRole);
        log.info("Returning {} items", summary.size());
        return ResponseEntity.ok(summary);
    }

    // Generar marbetes para múltiples productos
    @PostMapping("/generate/batch")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<?> generateBatchList(@Valid @RequestBody GenerateBatchListDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        labelService.generateBatchList(dto, userId, userRole);
        return ResponseEntity.ok().build();
    }

    // Consultar estado de marbete
    @GetMapping("/status")
    public ResponseEntity<?> getLabelStatus(@RequestParam Long folio,
                                            @RequestParam Long periodId,
                                            @RequestParam Long warehouseId) {
        Long userId = getUserIdFromToken();
        // El rol puede ser útil para validaciones futuras
        String userRole = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            userRole = auth.getAuthorities().iterator().next().getAuthority();
        }
        var status = labelService.getLabelStatus(folio, periodId, warehouseId, userId, userRole);
        return ResponseEntity.ok(status);
    }

    // Endpoint de diagnóstico para verificar marbetes generados
    @GetMapping("/debug/count")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<?> getLabelsCount(@RequestParam Long periodId,
                                            @RequestParam Long warehouseId) {
        log.info("Endpoint /debug/count llamado para periodId={}, warehouseId={}", periodId, warehouseId);
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        // Consultar directamente cuántos marbetes existen
        long count = labelService.countLabelsByPeriodAndWarehouse(periodId, warehouseId);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("periodId", periodId);
        response.put("warehouseId", warehouseId);
        response.put("totalLabels", count);
        response.put("userId", userId);
        response.put("userRole", userRole);

        log.info("Total de marbetes encontrados: {}", count);
        return ResponseEntity.ok(response);
    }

    // Consultar marbetes cancelados
    @GetMapping("/cancelled")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO>> getCancelledLabels(
            @RequestParam Long periodId,
            @RequestParam Long warehouseId) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Consultando marbetes cancelados para periodId={}, warehouseId={}, userId={}",
            periodId, warehouseId, userId);

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO> cancelledLabels =
            labelService.getCancelledLabels(periodId, warehouseId, userId, userRole);

        return ResponseEntity.ok(cancelledLabels);
    }

    // Actualizar existencias de marbete cancelado
    @PutMapping("/cancelled/update-stock")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO> updateCancelledStock(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCancelledStockDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Actualizando existencias de marbete cancelado folio={}, userId={}",
            dto.getFolio(), userId);

        tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO updated =
            labelService.updateCancelledStock(dto, userId, userRole);

        return ResponseEntity.ok(updated);
    }

    // Obtener detalles de marbetes de un producto
    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelDetailDTO>> getLabelsByProduct(
            @PathVariable Long productId,
            @RequestParam Long periodId,
            @RequestParam Long warehouseId) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Consultando marbetes del producto {} en periodo {} y almacén {}",
            productId, periodId, warehouseId);

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelDetailDTO> labels =
            labelService.getLabelsByProduct(productId, periodId, warehouseId, userId, userRole);

        return ResponseEntity.ok(labels);
    }

    // Cancelar un marbete
    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<Void> cancelLabel(@Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.CancelLabelRequestDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Cancelando marbete folio {} por usuario {} con rol {}", dto.getFolio(), userId, userRole);

        labelService.cancelLabel(dto, userId, userRole);

        return ResponseEntity.ok().build();
    }

    // ==================== REPORTES ====================

    // Reporte de Distribución de Marbetes
    @PostMapping("/reports/distribution")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.DistributionReportDTO>> getDistributionReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Generando reporte de distribución para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.DistributionReportDTO> report =
            labelService.getDistributionReport(filter, userId, userRole);

        return ResponseEntity.ok(report);
    }

    // Reporte de Listado de Marbetes
    @PostMapping("/reports/list")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.LabelListReportDTO>> getLabelListReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Generando reporte de listado para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.LabelListReportDTO> report =
            labelService.getLabelListReport(filter, userId, userRole);

        return ResponseEntity.ok(report);
    }

    // Reporte de Marbetes Pendientes
    @PostMapping("/reports/pending")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.PendingLabelsReportDTO>> getPendingLabelsReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Generando reporte de marbetes pendientes para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.PendingLabelsReportDTO> report =
            labelService.getPendingLabelsReport(filter, userId, userRole);

        return ResponseEntity.ok(report);
    }

    // Reporte de Marbetes con Diferencias
    @PostMapping("/reports/with-differences")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.DifferencesReportDTO>> getDifferencesReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Generando reporte de marbetes con diferencias para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.DifferencesReportDTO> report =
            labelService.getDifferencesReport(filter, userId, userRole);

        return ResponseEntity.ok(report);
    }

    // Reporte de Marbetes Cancelados
    @PostMapping("/reports/cancelled")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.CancelledLabelsReportDTO>> getCancelledLabelsReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Generando reporte de marbetes cancelados para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.CancelledLabelsReportDTO> report =
            labelService.getCancelledLabelsReport(filter, userId, userRole);

        return ResponseEntity.ok(report);
    }

    // Reporte Comparativo
    @PostMapping("/reports/comparative")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ComparativeReportDTO>> getComparativeReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Generando reporte comparativo para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ComparativeReportDTO> report =
            labelService.getComparativeReport(filter, userId, userRole);

        return ResponseEntity.ok(report);
    }

    // Reporte de Almacén con Detalle
    @PostMapping("/reports/warehouse-detail")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.WarehouseDetailReportDTO>> getWarehouseDetailReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Generando reporte de almacén con detalle para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.WarehouseDetailReportDTO> report =
            labelService.getWarehouseDetailReport(filter, userId, userRole);

        return ResponseEntity.ok(report);
    }

    // Reporte de Producto con Detalle
    @PostMapping("/reports/product-detail")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ProductDetailReportDTO>> getProductDetailReport(
            @Valid @RequestBody tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO filter) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();

        log.info("Generando reporte de producto con detalle para periodo {} y almacén {}", filter.getPeriodId(), filter.getWarehouseId());

        List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ProductDetailReportDTO> report =
            labelService.getProductDetailReport(filter, userId, userRole);

        return ResponseEntity.ok(report);
    }
}


