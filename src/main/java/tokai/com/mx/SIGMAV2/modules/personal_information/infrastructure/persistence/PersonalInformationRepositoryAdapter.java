package tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.output.PersonalInformationRepository;

import java.util.Optional;

/**
 * Adaptador que implementa el puerto de salida PersonalInformationRepository
 * Convierte entre el modelo de dominio y la entidad de persistencia
 */
@Repository
public class PersonalInformationRepositoryAdapter implements PersonalInformationRepository {

    private final JpaPersonalInformationRepository jpaRepository;
    private final PersonalInformationDomainMapper mapper;

    public PersonalInformationRepositoryAdapter(JpaPersonalInformationRepository jpaRepository, 
                                               PersonalInformationDomainMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PersonalInformation save(PersonalInformation personalInformation) {
        var entity = mapper.toEntity(personalInformation);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PersonalInformation> findByUserId(Long userId) {
        return jpaRepository.findByUser_Id(userId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PersonalInformation> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpaRepository.existsByUser_Id(userId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUser_Id(userId);
    }
}