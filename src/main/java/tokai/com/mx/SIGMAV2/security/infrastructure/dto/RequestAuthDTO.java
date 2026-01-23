package tokai.com.mx.SIGMAV2.security.infrastructure.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestAuthDTO {
    private final String email;
    private final String password;

    public RequestAuthDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
