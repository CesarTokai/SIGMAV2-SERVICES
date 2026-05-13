
package tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.port.input;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.RequestToResolveRequestDTO;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO;

public interface  PasswordRecoveryService {
    Page<ResponsePageRequestRecoveryDTO> findRequest(Pageable pageable);
    boolean completeRequest(RequestToResolveRequestDTO payload);
    boolean rejectRequest(RequestToResolveRequestDTO payload);
    boolean createRequest(String username);
    boolean verifyUser(String username);
    
}
