-- Persistencia del estado de los periodos
-- Ejecuta este script en la base de datos SIGMAV2 (MySQL 8+)

-- Agregar columna 'state' si no existe
-- Nota: Algunas versiones de MySQL no soportan IF NOT EXISTS en ADD COLUMN.
-- Si tu versión no lo soporta y la columna ya existe, verás un error que puedes ignorar.
ALTER TABLE periods
  ADD COLUMN state VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- Asegurar valores consistentes para registros existentes (por si el DEFAULT no se aplica retroactivamente)
UPDATE periods SET state = COALESCE(state, 'DRAFT');

-- Restringir valores posibles (opcional si tu versión soporta CHECK)
-- Si tu MySQL soporta CHECK constraints, puedes habilitar lo siguiente:
-- ALTER TABLE periods
--   ADD CONSTRAINT chk_period_state CHECK (state IN ('DRAFT','OPEN','CLOSED','LOCKED'));
