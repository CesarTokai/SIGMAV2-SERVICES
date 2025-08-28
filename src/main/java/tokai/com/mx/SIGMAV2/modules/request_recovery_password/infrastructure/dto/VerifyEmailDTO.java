package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerifyEmailDTO {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;

    // Constructor vacío requerido para deserialización JSON
    public VerifyEmailDTO() {}

    // Constructor con parámetros
    public VerifyEmailDTO(String email) {
        this.email = email;
    }

    // Getter
    public String email() {
        return email;
    }

    // Setter requerido para deserialización JSON
    public void setEmail(String email) {
        this.email = email;
    }

    // Getter alternativo (por si acaso)
    public String getEmail() {
        return email;
    }
}
