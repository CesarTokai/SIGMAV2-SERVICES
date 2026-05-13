package tokai.com.mx.SIGMAV2.security.domain.port.input;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
       private String email;
    private String password;

    public AuthRequest() {}

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
