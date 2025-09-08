package tokai.com.mx.SIGMAV2.modules.warehouse.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.AssignWarehousesDTO;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseAccessDeniedException;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseNotFoundException;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.UserWarehouseAssignment;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input.UserWarehouseService;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserWarehouseServiceImpl implements UserWarehouseService {

    private final UserWarehouseRepository userWarehouseRepository;
    private final WarehouseRepository warehouseRepository;
    private final JpaUserRepository userRepository;

    @Override
    public List<UserWarehouseAssignment> assignWarehouses(Long userId, AssignWarehousesDTO dto, Long assignedBy) {
        log.info("Asignando {} almacenes al usuario ID: {}", dto.getWarehouseIds().size(), userId);
        
        // Validar que el usuario existe
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));
        
        // Validar que el usuario asignador existe
        userRepository.findById(assignedBy)
                .orElseThrow(() -> new IllegalArgumentException("Usuario asignador no encontrado"));
        
        List<UserWarehouseAssignment> assignments = new ArrayList<>();
        
        for (Long warehouseId : dto.getWarehouseIds()) {
            // Verificar que el almacén existe y no está eliminado
            WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                    .filter(w -> !w.isDeleted())
                    .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));
            
            // Verificar si ya existe la asignación
            if (!userWarehouseRepository.existsByUserIdAndWarehouseIdAndWarehouseDeletedAtIsNull(userId, warehouseId)) {
                // Crear nueva asignación
                UserWarehouseEntity assignment = new UserWarehouseEntity(userId, warehouse, assignedBy);
                UserWarehouseEntity saved = userWarehouseRepository.save(assignment);
                assignments.add(mapToUserWarehouseAssignment(saved));
                
                log.info("Asignación creada: Usuario {} -> Almacén {} por {}", userId, warehouseId, assignedBy);
            } else {
                log.info("Asignación ya existe: Usuario {} -> Almacén {}", userId, warehouseId);
                // Agregar la asignación existente a la respuesta
                userWarehouseRepository.findByUserIdAndWarehouseId(userId, warehouseId)
                        .ifPresent(existing -> assignments.add(mapToUserWarehouseAssignment(existing)));
            }
        }
        
        log.info("Proceso de asignación completado. Total asignaciones: {}", assignments.size());
        return assignments;
    }

    @Override
    public void revokeWarehouse(Long userId, Long warehouseId, Long revokedBy) {
        log.info("Revocando acceso del usuario {} al almacén {}", userId, warehouseId);
        
        // Validar que la asignación existe
        UserWarehouseEntity assignment = userWarehouseRepository.findByUserIdAndWarehouseId(userId, warehouseId)
                .orElseThrow(() -> new WarehouseAccessDeniedException("No existe asignación entre usuario " + userId + " y almacén " + warehouseId));
        
        // Verificar si es el último almacén (regla de negocio)
        long totalWarehouses = userWarehouseRepository.countWarehousesByUserId(userId);
        if (totalWarehouses <= 1) {
            log.warn("Intento de revocar último almacén del usuario {}", userId);
            throw new IllegalStateException("No se puede revocar el último almacén asignado al usuario");
        }
        
        // Eliminar la asignación
        userWarehouseRepository.delete(assignment);
        
        log.info("Asignación revocada exitosamente: Usuario {} -> Almacén {}", userId, warehouseId);
    }

    @Override
    public void revokeAllWarehouses(Long userId, Long revokedBy) {
        log.info("Revocando todos los almacenes del usuario {}", userId);
        
        List<UserWarehouseEntity> assignments = userWarehouseRepository.findByUserIdWithActiveWarehouses(userId);
        
        if (assignments.isEmpty()) {
            log.info("Usuario {} no tiene almacenes asignados", userId);
            return;
        }
        
        userWarehouseRepository.deleteAll(assignments);
        
        log.info("Revocados {} almacenes del usuario {}", assignments.size(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserWarehouseAssignment> findAssignmentsByUserId(Long userId) {
        return userWarehouseRepository.findByUserIdWithActiveWarehouses(userId).stream()
                .map(this::mapToUserWarehouseAssignment)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserWarehouseAssignment> findAssignmentsByWarehouseId(Long warehouseId) {
        return userWarehouseRepository.findByWarehouseIdWithActiveWarehouse(warehouseId).stream()
                .map(this::mapToUserWarehouseAssignment)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserAccessToWarehouse(Long userId, Long warehouseId) {
        return userWarehouseRepository.existsByUserIdAndWarehouseIdAndWarehouseDeletedAtIsNull(userId, warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRevokeWarehouse(Long userId, Long warehouseId) {
        // No se puede revocar si es el último almacén
        long totalWarehouses = userWarehouseRepository.countWarehousesByUserId(userId);
        return totalWarehouses > 1 && hasUserAccessToWarehouse(userId, warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countWarehousesByUserId(Long userId) {
        return userWarehouseRepository.countWarehousesByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByWarehouseId(Long warehouseId) {
        return userWarehouseRepository.countUsersByWarehouseId(warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserAnyWarehouse(Long userId) {
        return userWarehouseRepository.countWarehousesByUserId(userId) > 0;
    }

    private UserWarehouseAssignment mapToUserWarehouseAssignment(UserWarehouseEntity entity) {
        return UserWarehouseAssignment.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .warehouseId(entity.getWarehouse().getId())
                .assignedBy(entity.getAssignedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .warehouseName(entity.getWarehouse().getNameWarehouse())
                .warehouseKey(entity.getWarehouse().getWarehouseKey())
                .build();
    }
}