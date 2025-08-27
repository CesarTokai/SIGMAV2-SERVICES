package tokai.com.mx.SIGMAV2.shared.exception;

public class MaxAttemptsExceededException extends RuntimeException {
    public MaxAttemptsExceededException(String message) {
        super(message);
    }
}
