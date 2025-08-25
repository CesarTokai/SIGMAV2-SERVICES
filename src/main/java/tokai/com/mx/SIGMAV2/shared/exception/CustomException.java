package tokai.com.mx.SIGMAV2.shared.exception;

public class CustomException extends RuntimeException {
    public CustomException(String errorCode) {
        super(errorCode);
    }
}
