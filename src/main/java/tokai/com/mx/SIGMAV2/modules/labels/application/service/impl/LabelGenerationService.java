package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
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
    private final JpaProductRepository productRepository;

    /**
     * Genera marbetes directamente para una lista de productos.
     * Es el método principal de generación: reemplaza el flujo antiguo request→generate.
     */
    @Transactional
    public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
        log.info("🚀 Generando marbetes para {} productos en almacén {} periodo {}",
                dto.getProducts().size(), dto.getWarehouseId(), dto.getPeriodId());

        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        List<Long> productosConFoliosExistentes = new ArrayList<>();
        List<LabelRequest> labelRequestsExistentes = new ArrayList<>();
        
        for (ProductBatchDTO product : dto.getProducts()) {
            Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
                    product.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
            
            if (existing.isPresent()) {
                LabelRequest lr = existing.get();
                labelRequestsExistentes.add(lr);
                
                // Solo bloquear si tiene folios generados (> 0)
                if (lr.getFoliosGenerados() != null && lr.getFoliosGenerados() > 0) {
                    productosConFoliosExistentes.add(product.getProductId());
                    log.warn("⚠️ Producto {} ya tiene {} folios generados en período/almacén",
                            product.getProductId(), lr.getFoliosGenerados());
                } else {
                    log.info("ℹ️ Producto {} tiene LabelRequest pero sin folios, se permitirá generar",
                            product.getProductId());
                }
            }
        }

        if (!productosConFoliosExistentes.isEmpty()) {
            throw new InvalidLabelStateException(
                "No se pueden regenerar marbetes para los productos que ya tienen folios: " + productosConFoliosExistentes +
                ". Solo se pueden generar productos sin folios existentes en este período/almacén.");
        }

        // ── Validación de productos: verificar que existen en el catálogo ──────
        List<Long> productosNoEncontrados = new ArrayList<>();
        for (ProductBatchDTO product : dto.getProducts()) {
            if (!productRepository.existsById(product.getProductId())) {
                productosNoEncontrados.add(product.getProductId());
                log.warn("⚠️ Producto {} no existe en el catálogo", product.getProductId());
            }
        }
        
        if (!productosNoEncontrados.isEmpty()) {
            throw new InvalidLabelStateException(
                "No se pueden generar marbetes: Los siguientes productos no existen en el catálogo: " + 
                productosNoEncontrados + ". Verifique que los productos hayan sido importados correctamente.");
        }
        log.info("✅ Todos los productos existen en el catálogo");

        LocalDateTime now = LocalDateTime.now();
        int totalGenerados = 0;

        for (ProductBatchDTO product : dto.getProducts()) {
            int cantidad = product.getLabelsToGenerate();

            // Reutilizar LabelRequest si existe pero sin folios, si no crear uno nuevo
            LabelRequest labelRequest;
            Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
                    product.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
            
            if (existing.isPresent()) {
                labelRequest = existing.get();
                log.info("📋 Reutilizando LabelRequest existente (ID: {}) para producto {}",
                        labelRequest.getIdLabelRequest(), product.getProductId());
                // Actualizar solo foliosGenerados
                labelRequest.setFoliosGenerados(cantidad);
                labelRequest.setRequestedLabels(cantidad);
                persistence.save(labelRequest);
            } else {
                labelRequest = new LabelRequest();
                labelRequest.setProductId(product.getProductId());
                labelRequest.setWarehouseId(dto.getWarehouseId());
                labelRequest.setPeriodId(dto.getPeriodId());
                labelRequest.setRequestedLabels(cantidad);
                labelRequest.setFoliosGenerados(cantidad);
                labelRequest.setCreatedBy(userId);
                labelRequest.setCreatedAt(now);
                labelRequest = persistence.save(labelRequest);
                log.info("📋 LabelRequest creado (ID: {}) para producto {}",
                        labelRequest.getIdLabelRequest(), product.getProductId());
            }


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
