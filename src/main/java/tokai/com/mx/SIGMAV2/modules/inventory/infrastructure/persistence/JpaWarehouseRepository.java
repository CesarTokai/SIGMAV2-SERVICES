package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;

import java.util.Optional;

@Repository
public interface JpaWarehouseRepository extends JpaRepository<WarehouseEntity, Long> {

    // Obtener el primer almacén ordenado por ID (para usar como default)
    Optional<WarehouseEntity> findFirstByOrderByIdWarehouseAsc();

}
