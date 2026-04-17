-- V1_3_2: Agregar campo 'comment' a la tabla labels
-- Permite registrar observaciones/comentarios por marbete individual
-- El comentario se muestra en consultas, NO en plantillas de impresión

ALTER TABLE labels
    ADD COLUMN comment VARCHAR(600) NULL COMMENT 'Comentario u observación individual del marbete';


