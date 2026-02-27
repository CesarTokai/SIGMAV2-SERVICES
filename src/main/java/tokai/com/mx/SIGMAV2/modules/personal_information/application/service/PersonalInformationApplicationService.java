package tokai.com.mx.SIGMAV2.modules.personal_information.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.CreatePersonalInformationCommand;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.UpdatePersonalInformationCommand;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.PersonalInformationService;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.output.PersonalInformationRepository;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.shared.exception.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de aplicación que implementa los casos de uso de información personal
 * Implementa el puerto de entrada (PersonalInformationService) y orquesta las operaciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalInformationApplicationService implements PersonalInformationService {

    private final PersonalInformationRepository personalInformationRepository;
    private final UserService userService;

    /**
     * Crea o actualiza información personal del usuario
     */
    @Transactional
    public PersonalInformation createOrUpdate(Long userId, CreatePersonalInformationCommand command) {
        log.info("Creando o actualizando información personal para usuario ID: {}", userId);

        // Validar que el usuario existe
        validateUserExists(userId);

        // Buscar información personal existente
        Optional<PersonalInformation> existingInfo = personalInformationRepository.findByUserId(userId);

        PersonalInformation personalInfo;

        if (existingInfo.isPresent()) {
            // Actualizar información existente
            personalInfo = existingInfo.get();
            personalInfo.updateInformation(
                command.getName(),
                command.getFirstLastName(),
                command.getSecondLastName(),
                command.getPhoneNumber()
            );
            log.info("Actualizando información personal existente para usuario ID: {}", userId);
        } else {
            // Crear nueva información personal
            personalInfo = new PersonalInformation(
                null,
                userId,
                command.getName(),
                command.getFirstLastName(),
                command.getSecondLastName(),
                command.getPhoneNumber(),
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
            log.info("Creando nueva información personal para usuario ID: {}", userId);
        }

        try {
            PersonalInformation saved = personalInformationRepository.save(personalInfo);
            log.info("Información personal guardada exitosamente para usuario ID: {}", userId);
            return saved;
        } catch (Exception e) {
            log.error("Error al guardar información personal para usuario ID {}: {}", userId, e.getMessage(), e);
            throw new CustomException("Error interno al guardar la información personal. Intente nuevamente.");
        }
    }

    /**
     * Actualiza información personal existente (solo campos no nulos)
     */
    @Transactional
    public PersonalInformation update(Long userId, UpdatePersonalInformationCommand command) {
        log.info("Actualizando información personal para usuario ID: {}", userId);

        validateUserExists(userId);

        PersonalInformation personalInfo = personalInformationRepository.findByUserId(userId)
            .orElseThrow(() -> new UserNotFoundException(
                "No se encontró información personal para el usuario ID: " + userId));

        if (command.getName() != null) {
            personalInfo.setName(command.getName());
        }
        if (command.getFirstLastName() != null) {
            personalInfo.setFirstLastName(command.getFirstLastName());
        }
        if (command.getSecondLastName() != null) {
            personalInfo.setSecondLastName(command.getSecondLastName());
        }
        if (command.getPhoneNumber() != null) {
            personalInfo.setPhoneNumber(command.getPhoneNumber());
        }

        personalInfo.setUpdatedAt(LocalDateTime.now());

        try {
            PersonalInformation updated = personalInformationRepository.save(personalInfo);
            log.info("Información personal actualizada exitosamente para usuario ID: {}", userId);
            return updated;
        } catch (Exception e) {
            log.error("Error al actualizar información personal para usuario ID {}: {}", userId, e.getMessage(), e);
            throw new CustomException("Error interno al actualizar la información personal. Intente nuevamente.");
        }
    }

    /**
     * Busca información personal por ID de usuario
     */
    public Optional<PersonalInformation> findByUserId(Long userId) {
        log.info("Buscando información personal para usuario ID: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }

        return personalInformationRepository.findByUserId(userId);
    }

    /**
     * Busca información personal por ID
     */
    public Optional<PersonalInformation> findById(Long id) {
        log.info("Buscando información personal por ID: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("El ID es obligatorio");
        }

        return personalInformationRepository.findById(id);
    }

    /**
     * Verifica si existe información personal para un usuario
     */
    public boolean existsByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }

        return personalInformationRepository.existsByUserId(userId);
    }

    /**
     * Actualiza la imagen del usuario
     */
    @Transactional
    public PersonalInformation updateImage(Long userId, byte[] image) {
        log.info("Actualizando imagen para usuario ID: {}", userId);

        // Validar que el usuario existe
        validateUserExists(userId);

        // Buscar información personal existente o crear una nueva
        PersonalInformation personalInfo = personalInformationRepository.findByUserId(userId)
            .orElse(new PersonalInformation(
                null,
                userId,
                null, null, null, null,
                image,
                null, // comments
                LocalDateTime.now(),
                LocalDateTime.now()
            ));

        personalInfo.updateImage(image);

        try {
            PersonalInformation updated = personalInformationRepository.save(personalInfo);
            log.info("Imagen actualizada exitosamente para usuario ID: {}", userId);
            return updated;
        } catch (Exception e) {
            log.error("Error al actualizar imagen para usuario ID {}: {}", userId, e.getMessage(), e);
            throw new CustomException("Error interno al actualizar la imagen. Intente nuevamente.");
        }
    }

    /**
     * Busca información personal por ID de usuario.
     * Los datos de User (email, role, status) se obtienen por separado en la capa del adaptador.
     */
    public Optional<PersonalInformation> findByUserIdWithUserDetails(Long userId) {
        log.info("Buscando información personal para ID: {}", userId);

        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }

        return personalInformationRepository.findByUserId(userId);
    }

    /**
     * Validar que el usuario existe
     */
    private void validateUserExists(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }

        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            throw new UserNotFoundException("No se encontró un usuario con ID: " + userId);
        }
    }
}
