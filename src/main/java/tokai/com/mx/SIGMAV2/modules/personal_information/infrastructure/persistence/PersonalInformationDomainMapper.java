package tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

/**
 * Mapper para convertir entre entidades de dominio y de persistencia
 */
@Component
public class PersonalInformationDomainMapper {

    /**
     * Convierte de entidad de persistencia a modelo de dominio
     */
    public PersonalInformation toDomain(BeanPersonalInformation entity) {
        if (entity == null) {
            return null;
        }

        return new PersonalInformation(
                entity.getPersonalInformationId(),
                entity.getUser() != null ? entity.getUser().getId() : null,
                entity.getName(),
                entity.getFirstLastName(),
                entity.getSecondLastName(),
                entity.getPhoneNumber(),
                entity.getImage(),
                entity.getComments(), // comments
                entity.getCreatedAt(), // Mapear createdAt
                entity.getUpdatedAt()  // Mapear updatedAt
        );
    }

    /**
     * Convierte de modelo de dominio a entidad de persistencia
     */
    public BeanPersonalInformation toEntity(PersonalInformation domain) {
        if (domain == null) {
            return null;
        }

        BeanPersonalInformation entity = new BeanPersonalInformation();
        entity.setPersonalInformationId(domain.getId());
        entity.setName(domain.getName());
        entity.setFirstLastName(domain.getFirstLastName());
        entity.setSecondLastName(domain.getSecondLastName());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setImage(domain.getImage());

        // Crear BeanUser con el userId
        if (domain.getUserId() != null) {
            BeanUser userEntity = new BeanUser();
            userEntity.setId(domain.getUserId());
            entity.setUser(userEntity);
        }

        return entity;
    }

    /**
     * Actualiza una entidad existente con datos del dominio
     */
    public void updateEntity(BeanPersonalInformation entity, PersonalInformation domain) {
        if (entity == null || domain == null) {
            return;
        }

        entity.setName(domain.getName());
        entity.setFirstLastName(domain.getFirstLastName());
        entity.setSecondLastName(domain.getSecondLastName());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setImage(domain.getImage());
    }
}
