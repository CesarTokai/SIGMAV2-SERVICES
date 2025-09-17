package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseSearchDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;

import java.util.Optional;

public interface MultiWarehouseRepository extends JpaRepository<MultiWarehouseExistence, Long> {

    @Query("SELECT e FROM MultiWarehouseExistence e WHERE " +
            "(:#{#search.periodId} IS NULL OR e.periodId = :#{#search.periodId}) AND " +
            "( :#{#search.search} IS NULL OR " +
            "LOWER(e.warehouseName) LIKE LOWER(CONCAT('%', :#{#search.search}, '%')) OR " +
            "LOWER(e.productCode) LIKE LOWER(CONCAT('%', :#{#search.search}, '%')) OR " +
            "LOWER(e.productName) LIKE LOWER(CONCAT('%', :#{#search.search}, '%')) )")
    Page<MultiWarehouseExistence> findExistences(@Param("search") MultiWarehouseSearchDTO search, Pageable pageable);

    @Query("SELECT MAX(e.id) FROM MultiWarehouseExistence e")
    Optional<Long> findMaxId();
}

