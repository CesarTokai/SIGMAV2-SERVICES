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
 *
 * IMPORTANTE: La actualización se realiza de forma SÍNCRONA pero con manejo de excepciones
 * para no bloquear el request si hay errores.
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

                // ✅ ACTUALIZAR de forma SÍNCRONA pero con manejo de errores
                updateUserActivity(email);
            }
        } catch (Exception e) {
            // No interrumpir el flujo si falla la actualización
            log.warn("⚠️ Error al actualizar actividad del usuario: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Actualiza la última actividad del usuario
     * Usa try-catch para no afectar al flujo del request
     */
    private void updateUserActivity(String email) {
        try {
            log.debug("🔄 Intentando actualizar actividad para usuario: {}", email);

            userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    try {
                        log.debug("✅ Usuario encontrado: {} (ID: {})", email, user.getId());
                        LocalDateTime now = LocalDateTime.now();
                        user.setLastActivityAt(now);
                        log.debug("⏰ Estableciendo lastActivityAt a: {}", now);

                        userRepository.save(user);
                        log.debug("💾 Usuario guardado exitosamente: {} - lastActivityAt actualizado a: {}", email, now);
                    } catch (Exception e) {
                        log.debug("❌ Error guardando usuario {}: {}", email, e.getMessage());
                        // No relanzar - permitir que el request continúe
                    }
                },
                () -> {
                    log.debug("⚠️ Usuario NO encontrado en BD: {}", email);
                }
            );
        } catch (Exception e) {
            log.debug("❌ Error al actualizar actividad para {}: {}", email, e.getMessage());
            // No relanzar - permitir que el request continúe
        }
    }
}


