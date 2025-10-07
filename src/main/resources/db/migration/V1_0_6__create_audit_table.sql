-- Flyway migration: crear tabla audit_entry para MySQL
CREATE TABLE IF NOT EXISTS audit_entry (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  principal VARCHAR(255),
  principal_name VARCHAR(255),
  action VARCHAR(100) NOT NULL,
  resource_type VARCHAR(100),
  resource_id VARCHAR(255),
  outcome VARCHAR(50),
  http_status INT,
  client_ip VARCHAR(100),
  user_agent VARCHAR(512),
  details TEXT,
  INDEX idx_audit_created_at (created_at),
  INDEX idx_audit_principal (principal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

