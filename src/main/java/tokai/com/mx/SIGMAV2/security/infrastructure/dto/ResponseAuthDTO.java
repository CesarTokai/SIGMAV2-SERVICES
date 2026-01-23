package tokai.com.mx.SIGMAV2.security.infrastructure.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseAuthDTO {
    private String email;
    private String token;
    private String role;
}
