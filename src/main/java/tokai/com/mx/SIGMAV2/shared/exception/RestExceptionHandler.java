package tokai.com.mx.SIGMAV2.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UserNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedAccessException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation error")
                .findFirst()
                .orElse("Validation error");
        body.put("message", message);
        return ResponseEntity.badRequest().body(body);
    }

    // ==================== EXCEPCIONES DEL MÓDULO DE MARBETES ====================

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException.class)
    public ResponseEntity<?> handleLabelNotFound(tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException ex) {
        log.warn("Label not found: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Marbete no encontrado");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException.class)
    public ResponseEntity<?> handleInvalidLabelState(tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException ex) {
        log.warn("Invalid label state: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Estado inválido");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException.class)
    public ResponseEntity<?> handlePermissionDenied(tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException ex) {
        log.warn("Permission denied: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Permiso denegado");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.labels.application.exception.DuplicateCountException.class)
    public ResponseEntity<?> handleDuplicateCount(tokai.com.mx.SIGMAV2.modules.labels.application.exception.DuplicateCountException ex) {
        log.warn("Duplicate count: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Conteo duplicado");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.labels.application.exception.CountSequenceException.class)
    public ResponseEntity<?> handleCountSequence(tokai.com.mx.SIGMAV2.modules.labels.application.exception.CountSequenceException ex) {
        log.warn("Count sequence error: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Secuencia de conteo inválida");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.labels.application.exception.CatalogNotLoadedException.class)
    public ResponseEntity<?> handleCatalogNotLoaded(tokai.com.mx.SIGMAV2.modules.labels.application.exception.CatalogNotLoadedException ex) {
        log.error("Catalog not loaded: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Catálogo no cargado");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.labels.domain.exception.LabelAlreadyCancelledException.class)
    public ResponseEntity<?> handleLabelAlreadyCancelled(tokai.com.mx.SIGMAV2.modules.labels.domain.exception.LabelAlreadyCancelledException ex) {
        log.warn("Label already cancelled: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Marbete ya cancelado");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ==================== EXCEPCIONES DEL MÓDULO DE ALMACENES ====================

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseAccessDeniedException.class)
    public ResponseEntity<?> handleWarehouseAccessDenied(tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseAccessDeniedException ex) {
        log.warn("Warehouse access denied: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Acceso denegado al almacén");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseNotFoundException.class)
    public ResponseEntity<?> handleWarehouseNotFound(tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseNotFoundException ex) {
        log.warn("Warehouse not found: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "Almacén no encontrado");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAll(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Error interno del servidor");
        // Log para diagnóstico (no exponemos detalles técnicos en la respuesta por seguridad)
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
