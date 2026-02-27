package tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input;

import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.CreatePersonalInformationCommand;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.UpdatePersonalInformationCommand;

import java.util.Optional;

/**
 * Puerto de entrada - Define los casos de uso del módulo de información personal.
 * Esta interfaz representa la API del dominio (hexágono).
 * NOTA: No debe depender de DTOs de la capa de adaptadores (web/dto).
 */
public interface PersonalInformationService {

    /**
     * Crea o actualiza información personal del usuario
     */
    PersonalInformation createOrUpdate(Long userId, CreatePersonalInformationCommand command);

    /**
     * Actualiza información personal existente
     */
    PersonalInformation update(Long userId, UpdatePersonalInformationCommand command);

    /**
     * Busca información personal por ID de usuario
     */
    Optional<PersonalInformation> findByUserId(Long userId);
    
    /**
     * Busca información personal por ID
     */
    Optional<PersonalInformation> findById(Long id);
    
    /**
     * Verifica si existe información personal para un usuario
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Actualiza la imagen del usuario
     */
    PersonalInformation updateImage(Long userId, byte[] image);

    /**
     * Busca información personal por ID de usuario y enriquece con detalles del usuario
     * (email, role, status). Implementación en la capa de aplicación realizará la
     * consulta a `UserService` y poblará esos campos en el `PersonalInformation`.
     */
    Optional<PersonalInformation> findByUserIdWithUserDetails(Long userId);
}
