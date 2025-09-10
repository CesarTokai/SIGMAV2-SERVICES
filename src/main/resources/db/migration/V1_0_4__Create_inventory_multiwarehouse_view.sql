CREATE VIEW v_inventory_multiwarehouse AS
SELECT
    p.id as product_id,
    p.name as product_name,
    p.sku,
    w.id as warehouse_id,
    w.name as warehouse_name,
    COALESCE(s.quantity, 0) as quantity,
    s.minimum_stock,
    s.maximum_stock
FROM products p
CROSS JOIN warehouses w
LEFT JOIN stock s ON s.product_id = p.id AND s.warehouse_id = w.id
WHERE p.deleted_at IS NULL AND w.deleted_at IS NULL;

