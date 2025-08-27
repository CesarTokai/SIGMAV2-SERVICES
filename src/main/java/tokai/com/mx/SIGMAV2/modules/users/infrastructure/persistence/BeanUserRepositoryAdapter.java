package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.port.out.UserRepository;

import java.util.Optional;

/**
 * Implementación temporal para la interfaz deprecated UserRepository
 * Esta clase permite que el código legacy siga funcionando mientras se migra al nuevo diseño
 */
@Repository
public class BeanUserRepositoryAdapter implements UserRepository {
    
    private final JpaUserRepository jpaRepository;

    public BeanUserRepositoryAdapter(JpaUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BeanUser save(BeanUser user) {
        return jpaRepository.save(user);
    }

    @Override
    public Optional<BeanUser> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }
    
    @Override
    public Optional<BeanUser> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public void deleteByEmail(String email) {
        jpaRepository.deleteByEmail(email);
    }

    @Override
    public Optional<BeanUser> verifyByEmailAndCode(String email, String code) {
        return jpaRepository.findByEmailAndVerificationCode(email, code);
    }

    @Override
    public void incrementAttempts(String email) {
        jpaRepository.incrementAttemptsByEmail(email);
    }
}
