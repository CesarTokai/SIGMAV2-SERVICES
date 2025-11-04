package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para creación de usuario por administrador
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateUserRequest {
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;
    
    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "ADMINISTRADOR|ALMACENISTA|AUXILIAR|AUXILIAR_DE_CONTEO", message = "El rol debe ser ADMINISTRADOR|ALMACENISTA|AUXILIAR|AUXILIAR_DE_CONTEO")
    private String role;
    
    private boolean preVerified = false; // Admin puede crear usuarios pre-verificados
    private boolean sendWelcomeEmail = true; // Enviar email de bienvenida
}
