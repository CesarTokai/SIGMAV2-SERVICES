package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(Long id);
    List<Warehouse> findAll();
}
