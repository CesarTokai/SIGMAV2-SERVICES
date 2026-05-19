package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

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
    private boolean verified;
    private int attempts;
    
    // Información Personal Consolidada
    private String name;
    private String firstLastName;
    private String secondLastName;
    private String phoneNumber;
    private String comments;
    
    // Campos de Actividad
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastBlockedAt;
}
