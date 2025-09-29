package tokai.com.mx.SIGMAV2.modules.inventory.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.PeriodRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sigmav2/inventory/period-details")
@RequiredArgsConstructor
public class InventoryPeriodController {
    private final PeriodRepository periodRepository;

    @GetMapping
    public ResponseEntity<List<Period>> listPeriods(Pageable pageable) {
        List<Period> periods = periodRepository.findAll(pageable).getContent();
        return ResponseEntity.ok(periods);
    }

    @PostMapping
    public ResponseEntity<Period> createPeriod(@RequestBody Period period) {
        // Validar que no exista un periodo para esa fecha (normalize to first day)
        LocalDate date = period.getDate();
        periodRepository.findByDate(date).ifPresent(p -> {
            throw new IllegalArgumentException("Ya existe un periodo para la fecha indicada");
        });
        // Default state and save
        period.setState(period.getState() == null ? period.getState() : period.getState());
        Period saved = periodRepository.save(period);
        return ResponseEntity.ok(saved);
    }
}
