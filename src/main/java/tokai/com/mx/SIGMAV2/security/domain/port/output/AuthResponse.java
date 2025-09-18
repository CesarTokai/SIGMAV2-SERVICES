package tokai.com.mx.SIGMAV2.security.domain.port.output;

public class AuthResponse {
    private String email;
    private String token;

    public AuthResponse() {}

    public AuthResponse(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}