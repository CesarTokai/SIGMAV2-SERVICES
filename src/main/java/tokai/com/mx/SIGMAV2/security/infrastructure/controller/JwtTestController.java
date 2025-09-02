package tokai.com.mx.SIGMAV2.security.infrastructure.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.security.infrastructure.jwt.JwtUtils;
import tokai.com.mx.SIGMAV2.shared.response.ApiResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para probar las respuestas de autenticación JWT
 */
@RestController
@RequestMapping("/api/sigmav2/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"*"})
public class JwtTestController {
    
    private final JwtUtils jwtUtils;

    /**
     * Endpoint protegido para probar autenticación
     */
    @GetMapping("/test-protected")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testProtectedEndpoint() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Acceso autorizado correctamente");
        data.put("timestamp", java.time.LocalDateTime.now());
        data.put("authenticated", true);
        
        return ResponseEntity.ok(
            ApiResponse.success(data, "Endpoint protegido accedido exitosamente")
        );
    }

    /**
     * Endpoint para validar token manualmente
     */
    @PostMapping("/validate-token")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("INVALID_HEADER", "Header Authorization inválido", 
                        "Debe proporcionar el token en formato: Bearer <token>")
                );
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtUtils.isTokenValid(token);
            
            Map<String, Object> data = new HashMap<>();
            data.put("valid", isValid);
            data.put("timestamp", java.time.LocalDateTime.now());
            
            if (isValid) {
                String username = jwtUtils.getUsernameFromToken(token);
                String role = jwtUtils.getRoleFromToken(token);
                data.put("username", username);
                data.put("role", role);
            }
            
            return ResponseEntity.ok(
                ApiResponse.success(data, isValid ? "Token válido" : "Token inválido")
            );
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("VALIDATION_ERROR", "Error validando token", e.getMessage())
            );
        }
    }

    /**
     * Endpoint público para verificar que la API funciona
     */
    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "OK");
        data.put("timestamp", java.time.LocalDateTime.now());
        data.put("message", "API funcionando correctamente");
        
        return ResponseEntity.ok(
            ApiResponse.success(data, "Sistema funcionando correctamente")
        );
    }
}
