package tokai.com.mx.SIGMAV2.modules.periods.adapter.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.PeriodEntity;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.time.LocalDate;
import java.util.Optional;

@Component("inventoryPeriodRepositoryAdapter")
public class InventoryPeriodRepositoryAdapter implements tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.PeriodRepository {
    private final JpaPeriodRepository jpaRepository;

    public InventoryPeriodRepositoryAdapter(JpaPeriodRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    private Period toPeriodsDomain(PeriodEntity entity) {
        if (entity == null) return null;
        return Period.builder()
                .id(entity.getId())
                .date(entity.getDate())
                .comments(entity.getComments())
                .state(entity.getState())
                .build();
    }

    private PeriodEntity toEntityFromPeriods(Period p) {
        if (p == null) return null;
        PeriodEntity e = new PeriodEntity();
        e.setId(p.getId());
        e.setDate(p.getDate());
        e.setComments(p.getComments());
        e.setState(p.getState());
        return e;
    }

    @Override
    public Optional<tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period> findById(Long id) {
        return jpaRepository.findById(id).map(this::toPeriodsDomain);
    }

    @Override
    public Optional<tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period> findByDate(LocalDate periodDate) {
        return jpaRepository.findByDate(periodDate).map(this::toPeriodsDomain);
    }

    @Override
    public Page<tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(this::toPeriodsDomain);
    }

    @Override
    public tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period save(tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period period) {
        PeriodEntity entity = toEntityFromPeriods(period);
        PeriodEntity saved = jpaRepository.save(entity);
        return toPeriodsDomain(saved);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean hasDependencies(Long id) {
        try {
            long c = jpaRepository.countDependencies(id);
            return c > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
