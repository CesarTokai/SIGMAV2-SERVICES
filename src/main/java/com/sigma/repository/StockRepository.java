package com.sigma.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<StockEntity, Long> {
    @Query(value = "SELECT s.id, s.product_id, p.name as product_name, " +
           "s.warehouse_id, w.name as warehouse_name, s.quantity, s.unit " +
           "FROM stock s " +
           "INNER JOIN products p ON s.product_id = p.id " +
           "INNER JOIN warehouses w ON s.warehouse_id = w.id " +
           "WHERE s.warehouse_id IN :warehouseIds AND w.active = true", nativeQuery = true)
    List<StockView> findCurrentStockByWarehouses(@Param("warehouseIds") List<Long> warehouseIds);

    @Query(value = "SELECT s.id, s.product_id, p.name as product_name, " +
           "s.warehouse_id, w.name as warehouse_name, s.quantity, s.unit " +
           "FROM stock s " +
           "INNER JOIN products p ON s.product_id = p.id " +
           "INNER JOIN warehouses w ON s.warehouse_id = w.id " +
           "WHERE s.product_id = :productId " +
           "AND s.warehouse_id IN :warehouseIds " +
           "AND w.active = true", nativeQuery = true)
    List<StockView> findStockByProductAndWarehouses(
        @Param("productId") Long productId,
        @Param("warehouseIds") List<Long> warehouseIds
    );

    @Query(value = "SELECT DISTINCT s.product_id, p.name as product_name " +
           "FROM stock s " +
           "INNER JOIN products p ON s.product_id = p.id " +
           "INNER JOIN warehouses w ON s.warehouse_id = w.id " +
           "WHERE s.warehouse_id IN :warehouseIds " +
           "AND w.active = true", nativeQuery = true)
    List<Object[]> findDistinctProductsByWarehouses(@Param("warehouseIds") List<Long> warehouseIds);

    @Query("SELECT s FROM StockEntity s WHERE s.productId = :productId AND s.warehouseId = :warehouseId")
    Optional<StockEntity> findByProductIdAndWarehouseId(
        @Param("productId") Long productId,
        @Param("warehouseId") Long warehouseId
    );
}
