package tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.out;

import tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence.BeanPersonalInformation;

/**
 * @deprecated Puerto legacy — usar domain.port.output.PersonalInformationRepository en su lugar.
 */
@Deprecated
public interface PersonalInformationRepository {
    BeanPersonalInformation findByUserId(Long userId);
    boolean existsById(Long id);
    BeanPersonalInformation save(BeanPersonalInformation info);
}