package tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.PersonalInformationService;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.PersonalInformationRequest;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.UpdatePersonalInformationRequest;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.PersonalInformationResponse;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.AssignRoleRequest;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/personal-information")
@RequiredArgsConstructor
public class PersonalInformationController {

    private final PersonalInformationService personalInformationService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> createOrUpdate(@RequestBody @Valid PersonalInformationRequest request) {
        log.info("Recibida solicitud para crear/actualizar información personal");
        
        // Obtener el usuario autenticado
        Long userId = getCurrentUserId();
        
        PersonalInformation personalInfo = personalInformationService.createOrUpdate(userId, request);
        
        PersonalInformationResponse response = mapToResponse(personalInfo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Información personal guardada exitosamente");
        result.put("data", response);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> update(@RequestBody @Valid UpdatePersonalInformationRequest request) {
        log.info("Recibida solicitud para actualizar información personal");
        
        // Obtener el usuario autenticado
        Long userId = getCurrentUserId();
        
        PersonalInformation personalInfo = personalInformationService.update(userId, request);
        
        PersonalInformationResponse response = mapToResponse(personalInfo);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Información personal actualizada exitosamente");
        result.put("data", response);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getMyPersonalInformation() {
        log.info("Obteniendo información personal del usuario autenticado");
        
        // Obtener el usuario autenticado
        Long userId = getCurrentUserId();
        
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
        
        if (personalInfoOpt.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "No se encontró información personal para este usuario");
            result.put("hasPersonalInformation", false);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        
        PersonalInformationResponse response = mapToResponse(personalInfoOpt.get());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("hasPersonalInformation", true);
        result.put("data", response);
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA')")
    public ResponseEntity<Map<String, Object>> getPersonalInformationByUserId(@PathVariable Long userId) {
        log.info("Obteniendo información personal para usuario ID: {}", userId);
        
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
        
        if (personalInfoOpt.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "No se encontró información personal para el usuario ID: " + userId);
            result.put("userId", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        
        PersonalInformationResponse response = mapToResponse(personalInfoOpt.get());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("image") MultipartFile file) {
        log.info("Recibida solicitud para subir imagen de perfil");
        
        try {
            // Validar archivo
            if (file.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "No se seleccionó ningún archivo");
                return ResponseEntity.badRequest().body(result);
            }
            
            // Validar tipo de archivo
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "El archivo debe ser una imagen");
                return ResponseEntity.badRequest().body(result);
            }
            
            // Validar tamaño (máximo 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "La imagen no puede ser mayor a 5MB");
                return ResponseEntity.badRequest().body(result);
            }
            
            // Obtener el usuario autenticado
            Long userId = getCurrentUserId();
            
            PersonalInformation personalInfo = personalInformationService.updateImage(userId, file.getBytes());
            
            PersonalInformationResponse response = mapToResponse(personalInfo);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Imagen de perfil actualizada exitosamente");
            result.put("data", response);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al procesar la imagen: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error al procesar la imagen");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/assign-role")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> assignRole(@RequestBody @Valid AssignRoleRequest request) {
        log.info("Asignando rol {} al usuario ID: {}", request.getRole(), request.getUserId());
        
        try {
            // Buscar el usuario
            Optional<User> userOpt = userService.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Usuario no encontrado");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            User user = userOpt.get();
            
            // Validar rol
            Role newRole;
            try {
                newRole = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Rol inválido: " + request.getRole());
                return ResponseEntity.badRequest().body(result);
            }
            
            // Actualizar rol del usuario
            user.setRole(newRole);
            user.setUpdatedAt(java.time.LocalDateTime.now());
            
            // Guardar cambios
            User updatedUser = userService.update(user);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Rol asignado exitosamente");
            result.put("userId", request.getUserId());
            result.put("newRole", updatedUser.getRole().name());
            result.put("reason", request.getReason());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al asignar rol: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al asignar el rol");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> checkPersonalInformationExists() {
        log.info("Verificando si existe información personal para el usuario autenticado");
        
        // Obtener el usuario autenticado
        Long userId = getCurrentUserId();
        
        boolean exists = personalInformationService.existsByUserId(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("exists", exists);
        result.put("userId", userId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Obtiene el ID del usuario autenticado desde el contexto de seguridad
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        // Buscar usuario por email
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuario autenticado no encontrado: " + email);
        }
        
        return userOpt.get().getId();
    }

    /**
     * Mapea de modelo de dominio a DTO de respuesta
     */
    private PersonalInformationResponse mapToResponse(PersonalInformation personalInfo) {
        return new PersonalInformationResponse(
                personalInfo.getId(),
                personalInfo.getUserId(),
                personalInfo.getName(),
                personalInfo.getFirstLastName(),
                personalInfo.getSecondLastName(),
                personalInfo.getPhoneNumber(),
                personalInfo.getImage() != null && personalInfo.getImage().length > 0,
                personalInfo.getFullName(),
                personalInfo.getCreatedAt(),
                personalInfo.getUpdatedAt()
        );
    }
}
