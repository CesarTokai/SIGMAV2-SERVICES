package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestToResolveRequestDTO {
    @NotNull(message="Something went wrong") 
    Long requestId;
}
