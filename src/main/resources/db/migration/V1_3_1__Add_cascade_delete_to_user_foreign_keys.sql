-- ✅ Agregar ON DELETE CASCADE a las claves foráneas que referencian users
-- Permite eliminar usuarios sin violar constraints de integridad referencial

-- 1. Tabla user_activity_log
ALTER TABLE user_activity_log
DROP FOREIGN KEY FKg0a6abtwa2f8ofixbxfi2f6yd;
ALTER TABLE user_activity_log
ADD CONSTRAINT FKg0a6abtwa2f8ofixbxfi2f6yd
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

-- 2. Tabla password_reset_attempts (si existe)
ALTER TABLE password_reset_attempts
DROP FOREIGN KEY FK_password_reset_attempts_user_id;
ALTER TABLE password_reset_attempts
ADD CONSTRAINT FK_password_reset_attempts_user_id
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

-- 3. Tabla request_recovery_password
ALTER TABLE request_recovery_password
DROP FOREIGN KEY FK7d422vikyrbnnpek3qsm9147m;
ALTER TABLE request_recovery_password
ADD CONSTRAINT FK7d422vikyrbnnpek3qsm9147m
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

-- 4. Tabla user_warehouse_assignments
ALTER TABLE user_warehouse_assignments
DROP FOREIGN KEY FK_user_warehouse_assignments_user_id;
ALTER TABLE user_warehouse_assignments
ADD CONSTRAINT FK_user_warehouse_assignments_user_id
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

-- 5. Tabla revoked_tokens
ALTER TABLE revoked_tokens
DROP FOREIGN KEY FK_revoked_tokens_user_id;
ALTER TABLE revoked_tokens
ADD CONSTRAINT FK_revoked_tokens_user_id
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

-- 6. Tabla audit_logs (si tiene referencia a users)
ALTER TABLE audit_logs
DROP FOREIGN KEY FK_audit_logs_user_id;
ALTER TABLE audit_logs
ADD CONSTRAINT FK_audit_logs_user_id
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

-- Fin de migración

