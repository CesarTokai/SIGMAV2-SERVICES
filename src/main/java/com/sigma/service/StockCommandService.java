package com.sigma.service;

import com.sigma.dto.StockChange;
import com.sigma.repository.StockEntity;
import com.sigma.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockCommandService {
    private final WarehouseAccessService warehouseAccessService;
    private final StockRepository stockRepository;

    @Transactional
    public void updateStock(Long warehouseId, StockChange stockChange) {
        // Validar acceso al almacén
        warehouseAccessService.validateAccess(warehouseId);

        // Buscar stock existente
        Optional<StockEntity> existingStock = stockRepository.findByProductIdAndWarehouseId(
            stockChange.getProductId(),
            warehouseId
        );

        StockEntity stock = existingStock.orElse(new StockEntity());
        stock.setProductId(stockChange.getProductId());
        stock.setWarehouseId(warehouseId);
        stock.setQuantity(stockChange.getQuantity());
        stock.setUnit(stockChange.getUnit());

        stockRepository.save(stock);
    }

    @Transactional
    public void transferStock(Long sourceWarehouseId, Long targetWarehouseId, StockChange stockChange) {
        // Validar acceso a ambos almacenes
        warehouseAccessService.validateAccess(sourceWarehouseId);
        warehouseAccessService.validateAccess(targetWarehouseId);

        // Verificar stock en almacén origen
        StockEntity sourceStock = stockRepository.findByProductIdAndWarehouseId(
            stockChange.getProductId(),
            sourceWarehouseId
        ).orElseThrow(() -> new EntityNotFoundException("No hay stock disponible en el almacén origen"));

        if (sourceStock.getQuantity() < stockChange.getQuantity()) {
            throw new IllegalStateException("Stock insuficiente para la transferencia");
        }

        // Actualizar stock en origen
        sourceStock.setQuantity(sourceStock.getQuantity() - stockChange.getQuantity());
        stockRepository.save(sourceStock);

        // Actualizar o crear stock en destino
        Optional<StockEntity> targetStockOpt = stockRepository.findByProductIdAndWarehouseId(
            stockChange.getProductId(),
            targetWarehouseId
        );

        StockEntity targetStock = targetStockOpt.orElse(new StockEntity());
        targetStock.setProductId(stockChange.getProductId());
        targetStock.setWarehouseId(targetWarehouseId);
        targetStock.setUnit(sourceStock.getUnit());

        if (targetStockOpt.isPresent()) {
            targetStock.setQuantity(targetStock.getQuantity() + stockChange.getQuantity());
        } else {
            targetStock.setQuantity(stockChange.getQuantity());
        }

        stockRepository.save(targetStock);
    }
}
