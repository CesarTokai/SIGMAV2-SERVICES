package com.sigma.service;

import com.sigma.dto.*;
import com.sigma.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockQueryService {
    private final StockRepository stockRepository;
    private final WarehouseAccessService warehouseAccessService;

    public List<ProductStockView> getAllStock() {
        return stockRepository.findStockForWarehouses(
            warehouseAccessService.getAccessibleWarehouses()
        );
    }

    public ProductStockView getStockByProduct(Long productId) {
        return stockRepository.findStockForProduct(
            productId,
            warehouseAccessService.getAccessibleWarehouses()
        );
    }

    public List<StockMatrixRow> getStockMatrix() {
        return stockRepository.findStockMatrix(
            warehouseAccessService.getAccessibleWarehouses()
        );
    }
}

