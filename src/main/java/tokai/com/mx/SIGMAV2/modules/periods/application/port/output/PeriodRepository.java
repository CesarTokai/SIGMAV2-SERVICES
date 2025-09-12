package tokai.com.mx.SIGMAV2.modules.periods.application.port.output;

import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;

public interface PeriodRepository {
    Period save(Period period);
    Optional<Period> findById(Long id);
    Optional<Period> findByDate(LocalDate date);
    Page<Period> findAll(Pageable pageable);
    void deleteById(Long id);
    boolean existsByDate(LocalDate date);
    long countDependencies(Long periodId);
}
