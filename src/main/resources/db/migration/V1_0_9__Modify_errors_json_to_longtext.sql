-- Modificar la columna errors_json de TEXT a LONGTEXT
ALTER TABLE inventory_import_jobs MODIFY COLUMN errors_json LONGTEXT CHARACTER SET utf8mb4;

