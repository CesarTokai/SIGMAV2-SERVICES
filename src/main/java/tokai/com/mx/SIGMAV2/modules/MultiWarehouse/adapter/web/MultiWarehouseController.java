package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseSearchDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseStockRequestDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseWizardStepDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.ExportResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.ImportResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.WizardStepResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.input.MultiWarehouseUseCase;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST del módulo MultiWarehouse.
 *
 * <p>Responsabilidad única: traducir peticiones HTTP en llamadas al caso de uso
 * ({@link MultiWarehouseUseCase}) y convertir los result objects en respuestas HTTP.
 * No contiene ninguna lógica de negocio.
 */
@RestController
@RequestMapping("/api/sigmav2/multi-warehouse")
@RequiredArgsConstructor
public class MultiWarehouseController {

    private static final Logger log = LoggerFactory.getLogger(MultiWarehouseController.class);

    private final MultiWarehouseUseCase multiWarehouseUseCase;


    @PostMapping("/existences")
    public List<MultiWarehouseExistence> getExistences(
            @RequestBody MultiWarehouseSearchDTO searchDTO) {
        log.debug("POST /existences - periodId={}, search={}", searchDTO.getPeriodId(), searchDTO.getSearch());
        return multiWarehouseUseCase.findExistences(searchDTO);
    }


    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/import")
    public ResponseEntity<?> importMultiWarehouse(
            @RequestParam("file") MultipartFile file,
            @RequestParam("period") String period) {
        log.info("=== POST /import === period='{}', file='{}', size={}",
                 period,
                 file != null ? file.getOriginalFilename() : "null",
                 file != null ? file.getSize() : 0);
        try {
            ImportResult result = multiWarehouseUseCase.importFile(file, period);
            log.info("Import result: status={}, procesados={}", result.getStatus(), result.getTotalProcesados());
            return toHttpResponse(result);
        } catch (IllegalArgumentException e) {
            log.error("Import - argumento invalido: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            log.error("Import - estado invalido: {}", msg);
            if (msg != null && (msg.startsWith("PERIOD_CLOSED") || msg.startsWith("PERIOD_LOCKED"))) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", msg));
        } catch (Exception e) {
            log.error("Import - error inesperado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error inesperado: " + e.getMessage()));
        }
    }

    /**
     * Convierte un {@link ImportResult} al {@code ResponseEntity} apropiado.
     * Aquí es donde vive la decisión HTTP: qué código de estado, qué cuerpo.
     */
    private ResponseEntity<?> toHttpResponse(ImportResult result) {
        switch (result.getStatus()) {
            case DUPLICATE:
                // 200 OK — no es un error, el archivo ya fue procesado
                return ResponseEntity.ok(Map.of(
                    "status", "DUPLICATE",
                    "message", result.getWarningMessage(),
                    "importLog", result.getImportLog()));
            case SUCCESS:
                return ResponseEntity.ok(buildImportBody(result));
            case SUCCESS_WITH_WARNINGS:
                return ResponseEntity.ok(buildImportBody(result));
            case ERROR:
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", result.getErrorMessage()));
        }
    }

    private Map<String, Object> buildImportBody(ImportResult r) {
        // Map.of() tiene un límite de 10 pares — usamos ofEntries para 11+
        return Map.ofEntries(
            Map.entry("status",               r.getStatus().name()),
            Map.entry("importLog",            r.getImportLog()),
            Map.entry("tieneWarnings",        r.hasWarnings()),
            Map.entry("mensajeWarning",       r.getWarningMessage() != null ? r.getWarningMessage() : ""),
            Map.entry("productosDadosDeBaja", r.getProductosDadosDeBaja()),
            Map.entry("totalProcesados",      r.getTotalProcesados()),
            Map.entry("totalActualizados",    r.getTotalActualizados()),
            Map.entry("totalCreados",         r.getTotalCreados()),
            Map.entry("totalAlmacenesCreados",r.getTotalAlmacenesCreados()),
            Map.entry("totalProductosCreados",r.getTotalProductosCreados()),
            Map.entry("totalDadosDeBaja",     r.getTotalDadosDeBaja())
        );
    }

    // -------------------------------------------------------------------------
    // Wizard
    // -------------------------------------------------------------------------

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/wizard/step")
    public ResponseEntity<?> wizardStep(@RequestBody MultiWarehouseWizardStepDTO stepDTO) {
        WizardStepResult result = multiWarehouseUseCase.processWizardStep(stepDTO);
        return toHttpResponse(result);
    }

    /**
     * Convierte un {@link WizardStepResult} al código HTTP apropiado.
     */
    private ResponseEntity<?> toHttpResponse(WizardStepResult result) {
        if (result.isValid()) {
            return ResponseEntity.ok(Map.of(
                "step",    result.getStepNumber(),
                "status",  "OK",
                "message", result.getMessage(),
                "data",    result.getData() != null ? result.getData() : Map.of()));
        }
        HttpStatus status;
        switch (result.getErrorCode()) {
            case PERIOD_CLOSED:
            case PERIOD_LOCKED:
                status = HttpStatus.CONFLICT;
                break;
            case STEP_NOT_SUPPORTED:
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Map.of(
            "step",      result.getStepNumber(),
            "status",    "ERROR",
            "errorCode", result.getErrorCode(),
            "message",   result.getMessage()));
    }

    // -------------------------------------------------------------------------
    // Exportación
    // -------------------------------------------------------------------------

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExistences(MultiWarehouseSearchDTO search) {
        ExportResult result = multiWarehouseUseCase.exportExistences(search);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
            org.springframework.http.ContentDisposition.attachment()
                .filename(result.getFileName()).build());
        headers.setContentType(MediaType.parseMediaType(result.getContentType()));
        return ResponseEntity.ok().headers(headers).body(result.getCsvBytes());
    }

    // -------------------------------------------------------------------------
    // Consultas puntuales
    // -------------------------------------------------------------------------

    @GetMapping("/import/log/{id}")
    public ResponseEntity<?> getImportLog(@PathVariable Long id) {
        return multiWarehouseUseCase.getImportLog(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/productos-baja")
    public ResponseEntity<?> getProductosDadosDeBaja(@RequestParam Long periodId) {
        try {
            return ResponseEntity.ok(multiWarehouseUseCase.getProductosDadosDeBaja(periodId));
        } catch (NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "periodId es obligatorio"));
        }
    }

    @PostMapping("/stock")
    public ResponseEntity<?> getStock(@RequestBody MultiWarehouseStockRequestDTO request) {
        return multiWarehouseUseCase.getStock(
                request.getProductCode(),
                request.getWarehouseKey(),
                request.getPeriodId())
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "No se encontro stock para ese producto, almacen y periodo")));
    }
}
