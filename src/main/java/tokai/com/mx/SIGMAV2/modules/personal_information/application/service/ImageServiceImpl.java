package tokai.com.mx.SIGMAV2.modules.personal_information.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.ImageResponse;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.PersonalInformationRequest;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.ImageValidationResult;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.ImageService;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.ImageValidationService;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.PersonalInformationService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {
    
    private final PersonalInformationService personalInformationService;
    private final ImageValidationService imageValidationService;
    private final UserService userService;

    @Override
    @Transactional
    public ImageResponse uploadImage(Long userId, MultipartFile file) {
        log.info("Subiendo imagen para usuario ID: {}", userId);
        
        ImageValidationResult validation = imageValidationService.validateImage(file);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }
        
        try {
            // Verificar si existe información personal, si no, crear una básica
            Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
            
            if (personalInfoOpt.isEmpty()) {
                log.info("No existe información personal para usuario ID: {}. Creando información básica.", userId);
                createBasicPersonalInformation(userId);
            }
            
            PersonalInformation updatedInfo = personalInformationService.updateImage(userId, file.getBytes());
            
            log.info("Imagen subida exitosamente para usuario ID: {}", userId);
            return mapToImageResponse(updatedInfo, file);
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("Data too long for column 'image'")) {
                log.error("Error: La columna 'image' en la base de datos es muy pequeña. Tamaño archivo: {} bytes", file.getSize());
                throw new IllegalArgumentException("Error de configuración: La base de datos no está configurada para almacenar imágenes. Contacte al administrador.");
            }
            throw new RuntimeException("Error de base de datos: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al subir imagen para usuario ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error al procesar la imagen: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ImageResponse updateImage(Long userId, MultipartFile file) {
        log.info("Actualizando imagen para usuario ID: {}", userId);
        
        ImageValidationResult validation = imageValidationService.validateImage(file);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }
        
        try {
            // Verificar si existe información personal, si no, crear una básica
            Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
            
            if (personalInfoOpt.isEmpty()) {
                log.info("No existe información personal para usuario ID: {}. Creando información básica.", userId);
                createBasicPersonalInformation(userId);
            }
            
            PersonalInformation updatedInfo = personalInformationService.updateImage(userId, file.getBytes());
            
            log.info("Imagen actualizada exitosamente para usuario ID: {}", userId);
            return mapToImageResponse(updatedInfo, file);
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            if (e.getMessage().contains("Data too long for column 'image'")) {
                log.error("Error: La columna 'image' en la base de datos es muy pequeña. Tamaño archivo: {} bytes", file.getSize());
                throw new IllegalArgumentException("Error de configuración: La base de datos no está configurada para almacenar imágenes. Contacte al administrador.");
            }
            throw new RuntimeException("Error de base de datos: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error al actualizar imagen para usuario ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error al procesar la imagen: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ImageResponse> getImageByUserId(Long userId) {
        log.debug("Obteniendo información de imagen para usuario ID: {}", userId);
        
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
        if (personalInfoOpt.isEmpty()) {
            return Optional.empty();
        }
        
        PersonalInformation personalInfo = personalInfoOpt.get();
        if (personalInfo.getImage() == null || personalInfo.getImage().length == 0) {
            return Optional.empty();
        }
        
        return Optional.of(mapToImageResponse(personalInfo, null));
    }

    @Override
    @Transactional
    public void deleteImageByUserId(Long userId) {
        log.info("Eliminando imagen para usuario ID: {}", userId);
        
        try {
            Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
            if (personalInfoOpt.isEmpty()) {
                throw new IllegalStateException("No se encontró información personal para el usuario");
            }
            
            personalInformationService.updateImage(userId, null);
            
            log.info("Imagen eliminada exitosamente para usuario ID: {}", userId);
            
        } catch (Exception e) {
            log.error("Error al eliminar imagen para usuario ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error al eliminar la imagen: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasImage(Long userId) {
        log.debug("Verificando si usuario ID {} tiene imagen", userId);
        
        Optional<PersonalInformation> personalInfoOpt = personalInformationService.findByUserId(userId);
        if (personalInfoOpt.isEmpty()) {
            return false;
        }
        
        PersonalInformation personalInfo = personalInfoOpt.get();
        return personalInfo.getImage() != null && personalInfo.getImage().length > 0;
    }
    
    private ImageResponse mapToImageResponse(PersonalInformation personalInfo, MultipartFile file) {
        ImageResponse response = new ImageResponse();
        response.setUserId(personalInfo.getUserId());
        response.setHasImage(personalInfo.getImage() != null && personalInfo.getImage().length > 0);
        
        if (file != null) {
            response.setFilename(file.getOriginalFilename());
            response.setSize(file.getSize());
            response.setContentType(file.getContentType());
            response.setUploadedAt(LocalDateTime.now());
        } else if (response.isHasImage()) {
            response.setSize(personalInfo.getImage().length);
        }
        
        response.setUpdatedAt(personalInfo.getUpdatedAt());
        
        return response;
    }

    /**
     * Crea información personal básica para permitir subir imagen
     */
    private void createBasicPersonalInformation(Long userId) {
        try {
            // Obtener información del usuario para usar su email como nombre temporal
            Optional<User> userOpt = userService.findById(userId);
            String defaultName = generateDefaultName(userOpt);

            PersonalInformationRequest basicRequest = new PersonalInformationRequest();
            basicRequest.setName(defaultName);
            basicRequest.setFirstLastName("Sin especificar");
            basicRequest.setSecondLastName("");
            basicRequest.setPhoneNumber("");
            
            personalInformationService.createOrUpdate(userId, basicRequest);
            log.info("Información personal básica creada para usuario ID: {} con nombre: {}", userId, defaultName);
            
        } catch (Exception e) {
            log.error("Error al crear información personal básica para usuario ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error al crear información personal básica: " + e.getMessage(), e);
        }
    }

    /**
     * Genera un nombre por defecto a partir del email del usuario
     * @param userOpt Optional del usuario
     * @return Nombre generado a partir del email o "Usuario" por defecto
     */
    private String generateDefaultName(Optional<User> userOpt) {
        if (userOpt.isEmpty()) {
            return "Usuario";
        }

        User user = userOpt.get();
        String email = user.getEmail();

        if (email == null || !email.contains("@")) {
            return "Usuario";
        }

        // Usar la parte antes del @ del email como nombre por defecto
        String nameFromEmail = email.substring(0, email.indexOf("@"));
        // Capitalizar primera letra
        return nameFromEmail.substring(0, 1).toUpperCase() + nameFromEmail.substring(1);
    }
}
