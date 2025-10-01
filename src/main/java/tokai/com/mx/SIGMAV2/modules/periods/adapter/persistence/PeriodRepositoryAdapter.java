package tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PeriodRepositoryAdapter implements PeriodRepository {

    private final JpaPeriodRepository jpaPeriodRepository;

    @Override
    public Period save(Period period) {
        PeriodEntity entity = toEntity(period);
        PeriodEntity saved = jpaPeriodRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Period> findById(Long id) {
        return jpaPeriodRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Period> findByDate(java.time.LocalDate date) {
        return jpaPeriodRepository.findByDate(date.withDayOfMonth(1)).map(this::toDomain);
    }

    @Override
    public Page<Period> findAll(Pageable pageable) {
        return jpaPeriodRepository.findAll(pageable).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaPeriodRepository.deleteById(id);
    }

    @Override
    public boolean existsByDate(java.time.LocalDate date) {
        return jpaPeriodRepository.existsByDate(date.withDayOfMonth(1));
    }

    @Override
    public long countDependencies(Long periodId) {
        return jpaPeriodRepository.countDependencies(periodId);
    }

    @Override
    public long countByYear(int year) {
        return jpaPeriodRepository.countByYear(year);
    }

    private PeriodEntity toEntity(Period period) {
        if (period == null) return null;
        PeriodEntity entity = new PeriodEntity();
        entity.setId(period.getId());
        entity.setDate(period.getDate() != null ? period.getDate().withDayOfMonth(1) : null);
        entity.setComments(period.getComments());
        entity.setState(period.getState());
        return entity;
    }

    private Period toDomain(PeriodEntity entity) {
        if (entity == null) return null;
        return Period.builder()
                .id(entity.getId())
                .date(entity.getDate())
                .comments(entity.getComments())
                .state(entity.getState())
                .build();
    }
}
