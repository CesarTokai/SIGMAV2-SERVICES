-- Migración para poblar inventory_stock desde multiwarehouse_existences
-- Esta migración sincroniza los datos existentes en multiwarehouse_existences hacia inventory_stock

-- Insertar datos desde multiwarehouse_existences a inventory_stock
INSERT INTO inventory_stock (id_product, id_warehouse, id_period, exist_qty, status, created_at, updated_at)
SELECT
    p.id_product,
    w.id_warehouse,
    mw.period_id,
    COALESCE(mw.stock, 0.00) AS exist_qty,
    COALESCE(mw.status, 'A') AS status,
    NOW() AS created_at,
    NOW() AS updated_at
FROM multiwarehouse_existences mw
INNER JOIN products p ON p.cve_art = mw.product_code
INNER JOIN warehouse w ON w.warehouse_key = mw.warehouse_key
WHERE mw.period_id IS NOT NULL
  AND p.id_product IS NOT NULL
  AND w.id_warehouse IS NOT NULL
ON DUPLICATE KEY UPDATE
    exist_qty = VALUES(exist_qty),
    status = VALUES(status),
    updated_at = NOW();

