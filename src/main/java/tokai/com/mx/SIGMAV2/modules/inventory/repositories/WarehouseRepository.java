package tokai.com.mx.SIGMAV2.modules.inventory.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.Warehouse;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    Optional<Warehouse> findByWarehouseKey(String warehouseKey);
    
    boolean existsByWarehouseKey(String warehouseKey);
}