package tokai.com.mx.SIGMAV2.modules.labels.adapter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.PrintRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;

@RestController
@RequestMapping("/api/sigmav2/labels")
@RequiredArgsConstructor
public class LabelsController {

    private final LabelService labelService;

    // Solicitar folios (crear LabelRequest)
    @PostMapping("/request")
    public ResponseEntity<Void> requestLabels(@Valid @RequestBody LabelRequestDTO dto, @RequestHeader("X-User-Id") Long userId) {
        labelService.requestLabels(dto, userId);
        return ResponseEntity.status(201).build();
    }

    // Generar marbetes a partir de una solicitud
    @PostMapping("/generate")
    public ResponseEntity<Void> generateBatch(@Valid @RequestBody GenerateBatchDTO dto, @RequestHeader("X-User-Id") Long userId) {
        labelService.generateBatch(dto, userId);
        return ResponseEntity.ok().build();
    }

    // Imprimir / Reimprimir rango de marbetes
    @PostMapping("/print")
    public ResponseEntity<LabelPrint> printLabels(@Valid @RequestBody PrintRequestDTO dto, @RequestHeader("X-User-Id") Long userId) {
        LabelPrint printed = labelService.printLabels(dto, userId);
        return ResponseEntity.ok(printed);
    }

    // Registrar Conteo C1
    @PostMapping("/counts/c1")
    public ResponseEntity<LabelCountEvent> registerCountC1(@Valid @RequestBody CountEventDTO dto,
                                                            @RequestHeader("X-User-Id") Long userId,
                                                            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        LabelCountEvent ev = labelService.registerCountC1(dto, userId, userRole);
        return ResponseEntity.ok(ev);
    }

    // Registrar Conteo C2
    @PostMapping("/counts/c2")
    public ResponseEntity<LabelCountEvent> registerCountC2(@Valid @RequestBody CountEventDTO dto,
                                                            @RequestHeader("X-User-Id") Long userId,
                                                            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        LabelCountEvent ev = labelService.registerCountC2(dto, userId, userRole);
        return ResponseEntity.ok(ev);
    }
}
