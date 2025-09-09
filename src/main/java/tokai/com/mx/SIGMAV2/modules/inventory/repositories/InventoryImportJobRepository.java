package tokai.com.mx.SIGMAV2.modules.inventory.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.InventoryImportJob;

import java.util.Optional;

@Repository
public interface InventoryImportJobRepository extends JpaRepository<InventoryImportJob, Long> {
    
    @Query("SELECT j FROM InventoryImportJob j WHERE " +
           "j.period.idPeriod = :periodId AND " +
           "(:warehouseId IS NULL OR j.warehouse.idWarehouse = :warehouseId) AND " +
           "j.checksum = :checksum")
    Optional<InventoryImportJob> findByPeriodAndWarehouseAndChecksum(
        @Param("periodId") Long periodId,
        @Param("warehouseId") Long warehouseId,
        @Param("checksum") String checksum);
}