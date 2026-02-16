-- Agregar columna last_blocked_at a la tabla users si no existe
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_blocked_at DATETIME NULL;

-- Crear índice para búsquedas rápidas
CREATE INDEX IF NOT EXISTS idx_users_last_blocked_at ON users(last_blocked_at);

-- Verificar que la columna se creó correctamente
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'last_blocked_at';

