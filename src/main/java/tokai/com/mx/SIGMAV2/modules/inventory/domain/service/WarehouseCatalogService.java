package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;


import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.WarehouseCatalogUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.WarehouseRepository;

import java.time.LocalDateTime;
import java.util.List;

public class WarehouseCatalogService implements WarehouseCatalogUseCase {

    private final WarehouseRepository warehouseRepository;

    public WarehouseCatalogService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    public List<Warehouse> listWarehouses() {
        return warehouseRepository.findAll();
    }

    @Override
    public Warehouse createWarehouse(Warehouse warehouse) {
        warehouse.setCreatedAt(LocalDateTime.now());
        return warehouseRepository.save(warehouse);
    }
}
