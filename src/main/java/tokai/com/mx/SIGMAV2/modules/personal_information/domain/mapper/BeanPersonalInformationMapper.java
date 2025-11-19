package tokai.com.mx.SIGMAV2.modules.personal_information.domain.mapper;

import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;

public class BeanPersonalInformationMapper {
    public static PersonalInformation toEntity(BeanPersonalInformation bean) {
        PersonalInformation entity = new PersonalInformation();
        entity.setName(bean.getName());
        entity.setFirstLastName(bean.getFirstLastName());
        entity.setSecondLastName(bean.getSecondLastName());
        entity.setPhoneNumber(bean.getPhoneNumber());
        entity.setComments(bean.getComments());
        // Mapear el userId correctamente
        if (bean.getUser() != null) {
            entity.setUserId(bean.getUser().getUserId());
        }
        return entity;
    }
}
