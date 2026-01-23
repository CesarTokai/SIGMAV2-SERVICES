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
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.ImageService;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.PersonalInformationRequest;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.UpdatePersonalInformationRequest;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.PersonalInformationResponse;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.AssignRoleRequest;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.ImageResponse;
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
    private final ImageService imageService;

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
        
        PersonalInformationResponse response = mapToResponseWithImageInfo(personalInfoOpt.get(), userId);
        
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
        
        // Usar el método que también carga detalles del usuario (email, role, status) en el modelo
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserIdWithUserDetails(userId);

        if (personalInfoOpt.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "No se encontró información personal para el usuario ID: " + userId);
            result.put("userId", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
        
        // Construir respuesta usando el método helper
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
            Long userId = getCurrentUserId();
            ImageResponse imageResponse = imageService.uploadImage(userId, file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Imagen de perfil subida exitosamente");
            result.put("data", imageResponse);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al subir imagen: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
            
        } catch (IllegalStateException e) {
            log.warn("Error de estado al subir imagen: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(result);
            
        } catch (Exception e) {
            log.error("Error al procesar la imagen: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al procesar la imagen");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PutMapping("/update-image")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> updateImage(@RequestParam("image") MultipartFile file) {
        log.info("Recibida solicitud para actualizar imagen de perfil");
        
        try {
            Long userId = getCurrentUserId();
            ImageResponse imageResponse = imageService.updateImage(userId, file);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Imagen de perfil actualizada exitosamente");
            result.put("data", imageResponse);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar imagen: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
            
        } catch (IllegalStateException e) {
            log.warn("Error de estado al actualizar imagen: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            
        } catch (Exception e) {
            log.error("Error al actualizar la imagen: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al actualizar la imagen");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/image")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getMyImage() {
        log.info("Obteniendo información de imagen del usuario autenticado");
        
        try {
            Long userId = getCurrentUserId();
            Optional<ImageResponse> imageResponseOpt = imageService.getImageByUserId(userId);
            
            if (imageResponseOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "No se encontró imagen para este usuario");
                result.put("hasImage", false);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("hasImage", true);
            result.put("data", imageResponseOpt.get());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al obtener imagen: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al obtener la imagen");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @DeleteMapping("/image")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> deleteMyImage() {
        log.info("Eliminando imagen del usuario autenticado");
        
        try {
            Long userId = getCurrentUserId();
            imageService.deleteImageByUserId(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Imagen eliminada exitosamente");
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalStateException e) {
            log.warn("Error de estado al eliminar imagen: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            
        } catch (Exception e) {
            log.error("Error al eliminar imagen: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al eliminar la imagen");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @GetMapping("/image/exists")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> checkImageExists() {
        log.info("Verificando si existe imagen para el usuario autenticado");
        
        try {
            Long userId = getCurrentUserId();
            boolean hasImage = imageService.hasImage(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("hasImage", hasImage);
            result.put("userId", userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al verificar existencia de imagen: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Error interno al verificar la imagen");
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
     * Mapea de modelo de dominio a DTO de respuesta usando el servicio de imagen
     */
    private PersonalInformationResponse mapToResponseWithImageInfo(PersonalInformation personalInfo, Long userId) {
         // Usar el servicio de imagen para verificar si tiene imagen
         boolean hasImage = imageService.hasImage(userId);

         return new PersonalInformationResponse(
                 personalInfo.getId(),
                 personalInfo.getUserId(),
                 personalInfo.getName(),
                 personalInfo.getFirstLastName(),
                 personalInfo.getSecondLastName(),
                 personalInfo.getPhoneNumber(),
                 hasImage,
                 personalInfo.getFullName(),
                 personalInfo.getCreatedAt(),
                 personalInfo.getUpdatedAt(),
                 personalInfo.getEmail(),
                 personalInfo.getRole(),
                 personalInfo.isStatus(),
                 personalInfo.getComments()
         );
     }

     /**
      * Mapea de modelo de dominio a DTO de respuesta (método legacy para compatibilidad)
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
                 personalInfo.getUpdatedAt(),
                 personalInfo.getEmail(),
                 personalInfo.getRole(),
                 personalInfo.isStatus(),
                 personalInfo.getComments()
         );
     }
 }
