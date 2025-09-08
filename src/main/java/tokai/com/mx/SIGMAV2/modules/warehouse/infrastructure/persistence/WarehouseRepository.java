package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Long> {

    // Consultas básicas
    Optional<WarehouseEntity> findByWarehouseKeyAndDeletedAtIsNull(String warehouseKey);
    
    Optional<WarehouseEntity> findByNameWarehouseAndDeletedAtIsNull(String nameWarehouse);
    
    List<WarehouseEntity> findAllByDeletedAtIsNull();
    
    Page<WarehouseEntity> findAllByDeletedAtIsNull(Pageable pageable);
    
    // Búsqueda con filtros
    @Query("SELECT w FROM WarehouseEntity w WHERE w.deletedAt IS NULL " +
           "AND (:search IS NULL OR w.warehouseKey LIKE %:search% OR w.nameWarehouse LIKE %:search%)")
    Page<WarehouseEntity> findAllWithSearch(@Param("search") String search, Pageable pageable);
    
    // Validaciones de unicidad
    boolean existsByWarehouseKeyAndIdNotAndDeletedAtIsNull(String warehouseKey, Long id);
    
    boolean existsByNameWarehouseAndIdNotAndDeletedAtIsNull(String nameWarehouse, Long id);
    
    // Verificar dependencias antes de eliminar (comentado temporalmente)
    // TODO: Implementar cuando existan las tablas reales
    /*
    @Query("SELECT COUNT(i) FROM InventoryStockEntity i WHERE i.warehouse.id = :warehouseId")
    long countInventoryStockByWarehouseId(@Param("warehouseId") Long warehouseId);
    
    @Query("SELECT COUNT(l) FROM LabelRequestEntity l WHERE l.warehouse.id = :warehouseId")
    long countLabelRequestsByWarehouseId(@Param("warehouseId") Long warehouseId);
    */
    
    // Almacenes por usuario
    @Query("SELECT uw.warehouse FROM UserWarehouseEntity uw WHERE uw.userId = :userId AND uw.warehouse.deletedAt IS NULL")
    List<WarehouseEntity> findWarehousesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT uw.warehouse FROM UserWarehouseEntity uw WHERE uw.userId = :userId AND uw.warehouse.deletedAt IS NULL")
    Page<WarehouseEntity> findWarehousesByUserId(@Param("userId") Long userId, Pageable pageable);
}