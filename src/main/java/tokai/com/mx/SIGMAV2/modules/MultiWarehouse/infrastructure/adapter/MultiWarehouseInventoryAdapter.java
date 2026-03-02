package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.output.MultiWarehouseInventoryPort;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Adaptador que implementa {@link MultiWarehouseInventoryPort} usando
 * los repositorios JPA del módulo Inventory.
 *
 * <p>Centraliza el acceso a productos e inventory_stock en un único lugar,
 * desacoplando el servicio de aplicación de los repositorios JPA concretos.
 */
@Component
@RequiredArgsConstructor
public class MultiWarehouseInventoryAdapter implements MultiWarehouseInventoryPort {

    private static final Logger log = LoggerFactory.getLogger(MultiWarehouseInventoryAdapter.class);

    private final JpaProductRepository productRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;
    /** Necesario para obtener proxies JPA válidos de WarehouseEntity del módulo inventory. */
    private final JpaWarehouseRepository inventoryWarehouseRepository;


    @Override
    public Optional<Long> findProductIdByCveArt(String cveArt) {
        return productRepository.findByCveArt(cveArt)
            .map(ProductEntity::getIdProduct);
    }

    @Override
    public Optional<String> findProductDescrById(Long productId) {
        return productRepository.findById(productId)
            .map(ProductEntity::getDescr);
    }

    @Override
    public Long createProduct(String cveArt, String description) {
        ProductEntity entity = new ProductEntity();
        entity.setCveArt(cveArt);
        entity.setDescr(description);
        entity.setStatus("A");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUniMed("PZA");
        ProductEntity saved = productRepository.save(entity);
        log.info("Producto creado via port: cveArt={}, id={}", cveArt, saved.getIdProduct());
        return saved.getIdProduct();
    }

    @Override
    public long countProducts() {
        return productRepository.count();
    }

    // -------------------------------------------------------------------------
    // Inventory Stock
    // -------------------------------------------------------------------------

    @Override
    public Optional<BigDecimal> findStock(Long productId, Long warehouseId, Long periodId) {
        return inventoryStockRepository
            .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId)
            .map(InventoryStockEntity::getExistQty);
    }

    @Override
    public void upsertStock(Long productId, Long warehouseId, Long periodId,
                            BigDecimal qty, String status) {
        if (productId == null || warehouseId == null || periodId == null) {
            log.warn("upsertStock omitido: productId={}, warehouseId={}, periodId={}",
                     productId, warehouseId, periodId);
            return;
        }
        try {
            InventoryStockEntity.Status inventoryStatus = "B".equalsIgnoreCase(status)
                ? InventoryStockEntity.Status.B
                : InventoryStockEntity.Status.A;

            var existing = inventoryStockRepository
                .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId);

            if (existing.isPresent()) {
                var se = existing.get();
                se.setExistQty(qty != null ? qty : BigDecimal.ZERO);
                se.setStatus(inventoryStatus);
                se.setUpdatedAt(LocalDateTime.now());
                inventoryStockRepository.save(se);
            } else {
                var ns = new InventoryStockEntity();
                // Usar getReferenceById para obtener proxies JPA válidos (no entidades detached)
                ns.setProduct(productRepository.getReferenceById(productId));
                ns.setWarehouse(inventoryWarehouseRepository.getReferenceById(warehouseId));
                ns.setPeriodId(periodId);
                ns.setExistQty(qty != null ? qty : BigDecimal.ZERO);
                ns.setStatus(inventoryStatus);
                ns.setCreatedAt(LocalDateTime.now());
                ns.setUpdatedAt(LocalDateTime.now());
                inventoryStockRepository.save(ns);
            }
            log.debug("upsertStock OK: prod={}, wh={}, period={}, qty={}", productId, warehouseId, periodId, qty);
        } catch (Exception e) {
            log.error("Error en upsertStock prod={}, wh={}, period={}: {}", productId, warehouseId, periodId, e.getMessage(), e);
        }
    }
}

