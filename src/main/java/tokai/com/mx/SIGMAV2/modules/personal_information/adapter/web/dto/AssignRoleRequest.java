package tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    @NotBlank(message = "El rol es obligatorio")
    private String role;

    private String reason; // Motivo del cambio de rol (opcional)

    // Constructor por defecto
    public AssignRoleRequest() {
    }

    // Constructor con parámetros
    public AssignRoleRequest(Long userId, String role, String reason) {
        this.userId = userId;
        this.role = role;
        this.reason = reason;
    }
}
