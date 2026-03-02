package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.output.MultiWarehouseWarehousePort;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador que implementa {@link MultiWarehouseWarehousePort} usando
 * el repositorio JPA del módulo Warehouse.
 *
 * <p>Esta clase es el único punto del módulo MultiWarehouse que conoce
 * a {@code WarehouseRepository}. El servicio de aplicación solo conoce
 * el puerto, no esta implementación.
 */
@Component
@RequiredArgsConstructor
public class MultiWarehouseWarehouseAdapter implements MultiWarehouseWarehousePort {

    private static final Logger log = LoggerFactory.getLogger(MultiWarehouseWarehouseAdapter.class);

    private final WarehouseRepository warehouseRepository;

    @Override
    public Optional<Long> findIdByWarehouseKey(String warehouseKey) {
        return warehouseRepository.findByWarehouseKeyAndDeletedAtIsNull(warehouseKey)
            .map(WarehouseEntity::getId);
    }

    @Override
    public List<Long> findIdsByName(String name) {
        return warehouseRepository.findAllByNameWarehouseAndDeletedAtIsNull(name)
            .stream()
            .map(WarehouseEntity::getId)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<String> findNameById(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
            .map(WarehouseEntity::getNameWarehouse);
    }

    @Override
    public Long createWarehouse(String key, String name, String observations) {
        WarehouseEntity entity = new WarehouseEntity();
        entity.setWarehouseKey(key);
        entity.setNameWarehouse(name);
        entity.setObservations(observations);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        WarehouseEntity saved = warehouseRepository.save(entity);
        log.info("Almacen creado via port: key={}, id={}, name={}", key, saved.getId(), name);
        return saved.getId();
    }

    @Override
    public long countActive() {
        return warehouseRepository.count();
    }
}

