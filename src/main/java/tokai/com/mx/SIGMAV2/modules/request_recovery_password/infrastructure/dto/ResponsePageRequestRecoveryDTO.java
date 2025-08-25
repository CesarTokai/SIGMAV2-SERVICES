

package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto;
import java.time.LocalDate;


import lombok.AllArgsConstructor;
import lombok.Data;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestStatus;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

@AllArgsConstructor
@Data
public class ResponsePageRequestRecoveryDTO {
    Long requestId;
    BeanRequestStatus status;
    LocalDate date;
    String username;
    String email;
    ERole role;
    
}
