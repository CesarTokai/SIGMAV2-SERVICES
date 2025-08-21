package tokai.com.mx.SIGMAV2.modules.users.port.in;

import java.util.Optional;

import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
public interface UserService {
    BeanUser createUser(BeanUser user);

    Optional<BeanUser> findByEmail(String email);

    void verifyUser(String token);

    void updateLoginAttempt(String email, boolean success);
}
