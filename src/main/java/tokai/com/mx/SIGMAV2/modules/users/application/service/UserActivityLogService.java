package tokai.com.mx.SIGMAV2.modules.users.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUserActivityLog;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserActivityLogRepository;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private final JpaUserActivityLogRepository activityLogRepository;

    /**
     * Registra un login de usuario
     */
    @Transactional
    public void logLogin(BeanUser user) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("LOGIN");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Usuario inició sesión");
        activityLogRepository.save(activityLog);
        log.info("✅ LOGIN registrado para usuario: {}", user.getEmail());
    }

    /**
     * Registra un logout de usuario
     */
    @Transactional
    public void logLogout(BeanUser user) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("LOGOUT");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Usuario cerró sesión");
        activityLogRepository.save(activityLog);
        log.info("✅ LOGOUT registrado para usuario: {}", user.getEmail());
    }

    /**
     * Registra un bloqueo de usuario por intentos fallidos
     */
    @Transactional
    public void logBlocked(BeanUser user, String reason) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("BLOCKED");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Usuario bloqueado: " + reason);
        activityLog.setPreviousStatus("ACTIVE");
        activityLog.setNewStatus("BLOCKED");
        activityLogRepository.save(activityLog);
        log.info("⛔ BLOQUEO registrado para usuario: {} - Razón: {}", user.getEmail(), reason);
    }

    /**
     * Registra un desbloqueo de usuario
     */
    @Transactional
    public void logUnblocked(BeanUser user) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("UNBLOCKED");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Usuario desbloqueado");
        activityLog.setPreviousStatus("BLOCKED");
        activityLog.setNewStatus("ACTIVE");
        activityLogRepository.save(activityLog);
        log.info("🔓 DESBLOQUEO registrado para usuario: {}", user.getEmail());
    }

    /**
     * Registra un intento de login fallido
     */
    @Transactional
    public void logFailedLogin(BeanUser user, String reason) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("FAILED_LOGIN");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Intento fallido de login: " + reason);
        activityLogRepository.save(activityLog);
        log.warn("⚠️ LOGIN FALLIDO registrado para usuario: {} - Razón: {}",
            user.getEmail(), reason);
    }

    /**
     * Registra un cambio de contraseña
     */
    @Transactional
    public void logPasswordChanged(BeanUser user) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("PASSWORD_CHANGED");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Contraseña cambiada");
        activityLogRepository.save(activityLog);
        log.info("🔑 CAMBIO DE CONTRASEÑA registrado para usuario: {}", user.getEmail());
    }

    /**
     * Registra una desactivación de usuario
     */
    @Transactional
    public void logDeactivated(BeanUser user, String reason) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("DEACTIVATED");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Usuario desactivado: " + reason);
        activityLog.setPreviousStatus("ACTIVE");
        activityLog.setNewStatus("INACTIVE");
        activityLogRepository.save(activityLog);
        log.info("🚫 DESACTIVACIÓN registrada para usuario: {} - Razón: {}", user.getEmail(), reason);
    }

    /**
     * Registra una activación de usuario
     */
    @Transactional
    public void logActivated(BeanUser user) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("ACTIVATED");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Usuario activado");
        activityLog.setPreviousStatus("INACTIVE");
        activityLog.setNewStatus("ACTIVE");
        activityLogRepository.save(activityLog);
        log.info("✅ ACTIVACIÓN registrada para usuario: {}", user.getEmail());
    }

    /**
     * Registra un intento de recuperación de contraseña
     */
    @Transactional
    public void logPasswordResetAttempt(BeanUser user, String status) {
        BeanUserActivityLog activityLog = new BeanUserActivityLog();
        activityLog.setUser(user);
        activityLog.setActionType("PASSWORD_RESET_ATTEMPT");
        activityLog.setTimestamp(LocalDateTime.now());
        activityLog.setActionDetails("Intento de recuperación de contraseña: " + status);
        activityLogRepository.save(activityLog);
        log.info("🔄 PASSWORD RESET registrado para usuario: {} - Status: {}", user.getEmail(), status);
    }
}

