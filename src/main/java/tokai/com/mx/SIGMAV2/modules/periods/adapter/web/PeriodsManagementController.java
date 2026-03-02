package tokai.com.mx.SIGMAV2.modules.periods.adapter.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto.UpdatePeriodDTO;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto.CreatePeriodDTO;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto.PeriodResponseDTO;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.input.PeriodManagementUseCase;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sigmav2/periods")
@RequiredArgsConstructor
public class PeriodsManagementController {

    private final PeriodManagementUseCase periodManagementUseCase;

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping
    public ResponseEntity<PeriodResponseDTO> createPeriod(@Valid @RequestBody CreatePeriodDTO dto) {
        Period period = periodManagementUseCase.createPeriod(dto.getDate(), dto.getComments());
        return ResponseEntity.ok(mapToResponseDTO(period, false));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/{id}")
    public ResponseEntity<PeriodResponseDTO> getPeriod(@PathVariable Long id) {
        Period period = periodManagementUseCase.findById(id);
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}/comments")
    public ResponseEntity<PeriodResponseDTO> updatePeriodComments(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePeriodDTO updatePeriodDTO) {
        Period period = periodManagementUseCase.updateComments(id, updatePeriodDTO.getComments());
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}/open")
    public ResponseEntity<PeriodResponseDTO> openPeriod(@PathVariable Long id) {
        Period period = periodManagementUseCase.openPeriod(id);
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}/close")
    public ResponseEntity<PeriodResponseDTO> closePeriod(@PathVariable Long id) {
        Period period = periodManagementUseCase.closePeriod(id);
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping("/{id}/lock")
    public ResponseEntity<PeriodResponseDTO> lockPeriod(@PathVariable Long id) {
        Period period = periodManagementUseCase.lockPeriod(id);
        return ResponseEntity.ok(mapToResponseDTO(period, true));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR')")
    @GetMapping
    public ResponseEntity<Page<PeriodResponseDTO>> getAllPeriods(Pageable pageable) {
        Page<Period> periods = periodManagementUseCase.findAll(pageable);
        return ResponseEntity.ok(periods.map(p -> mapToResponseDTO(p, false)));
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePeriod(@PathVariable Long id) {
        periodManagementUseCase.deletePeriod(id);
        return ResponseEntity.noContent().build();
    }

    private PeriodResponseDTO mapToResponseDTO(Period period, boolean includeDependencies) {
        boolean hasDeps = includeDependencies && period.getId() != null
                && periodManagementUseCase.hasDependencies(period.getId());
        return PeriodResponseDTO.builder()
                .id(period.getId())
                .date(period.getDate())
                .comments(period.getComments())
                .state(period.getState())
                .hasDependencies(hasDeps)
                .build();
    }
}
