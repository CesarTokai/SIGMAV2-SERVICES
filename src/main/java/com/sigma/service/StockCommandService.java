package com.sigma.service;

import com.sigma.dto.StockChange;
import com.sigma.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockCommandService {
    private final StockRepository stockRepository;
    private final WarehouseAccessService warehouseAccessService;

    @Transactional
    public void updateStock(StockChange change) throws AccessDeniedException {
        if (change.getQuantity() == null || change.getProductId() == null ||
            change.getWarehouseId() == null) {
            throw new IllegalArgumentException("Todos los campos son requeridos");
        }

        boolean hasAccess = warehouseAccessService.getAccessibleWarehouses()
                .stream()
                .anyMatch(w -> w.getId().equals(change.getWarehouseId()));

        if (!hasAccess) {
            throw new AccessDeniedException("No tienes acceso a este almacén");
        }

        stockRepository.updateStock(
            change.getProductId(),
            change.getWarehouseId(),
            change.getQuantity(),
            change.getReason()
        );
    }
}
