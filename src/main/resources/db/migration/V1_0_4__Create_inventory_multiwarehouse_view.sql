CREATE OR REPLACE VIEW v_inventory_multiwarehouse AS
SELECT
    s.id,
    s.product_id,
    p.name as product_name,
    s.warehouse_id,
    w.name as warehouse_name,
    s.quantity,
    s.unit
FROM
    stock s
    INNER JOIN products p ON s.product_id = p.id
    INNER JOIN warehouses w ON s.warehouse_id = w.id
WHERE
    w.active = true;

