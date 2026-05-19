package tokai.com.mx.SIGMAV2.security.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestResetPasswordStep1DTO(
        @NotBlank(message = "Email is required") String Email) {
}
