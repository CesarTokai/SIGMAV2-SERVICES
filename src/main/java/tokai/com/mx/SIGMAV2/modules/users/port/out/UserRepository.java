
package tokai.com.mx.SIGMAV2.modules.users.port.out;

import java.util.Optional;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

// ❌ Esta interfaz está DEPRECATED - Usar tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository
// Esta interfaz se mantiene temporalmente para compatibilidad, pero debe eliminarse
@Deprecated
public interface UserRepository {

    BeanUser save(BeanUser user);

    Optional<BeanUser> findByEmail(String email);
    
    Optional<BeanUser> findById(Long id);

    boolean existsByEmail(String email);

    void deleteByEmail(String email);

    Optional<BeanUser> verifyByEmailAndCode(String email, String code);

    void incrementAttempts(String email);

}
