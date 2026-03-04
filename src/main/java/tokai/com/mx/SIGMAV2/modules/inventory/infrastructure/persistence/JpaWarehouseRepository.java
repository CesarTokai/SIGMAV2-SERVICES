package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaWarehouseRepository extends JpaRepository<WarehouseEntity, Long> {

    Optional<WarehouseEntity> findFirstByOrderByIdWarehouseAsc();

    /** Carga varios almacenes en una sola query — evita N+1 en reportes. */
    @Query("SELECT w FROM InventoryWarehouseEntity w WHERE w.idWarehouse IN :ids")
    List<WarehouseEntity> findAllByIdIn(@Param("ids") Collection<Long> ids);
}


