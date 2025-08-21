package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.port.out.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final JpaUserRepository jpaUserRepository;

    public UserRepositoryAdapter(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    
    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }

    

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public BeanUser save(BeanUser user) {
        // Aquí debes mapear User a UserEntity y viceversa
        throw new UnsupportedOperationException("Implementa el mapeo User <-> UserEntity");
    }

    @Override
    public Optional<BeanUser> findByEmail(String email) {
        // Implementa el mapeo aquí
        throw new UnsupportedOperationException("Implementa el mapeo UserEntity -> User");
    }
}