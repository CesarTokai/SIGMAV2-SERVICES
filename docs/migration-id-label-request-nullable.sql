-- Migración: Permitir NULL en id_label_request
-- Fecha: 2025-12-29
-- Razón: Versión simplificada de generación de marbetes no requiere solicitud previa

-- Modificar la columna para permitir NULL
ALTER TABLE labels
MODIFY COLUMN id_label_request BIGINT NULL;

-- Verificar el cambio
DESCRIBE labels;

