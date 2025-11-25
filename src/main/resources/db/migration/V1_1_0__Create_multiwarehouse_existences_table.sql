-- Crear tabla multiwarehouse_existences para gestionar existencias de productos en múltiples almacenes
-- Esta tabla almacena el inventario de productos por almacén y periodo

CREATE TABLE IF NOT EXISTS multiwarehouse_existences (
    id BIGINT PRIMARY KEY,
    period_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    warehouse_key VARCHAR(50),
    warehouse_name VARCHAR(255),
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255),
    stock DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(1) DEFAULT 'A',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_multiwarehouse_period FOREIGN KEY (period_id) REFERENCES periods(id) ON DELETE CASCADE,
    CONSTRAINT fk_multiwarehouse_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id_warehouse) ON DELETE CASCADE
);

-- Índices para mejorar el rendimiento
CREATE INDEX IF NOT EXISTS idx_multiwarehouse_period ON multiwarehouse_existences(period_id);
CREATE INDEX IF NOT EXISTS idx_multiwarehouse_warehouse ON multiwarehouse_existences(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_multiwarehouse_product ON multiwarehouse_existences(product_code);
CREATE INDEX IF NOT EXISTS idx_multiwarehouse_warehouse_key ON multiwarehouse_existences(warehouse_key);
CREATE INDEX IF NOT EXISTS idx_warehouse_product ON multiwarehouse_existences(warehouse_id, product_code);
CREATE INDEX IF NOT EXISTS idx_multiwarehouse_product_warehouse ON multiwarehouse_existences(product_code, warehouse_key, period_id);

-- Índice compuesto para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_multiwarehouse_period_status ON multiwarehouse_existences(period_id, status);

