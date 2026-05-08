-- V1_2_5: Agrega columna 'copies' a la tabla labels
-- Propósito: Almacenar cuántas copias físicas se deben imprimir del mismo folio.
--            Un folio es ÚNICO por período; 'copies' indica cuántas veces se imprime.
-- Impacto: Sin ruptura (DEFAULT 1 asegura compatibilidad con registros existentes).

ALTER TABLE labels
    ADD COLUMN copies INT NOT NULL DEFAULT 1
        COMMENT 'Número de copias físicas a imprimir de este marbete (folio único)';

