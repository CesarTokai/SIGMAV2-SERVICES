package tokai.com.mx.SIGMAV2.security.infrastructure.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import tokai.com.mx.SIGMAV2.security.infrastructure.jwt.JwtUtils;
import tokai.com.mx.SIGMAV2.security.infrastructure.service.TokenRevocationService;
import tokai.com.mx.SIGMAV2.shared.response.ApiResponse;

import java.io.IOException;

/**
 * Filtro que verifica si el token JWT ha sido revocado
 * Se ejecuta antes del JwtAuthenticationFilter para rechazar tokens revocados inmediatamente
 */
@Slf4j
@RequiredArgsConstructor
public class JwtRevocationFilter extends OncePerRequestFilter {

    private final TokenRevocationService revocationService;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Solo procesar si hay un token Bearer
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Validar y extraer claims del token
                DecodedJWT decodedJWT = jwtUtils.validateToken(token);
                String jti = decodedJWT.getId(); // Obtener JWT ID

                // Verificar si el token está revocado
                if (jti != null && revocationService.isRevoked(jti)) {
                    log.warn("Token revocado detectado: jti={}, subject={}", jti, decodedJWT.getSubject());
                    sendRevokedTokenResponse(response);
                    return;
                }

                // Guardar el DecodedJWT en el request para evitar doble parsing
                request.setAttribute("DECODED_JWT", decodedJWT);

            } catch (Exception e) {
                // Si hay error al validar, dejamos que JwtAuthenticationFilter lo maneje
                log.debug("Error al verificar revocación (se manejará en JwtAuthenticationFilter): {}", e.getMessage());
            }
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    /**
     * Envía respuesta de error cuando el token está revocado
     */
    private void sendRevokedTokenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> errorResponse = ApiResponse.error(
            "TOKEN_REVOKED",
            "El token ha sido revocado",
            "Este token ya no es válido. Por favor, inicie sesión nuevamente."
        );

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}

