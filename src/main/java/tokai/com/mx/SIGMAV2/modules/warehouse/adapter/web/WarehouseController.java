package tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.security.WarehouseAccessValidator;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseAccessDeniedException;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseNotFoundException;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.UserWarehouseAssignment;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input.UserWarehouseService;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input.WarehouseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final UserWarehouseService userWarehouseService;
    private final WarehouseAccessValidator accessValidator;

    // ============ CRUD DE ALMACENES ============

    /**
     * Listar almacenes con búsqueda y paginación
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<Map<String, Object>> getAllWarehouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "warehouseKey") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {
        
        log.info("Listando almacenes - page: {}, size: {}, search: '{}'", page, size, search);
        
        try {
            Long currentUserId = getCurrentUserId();
            
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? 
                    Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Warehouse> warehouses;
            
            // Si es ADMINISTRADOR, ve todos; otros solo ven sus almacenes asignados
            if (hasRole("ADMINISTRADOR")) {
                warehouses = (search != null && !search.trim().isEmpty()) 
                    ? warehouseService.findAllWithSearch(search.trim(), pageable)
                    : warehouseService.findAllWarehouses(pageable);
            } else {
                // Filtrar por almacenes asignados al usuario
                warehouses = warehouseService.findWarehousesByUserId(currentUserId, pageable);
            }
            
            List<WarehouseResponseDTO> warehouseDTOs = warehouses.getContent().stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouseDTOs);
            response.put("pagination", Map.of(
                "currentPage", warehouses.getNumber(),
                "totalPages", warehouses.getTotalPages(),
                "totalElements", warehouses.getTotalElements(),
                "pageSize", warehouses.getSize(),
                "hasNext", warehouses.hasNext(),
                "hasPrevious", warehouses.hasPrevious()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al listar almacenes: {}", e.getMessage(), e);
            return handleError("Error interno al listar almacenes", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtener almacén por ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<Map<String, Object>> getWarehouseById(@PathVariable Long id) {
        log.info("Obteniendo almacén ID: {}", id);
        
        try {
            Long currentUserId = getCurrentUserId();
            
            Warehouse warehouse = warehouseService.findByIdWarehouse(id)
                    .orElseThrow(() -> new WarehouseNotFoundException(id));
            
            // Verificar acceso si no es administrador
            if (!hasRole("ADMINISTRADOR")) {
                accessValidator.validateAccess(currentUserId, id);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mapToResponseDTO(warehouse));
            
            return ResponseEntity.ok(response);
            
        } catch (WarehouseNotFoundException e) {
            return handleError("Almacén no encontrado", HttpStatus.NOT_FOUND);
        } catch (WarehouseAccessDeniedException e) {
            return handleError("No tienes acceso a este almacén", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Error al obtener almacén: {}", e.getMessage(), e);
            return handleError("Error interno al obtener el almacén", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Crear nuevo almacén (Solo ADMINISTRADOR)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> createWarehouse(@Valid @RequestBody WarehouseCreateDTO dto) {
        log.info("Creando almacén con clave: {}", dto.getWarehouseKey());
        
        try {
            Long currentUserId = getCurrentUserId();
            
            Warehouse warehouse = warehouseService.createWarehouse(dto, currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Almacén creado exitosamente");
            response.put("data", mapToResponseDTO(warehouse));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            return handleError(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Manejar violaciones de unicidad
            String message = "Ya existe un almacén con esa clave o nombre";
            if (e.getMessage().contains("uk_warehouse_key") || e.getMessage().contains("uk_wh_key")) {
                message = "Ya existe un almacén con la clave: " + dto.getWarehouseKey();
            } else if (e.getMessage().contains("uk_warehouse_name")) {
                message = "Ya existe un almacén con el nombre: " + dto.getNameWarehouse();
            }
            return handleError(message, HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("Error al crear almacén: {}", e.getMessage(), e);
            return handleError("Error interno al crear el almacén", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Actualizar almacén (Solo ADMINISTRADOR)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> updateWarehouse(
            @PathVariable Long id, 
            @Valid @RequestBody WarehouseUpdateDTO dto) {
        
        log.info("Actualizando almacén ID: {}", id);
        
        try {
            Long currentUserId = getCurrentUserId();
            
            Warehouse warehouse = warehouseService.updateWarehouse(id, dto, currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Almacén actualizado exitosamente");
            response.put("data", mapToResponseDTO(warehouse));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return handleError(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (WarehouseNotFoundException e) {
            return handleError("Almacén no encontrado", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al actualizar almacén: {}", e.getMessage(), e);
            return handleError("Error interno al actualizar el almacén", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Eliminar almacén (Solo ADMINISTRADOR)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> deleteWarehouse(@PathVariable Long id) {
        log.info("Eliminando almacén ID: {}", id);

        try {
            Long currentUserId = getCurrentUserId();

            warehouseService.deleteWarehouse(id, currentUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Almacén eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            return handleError("No se puede eliminar, almacén en uso", HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {
            // Capturar específicamente el caso de almacén ya eliminado
            return handleError(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (WarehouseNotFoundException e) {
            return handleError("Almacén no encontrado", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al eliminar almacén: {}", e.getMessage(), e);
            return handleError("Error interno al eliminar el almacén", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============ ASIGNACIÓN USUARIO-ALMACÉN ============

    /**
     * Obtener almacenes asignados a un usuario
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA')")
    public ResponseEntity<Map<String, Object>> getUserWarehouses(@PathVariable Long userId) {
        log.info("Obteniendo almacenes del usuario ID: {}", userId);
        
        try {
            List<UserWarehouseAssignment> assignments = userWarehouseService.findAssignmentsByUserId(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", assignments);
            response.put("total", assignments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener almacenes del usuario: {}", e.getMessage(), e);
            return handleError("Error interno al obtener los almacenes", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Asignar almacenes a un usuario (Solo ADMINISTRADOR)
     */
    @PostMapping("/users/{userId}/assign")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> assignWarehouses(
            @PathVariable Long userId,
            @Valid @RequestBody AssignWarehousesDTO dto) {
        
        log.info("Asignando {} almacenes al usuario ID: {}", dto.getWarehouseIds().size(), userId);
        
        try {
            Long currentUserId = getCurrentUserId();
            
            List<UserWarehouseAssignment> assignments = userWarehouseService.assignWarehouses(userId, dto, currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Almacenes asignados exitosamente");
            response.put("data", assignments);
            response.put("total", assignments.size());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return handleError(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (WarehouseNotFoundException e) {
            return handleError("Uno o más almacenes no fueron encontrados", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al asignar almacenes: {}", e.getMessage(), e);
            return handleError("Error interno al asignar almacenes", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Revocar acceso a un almacén específico (Solo ADMINISTRADOR)
     */
    @DeleteMapping("/users/{userId}/warehouses/{warehouseId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> revokeWarehouse(
            @PathVariable Long userId,
            @PathVariable Long warehouseId) {
        
        log.info("Revocando acceso del usuario {} al almacén {}", userId, warehouseId);
        
        try {
            Long currentUserId = getCurrentUserId();
            
            userWarehouseService.revokeWarehouse(userId, warehouseId, currentUserId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Acceso al almacén revocado exitosamente");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            return handleError(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
        } catch (WarehouseAccessDeniedException e) {
            return handleError("La asignación no existe", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al revocar acceso: {}", e.getMessage(), e);
            return handleError("Error interno al revocar el acceso", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtener mis almacenes asignados
     */
    @GetMapping("/my-warehouses")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<Map<String, Object>> getMyWarehouses() {
        try {
            Long currentUserId = getCurrentUserId();
            
            List<Warehouse> warehouses = warehouseService.findWarehousesByUserId(currentUserId);
            List<WarehouseResponseDTO> warehouseDTOs = warehouses.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", warehouseDTOs);
            response.put("total", warehouseDTOs.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al obtener mis almacenes: {}", e.getMessage(), e);
            return handleError("Error interno al obtener los almacenes", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ============ MÉTODOS UTILITARIOS ============

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Aquí deberías extraer el ID del usuario del token JWT
        // Por ahora, usamos un método simplificado
        return extractUserIdFromAuthentication(auth);
    }

    private Long extractUserIdFromAuthentication(Authentication auth) {
        // Implementar extracción del user ID del JWT
        // Por ahora retornamos un ID fijo para pruebas
        return 10L; // TODO: Implementar extracción real del JWT
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private WarehouseResponseDTO mapToResponseDTO(Warehouse warehouse) {
        WarehouseResponseDTO dto = new WarehouseResponseDTO();
        dto.setId(warehouse.getId());
        dto.setWarehouseKey(warehouse.getWarehouseKey());
        dto.setNameWarehouse(warehouse.getNameWarehouse());
        dto.setObservations(warehouse.getObservations());
        dto.setCreatedAt(warehouse.getCreatedAt());
        dto.setUpdatedAt(warehouse.getUpdatedAt());
        dto.setDeletedAt(warehouse.getDeletedAt());
        dto.setDeleted(warehouse.isDeleted());
        // Conteo de usuarios asignados (convertir null a 0)
        Integer count = warehouse.getAssignedUsersCount() == null ? 0 : warehouse.getAssignedUsersCount().intValue();
        dto.setAssignedUsersCount(count);
        // Los correos del creador/actualizador no están disponibles aquí; se dejan nulos por ahora.
        return dto;
    }


    private ResponseEntity<Map<String, Object>> handleError(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}