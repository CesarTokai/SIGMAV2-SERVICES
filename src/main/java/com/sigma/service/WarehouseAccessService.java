package com.sigma.service;

import com.sigma.security.SecurityUtils;
import com.sigma.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseAccessDeniedException;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseAccessService {
    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    public List<Warehouse> getAccessibleWarehouses() {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            Pageable unpaged = Pageable.unpaged();
            return warehouseRepository.findAllWarehousesByUserIdIncludingDeleted(userId, unpaged).stream()
                    .map(warehouseMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (SecurityException e) {
            throw new WarehouseAccessDeniedException("Error de autenticación: " + e.getMessage());
        }
    }

    public boolean canAccess(Long warehouseId) {
        if (warehouseId == null) {
            return false;
        }
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            return warehouseRepository.hasAccessToWarehouse(userId, warehouseId);
        } catch (SecurityException e) {
            return false;
        }
    }

    public void validateAccess(Long warehouseId) {
        if (warehouseId == null) {
            throw new WarehouseAccessDeniedException("ID de almacén no puede ser nulo");
        }
        if (!canAccess(warehouseId)) {
            throw new WarehouseAccessDeniedException("No tienes acceso a este almacén");
        }
    }
}
