-- Agregar columna comment a label_count_events
-- Permite registrar comentarios opcionales en cada evento de conteo físico

ALTER TABLE label_count_events
ADD COLUMN comment VARCHAR(600) DEFAULT NULL
AFTER updated_by;

-- Agregar índice para búsquedas por comentario (opcional, pero útil)
CREATE INDEX idx_label_count_events_comment ON label_count_events(comment(100));

