package tokai.com.mx.SIGMAV2.modules.users.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserActivityLogRepository;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUserActivityLog;
import tokai.com.mx.SIGMAV2.security.infrastructure.adapter.SecurityUserAdapter;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/users")
@RequiredArgsConstructor
public class UserActivityLogController {

    private final UserService userService;
    private final JpaUserActivityLogRepository activityLogRepository;
    private final SecurityUserAdapter securityUserAdapter;

    /**
     * Obtiene el historial de actividades de un usuario
     */
    @PostMapping("/admin/activity-log/by-email")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getActivityLogByEmail(
            @RequestBody Map<String, Object> request) {

        String email = (String) request.get("email");
        Integer page = (Integer) request.getOrDefault("page", 0);
        Integer size = (Integer) request.getOrDefault("size", 20);

        log.info("Admin consultando historial de actividades de usuario: {}", email);

        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Email no puede estar vacío"));
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            tokai.com.mx.SIGMAV2.modules.users.model.BeanUser beanUser =
                securityUserAdapter.toLegacyUser(user);

            Pageable pageable = PageRequest.of(page, size);
            Page<BeanUserActivityLog> activityPage = activityLogRepository
                .findByUserOrderByTimestampDesc(beanUser, pageable);

            List<Map<String, Object>> activities = new ArrayList<>();
            for (BeanUserActivityLog log : activityPage.getContent()) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("logId", log.getLogId());
                activity.put("actionType", log.getActionType());
                activity.put("actionDetails", log.getActionDetails());
                activity.put("timestamp", log.getTimestamp());
                activity.put("ipAddress", log.getIpAddress());
                activity.put("previousStatus", log.getPreviousStatus());
                activity.put("newStatus", log.getNewStatus());
                activities.add(activity);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("totalActivities", activityPage.getTotalElements());
            response.put("currentPage", activityPage.getNumber());
            response.put("totalPages", activityPage.getTotalPages());
            response.put("activities", activities);

            log.info("Retornando {} actividades para usuario: {}",
                activityPage.getNumberOfElements(), email);

            return ResponseEntity.ok(Map.of("success", true, "data", response));

        } catch (Exception e) {
            log.error("Error al obtener historial de actividades: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno"));
        }
    }

    /**
     * Obtiene resumen de actividades recientes
     */
    @PostMapping("/admin/activity-summary/by-email")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> getActivitySummary(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        log.info("Admin consultando resumen de actividades de usuario: {}", email);

        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Email no puede estar vacío"));
            }

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Usuario no encontrado"));
            }

            User user = userOpt.get();
            tokai.com.mx.SIGMAV2.modules.users.model.BeanUser beanUser =
                securityUserAdapter.toLegacyUser(user);

            // Obtener últimas actividades (máximo 10)
            Pageable pageable = PageRequest.of(0, 10);
            Page<BeanUserActivityLog> recentActivity = activityLogRepository
                .findByUserOrderByTimestampDesc(beanUser, pageable);

            Map<String, Object> summary = new HashMap<>();
            summary.put("userId", user.getId());
            summary.put("email", user.getEmail());
            summary.put("status", user.isStatus() ? "ACTIVO" : "INACTIVO");
            summary.put("isVerified", user.isVerified());
            summary.put("lastLogin", user.getLastLoginAt());
            summary.put("lastActivity", user.getLastActivityAt());
            summary.put("lastPasswordChange", user.getPasswordChangedAt());
            summary.put("lastBlocked", user.getLastBlockedAt());

            // Contar acciones por tipo en las últimas 10 actividades
            Map<String, Long> actionCounts = new HashMap<>();
            for (BeanUserActivityLog log : recentActivity.getContent()) {
                actionCounts.merge(log.getActionType(), 1L, Long::sum);
            }
            summary.put("recentActionCounts", actionCounts);
            summary.put("totalActivities", activityLogRepository
                .findByUserOrderByTimestampDesc(beanUser, PageRequest.of(0, 1))
                .getTotalElements());

            log.info("Resumen de actividades generado para usuario: {}", email);

            return ResponseEntity.ok(Map.of("success", true, "data", summary));

        } catch (Exception e) {
            log.error("Error al obtener resumen de actividades: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error interno"));
        }
    }
}

