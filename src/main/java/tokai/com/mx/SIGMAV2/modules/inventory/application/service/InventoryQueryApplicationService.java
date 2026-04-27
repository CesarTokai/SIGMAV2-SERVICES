package tokai.com.mx.SIGMAV2.modules.inventory.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryPeriodReportDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.InventorySnapshotJpaEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventorySnapshotRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryQueryApplicationService {

    private final JpaInventorySnapshotRepository jpaInventorySnapshotRepository;
    private final JpaProductRepository jpaProductRepository;

    public List<InventoryPeriodReportDTO> getPeriodReport(Long periodId, Long warehouseId, String search) {
        List<InventorySnapshotJpaEntity> entities =
                jpaInventorySnapshotRepository.findByPeriodWithSearchNoPage(periodId, warehouseId, search);

        Set<Long> productIds = entities.stream()
                .map(InventorySnapshotJpaEntity::getProductId)
                .collect(Collectors.toSet());

        Map<Long, ProductEntity> productCache = jpaProductRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(ProductEntity::getIdProduct, p -> p));

        List<InventoryPeriodReportDTO> reportList = new ArrayList<>();
        for (InventorySnapshotJpaEntity e : entities) {
            InventoryPeriodReportDTO dto = new InventoryPeriodReportDTO();
            ProductEntity pe = productCache.get(e.getProductId());
            if (pe != null) {
                dto.setCveArt(pe.getCveArt());
                dto.setDescr(pe.getDescr());
                dto.setUniMed(pe.getUniMed());
            } else {
                dto.setCveArt("N/A");
                dto.setDescr("Producto no encontrado");
                dto.setUniMed("-");
            }
            dto.setExistQty(e.getExistQty());
            dto.setStatus(e.getStatus());
            reportList.add(dto);
        }
        return reportList;
    }
}
