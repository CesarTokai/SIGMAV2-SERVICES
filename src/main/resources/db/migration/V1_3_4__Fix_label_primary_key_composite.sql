-- ============================================================================
-- V1_3_4 — CORRECCIÓN CRÍTICA: Primary Key compuesta en labels
-- ============================================================================
-- Problema: folio se repite entre periodos → Violación de PK al generar en
--           un segundo periodo con folio=1.
-- Solución: PK compuesta (folio, id_period) para garantizar unicidad por periodo.
-- Script idempotente: verifica antes de modificar.
-- ============================================================================

-- Verificar si la PK ya incluye id_period
SET @pk_composite = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'labels'
      AND CONSTRAINT_NAME = 'PRIMARY'
      AND COLUMN_NAME = 'id_period'
);

-- DROP PRIMARY KEY solo si aún es simple (solo folio)
SET @sql = IF(@pk_composite = 0, 'ALTER TABLE labels DROP PRIMARY KEY', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ADD PRIMARY KEY compuesta
SET @sql = IF(@pk_composite = 0, 'ALTER TABLE labels ADD PRIMARY KEY (folio, id_period)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Índice auxiliar para búsquedas por folio solo (si no existe)
SET @has_idx_folio = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'labels' AND INDEX_NAME = 'idx_labels_folio'
);
SET @sql = IF(@has_idx_folio = 0, 'ALTER TABLE labels ADD INDEX idx_labels_folio (folio)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Índice para búsquedas por periodo (si no existe)
SET @has_idx_period = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'labels' AND INDEX_NAME = 'idx_labels_period'
);
SET @sql = IF(@has_idx_period = 0, 'ALTER TABLE labels ADD INDEX idx_labels_period (id_period)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================================
-- VERIFICACIÓN POST-MIGRACIÓN
-- ============================================================================
-- SELECT folio, id_period, COUNT(*) as cnt
-- FROM labels
-- GROUP BY folio, id_period
-- HAVING cnt > 1;
-- Resultado esperado: 0 registros (sin duplicados)
-- ============================================================================
