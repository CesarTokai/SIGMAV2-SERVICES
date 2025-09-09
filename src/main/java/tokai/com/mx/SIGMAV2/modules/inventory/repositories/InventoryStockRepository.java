package tokai.com.mx.SIGMAV2.modules.inventory.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.InventoryStock;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    
    Optional<InventoryStock> findByProductIdProductAndWarehouseIdWarehouse(Long productId, Long warehouseId);
    
    @Query("SELECT i FROM InventoryStock i " +
           "JOIN FETCH i.product p " +
           "JOIN FETCH i.warehouse w " +
           "WHERE (:warehouseId IS NULL OR i.warehouse.idWarehouse = :warehouseId) " +
           "AND (:q IS NULL OR " +
           "     UPPER(p.cveArt) LIKE UPPER(CONCAT('%', :q, '%')) OR " +
           "     UPPER(p.description) LIKE UPPER(CONCAT('%', :q, '%')))")
    Page<InventoryStock> findInventoryWithFilters(@Param("warehouseId") Long warehouseId,
                                                  @Param("q") String query,
                                                  Pageable pageable);
    
    @Modifying
    @Query("INSERT INTO InventoryStock (product, warehouse, existQty, status) " +
           "SELECT p, w, :existQty, :status " +
           "FROM Product p, Warehouse w " +
           "WHERE p.idProduct = :productId AND w.idWarehouse = :warehouseId")
    int insertStock(@Param("productId") Long productId,
                   @Param("warehouseId") Long warehouseId,
                   @Param("existQty") BigDecimal existQty,
                   @Param("status") String status);
    
    @Modifying
    @Query("UPDATE InventoryStock i SET i.existQty = :existQty, i.status = :status " +
           "WHERE i.product.idProduct = :productId AND i.warehouse.idWarehouse = :warehouseId")
    int updateStock(@Param("productId") Long productId,
                   @Param("warehouseId") Long warehouseId,
                   @Param("existQty") BigDecimal existQty,
                   @Param("status") String status);
}