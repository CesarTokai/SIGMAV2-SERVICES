-- Script de verificación de la sincronización entre multiwarehouse_existences e inventory_stock

-- 1. Verificar que todos los registros de multiwarehouse_existences estén en inventory_stock
SELECT
    COUNT(*) AS total_multiwarehouse
FROM multiwarehouse_existences mw
WHERE mw.period_id IS NOT NULL;

SELECT
    COUNT(*) AS total_inventory_stock
FROM inventory_stock;

-- 2. Comparar datos entre ambas tablas
SELECT
    mw.product_code,
    p.descr AS producto,
    mw.warehouse_key,
    w.name_warehouse AS almacen,
    mw.period_id,
    mw.stock AS multiwarehouse_stock,
    ist.exist_qty AS inventory_stock_qty,
    mw.status AS mw_status,
    ist.status AS ist_status,
    CASE
        WHEN mw.stock = ist.exist_qty AND mw.status = ist.status THEN 'OK'
        ELSE 'DIFERENCIA'
    END AS sincronizacion
FROM multiwarehouse_existences mw
LEFT JOIN inventory_stock ist
    ON ist.id_product = (SELECT id_product FROM products WHERE cve_art = mw.product_code)
    AND ist.id_warehouse = (SELECT id_warehouse FROM warehouse WHERE warehouse_key = mw.warehouse_key)
    AND ist.id_period = mw.period_id
LEFT JOIN products p ON p.cve_art = mw.product_code
LEFT JOIN warehouse w ON w.warehouse_key = mw.warehouse_key
WHERE mw.period_id IS NOT NULL
ORDER BY mw.period_id DESC, mw.warehouse_key, mw.product_code
LIMIT 20;

-- 3. Registros en multiwarehouse que NO están en inventory_stock
SELECT
    mw.product_code,
    mw.warehouse_key,
    mw.period_id,
    mw.stock,
    mw.status,
    'FALTANTE EN INVENTORY_STOCK' AS observacion
FROM multiwarehouse_existences mw
WHERE mw.period_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM inventory_stock ist
      WHERE ist.id_product = (SELECT id_product FROM products WHERE cve_art = mw.product_code)
        AND ist.id_warehouse = (SELECT id_warehouse FROM warehouse WHERE warehouse_key = mw.warehouse_key)
        AND ist.id_period = mw.period_id
  );

-- 4. Estadísticas por periodo
SELECT
    ist.id_period,
    COUNT(DISTINCT ist.id_product) AS total_productos,
    COUNT(DISTINCT ist.id_warehouse) AS total_almacenes,
    COUNT(*) AS total_registros,
    SUM(ist.exist_qty) AS total_existencias,
    SUM(CASE WHEN ist.status = 'A' THEN 1 ELSE 0 END) AS activos,
    SUM(CASE WHEN ist.status = 'B' THEN 1 ELSE 0 END) AS bajas
FROM inventory_stock ist
GROUP BY ist.id_period
ORDER BY ist.id_period DESC;

-- 5. Top 10 productos con más existencias
SELECT
    p.cve_art,
    p.descr,
    w.warehouse_key,
    w.name_warehouse,
    ist.exist_qty,
    ist.status
FROM inventory_stock ist
INNER JOIN products p ON p.id_product = ist.id_product
INNER JOIN warehouse w ON w.id_warehouse = ist.id_warehouse
WHERE ist.id_period = (SELECT MAX(id_period) FROM inventory_stock)
  AND ist.status = 'A'
ORDER BY ist.exist_qty DESC
LIMIT 10;

