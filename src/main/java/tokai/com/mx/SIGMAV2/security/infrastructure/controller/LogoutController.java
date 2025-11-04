package tokai.com.mx.SIGMAV2.security.infrastructure.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tokai.com.mx.SIGMAV2.security.infrastructure.jwt.JwtUtils;
import tokai.com.mx.SIGMAV2.security.infrastructure.service.TokenRevocationService;
import tokai.com.mx.SIGMAV2.shared.response.ApiResponse;

import java.time.Instant;

/**
 * Controlador para manejo de cierre de sesión (logout)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class LogoutController {

    private final TokenRevocationService revocationService;
    private final JwtUtils jwtUtils;

    /**
     * Endpoint para cerrar sesión (logout)
     * Revoca el token JWT actual y limpia el contexto de seguridad
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Validar y extraer información del token
                DecodedJWT decodedJWT = jwtUtils.validateToken(token);
                String jti = decodedJWT.getId();
                String username = decodedJWT.getSubject();
                Instant expiration = decodedJWT.getExpiresAt().toInstant();

                if (jti != null && expiration != null) {
                    // Revocar el token en la base de datos
                    revocationService.revokeToken(jti, expiration, "LOGOUT", username);
                    log.info("Usuario cerró sesión exitosamente: username={}, jti={}", username, jti);
                } else {
                    log.warn("Token sin JTI o expiración, no se puede revocar correctamente");
                }
            } catch (Exception e) {
                log.warn("Error al procesar logout (continuando de todos modos): {}", e.getMessage());
                // Aún si falla la revocación, limpiamos el contexto
            }
        }

        // Limpiar el contexto de seguridad de Spring
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Sesión cerrada exitosamente")
                .build());
    }
}

