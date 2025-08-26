package tokai.com.mx.SIGMAV2.security.infrastructure.dto;

public class RequestAuthDTO {
    private final String email;
    private final String password;

    public RequestAuthDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
