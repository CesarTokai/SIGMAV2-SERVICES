package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;

import java.util.List;
import java.util.Optional;

public interface JpaInventoryStockRepository extends JpaRepository<InventoryStockEntity, Long> {

    /**
     * Buscar por producto y almacén (sin periodo)
     * @deprecated Usar findByProductIdProductAndWarehouseIdWarehouseAndPeriodId en su lugar
     */
    @Deprecated
    Optional<InventoryStockEntity> findByProductIdProductAndWarehouseIdWarehouse(Long productId, Long warehouseId);

    /**
     * Buscar por producto, almacén y periodo específico
     */
    Optional<InventoryStockEntity> findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
        Long productId,
        Long warehouseId,
        Long periodId
    );

    /**
     * Obtener todos los productos del inventario de un almacén específico
     * @deprecated Usar findByWarehouseIdWarehouseAndPeriodId en su lugar
     */
    @Deprecated
    List<InventoryStockEntity> findByWarehouseIdWarehouse(Long warehouseId);

    /**
     * Obtener todos los productos del inventario de un almacén y periodo específico
     */
    List<InventoryStockEntity> findByWarehouseIdWarehouseAndPeriodId(Long warehouseId, Long periodId);

    /**
     * Obtener existencias por periodo
     */
    List<InventoryStockEntity> findByPeriodId(Long periodId);

    /**
     * Eliminar existencias por almacén y periodo
     */
    void deleteByWarehouseIdWarehouseAndPeriodId(Long warehouseId, Long periodId);

    /**
     * Consultar existencias activas por almacén y periodo
     */
    @Query("""
        SELECT s 
        FROM InventoryStockEntity s 
        WHERE s.warehouse.idWarehouse = :warehouseId 
        AND s.periodId = :periodId 
        AND s.status = 'A'
        ORDER BY s.product.idProduct
        """)
    List<InventoryStockEntity> findActiveStockByWarehouseAndPeriod(
        @Param("warehouseId") Long warehouseId,
        @Param("periodId") Long periodId
    );

    /**
     * Contar existencias por almacén y periodo
     */
    @Query("SELECT COUNT(s) FROM InventoryStockEntity s WHERE s.warehouse.idWarehouse = :warehouseId AND s.periodId = :periodId")
    long countByWarehouseIdAndPeriodId(@Param("warehouseId") Long warehouseId, @Param("periodId") Long periodId);
}