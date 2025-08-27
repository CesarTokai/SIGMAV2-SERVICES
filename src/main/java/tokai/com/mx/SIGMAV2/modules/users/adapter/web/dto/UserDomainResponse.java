package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UserResponse DTO independiente de las entidades de infraestructura
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserDomainResponse {
    private Long id;
    private String email;
    private String role;  // String en lugar de enum para desacoplar
    private boolean status;
    private boolean isVerified;
    private int attempts;
    private LocalDateTime lastTryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
