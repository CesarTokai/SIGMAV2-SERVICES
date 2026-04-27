-- ============================================================================
-- FIX URGENTE: Cambiar PK de labels a compuesta (folio, id_period)
-- Ejecutar directamente en MySQL para resolver el error inmediatamente
-- ============================================================================

USE SIGMAV2_2;

-- Verificar PK actual antes de cambiar
SELECT
    kcu.COLUMN_NAME,
    tc.CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.KEY_COLUMN_USAGE kcu
    ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
    AND tc.TABLE_NAME = kcu.TABLE_NAME
WHERE tc.TABLE_SCHEMA = 'SIGMAV2_2'
  AND tc.TABLE_NAME = 'labels'
  AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY';

-- Eliminar PK actual (solo folio)
ALTER TABLE labels DROP PRIMARY KEY;

-- Crear PK compuesta (folio + periodo) — permite folios repetidos entre periodos
ALTER TABLE labels ADD PRIMARY KEY (folio, id_period);

-- Índice para búsquedas por folio únicamente
ALTER TABLE labels ADD INDEX idx_labels_folio (folio);

-- Índice para búsquedas por periodo
ALTER TABLE labels ADD INDEX idx_labels_period (id_period);

-- Verificar resultado
SELECT
    kcu.COLUMN_NAME,
    tc.CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.KEY_COLUMN_USAGE kcu
    ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
    AND tc.TABLE_NAME = kcu.TABLE_NAME
WHERE tc.TABLE_SCHEMA = 'SIGMAV2_2'
  AND tc.TABLE_NAME = 'labels'
  AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY';

-- Resultado esperado: 2 filas → folio + id_period como PRIMARY KEY

