
package tokai.com.mx.SIGMAV2.modules.users.port.out;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

@Repository
public interface UserRepository {

    BeanUser save(BeanUser user);

    Optional<BeanUser> findByEmail(String email);
    
    Optional<BeanUser> findById(Long id);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);

    Optional<BeanUser> verifyByEmailAndCode(String email, String code);

    void incrementAttempts(String email);

}
