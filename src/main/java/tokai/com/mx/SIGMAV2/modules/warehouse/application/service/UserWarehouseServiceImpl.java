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
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.UserWarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.UserWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseRepository;
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

    // Alias de tipos para mejor legibilidad
    private static final Class<WarehouseEntity> WAREHOUSE_ENTITY = WarehouseEntity.class;
    private static final Class<UserWarehouseEntity> USER_WAREHOUSE_ENTITY = UserWarehouseEntity.class;

    @Override
    public List<UserWarehouseAssignment> assignWarehouses(Long userId, AssignWarehousesDTO dto, Long assignedBy) {
        log.info("Asignando {} almacenes al usuario ID: {}", dto.getWarehouseIds().size(), userId);
        
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));
        
        userRepository.findById(assignedBy)
                .orElseThrow(() -> new IllegalArgumentException("Usuario asignador no encontrado"));
        
        List<UserWarehouseAssignment> assignments = new ArrayList<>();
        
        for (Long warehouseId : dto.getWarehouseIds()) {
            WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                    .filter(w -> !w.isDeleted())
                    .orElseThrow(() -> new WarehouseNotFoundException(warehouseId));
            
            // Buscar asignación existente
            var existingAssignment = userWarehouseRepository.findByUserIdAndWarehouseId(userId, warehouseId);

            if (existingAssignment.isPresent()) {
                // Si ya existe, solo activarla si no está activa
                UserWarehouseEntity assignment = existingAssignment.get();
                if (!assignment.getIsActive()) {
                    assignment.setIsActive(true);
                    userWarehouseRepository.save(assignment);
                    log.info("Asignación reactivada: Usuario {} -> Almacén {}", userId, warehouseId);
                } else {
                    log.info("Asignación ya existe y está activa: Usuario {} -> Almacén {}", userId, warehouseId);
                }
                assignments.add(mapToUserWarehouseAssignment(assignment));
            } else {
                // Si no existe, crear nueva asignación (isActive = true por defecto)
                UserWarehouseEntity assignment = new UserWarehouseEntity(userId, warehouse, assignedBy);
                userWarehouseRepository.save(assignment);
                assignments.add(mapToUserWarehouseAssignment(assignment));
                log.info("Asignación creada: Usuario {} -> Almacén {} por {}", userId, warehouseId, assignedBy);
            }
        }
        
        return assignments;
    }

    @Override
    public void revokeWarehouse(Long userId, Long warehouseId, Long revokedBy) {
        log.info("Revocando acceso del usuario {} al almacén {}", userId, warehouseId);
        
        UserWarehouseEntity assignment = userWarehouseRepository.findByUserIdAndWarehouseId(userId, warehouseId)
                .orElseThrow(() -> new WarehouseAccessDeniedException(
                    "No existe asignación entre usuario " + userId + " y almacén " + warehouseId));

        // Eliminar la asignación completamente (hard delete)
        userWarehouseRepository.delete(assignment);
        log.info("Acceso revocado (eliminado): Usuario {} -> Almacén {}", userId, warehouseId);
    }

    @Override
    public void revokeAllWarehouses(Long userId, Long revokedBy) {
        log.info("Revocando todos los almacenes del usuario {}", userId);
        
        List<UserWarehouseEntity> activeAssignments = userWarehouseRepository.findByUserIdWithActiveWarehouses(userId);

        if (!activeAssignments.isEmpty()) {
            // Desactivar todos en lugar de eliminar (soft delete)
            activeAssignments.forEach(assignment -> assignment.setIsActive(false));
            userWarehouseRepository.saveAll(activeAssignments);
            log.info("Revocados (desactivados) {} almacenes del usuario {}", activeAssignments.size(), userId);
        } else {
            log.info("Usuario {} no tiene almacenes activos asignados", userId);
        }
    }

    @Override
    public List<UserWarehouseAssignment> findAssignmentsByUserId(Long userId) {
        return userWarehouseRepository.findByUserIdWithActiveWarehouses(userId)
                .stream()
                .map(this::mapToUserWarehouseAssignment)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserWarehouseAssignment> findAssignmentsByWarehouseId(Long warehouseId) {
        return userWarehouseRepository.findByWarehouseIdWithActiveWarehouse(warehouseId)
                .stream()
                .map(this::mapToUserWarehouseAssignment)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasUserAccessToWarehouse(Long userId, Long warehouseId) {
        return userWarehouseRepository.existsByUserIdAndWarehouseIdAndWarehouseDeletedAtIsNull(userId, warehouseId);
    }

    @Override
    public boolean canRevokeWarehouse(Long userId, Long warehouseId) {
        return userWarehouseRepository.countWarehousesByUserId(userId) > 1 &&
               hasUserAccessToWarehouse(userId, warehouseId);
    }

    @Override
    public long countWarehousesByUserId(Long userId) {
        return userWarehouseRepository.countWarehousesByUserId(userId);
    }

    @Override
    public long countUsersByWarehouseId(Long warehouseId) {
        return userWarehouseRepository.countUsersByWarehouseId(warehouseId);
    }

    @Override
    public boolean hasUserAnyWarehouse(Long userId) {
        return userWarehouseRepository.countWarehousesByUserId(userId) > 0;
    }

    private UserWarehouseAssignment mapToUserWarehouseAssignment(UserWarehouseEntity entity) {
        if (entity == null) return null;

        return UserWarehouseAssignment.builder()
                .userId(entity.getUserId())
                .warehouseId(entity.getWarehouse().getId())
                .assignedBy(entity.getAssignedBy())
                .assignedAt(entity.getCreatedAt())
                .build();
    }
}

