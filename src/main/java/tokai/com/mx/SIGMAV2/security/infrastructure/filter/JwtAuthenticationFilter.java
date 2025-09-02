package tokai.com.mx.SIGMAV2.security.infrastructure.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tokai.com.mx.SIGMAV2.security.infrastructure.exception.*;
import tokai.com.mx.SIGMAV2.security.infrastructure.jwt.JwtUtils;
import tokai.com.mx.SIGMAV2.shared.response.ApiResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Filtro JWT mejorado con manejo de respuestas JSON
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Para LocalDateTime
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/swagger-resources")
            || path.startsWith("/webjars")
            || path.equals("/swagger-ui.html")
            // Endpoints públicos de usuario:
            || path.equals("/api/sigmav2/users/register")
            || path.equals("/api/sigmav2/users/verify")
            || path.equals("/api/sigmav2/users/exists")
            // Endpoints de autenticación:
            || path.startsWith("/api/sigmav2/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Obtener token del header
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            
            if (authHeader == null || authHeader.trim().isEmpty()) {
                sendErrorResponse(response, new TokenMissingException());
                return;
            }

            if (!authHeader.startsWith("Bearer ")) {
                sendErrorResponse(response, new TokenMalformedException());
                return;
            }

            // Extraer token (remover "Bearer ")
            String token = authHeader.substring(7);
            
            if (token.trim().isEmpty()) {
                sendErrorResponse(response, new TokenMalformedException());
                return;
            }

            // Validar token
            DecodedJWT decodedJWT = jwtUtils.validateToken(token);

            // Extraer información del token
            String username = decodedJWT.getSubject();
            String claims = decodedJWT.getClaim("authorities").asString();

            // Crear authorities
            Collection<? extends GrantedAuthority> authorities = 
                AuthorityUtils.commaSeparatedStringToAuthorityList(claims);

            // Establecer authentication en el contexto de seguridad
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken(username, null, authorities));
            SecurityContextHolder.setContext(context);

            // Continuar con la cadena de filtros
            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            log.warn("Error de autenticación JWT: {} - {}", e.getErrorCode(), e.getMessage());
            sendErrorResponse(response, e);
        } catch (Exception e) {
            log.error("Error inesperado en autenticación: {}", e.getMessage(), e);
            sendErrorResponse(response, new TokenInvalidException("Error interno de autenticación", 
                "Ha ocurrido un error inesperado durante la autenticación"));
        }
    }

    /**
     * Envía una respuesta de error en formato JSON
     */
    private void sendErrorResponse(HttpServletResponse response, JwtException jwtException) 
            throws IOException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> errorResponse;
        
        if (jwtException instanceof TokenExpiredException) {
            TokenExpiredException tokenExpired = (TokenExpiredException) jwtException;
            errorResponse = ApiResponse.<Object>builder()
                    .success(false)
                    .error(ApiResponse.ErrorDetails.builder()
                            .code(jwtException.getErrorCode())
                            .message(jwtException.getMessage())
                            .details(jwtException.getDetails())
                            .expiredAt(tokenExpired.getExpiredAt())
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        } else {
            errorResponse = ApiResponse.error(
                jwtException.getErrorCode(),
                jwtException.getMessage(),
                jwtException.getDetails()
            );
        }

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
