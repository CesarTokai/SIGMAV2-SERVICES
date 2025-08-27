package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper.UserDomainMapper;

import java.util.Optional;

/**
 * Adaptador que implementa el puerto de salida UserRepository
 * Traduce entre el modelo de dominio y la infraestructura JPA
 */
@Repository
public class UserRepositoryDomainAdapter implements UserRepository {
    
    private final JpaUserRepository jpaRepository;
    private final UserDomainMapper mapper;

    public UserRepositoryDomainAdapter(JpaUserRepository jpaRepository, UserDomainMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        var entity = mapper.toEntity(user);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public void incrementAttempts(String email) {
        jpaRepository.incrementAttemptsByEmail(email);
    }

    @Override
    public Optional<User> verifyByEmailAndCode(String email, String code) {
        return jpaRepository.findByEmailAndVerificationCode(email, code)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByEmail(String email) {
        jpaRepository.deleteByEmail(email);
    }
}
