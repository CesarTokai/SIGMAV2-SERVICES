package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Long> {

    @Query("SELECT w FROM WarehouseEntity w WHERE w.warehouseKey = :key AND w.deletedAt IS NULL")
    Optional<WarehouseEntity> findByWarehouseKeyAndDeletedAtIsNull(@Param("key") String key);

    @Query("SELECT w FROM WarehouseEntity w WHERE w.nameWarehouse = :name AND w.deletedAt IS NULL")
    Optional<WarehouseEntity> findByNameWarehouseAndDeletedAtIsNull(@Param("name") String name);

    @Query("SELECT w FROM WarehouseEntity w WHERE w.warehouseKey = :key AND w.id != :id AND w.deletedAt IS NULL")
    boolean existsByWarehouseKeyAndIdNotAndDeletedAtIsNull(@Param("key") String key, @Param("id") Long id);

    @Query("SELECT w FROM WarehouseEntity w WHERE w.nameWarehouse = :name AND w.id != :id AND w.deletedAt IS NULL")
    boolean existsByNameWarehouseAndIdNotAndDeletedAtIsNull(@Param("name") String name, @Param("id") Long id);

    @Query("SELECT w FROM WarehouseEntity w WHERE w.deletedAt IS NULL")
    Page<WarehouseEntity> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT w FROM WarehouseEntity w WHERE " +
           "(LOWER(w.warehouseKey) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(w.nameWarehouse) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND w.deletedAt IS NULL")
    Page<WarehouseEntity> findAllWithSearch(@Param("search") String search, Pageable pageable);



    @Query("SELECT DISTINCT w FROM WarehouseEntity w " +
            "JOIN UserWarehouseEntity uw ON w.id = uw.warehouse.id " +
            "WHERE uw.userId = :userId AND w.deletedAt IS NULL")
    List<WarehouseEntity> findActiveWarehousesByUserIdList(@Param("userId") Long userId);


    @Query("SELECT DISTINCT w FROM WarehouseEntity w " +
            "JOIN UserWarehouseEntity uw ON w.id = uw.warehouse.id " +
            "WHERE uw.userId = :userId AND w.deletedAt IS NULL")
    Page<WarehouseEntity> findActiveWarehousesByUserIdPage(@Param("userId") Long userId, Pageable pageable);


    @Query("SELECT w FROM WarehouseEntity w JOIN UserWarehouseEntity uw ON w.id = uw.warehouse.id WHERE uw.userId = :userId")
    Page<WarehouseEntity> findAllWarehousesByUserIdIncludingDeleted(@Param("userId") Long userId, Pageable pageable);

    // Optimized existence check to avoid loading all warehouses
    @Query("SELECT COUNT(w) > 0 FROM WarehouseEntity w JOIN UserWarehouseEntity uw ON w.id = uw.warehouse.id WHERE uw.userId = :userId AND w.id = :warehouseId")
    boolean hasAccessToWarehouse(@Param("userId") Long userId, @Param("warehouseId") Long warehouseId);

/// ///////////////////////////////////////////////////////////

}
