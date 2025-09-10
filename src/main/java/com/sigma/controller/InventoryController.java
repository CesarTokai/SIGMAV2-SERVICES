package com.sigma.controller;

import com.sigma.dto.*;
import com.sigma.exception.AccessDeniedException;
import com.sigma.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final WarehouseAccessService warehouseAccessService;
    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;

    @GetMapping("/warehouses/my")
    public List<Warehouse> getMyWarehouses() {
        return warehouseAccessService.getAccessibleWarehouses();
    }

    @GetMapping("/stock")
    public List<ProductStockView> getAllStock() {
        return stockQueryService.getAllStock();
    }

    @GetMapping("/stock/by-product/{productId}")
    public ProductStockView getStockByProduct(@PathVariable Long productId) {
        return stockQueryService.getStockByProduct(productId);
    }

    @GetMapping("/stock/matrix")
    public List<StockMatrixRow> getStockMatrix() {
        return stockQueryService.getStockMatrix();
    }

    @PostMapping("/stock")
    public ResponseEntity<?> updateStock(@RequestBody StockChange change) {
        try {
            stockCommandService.updateStock(change);
            return ResponseEntity.ok().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
