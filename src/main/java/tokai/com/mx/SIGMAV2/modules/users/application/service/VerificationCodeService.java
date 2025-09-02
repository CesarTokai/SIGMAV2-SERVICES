package tokai.com.mx.SIGMAV2.modules.users.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.VerificationCodeLog;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.VerificationCodeLogRepository;
import tokai.com.mx.SIGMAV2.shared.exception.CustomException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para manejo avanzado de códigos de verificación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {
    
    private final VerificationCodeLogRepository verificationCodeLogRepository;
    
    // Configuraciones
    private static final int MAX_RESENDS_PER_HOUR = 3;
    private static final int MAX_RESENDS_PER_DAY = 10;
    private static final int CODE_VALIDITY_HOURS = 24;
    
    /**
     * Genera un nuevo código de verificación con límites de reenvío
     */
    @Transactional
    public String generateVerificationCode(String email, String reason) {
        log.info("Generando código de verificación para: {} - Razón: {}", email, reason);
        
        // Verificar límites de reenvío
        validateResendLimits(email);
        
        // Invalidar códigos anteriores
        invalidatePreviousCodes(email);
        
        // Generar nuevo código
        String newCode = generateRandomCode();
        
        // Crear log del nuevo código
        VerificationCodeLog codeLog = new VerificationCodeLog(email, newCode, reason);
        verificationCodeLogRepository.save(codeLog);
        
        log.info("Código de verificación generado exitosamente para: {}", email);
        return newCode;
    }
    
    /**
     * Valida un código de verificación
     */
    @Transactional
    public boolean validateVerificationCode(String email, String code) {
        log.info("Validando código de verificación para: {}", email);
        
        Optional<VerificationCodeLog> codeLogOpt = verificationCodeLogRepository
                .findValidCode(email, code, LocalDateTime.now());
        
        if (codeLogOpt.isEmpty()) {
            log.warn("Código de verificación inválido o expirado para: {}", email);
            return false;
        }
        
        // Marcar código como usado
        VerificationCodeLog codeLog = codeLogOpt.get();
        codeLog.markAsUsed();
        verificationCodeLogRepository.save(codeLog);
        
        log.info("Código de verificación validado exitosamente para: {}", email);
        return true;
    }
    
    /**
     * Valida límites de reenvío
     */
    private void validateResendLimits(String email) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
        
        // Verificar límite por hora
        long resendsLastHour = verificationCodeLogRepository
                .countResentCodesInTimeRange(email, oneHourAgo);
        
        if (resendsLastHour >= MAX_RESENDS_PER_HOUR) {
            throw new CustomException("Se ha excedido el límite de " + MAX_RESENDS_PER_HOUR + 
                    " reenvíos por hora. Inténtalo más tarde.");
        }
        
        // Verificar límite por día
        long resendsLastDay = verificationCodeLogRepository
                .countResentCodesInTimeRange(email, oneDayAgo);
        
        if (resendsLastDay >= MAX_RESENDS_PER_DAY) {
            throw new CustomException("Se ha excedido el límite de " + MAX_RESENDS_PER_DAY + 
                    " reenvíos por día. Contacta al soporte si necesitas ayuda.");
        }
    }
    
    /**
     * Invalida códigos anteriores al generar uno nuevo
     */
    private void invalidatePreviousCodes(String email) {
        List<VerificationCodeLog> activeCodes = verificationCodeLogRepository
                .findActiveCodesByEmail(email);
        
        for (VerificationCodeLog code : activeCodes) {
            code.markAsReplaced();
            verificationCodeLogRepository.save(code);
        }
        
        log.info("Invalidados {} códigos anteriores para: {}", activeCodes.size(), email);
    }
    
    /**
     * Genera código aleatorio de 6 dígitos
     */
    private String generateRandomCode() {
        return String.valueOf((int) (Math.random() * 900000 + 100000));
    }
    
    /**
     * Limpia códigos expirados (para ser ejecutado por scheduler)
     */
    @Transactional
    public void cleanupExpiredCodes() {
        log.info("Iniciando limpieza de códigos expirados");
        
        List<VerificationCodeLog> expiredCodes = verificationCodeLogRepository
                .findExpiredCodes(LocalDateTime.now());
        
        for (VerificationCodeLog code : expiredCodes) {
            code.markAsExpired();
            verificationCodeLogRepository.save(code);
        }
        
        log.info("Limpieza completada: {} códigos marcados como expirados", expiredCodes.size());
    }
    
    /**
     * Obtiene información del último código para un usuario
     */
    public Optional<VerificationCodeLog> getLastActiveCode(String email) {
        return verificationCodeLogRepository.findLastActiveCodeByEmail(email);
    }
    
    /**
     * Verifica si un usuario puede solicitar un nuevo código
     */
    public boolean canRequestNewCode(String email) {
        try {
            validateResendLimits(email);
            return true;
        } catch (CustomException e) {
            return false;
        }
    }
}
