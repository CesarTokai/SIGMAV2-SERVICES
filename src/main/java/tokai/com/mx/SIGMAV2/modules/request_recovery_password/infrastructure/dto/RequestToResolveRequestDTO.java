package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto;


import jakarta.validation.constraints.NotNull;

public class RequestToResolveRequestDTO {
    @NotNull(message="Something went wrong") 
    Long requestId;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

}
