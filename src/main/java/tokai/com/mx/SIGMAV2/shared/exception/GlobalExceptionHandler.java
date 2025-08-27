package tokai.com.mx.SIGMAV2.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.NOT_FOUND, 
            "USER_NOT_FOUND", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.CONFLICT, 
            "USER_ALREADY_EXISTS", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidVerificationCodeException(
            InvalidVerificationCodeException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "INVALID_VERIFICATION_CODE", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRoleException(
            InvalidRoleException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "INVALID_ROLE", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(InvalidEmailFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEmailFormatException(
            InvalidEmailFormatException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "INVALID_EMAIL_FORMAT", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleWeakPasswordException(
            WeakPasswordException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "WEAK_PASSWORD", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedAccessException(
            UnauthorizedAccessException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED, 
            "UNAUTHORIZED_ACCESS", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(MaxAttemptsExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxAttemptsExceededException(
            MaxAttemptsExceededException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.TOO_MANY_REQUESTS, 
            "MAX_ATTEMPTS_EXCEEDED", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "INVALID_ARGUMENT", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(
            CustomException ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "CUSTOM_ERROR", 
            ex.getMessage(),
            request.getDescription(false)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "INTERNAL_SERVER_ERROR", 
            "Ha ocurrido un error interno en el servidor",
            request.getDescription(false)
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String errorCode, String message, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", path);
        
        return new ResponseEntity<>(errorResponse, status);
    }
}
