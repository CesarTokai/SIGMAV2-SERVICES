package tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.PeriodEntity;

@Repository
public interface JpaPeriodRepository extends JpaRepository<PeriodEntity, Long> {
    boolean existsByDate(LocalDate date);
    Optional<PeriodEntity> findByDate(LocalDate date);

    @Query(value = "SELECT COUNT(*) FROM label_requests WHERE id_period = :periodId", nativeQuery = true)
    long countDependencies(@Param("periodId") Long periodId);

    // Cuenta los periodos por año
    @Query("SELECT COUNT(p) FROM InventoryPeriodEntity p WHERE YEAR(p.date) = :year")
    long countByYear(@org.springframework.data.repository.query.Param("year") int year);

    // Obtener el último periodo registrado ordenado por fecha descendente
    @Query("SELECT p FROM InventoryPeriodEntity p ORDER BY p.date DESC LIMIT 1")
    Optional<PeriodEntity> findLatestPeriod();
}
