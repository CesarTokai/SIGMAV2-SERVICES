package tokai.com.mx.SIGMAV2.modules.periods.application.port.input;

import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;

public interface PeriodManagementUseCase {
    Period findById(Long id);
    Period createPeriod(LocalDate date, String comments);
    Page<Period> findAll(Pageable pageable);
    Period updateComments(Long id, String comments);
    void deletePeriod(Long id);

    @Transactional
    Period openPeriod(Long id);

    Period closePeriod(Long id);
    Period lockPeriod(Long id);
    boolean hasDependencies(Long id);
}
