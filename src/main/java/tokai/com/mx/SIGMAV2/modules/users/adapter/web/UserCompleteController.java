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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/users")
@RequiredArgsConstructor
public class UserCompleteController {

    private final UserService userService;
    private final PersonalInformationService personalInformationService;

    /**
     * Obtiene información completa del usuario autenticado (usuario + información personal)
     */
    @GetMapping("/me/complete")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getMyCompleteInfo() {
        log.info("Obteniendo información completa del usuario autenticado");
        
        try {
            // Obtener el email del usuario autenticado
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            
            Optional<UserCompleteResponse> userCompleteOpt = getUserCompleteInfo(email);
            
            if (userCompleteOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            UserCompleteResponse userComplete = userCompleteOpt.get();
            
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
        
        // Buscar información personal
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(user.getId());
        
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
        
        // Buscar información personal
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
        
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
        
        // Información personal
        if (personalInfoOpt.isPresent()) {
            PersonalInformation personalInfo = personalInfoOpt.get();
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
            
            response.setPersonalInformation(personalData);
        } else {
            response.setPersonalInformation(null);
        }
        
        return response;
    }
}
