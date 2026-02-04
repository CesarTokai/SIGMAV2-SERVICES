package tokai.com.mx.SIGMAV2.modules.users.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.users.application.service.VerificationCodeService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.VerificationCodeLogRepository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence.JpaPersonalInformationRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.repository.UserWarehouseAssignmentRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseRepository;
import tokai.com.mx.SIGMAV2.security.infrastructure.repository.RevokedTokenRepository;

import jakarta.validation.Valid;
import tokai.com.mx.SIGMAV2.shared.response.CustomResponse;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para operaciones administrativas de usuarios
 * Requiere privilegios de administrador
 */
@Slf4j
@RestController
@RequestMapping("/api/sigmav2/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUserController {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;
    private final VerificationCodeLogRepository verificationCodeLogRepository;
    private final JpaPersonalInformationRepository personalInformationRepository;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * Crea un nuevo usuario desde el panel de administración
     */
    @PostMapping
    public ResponseEntity<CustomResponse<AdminUserResponse>> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        log.info("Creando usuario desde panel de administración: {}", request.getEmail());
        try {
            UserRequest userRequest = mapToUserRequest(request);
            User user = userService.register(userRequest);

            AdminUserResponse userResponse = convertToAdminUserResponse(user);

            CustomResponse<AdminUserResponse> response = new CustomResponse<>(
                    userResponse, false, HttpStatus.CREATED.value(), "Usuario creado exitosamente por administrador"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (tokai.com.mx.SIGMAV2.shared.exception.WeakPasswordException e) {
            log.warn("Contraseña débil para usuario {}: {}", request.getEmail(), e.getMessage());
            CustomResponse<AdminUserResponse> errorResponse = new CustomResponse<>(
                    null, true, HttpStatus.BAD_REQUEST.value(), e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (tokai.com.mx.SIGMAV2.shared.exception.InvalidEmailFormatException e) {
            log.warn("Email inválido: {}", e.getMessage());
            CustomResponse<AdminUserResponse> errorResponse = new CustomResponse<>(
                    null, true, HttpStatus.BAD_REQUEST.value(), e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error al crear usuario {}: {}", request.getEmail(), e.getMessage(), e);
            CustomResponse<AdminUserResponse> errorResponse = new CustomResponse<>(
                    null, true, HttpStatus.BAD_REQUEST.value(), "Error al crear usuario: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Obtiene todos los usuarios con paginación y filtros
     */
    @GetMapping
    public ResponseEntity<AdminUserPageResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Boolean status) {
        
        log.info("Obteniendo usuarios - page: {}, size: {}, sortBy: {}, email: {}, role: {}, verified: {}, status: {}", 
                page, size, sortBy, email, role, verified, status);

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;
        if (email != null || role != null || verified != null || status != null) {
            userPage = userService.searchUsers(email, role, verified, status, pageable);
        } else {
            userPage = userService.findAllUsers(pageable);
        }

        // Convertir a DTOs con información adicional
        List<AdminUserResponse> userResponses = userPage.getContent().stream()
                .map(this::convertToAdminUserResponse)
                .collect(Collectors.toList());

        // Estadísticas
        long totalVerified = userService.countByVerified(true);
        long totalUnverified = userService.countByVerified(false);
        long totalActive = userService.countByStatus(true);
        long totalInactive = userService.countByStatus(false);

        AdminUserPageResponse response = AdminUserPageResponse.builder()
                .users(userResponses)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .totalVerifiedUsers(totalVerified)
                .totalUnverifiedUsers(totalUnverified)
                .totalActiveUsers(totalActive)
                .totalInactiveUsers(totalInactive)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene un usuario específico por ID
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long userId) {
        log.info("Obteniendo usuario por ID: {}", userId);

        User user = userService.findById(userId).orElseThrow(() -> 
                new RuntimeException("Usuario no encontrado"));

        AdminUserResponse userResponse = convertToAdminUserResponse(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", userResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza un usuario
     */
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        
        log.info("Actualizando usuario ID: {} con datos: {}", userId, request);

        User user = userService.findById(userId).orElseThrow(() -> 
                new RuntimeException("Usuario no encontrado"));

        // Aplicar cambios
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().toLowerCase().trim());
        }
        if (request.getRole() != null) {
            user.setRole(tokai.com.mx.SIGMAV2.modules.users.domain.model.Role.valueOf(request.getRole()));
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getVerified() != null) {
            user.setVerified(request.getVerified());
        }
        if (Boolean.TRUE.equals(request.getResetAttempts())) {
            user.setAttempts(0);
            user.setLastTryAt(null);
        }

        User updatedUser = userService.update(user);
        AdminUserResponse userResponse = convertToAdminUserResponse(updatedUser);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario actualizado exitosamente");
        response.put("data", userResponse);

        return ResponseEntity.ok(response);
    }


    /**
     * Actualiza solo el rol de un usuario
     */
    @PutMapping("/{userId}/role")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        log.info("Actualizando solo el rol del usuario ID: {} a {}", userId, role);
        User updatedUser = userService.updateUserRole(userId, role);
        AdminUserResponse userResponse = convertToAdminUserResponse(updatedUser);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Rol actualizado exitosamente");
        response.put("data", userResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina un usuario
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        log.info("Eliminando usuario ID: {}", userId);

        User user = userService.findById(userId).orElseThrow(() -> 
                new RuntimeException("Usuario no encontrado"));

        userService.deleteByEmail(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario eliminado exitosamente");
        response.put("userId", userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Fuerza la verificación de un usuario
     */
    @PostMapping("/{userId}/force-verify")
    public ResponseEntity<Map<String, Object>> forceVerifyUser(@PathVariable Long userId) {
        log.info("Forzando verificación del usuario ID: {}", userId);

        User user = userService.forceVerifyUser(userId);
        AdminUserResponse userResponse = convertToAdminUserResponse(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario verificado forzosamente");
        response.put("data", userResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Resetea los intentos de un usuario
     */
    @PostMapping("/{userId}/reset-attempts")
    public ResponseEntity<Map<String, Object>> resetUserAttempts(@PathVariable Long userId) {
        log.info("Reseteando intentos del usuario ID: {}", userId);

        User user = userService.resetUserAttempts(userId);
        AdminUserResponse userResponse = convertToAdminUserResponse(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Intentos de usuario reseteados");
        response.put("data", userResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Cambia el estado de un usuario (activo/inactivo)
     */
    @PostMapping("/{userId}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(@PathVariable Long userId) {
        log.info("Cambiando estado del usuario ID: {}", userId);

        User user = userService.toggleUserStatus(userId);
        AdminUserResponse userResponse = convertToAdminUserResponse(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Estado del usuario cambiado");
        response.put("data", userResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Reenvía código de verificación a un usuario
     */
    @PostMapping("/{userId}/resend-verification")
    public ResponseEntity<Map<String, Object>> resendVerificationCode(@PathVariable Long userId) {
        log.info("Reenviando código de verificación al usuario ID: {}", userId);

        User user = userService.findById(userId).orElseThrow(() -> 
                new RuntimeException("Usuario no encontrado"));

        userService.resendVerificationCode(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Código de verificación reenviado");
        response.put("email", user.getEmail());

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene estadísticas generales de usuarios
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        log.info("Obteniendo estadísticas de usuarios");

        long totalUsers = userService.countByVerified(true) + userService.countByVerified(false);
        long verifiedUsers = userService.countByVerified(true);
        long unverifiedUsers = userService.countByVerified(false);
        long activeUsers = userService.countByStatus(true);
        long inactiveUsers = userService.countByStatus(false);

        List<User> oldUnverifiedUsers = userService.findUnverifiedUsersOlderThan(7);
        long oldUnverifiedCount = oldUnverifiedUsers.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("verifiedUsers", verifiedUsers);
        stats.put("unverifiedUsers", unverifiedUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("inactiveUsers", inactiveUsers);
        stats.put("oldUnverifiedUsers", oldUnverifiedCount);
        stats.put("verificationRate", totalUsers > 0 ? (double) verifiedUsers / totalUsers * 100 : 0);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Operaciones masivas en usuarios
     */
    @PostMapping("/bulk-action")
    public ResponseEntity<Map<String, Object>> bulkUserAction(@Valid @RequestBody BulkUserActionRequest request) {
        log.info("Ejecutando acción masiva: {} en {} usuarios", request.getAction(), request.getUserIds().size());

        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        for (Long userId : request.getUserIds()) {
            try {
                switch (request.getAction()) {
                    case FORCE_VERIFY:
                        userService.forceVerifyUser(userId);
                        break;
                    case RESET_ATTEMPTS:
                        userService.resetUserAttempts(userId);
                        break;
                    case ACTIVATE:
                    case DEACTIVATE:
                        // Verificar estado actual antes de cambiar
                        User user = userService.findById(userId).orElseThrow(() -> 
                                new RuntimeException("Usuario no encontrado"));
                        boolean targetStatus = request.getAction() == BulkUserActionRequest.BulkAction.ACTIVATE;
                        if (user.isStatus() != targetStatus) {
                            userService.toggleUserStatus(userId);
                        }
                        break;
                    case DELETE:
                        User userToDelete = userService.findById(userId).orElseThrow(() -> 
                                new RuntimeException("Usuario no encontrado"));
                        userService.deleteByEmail(userToDelete.getEmail());
                        break;
                    case CHANGE_ROLE:
                        if (request.getNewRole() != null) {
                            User userToUpdate = userService.findById(userId).orElseThrow(() -> 
                                    new RuntimeException("Usuario no encontrado"));
                            userToUpdate.setRole(tokai.com.mx.SIGMAV2.modules.users.domain.model.Role.valueOf(request.getNewRole()));
                            userService.update(userToUpdate);
                        }
                        break;
                    case RESEND_VERIFICATION:
                        User userForResend = userService.findById(userId).orElseThrow(() -> 
                                new RuntimeException("Usuario no encontrado"));
                        
                        // Verificar si el usuario ya está verificado
                        if (userForResend.isVerified()) {
                            throw new RuntimeException("El usuario ya está verificado");
                        }
                        
                        // Usar el servicio de verificación para control de rate limiting
                        verificationCodeService.generateVerificationCode(
                                userForResend.getEmail(), 
                                "Reenvío solicitado por administrador"
                        );
                        userService.resendVerificationCode(userForResend.getEmail());
                        break;
                }
                successCount++;
            } catch (Exception e) {
                errorCount++;
                errors.add(String.format("Usuario ID %d: %s", userId, e.getMessage()));
                log.warn("Error en acción masiva para usuario {}: {}", userId, e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", errorCount == 0);
        response.put("message", String.format("Acción completada: %d exitosos, %d errores", successCount, errorCount));
        response.put("successCount", successCount);
        response.put("errorCount", errorCount);
        response.put("errors", errors);
        response.put("action", request.getAction());

        return ResponseEntity.ok(response);
    }

    /**
     * Limpia usuarios no verificados antiguos
     */
    @DeleteMapping("/cleanup-unverified")
    public ResponseEntity<Map<String, Object>> cleanupUnverifiedUsers(
            @RequestParam(defaultValue = "30") int daysOld) {
        
        log.info("Iniciando limpieza de usuarios no verificados hace más de {} días", daysOld);

        int deletedCount = userService.cleanupUnverifiedUsers(daysOld);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", String.format("Limpieza completada: %d usuarios eliminados", deletedCount));
        response.put("deletedCount", deletedCount);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista usuarios que tienen al menos un almacén asignado (paginado)
     */
    @GetMapping("/with-warehouses")
    public ResponseEntity<Map<String, Object>> getUsersWithWarehouses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "email") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        var assignmentsPage = userWarehouseAssignmentRepository.findUsersWithActiveWarehouses(pageable);

        List<UserWarehouseSummaryResponse> users = assignmentsPage.getContent().stream()
                .map(projection -> userService.findById(projection.getUserId())
                        .map(user -> UserWarehouseSummaryResponse.builder()
                                .userId(user.getId())
                                .email(user.getEmail())
                                .role(user.getRole().name())
                                .status(user.isStatus())
                                .warehousesCount(projection.getWarehousesCount())
                                .warehouseIds(userWarehouseAssignmentRepository.findWarehouseIdsByUserId(user.getId()))
                                .build())
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", users);
        response.put("totalElements", assignmentsPage.getTotalElements());
        response.put("totalPages", assignmentsPage.getTotalPages());
        response.put("currentPage", assignmentsPage.getNumber());
        response.put("pageSize", assignmentsPage.getSize());
        response.put("hasNext", assignmentsPage.hasNext());
        response.put("hasPrevious", assignmentsPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    /**
     * Mapea AdminCreateUserRequest a UserRequest
     */
    private UserRequest mapToUserRequest(AdminCreateUserRequest request) {
        UserRequest userRequest = new UserRequest();
        userRequest.setEmail(request.getEmail());
        userRequest.setPassword(request.getPassword());
        userRequest.setRole(request.getRole());
        userRequest.setName(request.getName());
        userRequest.setFirstLastName(request.getFirstLastName());
        userRequest.setSecondLastName(request.getSecondLastName());
        userRequest.setPhoneNumber(request.getPhoneNumber());
        userRequest.setComments(request.getComments());
        userRequest.setStatus(request.isStatus());
        userRequest.setPreVerified(request.isPreVerified());
        return userRequest;
    }

    /**
     * Convierte User a AdminUserResponse con información adicional
     */
    private AdminUserResponse convertToAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.isStatus())
                .build();
    }
}
