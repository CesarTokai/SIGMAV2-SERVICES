
package tokai.com.mx.SIGMAV2.security.domain.port.input;

public class ResetPasswordStep2Request {
    private String email;
    private String verificationCode;

    public ResetPasswordStep2Request() {}

    public ResetPasswordStep2Request(String email, String verificationCode) {
        this.email = email;
        this.verificationCode = verificationCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}