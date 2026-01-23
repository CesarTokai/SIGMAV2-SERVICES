package tokai.com.mx.SIGMAV2.modules.warehouse.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input.WarehouseService;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        // Normalizar clave a mayúsculas y nombre sin espacios sobrantes
        String normalizedKey = dto.getWarehouseKey().toUpperCase().trim();
        String normalizedName = StringUtils.trimToEmpty(dto.getNameWarehouse());
        
        // Validar unicidad de clave
        boolean keyExists = warehouseRepository.existsByWarehouseKeyAndIdNotAndDeletedAtIsNull(normalizedKey, 0L);
        if (keyExists) {
            throw new IllegalArgumentException("La clave de almacén ya existe: " + normalizedKey);
        }

        // Validar unicidad de nombre (modo robusto)
        List<WarehouseEntity> existingByName = warehouseRepository.findAllByNameWarehouseAndDeletedAtIsNull(normalizedName);
        boolean nameExists = existingByName.stream().anyMatch(w -> w.getId() != null && !w.getId().equals(0L));
        if (nameExists) {
            throw new IllegalArgumentException("El nombre de almacén ya existe: " + normalizedName);
        }

        // Validar que el usuario existe
        userRepository.findById(createdBy)
                .orElseThrow(() -> new IllegalArgumentException("Usuario creador no encontrado"));
        
        // Antes de crear, resolver posibles choques con registros eliminados (índices únicos en BD)
        ensureDeletedConflictsRenamed(normalizedName, normalizedKey, null);

        // Crear entidad
        WarehouseEntity entity = new WarehouseEntity();
        entity.setWarehouseKey(normalizedKey);
        entity.setNameWarehouse(normalizedName);
        // Normalizar observaciones: convertir cadenas vacías a null
        entity.setObservations(StringUtils.trimToNull(dto.getObservations()));
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(createdBy);
        // Establecer timestamps manualmente para garantizar valores no nulos sin depender de auditoría global
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        
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

        if (dto == null) {
            throw new IllegalArgumentException("El DTO de actualización no puede ser nulo");
        }

        // Normalizar el nombre: recortar espacios; comparación y validación serán case-insensitive
        String normalizedName = StringUtils.trimToEmpty(dto.getNameWarehouse());

        // Verificar si el nombre ha cambiado antes de validar (ignorar mayúsculas/minúsculas y espacios)
        String currentName = StringUtils.trimToEmpty(entity.getNameWarehouse());
        if (!normalizedName.equalsIgnoreCase(currentName)) {
            // Verificar si existe otro almacén activo con el mismo nombre (case-insensitive) excluyendo el propio ID
            boolean nameExists = warehouseRepository.existsByNameWarehouseAndIdNotAndDeletedAtIsNull(normalizedName, id);
            if (nameExists) {
                throw new IllegalArgumentException("El nombre de almacén ya existe: " + normalizedName);
            }
        }

        userRepository.findById(updatedBy)
                .orElseThrow(() -> new IllegalArgumentException("Usuario actualizador no encontrado"));

        // Resolver conflictos con registros eliminados que compartan el mismo nombre ANTES de modificar el entity,
        // para evitar auto-flush de JPA que podría violar la restricción única.
        ensureDeletedConflictsRenamed(normalizedName, null, id);

        // Ahora sí, aplicar los cambios al entity
        entity.setNameWarehouse(normalizedName);
        // Actualizar observaciones solo si vienen en el DTO (para no sobrescribir con null si no se envía)
        if (dto.getObservations() != null) {
            // Permitimos limpiar el campo si el cliente envía una cadena vacía;
            // StringUtils.trimToNull convertirá "" o espacios a null.
            entity.setObservations(StringUtils.trimToNull(dto.getObservations()));
        }
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedAt(LocalDateTime.now());

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
            log.warn("Intento de eliminar un almacén ya eliminado: ID={}", id);
            throw new IllegalArgumentException("El almacén ya está eliminado");
        }

        if (hasDependencies(id)) {
            throw new IllegalStateException("No se puede eliminar, almacén en uso");
        }

        userRepository.findById(deletedBy)
                .orElseThrow(() -> new IllegalArgumentException("Usuario eliminador no encontrado"));

        entity.markAsDeleted(deletedBy);
        // Al archivar (soft-delete), renombrar para liberar restricciones únicas en BD
        entity.setNameWarehouse(archiveLabel(entity.getNameWarehouse(), entity.getId()));
        entity.setWarehouseKey(archiveKey(entity.getWarehouseKey(), entity.getId()));
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
        // Devolver página de almacenes activos (deleted_at IS NULL) mapeando a modelo de dominio
        return warehouseRepository.findAllByDeletedAtIsNull(pageable)
                .map(this::mapToWarehouse);
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

    private void ensureDeletedConflictsRenamed(String nameOrNull, String keyOrNull, Long currentId) {
        // Renombrar registros eliminados que choquen con el nombre o clave solicitados
        // para no violar índices únicos en BD al crear/actualizar.
        if (nameOrNull != null && !nameOrNull.isBlank()) {
            List<WarehouseEntity> conflictsByName = warehouseRepository.findDeletedByNameIgnoreCase(nameOrNull);
            if (!conflictsByName.isEmpty()) {
                for (WarehouseEntity w : conflictsByName) {
                    if (currentId != null && currentId.equals(w.getId())) continue;
                    w.setNameWarehouse(archiveLabel(w.getNameWarehouse(), w.getId()));
                }
                warehouseRepository.saveAll(conflictsByName);
            }
        }
        if (keyOrNull != null && !keyOrNull.isBlank()) {
            List<WarehouseEntity> conflictsByKey = warehouseRepository.findDeletedByKeyIgnoreCase(keyOrNull);
            if (!conflictsByKey.isEmpty()) {
                for (WarehouseEntity w : conflictsByKey) {
                    if (currentId != null && currentId.equals(w.getId())) continue;
                    w.setWarehouseKey(archiveKey(w.getWarehouseKey(), w.getId()));
                }
                warehouseRepository.saveAll(conflictsByKey);
            }
        }
    }

    private String archiveLabel(String original, Long id) {
        String base = original == null ? "" : original.trim();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = " [DEL-" + ts + (id != null ? ("-" + id) : "") + "]";
        return base + suffix;
    }

    private String archiveKey(String original, Long id) {
        String base = original == null ? "" : original.trim();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = "_DEL_" + ts + (id != null ? ("_" + id) : "");
        int maxLen = 50; // column length
        if (base.length() + suffix.length() > maxLen) {
            base = base.substring(0, Math.max(0, maxLen - suffix.length()));
        }
        return base + suffix;
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
