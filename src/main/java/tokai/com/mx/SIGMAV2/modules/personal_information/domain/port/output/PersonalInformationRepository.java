package tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.output;

import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;

import java.util.Optional;

/**
 * Puerto de salida - Define el contrato para persistencia de información personal
 * Esta interfaz NO debe tener anotaciones de framework
 */
public interface PersonalInformationRepository {

    /**
     * Guarda información personal
     */
    PersonalInformation save(PersonalInformation personalInformation);

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
     * Elimina información personal por ID de usuario
     */
    void deleteByUserId(Long userId);
}
