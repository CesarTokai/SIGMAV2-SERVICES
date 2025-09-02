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

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para operaciones administrativas de usuarios
 * Requiere privilegios de administrador
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUserController {

    private final UserService userService;
    private final VerificationCodeService verificationCodeService;
    private final VerificationCodeLogRepository verificationCodeLogRepository;

    /**
     * Crea un nuevo usuario desde el panel de administración
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        log.info("Creando usuario desde panel de administración: {}", request.getEmail());

        // Convertir a UserRequest para usar el servicio existente
        UserRequest userRequest = new UserRequest();
        userRequest.setEmail(request.getEmail());
        userRequest.setPassword(request.getPassword());
        userRequest.setRole(request.getRole());

        User user = userService.register(userRequest);

        // Si el admin quiere que esté pre-verificado
        if (request.isPreVerified()) {
            user = userService.forceVerifyUser(user.getId());
        }

        AdminUserResponse userResponse = convertToAdminUserResponse(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario creado exitosamente por administrador");
        response.put("data", userResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
     * Convierte User a AdminUserResponse con información adicional
     */
    private AdminUserResponse convertToAdminUserResponse(User user) {
        // Obtener información adicional de códigos de verificación
        long totalCodes = verificationCodeLogRepository.countResentCodesInTimeRange(
                user.getEmail(), LocalDateTime.now().minusYears(1));
        
        var lastCode = verificationCodeLogRepository.findLastActiveCodeByEmail(user.getEmail());

        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.isStatus())
                .verified(user.isVerified())
                .attempts(user.getAttempts())
                .lastTryAt(user.getLastTryAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .verificationCode(user.getVerificationCode())
                .totalVerificationCodes((int) totalCodes)
                .lastVerificationCodeSent(lastCode.map(code -> code.getCreatedAt()).orElse(null))
                .accountLocked(user.getAttempts() >= 5) // Asumiendo 5 intentos como límite
                .build();
    }
}
