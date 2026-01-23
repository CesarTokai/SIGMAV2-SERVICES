package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaInventorySnapshotRepository extends JpaRepository<InventorySnapshotJpaEntity, Long> {

    @Query("SELECT s FROM InventorySnapshotJpaEntity s WHERE s.periodId = :periodId AND s.warehouseId = :warehouseId")
    List<InventorySnapshotJpaEntity> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId);

    @Query("SELECT s FROM InventorySnapshotJpaEntity s WHERE s.periodId = :periodId")
    List<InventorySnapshotJpaEntity> findByPeriodId(@Param("periodId") Long periodId);

    @Query("SELECT s FROM InventorySnapshotJpaEntity s " +
           "LEFT JOIN ProductEntity p ON s.productId = p.idProduct " +
           "WHERE s.periodId = :periodId " +
           "AND (:warehouseId IS NULL OR s.warehouseId = :warehouseId) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(p.cveArt) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(p.descr) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(p.uniMed) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<InventorySnapshotJpaEntity> findByPeriodWithSearch(
            @Param("periodId") Long periodId,
            @Param("warehouseId") Long warehouseId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT s FROM InventorySnapshotJpaEntity s WHERE s.productId = :productId AND s.warehouseId = :warehouseId AND s.periodId = :periodId")
    Optional<InventorySnapshotJpaEntity> findByProductIdAndWarehouseIdAndPeriodId(Long productId, Long warehouseId, Long periodId);

    @Modifying
    @Query("UPDATE InventorySnapshotJpaEntity s SET s.status = 'B' WHERE s.periodId = :periodId AND s.warehouseId = :warehouseId AND s.productId NOT IN :activeProductIds")
    int markAsInactiveNotInImport(Long periodId, Long warehouseId, List<Long> activeProductIds);



}
