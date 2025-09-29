package tokai.com.mx.SIGMAV2.modules.inventory.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.WarehouseRepository;

import java.util.List;

/**
 * Controlador REST para operaciones básicas de almacenes dentro del módulo de inventarios.
 * Permite listar y crear almacenes usando directamente el repositorio.
 */


@RestController
@RequestMapping("/api/sigmav2/inventory/warehouses/")
@RequiredArgsConstructor
public class InventoryWarehouseController {
    private final WarehouseRepository warehouseRepository;

    @GetMapping
    public ResponseEntity<List<Warehouse>> listWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Warehouse> createWarehouse(@RequestBody Warehouse warehouse) {
        Warehouse saved = warehouseRepository.save(warehouse);
        return ResponseEntity.ok(saved);
    }
}
