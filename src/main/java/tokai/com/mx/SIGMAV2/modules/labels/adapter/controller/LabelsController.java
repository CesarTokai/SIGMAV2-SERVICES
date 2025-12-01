package tokai.com.mx.SIGMAV2.modules.labels.adapter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
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
    public ResponseEntity<Void> generateBatch(@Valid @RequestBody GenerateBatchDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        labelService.generateBatch(dto, userId, userRole);
        return ResponseEntity.ok().build();
    }

    // Imprimir / Reimprimir rango de marbetes
    @PostMapping("/print")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<LabelPrint> printLabels(@Valid @RequestBody PrintRequestDTO dto) {
        Long userId = getUserIdFromToken();
        String userRole = getUserRoleFromToken();
        LabelPrint printed = labelService.printLabels(dto, userId, userRole);
        return ResponseEntity.ok(printed);
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
}
