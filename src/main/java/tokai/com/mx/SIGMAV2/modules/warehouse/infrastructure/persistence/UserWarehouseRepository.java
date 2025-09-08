package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWarehouseRepository extends JpaRepository<UserWarehouseEntity, Long> {

    // Verificar si un usuario tiene acceso a un almacén
    boolean existsByUserIdAndWarehouseIdAndWarehouseDeletedAtIsNull(Long userId, Long warehouseId);
    
    // Obtener asignaciones de un usuario
    @Query("SELECT uw FROM UserWarehouseEntity uw WHERE uw.userId = :userId AND uw.warehouse.deletedAt IS NULL")
    List<UserWarehouseEntity> findByUserIdWithActiveWarehouses(@Param("userId") Long userId);
    
    // Obtener usuarios asignados a un almacén
    @Query("SELECT uw FROM UserWarehouseEntity uw WHERE uw.warehouse.id = :warehouseId AND uw.warehouse.deletedAt IS NULL")
    List<UserWarehouseEntity> findByWarehouseIdWithActiveWarehouse(@Param("warehouseId") Long warehouseId);
    
    // Buscar asignación específica
    Optional<UserWarehouseEntity> findByUserIdAndWarehouseId(Long userId, Long warehouseId);
    
    // Eliminar asignación específica
    void deleteByUserIdAndWarehouseId(Long userId, Long warehouseId);
    
    // Contar usuarios por almacén
    @Query("SELECT COUNT(uw) FROM UserWarehouseEntity uw WHERE uw.warehouse.id = :warehouseId AND uw.warehouse.deletedAt IS NULL")
    long countUsersByWarehouseId(@Param("warehouseId") Long warehouseId);
    
    // Contar almacenes por usuario
    @Query("SELECT COUNT(uw) FROM UserWarehouseEntity uw WHERE uw.userId = :userId AND uw.warehouse.deletedAt IS NULL")
    long countWarehousesByUserId(@Param("userId") Long userId);
}