package tokai.com.mx.SIGMAV2.shared.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utilidad para crear respuestas consistentes en toda la API
 */
public class ResponseHelper {

    /**
     * Respuesta exitosa con datos
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /**
     * Respuesta exitosa sin datos
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(String message) {
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * Respuesta de error 400 (Bad Request)
     */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String code, String message, String details) {
        return ResponseEntity.badRequest().body(ApiResponse.error(code, message, details));
    }

    /**
     * Respuesta de error 401 (Unauthorized)
     */
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String code, String message, String details) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(code, message, details));
    }

    /**
     * Respuesta de error 403 (Forbidden)
     */
    public static <T> ResponseEntity<ApiResponse<T>> forbidden(String code, String message, String details) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(code, message, details));
    }

    /**
     * Respuesta de error 404 (Not Found)
     */
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String code, String message, String details) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(code, message, details));
    }

    /**
     * Respuesta de error 409 (Conflict)
     */
    public static <T> ResponseEntity<ApiResponse<T>> conflict(String code, String message, String details) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(code, message, details));
    }

    /**
     * Respuesta de error 500 (Internal Server Error)
     */
    public static <T> ResponseEntity<ApiResponse<T>> internalServerError(String code, String message, String details) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(code, message, details));
    }

    /**
     * Respuesta de error personalizada
     */
    public static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String code, String message, String details) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(code, message, details));
    }
}
