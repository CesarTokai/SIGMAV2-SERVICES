-- Asegurar que existen las tablas necesarias para el módulo de inventario

-- Tabla de productos
CREATE TABLE IF NOT EXISTS products (
    id_product BIGINT AUTO_INCREMENT PRIMARY KEY,
    cve_art VARCHAR(50) NOT NULL UNIQUE,
    descr VARCHAR(255) NOT NULL,
    uni_med VARCHAR(20) NOT NULL,
    lin_prod VARCHAR(100),
    status VARCHAR(1) DEFAULT 'A',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de periodos para inventario
CREATE TABLE IF NOT EXISTS periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_date DATE NOT NULL,
    comments TEXT,
    state VARCHAR(20) DEFAULT 'ABIERTO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de almacenes
CREATE TABLE IF NOT EXISTS warehouses (
    id_warehouse BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_name VARCHAR(100) NOT NULL,
    warehouse_location VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de snapshots de inventario
CREATE TABLE IF NOT EXISTS inventory_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT,
    period_id BIGINT NOT NULL,
    exist_qty DECIMAL(10,2) DEFAULT 0.00,
    status VARCHAR(1) DEFAULT 'A',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id_product) ON DELETE CASCADE,
    FOREIGN KEY (period_id) REFERENCES periods(id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id_warehouse) ON DELETE SET NULL,
    UNIQUE KEY unique_product_period_warehouse (product_id, period_id, warehouse_id)
);

-- Tabla de trabajos de importación
CREATE TABLE IF NOT EXISTS inventory_import_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    user VARCHAR(100) NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    total_records INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    inserted_rows INT DEFAULT 0,
    updated_rows INT DEFAULT 0,
    skipped_rows INT DEFAULT 0,
    total_rows INT DEFAULT 0,
    id_period BIGINT,
    created_by VARCHAR(100),
    log_file_path VARCHAR(500),
    errors_json LONGTEXT,
    checksum VARCHAR(255),
    FOREIGN KEY (id_period) REFERENCES periods(id) ON DELETE SET NULL
);

-- Índices para mejorar el rendimiento
CREATE INDEX IF NOT EXISTS idx_snapshot_period ON inventory_snapshot(period_id);
CREATE INDEX IF NOT EXISTS idx_snapshot_product ON inventory_snapshot(product_id);
CREATE INDEX IF NOT EXISTS idx_snapshot_warehouse ON inventory_snapshot(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_products_cve_art ON products(cve_art);
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);

