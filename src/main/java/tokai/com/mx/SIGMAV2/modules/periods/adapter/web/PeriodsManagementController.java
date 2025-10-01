package tokai.com.mx.SIGMAV2.modules.periods.adapter.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.IdDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.UpdatePeriodDTO;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto.CreatePeriodDTO;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto.PeriodResponseDTO;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.input.PeriodManagementUseCase;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sigmav2/periods")
@RequiredArgsConstructor

/**
 * Controlador REST principal para la gestión avanzada de periodos.
 * Expone endpoints para crear, consultar, actualizar, eliminar, cerrar y bloquear periodos,
 * utilizando casos de uso y DTOs para aplicar lógica de negocio y validaciones.
 */

public class PeriodsManagementController {
    private final PeriodManagementUseCase periodManagementUseCase;

    @PostMapping
    public ResponseEntity<PeriodResponseDTO> createPeriod(@Valid @RequestBody CreatePeriodDTO createPeriodDTO) {
        Period period = periodManagementUseCase.createPeriod(createPeriodDTO.getDate(), createPeriodDTO.getComments());
        return ResponseEntity.ok(mapToResponseDTO(period, false));
    }

    @PostMapping("/get")
    public ResponseEntity<PeriodResponseDTO> getPeriod(@Valid @RequestBody IdDTO idDTO) {
        Period period = periodManagementUseCase.findById(idDTO.getId());
        if (period == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @PostMapping("/update-comments")
    public ResponseEntity<PeriodResponseDTO> updatePeriodComments(
            @Valid @RequestBody UpdatePeriodDTO updatePeriodDTO) {
        Period period = periodManagementUseCase.updateComments(updatePeriodDTO.getId(), updatePeriodDTO.getComments());
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }



    @PostMapping("/open")
    public ResponseEntity<PeriodResponseDTO> openPeriod(@Valid @RequestBody IdDTO idDTO) {
        Period period = periodManagementUseCase.openPeriod(idDTO.getId());
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @GetMapping
    public ResponseEntity<Page<PeriodResponseDTO>> getAllPeriods(Pageable pageable) {
        Page<Period> periods = periodManagementUseCase.findAll(pageable);
        Page<PeriodResponseDTO> responseDTOs = periods.map(p -> mapToResponseDTO(p, false));
        return ResponseEntity.ok(responseDTOs);
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deletePeriod(@Valid @RequestBody IdDTO idDTO) {
        periodManagementUseCase.deletePeriod(idDTO.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/close")
    public ResponseEntity<PeriodResponseDTO> closePeriod(@Valid @RequestBody IdDTO idDTO) {
        Period period = periodManagementUseCase.closePeriod(idDTO.getId());
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @PostMapping("/lock")
    public ResponseEntity<PeriodResponseDTO> lockPeriod(@Valid @RequestBody IdDTO idDTO) {
        Period period = periodManagementUseCase.lockPeriod(idDTO.getId());
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    private PeriodResponseDTO mapToResponseDTO(Period period, boolean includeDependencies) {
        boolean hasDeps = includeDependencies && period.getId() != null && periodManagementUseCase.hasDependencies(period.getId());
        return PeriodResponseDTO.builder()
                .id(period.getId())
                .date(period.getDate())
                .comments(period.getComments())
                .state(period.getState())
                .hasDependencies(hasDeps)
                .build();
    }
}
