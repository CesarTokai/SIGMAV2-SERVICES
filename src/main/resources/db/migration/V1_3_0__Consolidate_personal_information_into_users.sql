-- ✅ Consolidar información personal en tabla users
-- Se agregan 5 campos directamente a users eliminando la tabla personal_information

-- Paso 1: Agregar nuevas columnas a tabla users
ALTER TABLE users
ADD COLUMN name VARCHAR(100),
ADD COLUMN first_last_name VARCHAR(100),
ADD COLUMN second_last_name VARCHAR(100),
ADD COLUMN phone_number VARCHAR(20),
ADD COLUMN comments LONGTEXT;

-- Paso 2: Agregar índices para búsquedas frecuentes
ALTER TABLE users
ADD INDEX idx_users_name (name),
ADD INDEX idx_users_first_last_name (first_last_name);

