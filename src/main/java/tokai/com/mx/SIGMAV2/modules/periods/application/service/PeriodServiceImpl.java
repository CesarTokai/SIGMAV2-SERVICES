package tokai.com.mx.SIGMAV2.modules.periods.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.input.PeriodManagementUseCase;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class PeriodServiceImpl implements PeriodManagementUseCase {
    private final PeriodRepository periodRepository;

    @Override
    @Transactional
    public Period createPeriod(LocalDate date, String comments) {
        LocalDate normalizedDate = date.withDayOfMonth(1);
        if (periodRepository.existsByDate(normalizedDate)) {
            throw new IllegalArgumentException("Ya existe un periodo para esta fecha");
        }

        Period period = Period.builder()
                .date(normalizedDate)
                .comments(comments)
                .state(Period.PeriodState.DRAFT)
                .build();

        return periodRepository.save(period);
    }

    @Override
    public Period findById(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado"));
    }

    @Override
    public Page<Period> findAll(Pageable pageable) {
        return periodRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public Period updateComments(Long id, String comments) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado"));

        period.setComments(comments);
        return periodRepository.save(period);
    }

    @Override
    @Transactional
    public void deletePeriod(Long id) {
        if (hasDependencies(id)) {
            throw new IllegalStateException("No se puede eliminar el periodo porque tiene dependencias");
        }
        periodRepository.deleteById(id);
    }

    @Transactional
    @Override
    public Period openPeriod(Long id) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado"));



       if (period.getState() != Period.PeriodState.DRAFT) {
            throw new IllegalStateException("Solo se pueden abrir periodos en estado DRAFT");
        }



        period.setState(Period.PeriodState.OPEN);
        return periodRepository.save(period);
    }

    @Override
    @Transactional
    public Period closePeriod(Long id) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado"));

        if (period.getState() != Period.PeriodState.DRAFT &&
            period.getState() != Period.PeriodState.OPEN) {
            throw new IllegalStateException("Solo se pueden cerrar periodos en estado DRAFT u OPEN");
        }

        period.setState(Period.PeriodState.CLOSED);
        return periodRepository.save(period);
    }

    @Override
    @Transactional
    public Period lockPeriod(Long id) {
        Period period = periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado"));

        if (period.getState() != Period.PeriodState.CLOSED) {
            throw new IllegalStateException("Solo se pueden bloquear periodos en estado CLOSED");
        }

        period.setState(Period.PeriodState.LOCKED);
        return periodRepository.save(period);
    }

    @Override
    public boolean hasDependencies(Long id) {
        return periodRepository.countDependencies(id) > 0;
    }
}
