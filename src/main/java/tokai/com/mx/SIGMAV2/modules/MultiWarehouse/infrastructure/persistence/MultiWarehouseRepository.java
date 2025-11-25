package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseSearchDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;

import java.util.Optional;
import java.util.List;

public interface MultiWarehouseRepository extends JpaRepository<MultiWarehouseExistence, Long> {

    @Query("SELECT e FROM MultiWarehouseExistence e WHERE " +
            "(:#{#search.periodId} IS NULL OR e.periodId = :#{#search.periodId}) AND " +
            "( :#{#search.search} IS NULL OR " +
            "LOWER(e.warehouseName) LIKE LOWER(CONCAT('%', :#{#search.search}, '%')) OR " +
            "LOWER(e.warehouseKey) LIKE LOWER(CONCAT('%', :#{#search.search}, '%')) OR " +
            "LOWER(e.productCode) LIKE LOWER(CONCAT('%', :#{#search.search}, '%')) OR " +
            "LOWER(e.productName) LIKE LOWER(CONCAT('%', :#{#search.search}, '%')) OR " +
            "CAST(e.stock AS string) LIKE CONCAT('%', :#{#search.search}, '%') )")
    Page<MultiWarehouseExistence> findExistences(@Param("search") MultiWarehouseSearchDTO search, Pageable pageable);

    @Query("SELECT MAX(e.id) FROM MultiWarehouseExistence e")
    Optional<Long> findMaxId();

    // Búsqueda específica para productos y almacenes por periodo
    @Query("SELECT e FROM MultiWarehouseExistence e WHERE e.periodId = :periodId")
    List<MultiWarehouseExistence> findByPeriodId(@Param("periodId") Long periodId);

    // Buscar por producto y almacén específico (usando warehouse key)
    @Query("SELECT e FROM MultiWarehouseExistence e WHERE " +
           "e.productCode = :productCode AND e.warehouseKey = :warehouseKey AND e.periodId = :periodId")
    Optional<MultiWarehouseExistence> findByProductCodeAndWarehouseKeyAndPeriodId(
        @Param("productCode") String productCode,
        @Param("warehouseKey") String warehouseKey,
        @Param("periodId") Long periodId);

    // Buscar registros existentes para actualización
    @Query("SELECT e FROM MultiWarehouseExistence e WHERE e.periodId = :periodId AND e.status <> 'B'")
    List<MultiWarehouseExistence> findActiveByPeriodId(@Param("periodId") Long periodId);
}
