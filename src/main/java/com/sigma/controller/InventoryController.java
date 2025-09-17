package com.sigma.controller;

import com.sigma.dto.ProductStockView;
import com.sigma.dto.StockChange;
import com.sigma.dto.StockMatrixRow;
import com.sigma.dto.StockRow;
import com.sigma.service.StockCommandService;
import com.sigma.service.StockQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sigmav2/inventorys")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "API para gestión de inventario")
public class InventoryController {

    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;

    @GetMapping("/warehouses/my")
    @Operation(summary = "Obtener stock actual en almacenes accesibles")
    public ResponseEntity<List<StockRow>> getCurrentStock() {
        return ResponseEntity.ok(stockQueryService.getCurrentStock());
    }

    @GetMapping("/stock")
    @Operation(summary = "Obtener todo el stock agrupado por producto")
    public ResponseEntity<List<ProductStockView>> getAllStock() {
        return ResponseEntity.ok(stockQueryService.getAllStock());
    }

    @GetMapping("/stock/by-product/{productId}")
    @Operation(summary = "Obtener stock de un producto específico")
    public ResponseEntity<ProductStockView> getStockByProduct(@PathVariable Long productId) {
        ProductStockView stock = stockQueryService.getStockByProduct(productId);
        return stock != null ? ResponseEntity.ok(stock) : ResponseEntity.notFound().build();
    }

    @GetMapping("/stock/matrix")
    @Operation(summary = "Obtener matriz de stock (productos x almacenes)")
    public ResponseEntity<List<StockMatrixRow>> getStockMatrix() {
        return ResponseEntity.ok(stockQueryService.getStockMatrix());
    }

    @PutMapping("/stock/warehouse/{warehouseId}")
    @Operation(summary = "Actualizar stock en un almacén")
    public ResponseEntity<Void> updateStock(
            @PathVariable Long warehouseId,
            @RequestBody StockChange stockChange) {
        stockCommandService.updateStock(warehouseId, stockChange);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stock/transfer")
    @Operation(summary = "Transferir stock entre almacenes")
    public ResponseEntity<Void> transferStock(
            @RequestParam Long sourceWarehouseId,
            @RequestParam Long targetWarehouseId,
            @RequestBody StockChange stockChange) {
        stockCommandService.transferStock(sourceWarehouseId, targetWarehouseId, stockChange);
        return ResponseEntity.ok().build();
    }
}
