package tokai.com.mx.SIGMAV2.modules.users.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserCompleteResponse;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.PersonalInformationService;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.UserWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.repository.IRequestRecoveryPassword;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestStatus;
import tokai.com.mx.SIGMAV2.security.infrastructure.adapter.SecurityUserAdapter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/users")
@RequiredArgsConstructor
public class UserCompleteController {

    private final UserService userService;
    private final PersonalInformationService personalInformationService;
    private final UserWarehouseRepository userWarehouseRepository;
    private final IRequestRecoveryPassword requestRecoveryPasswordRepository;
    private final SecurityUserAdapter securityUserAdapter;

    /**
     * Obtiene información completa del usuario autenticado (usuario + información personal)
     */
    @GetMapping("/me/complete")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getMyCompleteInfo() {
        log.info("Obteniendo información completa del usuario autenticado");

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();

            // Extracción robusta del email/username del principal
            String email;
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            } else {
                email = authentication.getName();
            }

            log.info("Principal type: {}, extracted identifier: {}", principal != null ? principal.getClass().getName() : "null", email);

            Optional<UserCompleteResponse> userCompleteOpt = getUserCompleteInfo(email);

            if (userCompleteOpt.isEmpty()) {
                // Loguear para depuración: intentar mostrar si existe usuario en DB con ese email
                log.warn("No se encontró usuario con email extraído: {}", email);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Usuario no encontrado");
                result.put("emailSearched", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }

            UserCompleteResponse userComplete = userCompleteOpt.get();

            // Loguear id y si se encontró información personal
            log.info("Usuario encontrado id={}, email={}, hasPersonalInformation={}",
                    userComplete.getUserId(), userComplete.getEmail(), userComplete.getPersonalInformation() != null);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", userComplete);
            result.put("hasPersonalInformation", userComplete.getPersonalInformation() != null);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error al obtener información completa del usuario: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al obtener la información del usuario");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Obtiene información completa de un usuario por email (Solo Administradores y Almacenistas)
     */
    @GetMapping("/complete/email/{email}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA')")
    public ResponseEntity<Map<String, Object>> getUserCompleteByEmail(@PathVariable String email) {
        log.info("Obteniendo información completa para usuario: {}", email);
        
        try {
            Optional<UserCompleteResponse> userCompleteOpt = getUserCompleteInfo(email);
            
            if (userCompleteOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Usuario no encontrado: " + email);
                result.put("email", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            UserCompleteResponse userComplete = userCompleteOpt.get();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", userComplete);
            result.put("hasPersonalInformation", userComplete.getPersonalInformation() != null);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al obtener información completa del usuario {}: {}", email, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al obtener la información del usuario");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Obtiene información completa de un usuario por ID (Solo Administradores y Almacenistas)
     */
    @GetMapping("/complete/id/{userId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA')")
    public ResponseEntity<Map<String, Object>> getUserCompleteById(@PathVariable Long userId) {
        log.info("Obteniendo información completa para usuario ID: {}", userId);
        
        try {
            Optional<UserCompleteResponse> userCompleteOpt = getUserCompleteInfoById(userId);
            
            if (userCompleteOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Usuario no encontrado con ID: " + userId);
                result.put("userId", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            UserCompleteResponse userComplete = userCompleteOpt.get();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", userComplete);
            result.put("hasPersonalInformation", userComplete.getPersonalInformation() != null);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al obtener información completa del usuario ID {}: {}", userId, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al obtener la información del usuario");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Busca usuarios por email parcial (Solo Administradores)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> searchUsersByEmail(@RequestParam String email) {
        log.info("Buscando usuarios por email: {}", email);
        
        try {
            if (email == null || email.trim().length() < 3) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "El email debe tener al menos 3 caracteres");
                return ResponseEntity.badRequest().body(result);
            }
            
            // Por ahora, solo buscamos exacto. En el futuro se puede implementar búsqueda parcial
            Optional<UserCompleteResponse> userCompleteOpt = getUserCompleteInfo(email.trim());
            
            Map<String, Object> result = new HashMap<>();
            if (userCompleteOpt.isPresent()) {
                result.put("success", true);
                result.put("found", true);
                result.put("data", userCompleteOpt.get());
            } else {
                result.put("success", true);
                result.put("found", false);
                result.put("message", "No se encontraron usuarios con ese email");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al buscar usuarios por email {}: {}", email, e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al buscar usuarios");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Obtiene SOLO información de actividad del usuario autenticado
     * Última Actividad, Último Login, Fecha de Registro, Última Actualización
     */
    @GetMapping("/me/activity")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getMyActivityInfo() {
        log.info("Obteniendo información de actividad del usuario autenticado");

        try {
            // Obtener el email del usuario autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();
            final String email;

            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            } else {
                email = authentication.getName();
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();

            Map<String, Object> activity = new HashMap<>();
            activity.put("userId", user.getId());
            activity.put("email", user.getEmail());
            activity.put("lastLoginAt", user.getLastLoginAt());           // Último Login
            activity.put("lastActivityAt", user.getLastActivityAt());     // Última Actividad
            activity.put("createdAt", user.getCreatedAt());               // Fecha de Registro
            activity.put("updatedAt", user.getUpdatedAt());               // Última Actualización
            activity.put("passwordChangedAt", user.getPasswordChangedAt()); // Último Cambio de Contraseña

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", activity);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error al obtener información de actividad: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener actividad"));
        }
    }

    /**
     * Obtiene SOLO información de seguridad del usuario autenticado
     * Intentos Fallidos, Último Intento Fallido, Códigos de Verificación
     */
    @GetMapping("/me/security")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getMySecurityInfo() {
        log.info("Obteniendo información de seguridad del usuario autenticado");

        try {
            // Obtener el email del usuario autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();
            final String email;

            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            } else {
                email = authentication.getName();
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();

            Map<String, Object> security = new HashMap<>();
            security.put("userId", user.getId());
            security.put("email", user.getEmail());
            security.put("isVerified", user.isVerified());                    // ✓ Verificado
            security.put("failedAttempts", user.getAttempts());               // Intentos Fallidos
            security.put("lastFailedAttempt", user.getLastTryAt());           // Último Intento Fallido
            security.put("isBlocked", user.isBlocked());                      // Cuenta Bloqueada
            security.put("status", user.isStatus());                          // Desbloqueada

            // Códigos de Verificación
            if (user.getVerificationCode() != null && !user.getVerificationCode().isEmpty()) {
                security.put("verificationCodesCount", 1);
                security.put("hasVerificationCode", true);
            } else {
                security.put("verificationCodesCount", 0);
                security.put("hasVerificationCode", false);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", security);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error al obtener información de seguridad: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener seguridad"));
        }
    }

    /**
     * Obtiene SOLO almacenes asignados del usuario autenticado
     */
    @GetMapping("/me/assignments")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getMyAssignments() {
        log.info("Obteniendo almacenes asignados del usuario autenticado");

        try {
            // Obtener el email del usuario autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();
            final String email;

            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            } else {
                email = authentication.getName();
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("Usuario no encontrado: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            log.info("Obteniendo almacenes para usuario ID: {} ({})", user.getId(), email);

            // Obtener almacenes asignados - usar la query que funciona correctamente
            List<Map<String, Object>> warehouses = userWarehouseRepository
                    .findByUserIdWithActiveWarehouses(user.getId())  // ✅ Usa query específica
                    .stream()
                    .map(uw -> {
                        Map<String, Object> warehouseMap = new HashMap<>();
                        warehouseMap.put("warehouseId", uw.getWarehouse().getId());
                        warehouseMap.put("warehouseKey", uw.getWarehouse().getWarehouseKey());
                        warehouseMap.put("warehouseName", uw.getWarehouse().getNameWarehouse());
                        warehouseMap.put("assignedAt", uw.getCreatedAt());
                        warehouseMap.put("isActive", uw.getIsActive() != null ? uw.getIsActive() : true);
                        log.debug("Almacén: {} - {}", uw.getWarehouse().getId(), uw.getWarehouse().getNameWarehouse());
                        return warehouseMap;
                    })
                    .collect(Collectors.toList());

            log.info("Se encontraron {} almacenes para usuario {}", warehouses.size(), user.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "assignedWarehouses", warehouses,
                "totalWarehouses", warehouses.size()
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error al obtener almacenes asignados: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener almacenes"));
        }
    }

    /**
     * Obtiene información personal del usuario autenticado
     */
    @GetMapping("/me/personal-info")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getMyPersonalInfo() {
        log.info("Obteniendo información personal del usuario autenticado");

        try {
            // Obtener el email del usuario autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Object principal = authentication.getPrincipal();
            final String email;

            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                email = (String) principal;
            } else {
                email = authentication.getName();
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            Long userId = userOpt.get().getId();
            Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);

            if (personalInfoOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "No se encontró información personal");
                result.put("hasPersonalInformation", false);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }

            PersonalInformation personalInfo = personalInfoOpt.get();
            UserCompleteResponse.PersonalInfoData personalData = buildPersonalInfoData(personalInfo);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", personalData);
            result.put("hasPersonalInformation", true);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error al obtener información personal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener información personal"));
        }
    }


    /**
     * Obtiene información completa del usuario por email
     */
    private Optional<UserCompleteResponse> getUserCompleteInfo(String email) {
        log.info("Obteniendo información completa para usuario: {}", email);
        
        // Buscar usuario por email
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Usuario no encontrado: {}", email);
            return Optional.empty();
        }
        
        User user = userOpt.get();
        log.info("Usuario encontrado: email={}, userId={}", user.getEmail(), user.getId());

        // Buscar información personal
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(user.getId());
        
        if (personalInfoOpt.isPresent()) {
            PersonalInformation pi = personalInfoOpt.get();
            log.info("Información personal encontrada: personalInfoId={}, userId={}, name={}",
                    pi.getId(), pi.getUserId(), pi.getName());
        } else {
            log.warn("NO se encontró información personal para userId={}", user.getId());
        }

        UserCompleteResponse response = buildUserCompleteResponse(user, personalInfoOpt);
        
        log.info("Información completa obtenida para usuario: {}", email);
        return Optional.of(response);
    }

    /**
     * Obtiene información completa del usuario por ID
     */
    private Optional<UserCompleteResponse> getUserCompleteInfoById(Long userId) {
        log.info("Obteniendo información completa para usuario ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }
        
        // Buscar usuario por ID
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("Usuario no encontrado con ID: {}", userId);
            return Optional.empty();
        }
        
        User user = userOpt.get();
        log.info("Usuario encontrado: email={}, userId={}", user.getEmail(), user.getId());

        // Buscar información personal
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
        
        if (personalInfoOpt.isPresent()) {
            PersonalInformation pi = personalInfoOpt.get();
            log.info("Información personal encontrada: personalInfoId={}, userId={}, name={}",
                    pi.getId(), pi.getUserId(), pi.getName());
        } else {
            log.warn("NO se encontró información personal para userId={}", userId);
        }

        UserCompleteResponse response = buildUserCompleteResponse(user, personalInfoOpt);
        
        log.info("Información completa obtenida para usuario ID: {}", userId);
        return Optional.of(response);
    }

    /**
     * Construye la respuesta completa del usuario
     */
    private UserCompleteResponse buildUserCompleteResponse(User user, Optional<PersonalInformation> personalInfoOpt) {
        UserCompleteResponse response = new UserCompleteResponse();
        
        // Datos del usuario
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setStatus(user.isStatus());
        response.setVerified(user.isVerified());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        
        // Información de Seguridad y Sesión
        response.setLastLoginAt(user.getLastLoginAt());
        response.setLastActivityAt(user.getLastActivityAt());
        response.setPasswordChangedAt(user.getPasswordChangedAt());
        response.setLastFailedAttempt(user.getLastTryAt());
        response.setFailedAttempts(user.getAttempts());

        // Códigos de Verificación - si existe verificationCode, contar como 1
        if (user.getVerificationCode() != null && !user.getVerificationCode().isEmpty()) {
            response.setVerificationCodesCount(1);
            response.setLastVerificationCodeSent(user.getCreatedAt()); // En caso de no tener tabla de auditoría
        } else {
            response.setVerificationCodesCount(0);
        }

        // Almacenes Asignados - obtener todos (activos e inactivos)
        List<UserCompleteResponse.WarehouseInfo> warehouses = userWarehouseRepository
                .findByUserIdWithActiveWarehouses(user.getId())  // ✅ Usa query específica
                .stream()
                .map(uw -> {
                    UserCompleteResponse.WarehouseInfo info = new UserCompleteResponse.WarehouseInfo();
                    info.setWarehouseId(uw.getWarehouse().getId());
                    info.setWarehouseKey(uw.getWarehouse().getWarehouseKey());
                    info.setWarehouseName(uw.getWarehouse().getNameWarehouse());
                    info.setAssignedAt(uw.getCreatedAt());
                    return info;
                })
                .collect(Collectors.toList());
        response.setAssignedWarehouses(warehouses);

        log.info("Usuario {} tiene {} almacenes asignados", user.getId(), warehouses.size());

        // Información personal - usar map para transformar Optional directamente
        response.setPersonalInformation(
            personalInfoOpt.map(this::buildPersonalInfoData).orElse(null)
        );

        return response;
    }

    /**
     * Construye el objeto PersonalInfoData a partir de PersonalInformation
     */
    private UserCompleteResponse.PersonalInfoData buildPersonalInfoData(PersonalInformation personalInfo) {
        UserCompleteResponse.PersonalInfoData personalData = new UserCompleteResponse.PersonalInfoData();

        personalData.setPersonalInformationId(personalInfo.getId());
        personalData.setName(personalInfo.getName());
        personalData.setFirstLastName(personalInfo.getFirstLastName());
        personalData.setSecondLastName(personalInfo.getSecondLastName());
        personalData.setPhoneNumber(personalInfo.getPhoneNumber());
        personalData.setHasImage(personalInfo.getImage() != null && personalInfo.getImage().length > 0);
        personalData.setFullName(personalInfo.getFullName());
        personalData.setHasCompleteInfo(personalInfo.hasCompleteBasicInfo());
        personalData.setCreatedAt(personalInfo.getCreatedAt());
        personalData.setUpdatedAt(personalInfo.getUpdatedAt());

        return personalData;
    }

    // ============================================
    // ENDPOINTS PARA ADMINISTRADOR (Consultar otros usuarios)
    // ============================================

    /**
     * Obtiene información de actividad de un usuario por email (POST con body)
     */
    @PostMapping("/admin/activity/by-email")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getActivityByEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Admin solicitando actividad de usuario: {}", email);

        try {
            if (email == null || email.trim().isEmpty()) {
                log.warn("Email vacío proporcionado");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Email no puede estar vacío"));
            }

            log.debug("Buscando usuario con email: '{}'", email);
            Optional<User> userOpt = userService.findByEmail(email);

            if (userOpt.isEmpty()) {
                log.warn("Usuario NO encontrado con email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            log.info("Usuario encontrado: ID={}, Email={}", user.getId(), user.getEmail());

            Map<String, Object> activity = new HashMap<>();
            activity.put("userId", user.getId());
            activity.put("email", user.getEmail());
            activity.put("lastLoginAt", user.getLastLoginAt());
            activity.put("lastActivityAt", user.getLastActivityAt());
            activity.put("createdAt", user.getCreatedAt());
            activity.put("updatedAt", user.getUpdatedAt());
            activity.put("passwordChangedAt", user.getPasswordChangedAt());

            log.info("Retornando actividad para usuario: {}", email);
            return ResponseEntity.ok(Map.of("success", true, "data", activity));

        } catch (Exception e) {
            log.error("❌ Error al obtener actividad del usuario: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener actividad"));
        }
    }

    /**
     * Obtiene información de actividad de un usuario por ID (POST con body)
     */
    @PostMapping("/admin/activity/by-id")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getActivityById(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Admin solicitando actividad de usuario ID: {}", userId);

        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "userId no puede estar vacío"));
            }

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            Map<String, Object> activity = new HashMap<>();
            activity.put("userId", user.getId());
            activity.put("email", user.getEmail());
            activity.put("lastLoginAt", user.getLastLoginAt());
            activity.put("lastActivityAt", user.getLastActivityAt());
            activity.put("createdAt", user.getCreatedAt());
            activity.put("updatedAt", user.getUpdatedAt());
            activity.put("passwordChangedAt", user.getPasswordChangedAt());

            return ResponseEntity.ok(Map.of("success", true, "data", activity));

        } catch (Exception e) {
            log.error("Error al obtener actividad: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener actividad"));
        }
    }

    /**
     * Obtiene información de seguridad de un usuario por email (POST con body)
     */
    @PostMapping("/admin/security/by-email")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getSecurityByEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Admin solicitando seguridad de usuario: {}", email);

        try {
            if (email == null || email.trim().isEmpty()) {
                log.warn("Email vacío proporcionado");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Email no puede estar vacío"));
            }

            log.debug("Buscando usuario con email: '{}'", email);
            Optional<User> userOpt = userService.findByEmail(email);

            if (userOpt.isEmpty()) {
                log.warn("Usuario NO encontrado con email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            log.info("Usuario encontrado: ID={}, Email={}", user.getId(), user.getEmail());

            Map<String, Object> security = new HashMap<>();
            security.put("userId", user.getId());
            security.put("email", user.getEmail());
            security.put("isVerified", user.isVerified());
            security.put("failedAttempts", user.getAttempts());
            security.put("lastFailedAttempt", user.getLastTryAt());
            security.put("lastBlocked", user.getLastBlockedAt());
            security.put("status", user.isStatus());

            // Agregar información de solicitudes de cambio de contraseña
            try {
                tokai.com.mx.SIGMAV2.modules.users.model.BeanUser beanUser = securityUserAdapter.toLegacyUser(user);
                int pendingRequests = requestRecoveryPasswordRepository.countAllByUserAndStatus(beanUser, BeanRequestStatus.PENDING);
                security.put("pendingPasswordChangeRequests", pendingRequests);

                log.debug("Usuario {} - Solicitudes pendientes de cambio: {}", email, pendingRequests);
            } catch (Exception e) {
                log.warn("No se pudo obtener solicitudes de cambio de contraseña: {}", e.getMessage());
                security.put("pendingPasswordChangeRequests", 0);
            }

            log.info("Retornando seguridad para usuario: {}", email);
            return ResponseEntity.ok(Map.of("success", true, "data", security));

        } catch (Exception e) {
            log.error("❌ Error al obtener seguridad: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener seguridad"));
        }
    }

    /**
     * Obtiene información de seguridad de un usuario por ID (POST con body)
     */
    @PostMapping("/admin/security/by-id")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getSecurityById(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Admin solicitando seguridad de usuario ID: {}", userId);

        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "userId no puede estar vacío"));
            }

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            Map<String, Object> security = new HashMap<>();
            security.put("userId", user.getId());
            security.put("email", user.getEmail());
            security.put("isVerified", user.isVerified());
            security.put("failedAttempts", user.getAttempts());
            security.put("lastFailedAttempt", user.getLastTryAt());
            security.put("lastBlocked", user.getLastBlockedAt());
            security.put("status", user.isStatus());

            // Agregar información de solicitudes de cambio de contraseña pendientes para este usuario
            try {
                tokai.com.mx.SIGMAV2.modules.users.model.BeanUser beanUser = securityUserAdapter.toLegacyUser(user);
                int pendingRequests = requestRecoveryPasswordRepository.countAllByUserAndStatus(beanUser, BeanRequestStatus.PENDING);
                security.put("pendingPasswordChangeRequests", pendingRequests);
                log.debug("Usuario {} tiene {} solicitudes pendientes de cambio de contraseña", user.getEmail(), pendingRequests);
            } catch (Exception e) {
                log.warn("No se pudo obtener solicitudes de cambio de contraseña para usuario ID {}: {}", userId, e.getMessage());
                security.put("pendingPasswordChangeRequests", 0);
            }

            return ResponseEntity.ok(Map.of("success", true, "data", security));

        } catch (Exception e) {
            log.error("Error al obtener seguridad: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener seguridad"));
        }
    }


    /**
     * Obtiene almacenes asignados de un usuario por email (POST con body)
     */
    @PostMapping("/admin/assignments/by-email")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getAssignmentsByEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Admin solicitando almacenes de usuario: {}", email);

        try {
            if (email == null || email.trim().isEmpty()) {
                log.warn("Email vacío proporcionado");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Email no puede estar vacío"));
            }

            log.debug("Buscando usuario con email: '{}'", email);
            Optional<User> userOpt = userService.findByEmail(email);

            if (userOpt.isEmpty()) {
                log.warn("Usuario NO encontrado con email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            log.info("Usuario encontrado: ID={}, Email={}", user.getId(), user.getEmail());

            List<Map<String, Object>> warehouses = userWarehouseRepository
                    .findByUserIdWithActiveWarehouses(user.getId())
                    .stream()
                    .map(uw -> {
                        Map<String, Object> warehouseMap = new HashMap<>();
                        warehouseMap.put("warehouseId", uw.getWarehouse().getId());
                        warehouseMap.put("warehouseKey", uw.getWarehouse().getWarehouseKey());
                        warehouseMap.put("warehouseName", uw.getWarehouse().getNameWarehouse());
                        warehouseMap.put("assignedAt", uw.getCreatedAt());
                        warehouseMap.put("isActive", uw.getIsActive() != null ? uw.getIsActive() : true);
                        return warehouseMap;
                    })
                    .collect(Collectors.toList());

            log.info("Se encontraron {} almacenes para usuario: {}", warehouses.size(), email);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "assignedWarehouses", warehouses,
                "totalWarehouses", warehouses.size()
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error al obtener almacenes: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener almacenes"));
        }
    }

    /**
     * Obtiene almacenes asignados de un usuario por ID (POST con body)
     */
    @PostMapping("/admin/assignments/by-id")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getAssignmentsById(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Admin solicitando almacenes de usuario ID: {}", userId);

        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "userId no puede estar vacío"));
            }

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            List<Map<String, Object>> warehouses = userWarehouseRepository
                    .findByUserIdWithActiveWarehouses(user.getId())
                    .stream()
                    .map(uw -> {
                        Map<String, Object> warehouseMap = new HashMap<>();
                        warehouseMap.put("warehouseId", uw.getWarehouse().getId());
                        warehouseMap.put("warehouseKey", uw.getWarehouse().getWarehouseKey());
                        warehouseMap.put("warehouseName", uw.getWarehouse().getNameWarehouse());
                        warehouseMap.put("assignedAt", uw.getCreatedAt());
                        warehouseMap.put("isActive", uw.getIsActive() != null ? uw.getIsActive() : true);
                        return warehouseMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "assignedWarehouses", warehouses,
                "totalWarehouses", warehouses.size()
            ));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error al obtener almacenes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener almacenes"));
        }
    }

    /**
     * Obtiene información personal de un usuario por email (POST con body)
     */
    @PostMapping("/admin/personal-info/by-email")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getPersonalInfoByEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Admin solicitando información personal de usuario: {}", email);

        try {
            if (email == null || email.trim().isEmpty()) {
                log.warn("Email vacío proporcionado");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Email no puede estar vacío"));
            }

            log.debug("Buscando usuario con email: '{}'", email);
            Optional<User> userOpt = userService.findByEmail(email);

            if (userOpt.isEmpty()) {
                log.warn("Usuario NO encontrado con email: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userOpt.get().getId());

            if (personalInfoOpt.isEmpty()) {
                log.warn("Información personal NO encontrada para usuario: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "No se encontró información personal"));
            }

            UserCompleteResponse.PersonalInfoData personalData = buildPersonalInfoData(personalInfoOpt.get());
            log.info("Retornando información personal para usuario: {}", email);
            return ResponseEntity.ok(Map.of("success", true, "data", personalData, "hasPersonalInformation", true));

        } catch (Exception e) {
            log.error("❌ Error al obtener información personal: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener información personal"));
        }
    }

    /**
     * Obtiene información personal de un usuario por ID (POST con body)
     */
    @PostMapping("/admin/personal-info/by-id")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getPersonalInfoById(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        log.info("Admin solicitando información personal de usuario ID: {}", userId);

        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "userId no puede estar vacío"));
            }

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
            if (personalInfoOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "No se encontró información personal"));
            }

            UserCompleteResponse.PersonalInfoData personalData = buildPersonalInfoData(personalInfoOpt.get());
            return ResponseEntity.ok(Map.of("success", true, "data", personalData, "hasPersonalInformation", true));

        } catch (Exception e) {
            log.error("Error al obtener información personal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno al obtener información personal"));
        }
    }
}
