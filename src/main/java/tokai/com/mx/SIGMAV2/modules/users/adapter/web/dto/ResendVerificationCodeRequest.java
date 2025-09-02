package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar reenvío de código de verificación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendVerificationCodeRequest {
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;
    
    // Opcional: motivo del reenvío para logging
    private String reason;
}
