-- ============================================
-- DIAGNÓSTICO COMPLETO DE FOLIOS Y MARBETES
-- ============================================
-- Este script te ayudará a entender por qué parece que hay "saltos" en los folios

-- 1. Ver TODOS los marbetes generados ordenados por folio
SELECT
    l.folio,
    p.product_key AS clave_producto,
    p.description AS producto,
    w.warehouse_key AS clave_almacen,
    w.name_warehouse AS almacen,
    COALESCE(inv.exist_qty, 0) AS existencias,
    l.estado,
    CASE WHEN lp.id_label_print IS NOT NULL THEN 'Sí' ELSE 'No' END AS impreso,
    lp.printed_at AS fecha_impresion
FROM labels l
INNER JOIN products p ON l.product_id = p.id_product
INNER JOIN warehouses w ON l.warehouse_id = w.id_warehouse
LEFT JOIN inventory_stock inv ON inv.product_id = p.id_product
    AND inv.warehouse_id = w.id_warehouse
    AND inv.period_id = l.period_id
LEFT JOIN labels_print lp ON lp.folio = l.folio
    AND lp.period_id = l.period_id
    AND lp.warehouse_id = l.warehouse_id
WHERE l.period_id = (SELECT MAX(id_period) FROM periods)
ORDER BY l.folio;

-- 2. Verificar si hay "huecos" en la secuencia de folios
-- Si esta consulta devuelve resultados, hay folios faltantes
WITH RECURSIVE folio_sequence AS (
    SELECT MIN(folio) AS expected_folio, MAX(folio) AS max_folio
    FROM labels
    WHERE period_id = (SELECT MAX(id_period) FROM periods)

    UNION ALL

    SELECT expected_folio + 1, max_folio
    FROM folio_sequence
    WHERE expected_folio < max_folio
)
SELECT fs.expected_folio AS folio_faltante
FROM folio_sequence fs
LEFT JOIN labels l ON l.folio = fs.expected_folio
    AND l.period_id = (SELECT MAX(id_period) FROM periods)
WHERE l.folio IS NULL
AND fs.expected_folio IS NOT NULL
ORDER BY fs.expected_folio
LIMIT 50;

-- 3. Ver el resumen por producto (esto es lo que muestra el frontend)
SELECT
    p.product_key AS clave_producto,
    p.description AS producto,
    COALESCE(inv.exist_qty, 0) AS existencias,
    lr.requested_labels AS folios_solicitados,
    COUNT(l.folio) AS folios_generados,
    MIN(l.folio) AS primer_folio,
    MAX(l.folio) AS ultimo_folio,
    CASE
        WHEN COUNT(l.folio) > 0 THEN CONCAT(MIN(l.folio), ' - ', MAX(l.folio))
        ELSE '-'
    END AS rango_folios,
    CASE WHEN COUNT(lp.id_label_print) > 0 THEN 'Sí' ELSE 'No' END AS impreso
FROM products p
LEFT JOIN label_requests lr ON lr.product_id = p.id_product
    AND lr.period_id = (SELECT MAX(id_period) FROM periods)
    AND lr.warehouse_id = 1
LEFT JOIN labels l ON l.product_id = p.id_product
    AND l.period_id = (SELECT MAX(id_period) FROM periods)
    AND l.warehouse_id = 1
LEFT JOIN inventory_stock inv ON inv.product_id = p.id_product
    AND inv.warehouse_id = 1
    AND inv.period_id = (SELECT MAX(id_period) FROM periods)
LEFT JOIN labels_print lp ON lp.product_id = p.id_product
    AND lp.period_id = (SELECT MAX(id_period) FROM periods)
    AND lp.warehouse_id = 1
WHERE lr.id_label_request IS NOT NULL
   OR l.id_label IS NOT NULL
   OR inv.id_inventory_stock IS NOT NULL
GROUP BY p.id_product, p.product_key, p.description, inv.exist_qty, lr.requested_labels
ORDER BY MIN(l.folio) NULLS LAST;

-- 4. Verificar la secuencia de folios
SELECT
    period_id,
    ultimo_folio AS ultimo_folio_asignado
FROM labels_folio_sequence
WHERE period_id = (SELECT MAX(id_period) FROM periods);

-- 5. Contar marbetes por estado
SELECT
    estado,
    COUNT(*) AS cantidad
FROM labels
WHERE period_id = (SELECT MAX(id_period) FROM periods)
  AND warehouse_id = 1
GROUP BY estado;

-- 6. Ver productos con existencias = 0 que tienen marbetes generados
SELECT
    l.folio,
    p.product_key,
    p.description,
    COALESCE(inv.exist_qty, 0) AS existencias,
    l.estado,
    CASE WHEN lp.id_label_print IS NOT NULL THEN 'Sí' ELSE 'No' END AS impreso
FROM labels l
INNER JOIN products p ON l.product_id = p.id_product
LEFT JOIN inventory_stock inv ON inv.product_id = p.id_product
    AND inv.warehouse_id = l.warehouse_id
    AND inv.period_id = l.period_id
LEFT JOIN labels_print lp ON lp.folio = l.folio
    AND lp.period_id = l.period_id
    AND lp.warehouse_id = l.warehouse_id
WHERE l.period_id = (SELECT MAX(id_period) FROM periods)
  AND l.warehouse_id = 1
  AND COALESCE(inv.exist_qty, 0) = 0
ORDER BY l.folio;

