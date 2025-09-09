package tokai.com.mx.SIGMAV2.modules.inventory.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.Period;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {
    
    Optional<Period> findByPeriod(LocalDate period);
    
    boolean existsByPeriod(LocalDate period);
}