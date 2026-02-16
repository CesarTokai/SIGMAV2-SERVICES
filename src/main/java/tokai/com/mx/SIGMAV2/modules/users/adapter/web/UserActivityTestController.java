package tokai.com.mx.SIGMAV2.modules.users.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.application.service.UserActivityLogService;
import tokai.com.mx.SIGMAV2.security.infrastructure.adapter.SecurityUserAdapter;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/users")
@RequiredArgsConstructor
public class UserActivityTestController {

    private final UserService userService;
    private final UserActivityLogService activityLogService;
    private final SecurityUserAdapter securityUserAdapter;

    /**
     * ENDPOINT DE PRUEBA: Registra una actividad manualmente para testing
     */
    @PostMapping("/admin/test/log-activity")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> testLogActivity(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String actionType = request.getOrDefault("actionType", "LOGIN");

        log.info("TEST: Registrando actividad manual para usuario: {} - Acción: {}", email, actionType);

        try {
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            tokai.com.mx.SIGMAV2.modules.users.model.BeanUser beanUser =
                securityUserAdapter.toLegacyUser(user);

            switch (actionType) {
                case "LOGIN":
                    activityLogService.logLogin(beanUser);
                    break;
                case "LOGOUT":
                    activityLogService.logLogout(beanUser);
                    break;
                case "FAILED_LOGIN":
                    activityLogService.logFailedLogin(beanUser, "Prueba");
                    break;
                case "BLOCKED":
                    activityLogService.logBlocked(beanUser, "Prueba de bloqueo");
                    break;
                case "UNBLOCKED":
                    activityLogService.logUnblocked(beanUser);
                    break;
                case "PASSWORD_CHANGED":
                    activityLogService.logPasswordChanged(beanUser);
                    break;
                case "DEACTIVATED":
                    activityLogService.logDeactivated(beanUser, "Prueba");
                    break;
                case "ACTIVATED":
                    activityLogService.logActivated(beanUser);
                    break;
                default:
                    return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Tipo de acción no válido"));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Actividad registrada correctamente",
                "user", email,
                "action", actionType
            ));

        } catch (Exception e) {
            log.error("Error al registrar actividad de prueba: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }
}

