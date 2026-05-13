
package tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.port.output;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestStatus;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.port.input.PasswordRecoveryService;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.mapper.PasswordRecoveryMapper;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

public interface  PasswordRecoveryRepository {
    
      int countAllByUserAndStatus(BeanUser user, BeanRequestStatus status);
      // Método para obtener la información completa del usuario por su ID
      Optional<BeanUser> findUserInfoById(Long userId);  
    Page<ResponsePageRequestRecoveryDTO> findAllByUserAndRole(BeanUser user, ERole role, BeanRequestStatus status, Pageable pageable);
    Page<ResponsePageRequestRecoveryDTO> findAllByRole(ERole role, BeanRequestStatus status, Pageable pageable);
    Page<ResponsePageRequestRecoveryDTO> getRequestByRole(ERole role, BeanRequestStatus status, Pageable pageable);
    Optional<PasswordRecoveryMapper> findById(Long id);
    PasswordRecoveryMapper save(PasswordRecoveryService request);
}
