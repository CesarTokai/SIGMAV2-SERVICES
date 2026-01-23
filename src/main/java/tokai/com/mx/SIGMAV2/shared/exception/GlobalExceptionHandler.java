package tokai.com.mx.SIGMAV2.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import tokai.com.mx.SIGMAV2.security.infrastructure.exception.*;
import tokai.com.mx.SIGMAV2.shared.response.ApiResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Importar nueva0s excepciones del módulo labels
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.DuplicateCountException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.CountSequenceException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== MANEJADORES DE EXCEPCIONES JWT =====
    
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenExpiredException(
            TokenExpiredException ex, WebRequest request) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .error(ApiResponse.ErrorDetails.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .details(ex.getDetails())
                        .expiredAt(ex.getExpiredAt())
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenInvalidException(
            TokenInvalidException ex, WebRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getDetails()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenMissingException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenMissingException(
            TokenMissingException ex, WebRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getDetails()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenMalformedException.class)
    public ResponseEntity<ApiResponse<Object>> handleTokenMalformedException(
            TokenMalformedException ex, WebRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getDetails()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleJwtException(
            JwtException ex, WebRequest request) {
        
        ApiResponse<Object> response = ApiResponse.error(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getDetails()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // ===== MANEJADORES DE EXCEPCIONES EXISTENTES =====

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

    @ExceptionHandler(LabelNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLabelNotFound(LabelNotFoundException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "LABEL_NOT_FOUND", ex.getMessage(), request.getDescription(false));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<Map<String, Object>> handlePermissionDenied(PermissionDeniedException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", ex.getMessage(), request.getDescription(false));
    }

    @ExceptionHandler({DuplicateCountException.class, CountSequenceException.class, InvalidLabelStateException.class})
    public ResponseEntity<Map<String, Object>> handleBusinessConflict(RuntimeException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", ex.getMessage(), request.getDescription(false));
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


    /// ////////////////Exeption with WEB////////////////////////////
    private Map<String, Object> baseBody(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());
        Map<String, Object> body = baseBody(status, "Error de validación", request.getRequestURI());
        body.put("errors", errors);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, String> toFieldError(FieldError fe) {
        Map<String, String> map = new HashMap<>();
        map.put("field", fe.getField());
        map.put("message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valor inválido");
        return map;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Formato de JSON inválido";
        Throwable cause = ex.getCause();

        // Manejo específico para errores de parseo con Jackson
        if (cause instanceof InvalidFormatException ife) {
            boolean isLocalDate = ife.getTargetType() != null && ife.getTargetType().equals(java.time.LocalDate.class);
            boolean isDateField = ife.getPath() != null && ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .anyMatch("date"::equals);
            if (isLocalDate || isDateField) {
                message = "El campo 'date' es inválido. Usa el formato YYYY-MM-DD";
            }
        } else if (cause instanceof DateTimeParseException) {
            message = "El campo 'date' es inválido. Usa el formato YYYY-MM-DD";
        } else if (cause != null && cause.getMessage() != null && cause.getMessage().contains("LocalDate")) {
            message = "El campo 'date' es inválido. Usa el formato YYYY-MM-DD";
        } else {
            // Buscar en la causa raíz por DateTimeParseException
            Throwable root = cause;
            while (root != null && root.getCause() != null) root = root.getCause();
            if (root instanceof DateTimeParseException) {
                message = "El campo 'date' es inválido. Usa el formato YYYY-MM-DD";
            }
        }

        Map<String, Object> body = baseBody(status, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> body = baseBody(status, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        Map<String, Object> body = baseBody(status, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        String message = "No se pudo guardar el registro por restricción de datos";
        String lower = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (lower.contains("uk_period") || lower.contains("unique") || lower.contains("duplicate")) {
            message = "Ya existe un periodo para esta fecha";
        }
        Map<String, Object> body = baseBody(status, message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
