package tokai.com.mx.SIGMAV2.modules.periods.application.port.input;

import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;

public interface PeriodManagementUseCase {
    Period createPeriod(LocalDate date, String comments);
    Optional<Period> findById(Long id);
    Page<Period> findAll(Pageable pageable);
    Period updateComments(Long id, String comments);
    void deletePeriod(Long id);
    Period closePeriod(Long id);
    Period lockPeriod(Long id);
    boolean hasDependencies(Long id);
}
