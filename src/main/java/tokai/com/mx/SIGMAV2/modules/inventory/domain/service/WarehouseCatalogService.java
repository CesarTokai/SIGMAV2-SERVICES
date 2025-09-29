package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;


import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class WarehouseCatalogService  {

    private final WarehouseRepository warehouseRepository;

    public WarehouseCatalogService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public List<Warehouse> listWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(entity -> new Warehouse(
                        entity.getId(),
                        entity.getNameWarehouse(),
                        entity.getObservations()
                ))
                .collect(Collectors.toList());
    }
}
