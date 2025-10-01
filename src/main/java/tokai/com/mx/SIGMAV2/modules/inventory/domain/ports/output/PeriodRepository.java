package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.time.LocalDate;
import java.util.Optional;

public interface PeriodRepository {

    Optional<Period> findById(Long id);
    Optional<Period> findByDate(LocalDate periodDate);
    Page<Period> findAll(Pageable pageable);
    Period save(Period period);
    void deleteById(Long id);
    boolean hasDependencies(Long id);
    void OpenPeriod(Long id);

}