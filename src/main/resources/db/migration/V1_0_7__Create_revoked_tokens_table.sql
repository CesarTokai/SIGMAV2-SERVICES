-- Crear tabla para tokens revocados
CREATE TABLE revoked_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    jti VARCHAR(512) NOT NULL UNIQUE,
    revoked_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    reason VARCHAR(100),
    username VARCHAR(255),
    INDEX idx_revoked_jti (jti),
    INDEX idx_expires_at (expires_at)
);

