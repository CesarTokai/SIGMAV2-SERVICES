-- =============================================================================
-- V1_2_2: Mejoras de auditoría en el módulo Labels
--
-- Cambios:
-- 1. Agrega columnas conteo1_al_cancelar / conteo2_al_cancelar en labels_cancelled
--    para archivar los valores de conteo cuando se cancela un marbete,
--    evitando borrarlos y perdiendo la evidencia de auditoría.
-- 2. Agrega columnas de auditoría (previous_value, updated_at, updated_by)
--    en label_count_events para rastrear cambios en updateCountC1/C2.
-- =============================================================================

-- ─── 1. Archivar conteos al cancelar ────────────────────────────────────────
ALTER TABLE labels_cancelled
    ADD COLUMN IF NOT EXISTS conteo1_al_cancelar DECIMAL(14, 4) NULL
        COMMENT 'Valor del conteo C1 al momento de cancelar el marbete. NULL si no había C1.',
    ADD COLUMN IF NOT EXISTS conteo2_al_cancelar DECIMAL(14, 4) NULL
        COMMENT 'Valor del conteo C2 al momento de cancelar el marbete. NULL si no había C2.';

-- ─── 2. Auditoría de cambios en conteos ────────────────────────────────────
ALTER TABLE label_count_events
    ADD COLUMN IF NOT EXISTS previous_value DECIMAL(14, 4) NULL
        COMMENT 'Valor anterior antes de la última actualización (updateCountC1/C2).',
    ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL
        COMMENT 'Timestamp de la última actualización. NULL si nunca fue modificado.',
    ADD COLUMN IF NOT EXISTS updated_by BIGINT NULL
        COMMENT 'ID del usuario que realizó la última actualización.';

