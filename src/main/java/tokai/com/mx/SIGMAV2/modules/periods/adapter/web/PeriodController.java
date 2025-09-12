package tokai.com.mx.SIGMAV2.modules.periods.adapter.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto.CreatePeriodDTO;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto.PeriodResponseDTO;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto.UpdatePeriodDTO;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.input.PeriodManagementUseCase;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sigmav2/periods")
@RequiredArgsConstructor
public class PeriodController {
    private final PeriodManagementUseCase periodManagementUseCase;

    @PostMapping
    public ResponseEntity<PeriodResponseDTO> createPeriod(@Valid @RequestBody CreatePeriodDTO createPeriodDTO) {
        Period period = periodManagementUseCase.createPeriod(createPeriodDTO.getDate(), createPeriodDTO.getComments());
        // Recién creado no debería tener dependencias, evitamos la consulta adicional
        return ResponseEntity.ok(mapToResponseDTO(period, false));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PeriodResponseDTO> getPeriod(@PathVariable Long id) {
        return periodManagementUseCase.findById(id)
                .map(period -> ResponseEntity.ok(mapToResponseDTO(period, true)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<PeriodResponseDTO>> getAllPeriods(Pageable pageable) {
        Page<Period> periods = periodManagementUseCase.findAll(pageable);
        Page<PeriodResponseDTO> responseDTOs = periods.map(p -> mapToResponseDTO(p, false));
        return ResponseEntity.ok(responseDTOs);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PeriodResponseDTO> updatePeriodComments(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePeriodDTO updatePeriodDTO) {
        Period period = periodManagementUseCase.updateComments(id, updatePeriodDTO.getComments());
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePeriod(@PathVariable Long id) {
        periodManagementUseCase.deletePeriod(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<PeriodResponseDTO> closePeriod(@PathVariable Long id) {
        Period period = periodManagementUseCase.closePeriod(id);
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<PeriodResponseDTO> lockPeriod(@PathVariable Long id) {
        Period period = periodManagementUseCase.lockPeriod(id);
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
