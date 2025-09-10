package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserWarehouseRepository extends JpaRepository<UserWarehouseEntity, Long> {

    @Query("SELECT uw FROM UserWarehouseEntity uw " +
           "JOIN FETCH uw.warehouse w " +
           "WHERE uw.userId = :userId AND w.deletedAt IS NULL")
    List<UserWarehouseEntity> findByUserIdWithActiveWarehouses(@Param("userId") Long userId);

    @Query("SELECT uw FROM UserWarehouseEntity uw " +
           "JOIN FETCH uw.warehouse w " +
           "WHERE uw.warehouse.id = :warehouseId AND w.deletedAt IS NULL")
    List<UserWarehouseEntity> findByWarehouseIdWithActiveWarehouse(@Param("warehouseId") Long warehouseId);

    @Query("SELECT COUNT(uw) > 0 FROM UserWarehouseEntity uw " +
           "JOIN uw.warehouse w " +
           "WHERE uw.userId = :userId AND uw.warehouse.id = :warehouseId AND w.deletedAt IS NULL")
    boolean existsByUserIdAndWarehouseIdAndWarehouseDeletedAtIsNull(
            @Param("userId") Long userId,
            @Param("warehouseId") Long warehouseId);

    @Query("SELECT COUNT(DISTINCT uw.warehouse.id) FROM UserWarehouseEntity uw " +
           "JOIN uw.warehouse w " +
           "WHERE uw.userId = :userId AND w.deletedAt IS NULL")
    long countWarehousesByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT uw.userId) FROM UserWarehouseEntity uw " +
           "JOIN uw.warehouse w " +
           "WHERE uw.warehouse.id = :warehouseId AND w.deletedAt IS NULL")
    long countUsersByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT uw FROM UserWarehouseEntity uw " +
           "JOIN FETCH uw.warehouse w " +
           "WHERE uw.userId = :userId AND uw.warehouse.id = :warehouseId")
    Optional<UserWarehouseEntity> findByUserIdAndWarehouseId(
            @Param("userId") Long userId,
            @Param("warehouseId") Long warehouseId);
}
