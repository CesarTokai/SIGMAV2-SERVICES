package tokai.com.mx.SIGMAV2.modules.warehouse.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input.WarehouseService;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserWarehouseRepository userWarehouseRepository;
    private final JpaUserRepository userRepository;

    @Override
    public Warehouse createWarehouse(WarehouseCreateDTO dto, Long createdBy) {
        log.info("Creando almacén con clave: {}", dto.getWarehouseKey());
        
        // Normalizar clave a mayúsculas
        String normalizedKey = dto.getWarehouseKey().toUpperCase().trim();
        
        // Validar unicidad
        if (warehouseRepository.existsByWarehouseKeyAndIdNotAndDeletedAtIsNull(normalizedKey, 0L)) {
            throw new IllegalArgumentException("La clave de almacén ya existe: " + normalizedKey);
        }
        
        if (warehouseRepository.existsByNameWarehouseAndIdNotAndDeletedAtIsNull(dto.getNameWarehouse().trim(), 0L)) {
            throw new IllegalArgumentException("El nombre de almacén ya existe: " + dto.getNameWarehouse());
        }
        
        // Validar que el usuario existe
        userRepository.findById(createdBy)
                .orElseThrow(() -> new IllegalArgumentException("Usuario creador no encontrado"));
        
        // Crear entidad
        WarehouseEntity entity = new WarehouseEntity();
        entity.setWarehouseKey(normalizedKey);
        entity.setNameWarehouse(dto.getNameWarehouse().trim());
        entity.setObservations(dto.getObservations() != null ? dto.getObservations().trim() : null);
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        
        WarehouseEntity saved = warehouseRepository.save(entity);
        
        log.info("Almacén creado exitosamente: ID={}, Clave={}", saved.getId(), saved.getWarehouseKey());
        return mapToWarehouse(saved);
    }

    @Override
    public Warehouse updateWarehouse(Long id, WarehouseUpdateDTO dto, Long updatedBy) {
        log.info("Actualizando almacén ID: {}", id);
        
        WarehouseEntity entity = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));
        
        if (entity.isDeleted()) {
            throw new IllegalArgumentException("No se puede actualizar un almacén eliminado");
        }
        
        if (warehouseRepository.existsByNameWarehouseAndIdNotAndDeletedAtIsNull(dto.getNameWarehouse().trim(), id)) {
            throw new IllegalArgumentException("El nombre de almacén ya existe: " + dto.getNameWarehouse());
        }
        
        userRepository.findById(updatedBy)
                .orElseThrow(() -> new IllegalArgumentException("Usuario actualizador no encontrado"));
        
        entity.setNameWarehouse(dto.getNameWarehouse().trim());
        entity.setObservations(dto.getObservations() != null ? dto.getObservations().trim() : null);
        entity.setUpdatedBy(updatedBy);
        
        WarehouseEntity saved = warehouseRepository.save(entity);
        
        log.info("Almacén actualizado exitosamente: ID={}", id);
        return mapToWarehouse(saved);
    }

    @Override
    public void deleteWarehouse(Long id, Long deletedBy) {
        log.info("Eliminando almacén ID: {}", id);
        
        WarehouseEntity entity = warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Almacén no encontrado"));
        
        if (entity.isDeleted()) {
            throw new IllegalArgumentException("El almacén ya está eliminado");
        }
        
        if (hasDependencies(id)) {
            throw new IllegalStateException("No se puede eliminar, almacén en uso");
        }
        
        userRepository.findById(deletedBy)
                .orElseThrow(() -> new IllegalArgumentException("Usuario eliminador no encontrado"));
        
        entity.markAsDeleted(deletedBy);
        warehouseRepository.save(entity);
        
        log.info("Almacén eliminado exitosamente: ID={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findAllWarehouses() {
        return warehouseRepository.findAllByDeletedAtIsNull(Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::mapToWarehouse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> findByIdWarehouse(Long id) {
        return warehouseRepository.findById(id)
                .filter(entity -> !entity.isDeleted())
                .map(this::mapToWarehouse);
    }

    @Override
    public Optional<Warehouse> findByWarehouseKey(String warehouseKey) {
        return Optional.empty();
    }

    @Override
    public Page<Warehouse> findAllWarehouses(Pageable pageable) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findWarehousesByUserId(Long userId) {
        return warehouseRepository.findActiveWarehousesByUserIdList(userId).stream()
                .map(this::mapToWarehouse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Warehouse> findWarehousesByUserId(Long userId, Pageable pageable) {
        return warehouseRepository.findActiveWarehousesByUserIdPage(userId, pageable)
                .map(this::mapToWarehouse);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<Warehouse> findAllWithSearch(String search, Pageable pageable) {
        return warehouseRepository.findAllWithSearch(search, pageable)
                .map(this::mapToWarehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByWarehouseKey(String warehouseKey) {
        return warehouseRepository.findByWarehouseKeyAndDeletedAtIsNull(warehouseKey.toUpperCase()).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameWarehouse(String nameWarehouse) {
        return warehouseRepository.findByNameWarehouseAndDeletedAtIsNull(nameWarehouse).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByWarehouseKeyAndIdNot(String warehouseKey, Long id) {
        return warehouseRepository.existsByWarehouseKeyAndIdNotAndDeletedAtIsNull(warehouseKey.toUpperCase(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByNameWarehouseAndIdNot(String nameWarehouse, Long id) {
        return warehouseRepository.existsByNameWarehouseAndIdNotAndDeletedAtIsNull(nameWarehouse, id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasDependencies(Long warehouseId) {
        // TODO: Implementar cuando existan las tablas reales
        // Por ahora retornamos false para permitir eliminaciones
        log.warn("Verificación de dependencias deshabilitada temporalmente para almacén ID: {}", warehouseId);
        return false;
        
        /* Código original comentado hasta que existan las tablas:
        long inventoryCount = warehouseRepository.countInventoryStockByWarehouseId(warehouseId);
        long labelCount = warehouseRepository.countLabelRequestsByWarehouseId(warehouseId);
        
        return inventoryCount > 0 || labelCount > 0;
        */
    }




    @Transactional(readOnly = true)
    public List<Warehouse> findActiveWarehousesByUserId(Long userId) {
        return warehouseRepository.findActiveWarehousesByUserIdList(userId).stream()
                .map(this::mapToWarehouse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Warehouse> findActiveWarehousesByUserId(Long userId, Pageable pageable) {
        return warehouseRepository.findActiveWarehousesByUserIdPage(userId, pageable)
                .map(this::mapToWarehouse);
    }

    @Transactional(readOnly = true)
    public boolean hasUserAccessToWarehouse(Long userId, Long warehouseId) {
        return userWarehouseRepository.existsByUserIdAndWarehouseIdAndWarehouseDeletedAtIsNull(userId, warehouseId);
    }

    private Warehouse mapToWarehouse(WarehouseEntity entity) {
        if (entity == null) return null;
        return Warehouse.builder()
                .id(entity.getId())
                .warehouseKey(entity.getWarehouseKey())
                .nameWarehouse(entity.getNameWarehouse())
                .observations(entity.getObservations())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .deletedBy(entity.getDeletedBy())
                .assignedUsersCount(userWarehouseRepository.countUsersByWarehouseId(entity.getId()))
                .deleted(entity.isDeleted())
                .build();
    }
}
