package tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;

@Repository
public interface JpaPersonalInformationRepository extends JpaRepository<BeanPersonalInformation, Long> {
    BeanPersonalInformation findByUser_UserId(Long userId);
    boolean existsById(Long id);
}