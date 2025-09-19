package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Period;

import java.util.Optional;
public interface PeriodRepository {
    Optional<Period> findById(Long id);
}