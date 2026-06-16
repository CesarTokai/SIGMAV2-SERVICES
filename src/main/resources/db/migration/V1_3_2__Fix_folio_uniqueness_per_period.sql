-- ============================================================
-- V1_3_2: Corrección de unicidad de folios por período
--
-- PROBLEMA: El diseño del sistema usa LabelFolioSequence por
-- período, lo que significa que el mismo número de folio puede
-- existir en períodos distintos (folio 1 del período 1 ≠ folio 1 del período 2).
--
-- SOLUCIÓN:
--  1. labels_cancelled: cambiar UNIQUE(folio) → UNIQUE(folio, id_period)
--  2. label_count_events: agregar columna id_period y actualizar
--     el constraint único de (folio, count_number) → (folio, count_number, id_period)
-- ============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. TABLA: labels_cancelled
-- ─────────────────────────────────────────────────────────────

-- Eliminar el índice único existente sobre folio solo.
-- El nombre del índice lo genera Hibernate como "folio" (del @Column unique=true).
-- Usamos procedimiento seguro para no fallar si ya fue eliminado.
SET @idx_cancelled := (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'labels_cancelled'
      AND COLUMN_NAME  = 'folio'
      AND NON_UNIQUE   = 0
      AND SEQ_IN_INDEX = 1
    LIMIT 1
);

SET @drop_cancelled := IF(
    @idx_cancelled IS NOT NULL AND @idx_cancelled != 'PRIMARY',
    CONCAT('ALTER TABLE labels_cancelled DROP INDEX `', @idx_cancelled, '`'),
    'SELECT 1 -- no single-folio unique index found on labels_cancelled'
);
PREPARE stmt FROM @drop_cancelled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Agregar nuevo constraint compuesto (folio, id_period) si no existe
SET @uk_cancelled_exists := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA   = DATABASE()
      AND TABLE_NAME     = 'labels_cancelled'
      AND CONSTRAINT_NAME = 'uk_cancelled_folio_period'
);

SET @add_cancelled := IF(
    @uk_cancelled_exists = 0,
    'ALTER TABLE labels_cancelled ADD CONSTRAINT uk_cancelled_folio_period UNIQUE (folio, id_period)',
    'SELECT 1 -- uk_cancelled_folio_period ya existe'
);
PREPARE stmt FROM @add_cancelled;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ─────────────────────────────────────────────────────────────
-- 2. TABLA: label_count_events
-- ─────────────────────────────────────────────────────────────

-- 2a. Agregar columna id_period si no existe
ALTER TABLE label_count_events
    ADD COLUMN IF NOT EXISTS id_period BIGINT NULL
        COMMENT 'Período al que pertenece el folio — parte de la PK compuesta del marbete';

-- 2b. Poblar id_period desde la tabla labels (join por folio)
-- Solo actualiza registros donde id_period sea NULL
UPDATE label_count_events lce
    INNER JOIN labels l ON l.folio = lce.folio
SET lce.id_period = l.id_period
WHERE lce.id_period IS NULL;

-- 2c. Eliminar el unique constraint antiguo (folio, count_number)
SET @idx_count := (
    SELECT tc.CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS tc
    INNER JOIN information_schema.KEY_COLUMN_USAGE kcu
        ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
        AND tc.TABLE_SCHEMA   = kcu.TABLE_SCHEMA
        AND tc.TABLE_NAME     = kcu.TABLE_NAME
    WHERE tc.TABLE_SCHEMA   = DATABASE()
      AND tc.TABLE_NAME     = 'label_count_events'
      AND tc.CONSTRAINT_TYPE = 'UNIQUE'
      AND kcu.COLUMN_NAME    = 'folio'
      AND tc.CONSTRAINT_NAME != 'uk_count_event_folio_count_period'
    LIMIT 1
);

SET @drop_count := IF(
    @idx_count IS NOT NULL,
    CONCAT('ALTER TABLE label_count_events DROP INDEX `', @idx_count, '`'),
    'SELECT 1 -- no old unique index found on label_count_events'
);
PREPARE stmt FROM @drop_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2d. Agregar nuevo constraint compuesto (folio, count_number, id_period)
SET @uk_count_exists := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA    = DATABASE()
      AND TABLE_NAME      = 'label_count_events'
      AND CONSTRAINT_NAME = 'uk_count_event_folio_count_period'
);

SET @add_count := IF(
    @uk_count_exists = 0,
    'ALTER TABLE label_count_events ADD CONSTRAINT uk_count_event_folio_count_period UNIQUE (folio, count_number, id_period)',
    'SELECT 1 -- uk_count_event_folio_count_period ya existe'
);
PREPARE stmt FROM @add_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ─────────────────────────────────────────────────────────────
-- 3. Índice adicional para mejorar performance de búsquedas por folio+período
-- ─────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_count_events_folio_period
    ON label_count_events (folio, id_period);

CREATE INDEX IF NOT EXISTS idx_cancelled_folio_period
    ON labels_cancelled (folio, id_period);

