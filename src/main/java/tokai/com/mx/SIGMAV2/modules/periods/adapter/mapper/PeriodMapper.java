package tokai.com.mx.SIGMAV2.modules.periods.adapter.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.PeriodEntity;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

@Component
public class PeriodMapper {
    public Period toDomain(PeriodEntity entity) {
        if (entity == null) return null;
        Period period = new Period();
        period.setId(entity.getId());
        period.setDate(entity.getDate());
        period.setComments(entity.getComments());
        // entity.getState() is already Period.PeriodState
        period.setState(entity.getState());
        return period;
    }

    public PeriodEntity toEntity(Period domain) {
        if (domain == null) return null;
        PeriodEntity entity = new PeriodEntity();
        entity.setId(domain.getId());
        entity.setDate(domain.getDate());
        entity.setComments(domain.getComments());
        entity.setState(domain.getState() != null ? domain.getState() : Period.PeriodState.DRAFT);
        return entity;
    }
}
