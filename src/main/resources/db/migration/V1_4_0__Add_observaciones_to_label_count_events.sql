-- Agregar columna observaciones a label_count_events para registrar comentarios de conteos
ALTER TABLE label_count_events
ADD COLUMN observaciones VARCHAR(500) NULL AFTER updated_by;
