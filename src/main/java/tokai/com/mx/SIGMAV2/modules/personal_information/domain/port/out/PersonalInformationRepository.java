package tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.out;

import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;

public interface PersonalInformationRepository {
    BeanPersonalInformation findByUserId(Long userId);
    boolean existsById(Long id);
    BeanPersonalInformation save(BeanPersonalInformation info);
}