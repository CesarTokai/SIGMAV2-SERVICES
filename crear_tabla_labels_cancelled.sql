-- Script para crear tabla de marbetes cancelados por falta de existencias
-- Esta tabla permite rastrear y potencialmente reactivar marbetes cancelados

CREATE TABLE IF NOT EXISTS labels_cancelled (
    id_label_cancelled BIGINT AUTO_INCREMENT PRIMARY KEY,
    folio BIGINT NOT NULL UNIQUE,
    id_label_request BIGINT NOT NULL,
    id_period BIGINT NOT NULL,
    id_warehouse BIGINT NOT NULL,
    id_product BIGINT NOT NULL,
    existencias_al_cancelar INT DEFAULT 0,
    existencias_actuales INT DEFAULT 0,
    motivo_cancelacion VARCHAR(500),
    cancelado_at DATETIME NOT NULL,
    cancelado_by BIGINT NOT NULL,
    reactivado BOOLEAN DEFAULT FALSE,
    reactivado_at DATETIME,
    reactivado_by BIGINT,
    notas TEXT,

    INDEX idx_folio (folio),
    INDEX idx_period_warehouse (id_period, id_warehouse),
    INDEX idx_product (id_product),
    INDEX idx_reactivado (reactivado),

    FOREIGN KEY (id_label_request) REFERENCES label_request(id_label_request),
    FOREIGN KEY (id_period) REFERENCES periods(id),
    FOREIGN KEY (id_warehouse) REFERENCES warehouse(id_warehouse),
    FOREIGN KEY (id_product) REFERENCES products(id_product)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comentarios de la tabla
ALTER TABLE labels_cancelled COMMENT = 'Marbetes cancelados por falta de existencias que pueden ser reactivados';

