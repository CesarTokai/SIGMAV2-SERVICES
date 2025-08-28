package tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;

import java.util.Optional;

/**
 * Repositorio JPA para persistencia de información personal
 */
@Repository
public interface JpaPersonalInformationRepository extends JpaRepository<BeanPersonalInformation, Long> {
    
    /**
     * Busca información personal por ID de usuario
     */
    Optional<BeanPersonalInformation> findByUser_UserId(Long userId);
    
    /**
     * Verifica si existe información personal para un usuario
     */
    boolean existsByUser_UserId(Long userId);
    
    /**
     * Elimina información personal por ID de usuario
     */
    void deleteByUser_UserId(Long userId);
}