package tokai.com.mx.SIGMAV2.modules.inventory.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.InventoryDto;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.InventoryStock;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.repositories.InventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.repositories.PeriodRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryQueryService {
    
    @Autowired
    private InventoryStockRepository inventoryStockRepository;
    
    @Autowired
    private PeriodRepository periodRepository;
    
    public Page<InventoryDto> getInventory(Long idPeriod, Long idWarehouse, String query, Pageable pageable) {
        // Validar que el periodo existe
        Optional<Period> periodOpt = periodRepository.findById(idPeriod);
        if (periodOpt.isEmpty()) {
            throw new IllegalArgumentException("Periodo no encontrado: " + idPeriod);
        }
        Period period = periodOpt.get();
        
        // Obtener inventarios con filtros
        Page<InventoryStock> stockPage = inventoryStockRepository.findInventoryWithFilters(
            idWarehouse, query, pageable);
        
        // Convertir a DTOs
        List<InventoryDto> inventoryDtos = stockPage.getContent().stream()
            .map(stock -> mapToInventoryDto(stock, period.getPeriod().toString()))
            .collect(Collectors.toList());
        
        return new PageImpl<>(inventoryDtos, pageable, stockPage.getTotalElements());
    }
    
    private InventoryDto mapToInventoryDto(InventoryStock stock, String period) {
        return new InventoryDto(
            stock.getProduct().getCveArt(),
            stock.getProduct().getDescription(),
            stock.getProduct().getUnitOfMeasure(),
            stock.getExistQty(),
            stock.getStatus().name(),
            stock.getWarehouse().getWarehouseKey(),
            stock.getWarehouse().getNameWarehouse(),
            period
        );
    }
}