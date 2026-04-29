-- ============================================================================
-- V1_3_5 — Fix: labels_cancelled unique constraint debe ser compuesta
-- ============================================================================
-- Problema: con PK compuesta (folio, id_period) en labels, el mismo folio puede
-- existir en múltiples periodos. La constraint UNIQUE(folio) en labels_cancelled
-- impide cancelar folio=1 del periodo 2 si ya existe folio=1 del periodo 1.
-- Solución: reemplazar UNIQUE(folio) por UNIQUE(folio, id_period).
-- Script idempotente.
-- ============================================================================

-- Drop constraint simple (folio) si existe
SET @has_uk_folio = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'labels_cancelled'
      AND CONSTRAINT_NAME = 'folio'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);
SET @sql = IF(@has_uk_folio > 0, 'ALTER TABLE labels_cancelled DROP INDEX folio', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Drop constraint si tiene nombre generado por Hibernate
SET @has_uk_gen = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'labels_cancelled'
      AND CONSTRAINT_NAME NOT IN ('PRIMARY', 'uk_cancelled_folio_period')
      AND CONSTRAINT_TYPE = 'UNIQUE'
      AND CONSTRAINT_NAME LIKE '%folio%'
);
-- Si hay constraint con nombre que contenga 'folio', la quitamos
SET @old_uk_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'labels_cancelled'
      AND CONSTRAINT_NAME NOT IN ('PRIMARY', 'uk_cancelled_folio_period')
      AND CONSTRAINT_TYPE = 'UNIQUE'
      AND CONSTRAINT_NAME LIKE '%folio%'
    LIMIT 1
);
SET @sql2 = IF(@old_uk_name IS NOT NULL AND @old_uk_name != '',
    CONCAT('ALTER TABLE labels_cancelled DROP INDEX `', @old_uk_name, '`'),
    'SELECT 1');
PREPARE s FROM @sql2; EXECUTE s; DEALLOCATE PREPARE s;

-- Agregar constraint compuesta si no existe
SET @has_composite = (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'labels_cancelled'
      AND CONSTRAINT_NAME = 'uk_cancelled_folio_period'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);
SET @sql3 = IF(@has_composite = 0,
    'ALTER TABLE labels_cancelled ADD CONSTRAINT uk_cancelled_folio_period UNIQUE (folio, id_period)',
    'SELECT 1');
PREPARE s FROM @sql3; EXECUTE s; DEALLOCATE PREPARE s;
