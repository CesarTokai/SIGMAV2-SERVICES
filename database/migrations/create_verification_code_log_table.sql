-- Migración para crear tabla de logs de códigos de verificación
-- Autor: Sistema SIGMAV2
-- Fecha: 2025-01-20
-- Descripción: Tabla para rastrear códigos de verificación enviados y validar rate limiting

CREATE TABLE verification_code_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    verification_code VARCHAR(10) NOT NULL,
    purpose VARCHAR(100) NOT NULL DEFAULT 'Email verification',
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP NULL,
    ip_address VARCHAR(45) NULL,
    user_agent TEXT NULL,
    attempts_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    PRIMARY KEY (id),
    INDEX idx_email (email),
    INDEX idx_email_code (email, verification_code),
    INDEX idx_expires_at (expires_at),
    INDEX idx_sent_at (sent_at),
    INDEX idx_is_used (is_used)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comentarios de las columnas
ALTER TABLE verification_code_log 
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID único del registro',
    MODIFY COLUMN email VARCHAR(255) NOT NULL COMMENT 'Email del usuario al que se envió el código',
    MODIFY COLUMN verification_code VARCHAR(10) NOT NULL COMMENT 'Código de verificación enviado',
    MODIFY COLUMN purpose VARCHAR(100) NOT NULL DEFAULT 'Email verification' COMMENT 'Propósito del código (registro, recuperación, etc.)',
    MODIFY COLUMN sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de envío',
    MODIFY COLUMN expires_at TIMESTAMP NOT NULL COMMENT 'Fecha y hora de expiración',
    MODIFY COLUMN is_used BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Indica si el código ya fue utilizado',
    MODIFY COLUMN used_at TIMESTAMP NULL COMMENT 'Fecha y hora en que fue utilizado',
    MODIFY COLUMN ip_address VARCHAR(45) NULL COMMENT 'Dirección IP desde donde se solicitó',
    MODIFY COLUMN user_agent TEXT NULL COMMENT 'User Agent del navegador',
    MODIFY COLUMN attempts_count INT NOT NULL DEFAULT 0 COMMENT 'Número de intentos de validación',
    MODIFY COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de creación del registro',
    MODIFY COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización';

-- Política de limpieza automática para códigos expirados (opcional)
-- DELETE FROM verification_code_log WHERE expires_at < NOW() - INTERVAL 30 DAY;
