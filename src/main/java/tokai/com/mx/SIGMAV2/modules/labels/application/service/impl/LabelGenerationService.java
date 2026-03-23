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

        // ── Separar productos en dos grupos: con folios y sin folios ──────
        List<Long> productosConFoliosExistentes = new ArrayList<>();
        List<ProductBatchDTO> productosAGenerar = new ArrayList<>();
        
        for (ProductBatchDTO product : dto.getProducts()) {
            Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
                    product.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
            
            if (existing.isPresent()) {
                LabelRequest lr = existing.get();
                
                // Si tiene folios generados (> 0), NO puede generarse
                if (lr.getFoliosGenerados() != null && lr.getFoliosGenerados() > 0) {
                    productosConFoliosExistentes.add(product.getProductId());
                    log.warn("⚠️ Producto {} ya tiene {} folios generados, será saltado",
                            product.getProductId(), lr.getFoliosGenerados());
                } else {
                    // Puede generarse (tiene solicitud pero sin folios)
                    productosAGenerar.add(product);
                    log.info("✅ Producto {} puede generarse (solicitud sin folios)",
                            product.getProductId());
                }
            } else {
                // No tiene solicitud previa, pero se puede generar
                productosAGenerar.add(product);
                log.info("✅ Producto {} puede generarse (sin solicitud previa)",
                        product.getProductId());
            }
        }

        // Si todos los productos tienen folios, bloquear
        if (productosAGenerar.isEmpty()) {
            throw new InvalidLabelStateException(
                "No se pueden generar marbetes: Todos los productos ya tienen folios generados: " + 
                productosConFoliosExistentes + ". Solo se pueden generar productos sin folios en este período/almacén.");
        }

        // Si hay algunos productos con folios, avisar pero continuar
        if (!productosConFoliosExistentes.isEmpty()) {
            log.warn("⚠️ Los siguientes productos serán saltados por tener folios: {}", 
                    productosConFoliosExistentes);
        }

        // ── Validación de productos: verificar que existen en el catálogo ──────
        List<Long> productosNoEncontrados = new ArrayList<>();
        for (ProductBatchDTO product : productosAGenerar) {
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
        log.info("✅ Todos los productos a generar existen en el catálogo");

        LocalDateTime now = LocalDateTime.now();
        int totalGenerados = 0;

        for (ProductBatchDTO product : productosAGenerar) {
            int cantidad = product.getLabelsToGenerate();

            // Reutilizar LabelRequest si existe pero sin folios, si no crear uno nuevo
            LabelRequest labelRequest;
            Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
                    product.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
            
            if (existing.isPresent()) {
                labelRequest = existing.get();
                log.info("📋 Reutilizando LabelRequest existente (ID: {}) para producto {}",
                        labelRequest.getIdLabelRequest(), product.getProductId());
                // Actualizar solo foliosGenerados — NO sobrescribir requestedLabels
                // requestedLabels conserva el valor original solicitado
                labelRequest.setFoliosGenerados(cantidad);
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
