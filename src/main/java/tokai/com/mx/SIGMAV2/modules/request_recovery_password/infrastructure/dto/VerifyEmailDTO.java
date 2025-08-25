package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailDTO {
    @NotBlank (message = "El email es obligatorio")
    String email;

    public String email() {
        return email;
    }
}
