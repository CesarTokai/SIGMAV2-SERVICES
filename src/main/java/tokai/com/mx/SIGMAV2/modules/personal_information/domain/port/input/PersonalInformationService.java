package tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input;

import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.PersonalInformationRequest;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.UpdatePersonalInformationRequest;

import java.util.Optional;

/**
 * Puerto de entrada - Define los casos de uso del módulo de información personal
 * Esta interfaz representa la API del dominio (hexágono)
 */
public interface PersonalInformationService {
    
    /**
     * Crea o actualiza información personal del usuario
     */
    PersonalInformation createOrUpdate(Long userId, PersonalInformationRequest request);
    
    /**
     * Actualiza información personal existente
     */
    PersonalInformation update(Long userId, UpdatePersonalInformationRequest request);
    
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
}
