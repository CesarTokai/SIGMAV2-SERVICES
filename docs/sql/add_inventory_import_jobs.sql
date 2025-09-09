-- Script para agregar tabla de jobs de importación de inventarios
-- Ejecutar después de la creación inicial de la base de datos

USE SIGMAV2;

-- Tabla para tracking de jobs de importación
CREATE TABLE IF NOT EXISTS inventory_import_jobs (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  id_period         BIGINT NOT NULL,
  id_warehouse      BIGINT NULL,  -- NULL significa ALL warehouses
  filename          VARCHAR(255) NOT NULL,
  checksum          VARCHAR(64) NULL,
  created_by        VARCHAR(255) NULL,  -- user ID o email
  started_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at       DATETIME NULL,
  status            ENUM('PENDING','RUNNING','DONE','ERROR') NOT NULL DEFAULT 'PENDING',
  total_rows        INT NULL DEFAULT 0,
  inserted_rows     INT NULL DEFAULT 0,
  updated_rows      INT NULL DEFAULT 0,
  skipped_rows      INT NULL DEFAULT 0,
  errors_json       TEXT NULL,
  log_file_path     VARCHAR(500) NULL,  -- Ruta al archivo CSV de log
  
  CONSTRAINT fk_iij_period    FOREIGN KEY (id_period)    REFERENCES periods(id_period),
  CONSTRAINT fk_iij_warehouse FOREIGN KEY (id_warehouse) REFERENCES warehouse(id_warehouse),
  
  KEY idx_iij_period (id_period),
  KEY idx_iij_status (status),
  KEY idx_iij_started (started_at)
) ENGINE=InnoDB;

-- Índice para búsqueda de duplicados por checksum
CREATE INDEX idx_iij_checksum ON inventory_import_jobs(id_period, id_warehouse, checksum);

-- Comentarios para documentación
ALTER TABLE inventory_import_jobs COMMENT = 'Tabla para tracking de jobs de importación de inventarios con logs descargables';