-- Agregar campos de seguimiento de actividad a la tabla users
ALTER TABLE users
ADD COLUMN last_login_at DATETIME NULL COMMENT 'Última vez que el usuario inició sesión',
ADD COLUMN last_activity_at DATETIME NULL COMMENT 'Última actividad del usuario en el sistema',
ADD COLUMN password_changed_at DATETIME NULL COMMENT 'Última vez que cambió la contraseña';

-- Índices para mejorar performance de consultas
CREATE INDEX idx_users_last_login ON users(last_login_at);
CREATE INDEX idx_users_last_activity ON users(last_activity_at);
