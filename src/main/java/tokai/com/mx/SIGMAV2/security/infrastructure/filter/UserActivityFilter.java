package tokai.com.mx.SIGMAV2.security.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Filtro que actualiza la última actividad del usuario en cada request autenticado
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Obtener autenticación
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof String) {

                String email = (String) authentication.getPrincipal();

                // Actualizar última actividad de forma asíncrona para no bloquear el request
                updateUserActivity(email);
            }
        } catch (Exception e) {
            // No interrumpir el flujo si falla la actualización
            log.warn("Error al actualizar actividad del usuario: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void updateUserActivity(String email) {
        try {
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setLastActivityAt(LocalDateTime.now());
                userRepository.save(user);
            });
        } catch (Exception e) {
            log.debug("No se pudo actualizar actividad para {}: {}", email, e.getMessage());
        }
    }
}
