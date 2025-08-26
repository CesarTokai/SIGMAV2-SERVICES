
package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UserResponse is a Data Transfer Object (DTO) that represents the user data
 * to be sent in API responses.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserResponse {
    private Long id;
    private String email;
    private ERole role;
    private boolean status;
    private boolean isVerified;
    private int attempts;
    private LocalDateTime lastTryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  

    
}