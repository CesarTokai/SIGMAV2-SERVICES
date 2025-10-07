package tokai.com.mx.SIGMAV2.modules.labels.adapter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;

@RestController
@RequestMapping("/api/sigmav2/labels")
@RequiredArgsConstructor
public class LabelsController {

    private final LabelService labelService;

    // Solicitar folios (crear LabelRequest)
    @PostMapping("/request")
    public ResponseEntity<?> requestLabels(@Valid @RequestBody LabelRequestDTO dto, @RequestHeader("X-User-Id") Long userId) {
        labelService.requestLabels(dto, userId);
        return ResponseEntity.status(201).build();
    }

    // Generar marbetes a partir de una solicitud
    @PostMapping("/generate")
    public ResponseEntity<?> generateBatch(@Valid @RequestBody GenerateBatchDTO dto, @RequestHeader("X-User-Id") Long userId) {
        labelService.generateBatch(dto, userId);
        return ResponseEntity.ok().build();
    }
}

