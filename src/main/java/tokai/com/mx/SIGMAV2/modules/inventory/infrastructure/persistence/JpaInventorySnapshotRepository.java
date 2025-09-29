package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaInventorySnapshotRepository extends JpaRepository<InventorySnapshotJpaEntity, Long> {

    @Query("SELECT s FROM InventorySnapshotJpaEntity s WHERE s.periodId = :periodId AND s.warehouseId = :warehouseId")
    List<InventorySnapshotJpaEntity> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId);

    @Query("SELECT s FROM InventorySnapshotJpaEntity s WHERE s.productId = :productId AND s.warehouseId = :warehouseId AND s.periodId = :periodId")
    Optional<InventorySnapshotJpaEntity> findByProductIdAndWarehouseIdAndPeriodId(Long productId, Long warehouseId, Long periodId);

    @Modifying
    @Query("UPDATE InventorySnapshotJpaEntity s SET s.status = 'B' WHERE s.periodId = :periodId AND s.warehouseId = :warehouseId AND s.productId NOT IN :activeProductIds")
    int markAsInactiveNotInImport(Long periodId, Long warehouseId, List<Long> activeProductIds);



}
