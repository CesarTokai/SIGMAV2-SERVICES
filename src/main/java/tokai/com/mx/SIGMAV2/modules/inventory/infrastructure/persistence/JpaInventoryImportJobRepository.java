package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaInventoryImportJobRepository extends JpaRepository<InventoryImportJobEntity, Long> {

    @Query("SELECT j FROM InventoryImportJobEntity j WHERE j.idPeriod = :periodId ORDER BY j.finishedAt DESC LIMIT 1")
    Optional<InventoryImportJobEntity> findLatestByPeriodId(@Param("periodId") Long periodId);
}
