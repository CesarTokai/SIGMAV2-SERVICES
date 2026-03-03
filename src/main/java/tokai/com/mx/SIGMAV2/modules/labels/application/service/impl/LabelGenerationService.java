package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio especializado en la generación y solicitud de marbetes.
 * Extraído de LabelServiceImpl para cumplir con el Principio de Responsabilidad Única.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelGenerationService {

    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;

    /**
     * Genera marbetes directamente para una lista de productos.
     * Es el método principal de generación: reemplaza el flujo antiguo request→generate.
     */
    @Transactional
    public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
        log.info("🚀 Generando marbetes para {} productos en almacén {} periodo {}",
                dto.getProducts().size(), dto.getWarehouseId(), dto.getPeriodId());

        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        LocalDateTime now = LocalDateTime.now();
        int totalGenerados = 0;

        for (ProductBatchDTO product : dto.getProducts()) {
            int cantidad = product.getLabelsToGenerate();

            // Crear o reutilizar LabelRequest para este producto/periodo/almacén
            Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
                    product.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

            LabelRequest labelRequest;
            if (existing.isPresent()) {
                labelRequest = existing.get();
                log.info("📋 Usando LabelRequest existente (ID: {}) para producto {}",
                        labelRequest.getIdLabelRequest(), product.getProductId());
            } else {
                labelRequest = new LabelRequest();
                labelRequest.setProductId(product.getProductId());
                labelRequest.setWarehouseId(dto.getWarehouseId());
                labelRequest.setPeriodId(dto.getPeriodId());
                labelRequest.setRequestedLabels(cantidad);
                labelRequest.setFoliosGenerados(0);
                labelRequest.setCreatedBy(userId);
                labelRequest.setCreatedAt(now);
                labelRequest = persistence.save(labelRequest);
                log.info("📋 LabelRequest creado (ID: {}) para producto {}",
                        labelRequest.getIdLabelRequest(), product.getProductId());
            }

            // Actualizar foliosGenerados
            labelRequest.setFoliosGenerados(labelRequest.getFoliosGenerados() + cantidad);
            persistence.save(labelRequest);

            long[] range = persistence.allocateFolioRange(dto.getPeriodId(), cantidad);

            List<Label> labels = new ArrayList<>(cantidad);
            for (long folio = range[0]; folio <= range[1]; folio++) {
                Label label = new Label();
                label.setFolio(folio);
                label.setLabelRequestId(labelRequest.getIdLabelRequest());
                label.setPeriodId(dto.getPeriodId());
                label.setWarehouseId(dto.getWarehouseId());
                label.setProductId(product.getProductId());
                label.setEstado(Label.State.GENERADO);
                label.setCreatedBy(userId);
                label.setCreatedAt(now);
                labels.add(label);
            }

            persistence.saveAll(labels);
            totalGenerados += cantidad;

            log.info("✅ Producto {}: {} marbetes (folios {}-{}) asignados a LabelRequest {}",
                    product.getProductId(), cantidad, range[0], range[1], labelRequest.getIdLabelRequest());
        }

        log.info("✅ Total generado: {} marbetes", totalGenerados);
    }
}
