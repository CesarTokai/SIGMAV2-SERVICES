package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.PeriodEntity;

@Component
public class PeriodMapperInventory {
    public Period toDomain(PeriodEntity entity) {
        if (entity == null) return null;
        Period period = new Period();
        period.setId(entity.getId());
        period.setPeriodDate(entity.getPeriodDate());
        period.setComments(entity.getComments());
        period.setState(Period.State.valueOf(entity.getState()));
        return period;
    }
    public PeriodEntity toEntity(Period period) {
        if (period == null) return null;
        PeriodEntity entity = new PeriodEntity();
        entity.setId(period.getId());
        entity.setPeriodDate(period.getPeriodDate());
        entity.setComments(period.getComments());
        entity.setState(period.getState().name());
        return entity;
    }
}

