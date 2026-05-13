package tokai.com.mx.SIGMAV2.security.domain.port.input;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String email;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String email) {
        this.email = email;
    }
}