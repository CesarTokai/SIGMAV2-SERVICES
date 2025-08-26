package tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.out.PersonalInformationRepository;

@Repository
public class PersonalInformationRepositoryAdapter implements PersonalInformationRepository {

    private final JpaPersonalInformationRepository jpaRepo;

    public PersonalInformationRepositoryAdapter(JpaPersonalInformationRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public BeanPersonalInformation findByUserId(Long userId) {
        return jpaRepo.findByUser_UserId(userId);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepo.existsById(id);
    }

    @Override
    public BeanPersonalInformation save(BeanPersonalInformation info) {
        return jpaRepo.save(info);
    }
}