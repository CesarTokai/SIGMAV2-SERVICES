package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuesta completa de usuario en operaciones de administrador
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    
    private Long id;
    private String email;
    private String role;
    private boolean status;
}
