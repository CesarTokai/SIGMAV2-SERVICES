package tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface JpaPeriodRepository extends JpaRepository<PeriodEntity, Long> {
    boolean existsByDate(LocalDate date);
    Optional<PeriodEntity> findByDate(LocalDate date);

    @Query(value = "SELECT COUNT(*) FROM label_requests WHERE id_period = :periodId", nativeQuery = true)
    long countDependencies(@Param("periodId") Long periodId);
}
