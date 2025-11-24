-- Script para verificar datos de inventario

-- Ver periodos
SELECT * FROM periods ORDER BY id;

-- Ver productos
SELECT * FROM products ORDER BY id_product LIMIT 10;

-- Ver snapshots de inventario
SELECT * FROM inventory_snapshot ORDER BY id LIMIT 10;

-- Contar registros por tabla
SELECT 'periods' as tabla, COUNT(*) as total FROM periods
UNION ALL
SELECT 'products' as tabla, COUNT(*) as total FROM products
UNION ALL
SELECT 'inventory_snapshot' as tabla, COUNT(*) as total FROM inventory_snapshot;

-- Ver snapshots con datos de productos para periodo 1
SELECT
    s.id,
    s.product_id,
    s.period_id,
    s.warehouse_id,
    s.exist_qty,
    s.status,
    p.cve_art,
    p.descr,
    p.uni_med
FROM inventory_snapshot s
LEFT JOIN products p ON s.product_id = p.id_product
WHERE s.period_id = 1
LIMIT 10;

