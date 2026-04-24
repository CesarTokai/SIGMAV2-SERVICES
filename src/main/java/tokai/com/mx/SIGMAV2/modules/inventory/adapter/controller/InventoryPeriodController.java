package tokai.com.mx.SIGMAV2.modules.inventory.adapter.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.PeriodRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sigmav2/inventory/period-details")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
public class InventoryPeriodController {
    private final PeriodRepository periodRepository;

    @GetMapping
    public ResponseEntity<List<Period>> listPeriods(Pageable pageable) {
        List<Period> periods = periodRepository.findAll(pageable).getContent();
        return ResponseEntity.ok(periods);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Period> createPeriod(@RequestBody Period period) {
        LocalDate date = period.getDate();
        periodRepository.findByDate(date).ifPresent(p -> {
            throw new IllegalArgumentException("Ya existe un periodo para la fecha indicada");
        });
        if (period.getState() == null) {
            period.setState(Period.PeriodState.OPEN);
        }
        Period saved = periodRepository.save(period);
        return ResponseEntity.ok(saved);
    }
}
