package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.port.out.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper.UserMapper;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final JpaUserRepository jpaRepository;
    private final UserMapper mapper;

    public UserRepositoryAdapter(JpaUserRepository jpaRepository, UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public BeanUser save(BeanUser user) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(user)));
    }

    @Override
    public Optional<BeanUser> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }



    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }


    public void incrementAttempts(String email) {
        jpaRepository.incrementAttempts(email);
    }

 
    @Override
    public Optional<BeanUser> verifyByEmailAndCode(String email, String code) {
        return jpaRepository.verifyByEmailAndCode(email, code);
    }

    @Override
    public void deleteByEmail(String email) {
        jpaRepository.deleteByEmail(email);
    }

  
   

   
}
