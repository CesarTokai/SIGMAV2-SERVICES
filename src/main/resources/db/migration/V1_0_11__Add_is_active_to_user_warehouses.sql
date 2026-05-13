-- Agregar columna is_active a la tabla user_warehouses si no existe
-- Esta columna es requerida para controlar el estado de las asignaciones

ALTER TABLE user_warehouses
ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Comentario
COMMENT ON COLUMN user_warehouses.is_active IS 'Indica si la asignación del almacén al usuario está activa';

