package tokai.com.mx.SIGMAV2.modules.users.application.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.port.in.UserService;
import tokai.com.mx.SIGMAV2.modules.users.port.out.UserRepository;

@Service
public class UserServiceImpl implements UserService {
private final UserRepository userRepository;


public UserServiceImpl(UserRepository userRepository) {
this.userRepository = userRepository;
}


@Override
public BeanUser createUser(BeanUser user) {
if (userRepository.existsByEmail(user.getEmail())) {
throw new IllegalArgumentException("Email ya registrado");
}
return userRepository.save(user);
}


@Override
public Optional<BeanUser> findByEmail(String email) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findByEmail'");
}


@Override
public void verifyUser(String token) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'verifyUser'");
}


@Override
public void updateLoginAttempt(String email, boolean success) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'updateLoginAttempt'");
}

//Parameter 0 of constructor in tokai.com.mx.SIGMAV2.modules.users.application.service.UserServiceImpl required a bean of type 'tokai.com.mx.SIGMAV2.modules.users.port.out.UserRepository' that could not be found.

}