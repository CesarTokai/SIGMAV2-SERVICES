package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestStatus;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.port.input.PasswordRecoveryService;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.port.output.PasswordRecoveryRepository;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.mapper.PasswordRecoveryMapper;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.repository.IRequestRecoveryPassword;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper.UserDomainMapper;

@Repository
public class PasswordRecoveryRepositoryAdapter implements PasswordRecoveryRepository {

    private final IRequestRecoveryPassword requestRecoveryPasswordRepository;
    private final UserRepository userRepository;
    private final UserDomainMapper userMapper;

    public PasswordRecoveryRepositoryAdapter(IRequestRecoveryPassword requestRecoveryPasswordRepository, 
                                           UserRepository userRepository,
                                           UserDomainMapper userMapper) {
        this.requestRecoveryPasswordRepository = requestRecoveryPasswordRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public int countAllByUserAndStatus(BeanUser user, BeanRequestStatus status) {
        return requestRecoveryPasswordRepository.countAllByUserAndStatus(user, status);
    }

    @Override
    public Optional<BeanUser> findUserInfoById(Long userId) {
        return userRepository.findById(userId)
                .map(userMapper::toEntity);
    }

    @Override
    public Page<ResponsePageRequestRecoveryDTO> findAllByUserAndRole(BeanUser user, ERole role, 
                                                                    BeanRequestStatus status, Pageable pageable) {
        // Por ahora retornamos página vacía, se puede implementar más tarde
        return Page.empty();
    }

    @Override
    public Page<ResponsePageRequestRecoveryDTO> findAllByRole(ERole role, BeanRequestStatus status, 
                                                             Pageable pageable) {
        // Por ahora retornamos página vacía, se puede implementar más tarde
        return Page.empty();
    }

    @Override
    public Page<ResponsePageRequestRecoveryDTO> getRequestByRole(ERole role, BeanRequestStatus status, 
                                                                Pageable pageable) {
        return requestRecoveryPasswordRepository.getRequestByRole(role, status, pageable);
    }

    @Override
    public Optional<PasswordRecoveryMapper> findById(Long id) {
        // Por ahora retornamos Optional vacío, se puede implementar más tarde
        return Optional.empty();
    }

    @Override
    public PasswordRecoveryMapper save(PasswordRecoveryService request) {
        // Por ahora retornamos null, se puede implementar más tarde
        return null;
    }
}
