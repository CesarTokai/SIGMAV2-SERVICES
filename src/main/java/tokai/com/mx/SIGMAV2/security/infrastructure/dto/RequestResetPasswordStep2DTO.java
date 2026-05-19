package tokai.com.mx.SIGMAV2.security.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;


public record RequestResetPasswordStep2DTO(
        @NotBlank(message = "Verification code is required") String verificationCode,
        @NotBlank(message = "Something went wrong") String Email) {
}

