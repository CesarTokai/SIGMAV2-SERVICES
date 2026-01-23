package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualización de usuario por administrador
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {
    
    @Email(message = "El formato del email no es válido")
    private String email;
    
    @Pattern(regexp = "ADMINISTRADOR| ALMACENISTA |AUXILIAR | AUXILIAR_DE_CONTEO", message = "El rol debe ser ADMINISTRADOR")
    private String role;
    
    private Boolean status;
    private Boolean verified;
    
    // Opciones administrativas
    private Boolean resetAttempts;
    private Boolean forcePasswordReset;
    private String adminNotes;
}
