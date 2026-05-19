-- Creación de tabla de asignación de usuarios a almacenes
-- Esta tabla permite implementar el control de contexto informativo
-- según las reglas de negocio del módulo de Marbetes

CREATE TABLE IF NOT EXISTS user_warehouse_assignments (
    id_user BIGINT NOT NULL,
    id_warehouse BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (id_user, id_warehouse),
    FOREIGN KEY (id_user) REFERENCES users(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_warehouse) REFERENCES main_warehouse(id_warehouse) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id_user) ON DELETE SET NULL
);

-- Índices para mejorar el rendimiento de consultas
CREATE INDEX idx_user_warehouse_user ON user_warehouse_assignments(id_user) WHERE is_active = TRUE;
CREATE INDEX idx_user_warehouse_warehouse ON user_warehouse_assignments(id_warehouse) WHERE is_active = TRUE;

-- Comentarios
COMMENT ON TABLE user_warehouse_assignments IS 'Asignación de almacenes a usuarios para control de contexto informativo';
COMMENT ON COLUMN user_warehouse_assignments.id_user IS 'ID del usuario asignado';
COMMENT ON COLUMN user_warehouse_assignments.id_warehouse IS 'ID del almacén asignado';
COMMENT ON COLUMN user_warehouse_assignments.assigned_at IS 'Fecha y hora de asignación';
COMMENT ON COLUMN user_warehouse_assignments.assigned_by IS 'Usuario que realizó la asignación (puede ser null para asignaciones automáticas)';
COMMENT ON COLUMN user_warehouse_assignments.is_active IS 'Indica si la asignación está activa';

