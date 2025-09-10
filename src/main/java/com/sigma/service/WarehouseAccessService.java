package com.sigma.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.sigma.security.SecurityUtils;
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
        Long userId = SecurityUtils.getCurrentUserId();
        return warehouseRepository.findWarehousesByUserId(userId).stream()
                .map(warehouseMapper::toDomain)
                .collect(Collectors.toList());
    }
}
