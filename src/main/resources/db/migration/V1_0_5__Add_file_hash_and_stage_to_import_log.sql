-- Agrega las columnas file_hash y stage a multiwarehouse_import_log
ALTER TABLE multiwarehouse_import_log
ADD COLUMN file_hash VARCHAR(64),
ADD COLUMN stage VARCHAR(20);

