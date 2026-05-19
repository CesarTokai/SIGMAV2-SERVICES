package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.mapper.PeriodMapper;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.PeriodEntity;

import java.util.Optional;
import java.time.LocalDate;

public class PeriodRepositoryAdapter implements PeriodRepository {
    private final JpaPeriodRepository jpaRepository;
    private final PeriodMapper mapper;

    public PeriodRepositoryAdapter(JpaPeriodRepository jpaRepository, PeriodMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Period> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Period> findByDate(LocalDate periodDate) {
        return jpaRepository.findByDate(periodDate).map(mapper::toDomain);
    }

    @Override
    public Page<Period> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Period save(Period period) {
        PeriodEntity entity = mapper.toEntity(period);
        PeriodEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean hasDependencies(Long id) {
        // Implementa la lógica real si tienes dependencias
        return false;
    }

    @Override
    public void OpenPeriod(Long id) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setState(tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period.PeriodState.OPEN);
            jpaRepository.save(entity);
        });
    }

    @Override
    public Optional<Period> findLatest() {
        return jpaRepository.findLatestPeriod().map(mapper::toDomain);
    }

}
