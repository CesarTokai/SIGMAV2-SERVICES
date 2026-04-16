-- V1_3_0__Add_qr_content_to_labels.sql
-- Agregar soporte QR a tabla labels

ALTER TABLE labels ADD COLUMN (
    qr_content VARCHAR(255) DEFAULT NULL AFTER estado,
    COMMENT 'Contenido QR generado (ej: número folio "42")'
);

-- Índice para búsquedas por qr_content
CREATE INDEX idx_labels_qr_content ON labels(qr_content);

-- Migración: Llenar qr_content con folio para labels existentes
UPDATE labels SET qr_content = CAST(folio AS CHAR(255)) WHERE qr_content IS NULL;

-- Log
INSERT INTO migration_log (version, description, executed_at)
VALUES ('V1_3_0', 'Add QR content support to labels', NOW());

