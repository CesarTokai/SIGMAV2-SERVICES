-- ============================================
-- VERIFICAR Y CORREGIR FOLIOS SALTADOS
-- ============================================

-- PASO 1: DIAGNOSTICAR EL PROBLEMA
-- ============================================

-- 1.1 Ver productos con 0 folios solicitados que SÍ existen en inventario
SELECT
    p.product_key AS clave_producto,
    p.description AS producto,
    COALESCE(inv.exist_qty, 0) AS existencias_sistema,
    COALESCE(lr.requested_labels, 0) AS folios_solicitados,
    COALESCE(lr.folios_generados, 0) AS folios_generados,
    CASE
        WHEN lr.requested_labels = 0 THEN '❌ PROBLEMA: 0 folios solicitados'
        WHEN lr.requested_labels IS NULL THEN '⚠️ Sin solicitud'
        ELSE '✓ OK'
    END AS estado
FROM products p
INNER JOIN inventory_stock inv ON inv.product_id = p.id_product
LEFT JOIN label_requests lr ON lr.product_id = p.id_product
    AND lr.period_id = inv.period_id
    AND lr.warehouse_id = inv.warehouse_id
WHERE inv.period_id = (SELECT MAX(id_period) FROM periods)
  AND inv.warehouse_id = 1
  AND (lr.requested_labels = 0 OR lr.requested_labels IS NULL)
ORDER BY p.product_key;

-- 1.2 Contar cuántos productos tienen este problema
SELECT
    COUNT(*) AS productos_con_problema,
    SUM(CASE WHEN lr.requested_labels = 0 THEN 1 ELSE 0 END) AS con_cero_folios,
    SUM(CASE WHEN lr.requested_labels IS NULL THEN 1 ELSE 0 END) AS sin_solicitud
FROM products p
INNER JOIN inventory_stock inv ON inv.product_id = p.id_product
LEFT JOIN label_requests lr ON lr.product_id = p.id_product
    AND lr.period_id = inv.period_id
    AND lr.warehouse_id = inv.warehouse_id
WHERE inv.period_id = (SELECT MAX(id_period) FROM periods)
  AND inv.warehouse_id = 1
  AND (lr.requested_labels = 0 OR lr.requested_labels IS NULL);

-- 1.3 Ver la secuencia actual de folios y detectar huecos
SELECT
    l.folio,
    p.product_key,
    COALESCE(inv.exist_qty, 0) AS existencias,
    l.estado,
    CASE
        WHEN LAG(l.folio) OVER (ORDER BY l.folio) IS NULL THEN NULL
        WHEN l.folio - LAG(l.folio) OVER (ORDER BY l.folio) > 1 THEN
            CONCAT('⚠️ HUECO: Faltan ', l.folio - LAG(l.folio) OVER (ORDER BY l.folio) - 1, ' folios')
        ELSE '✓'
    END AS continuidad
FROM labels l
INNER JOIN products p ON l.product_id = p.id_product
LEFT JOIN inventory_stock inv ON inv.product_id = p.id_product
    AND inv.warehouse_id = l.warehouse_id
    AND inv.period_id = l.period_id
WHERE l.period_id = (SELECT MAX(id_period) FROM periods)
  AND l.warehouse_id = 1
ORDER BY l.folio;


-- PASO 2: SOLUCIÓN TEMPORAL
-- ============================================
-- ADVERTENCIA: Ejecutar SOLO si confirmaste el problema en PASO 1
-- ============================================

-- 2.1 Opción A: Establecer 1 folio para productos con requested_labels = 0
-- (Solo para productos que SÍ existen en inventario)
/*
UPDATE label_requests lr
SET requested_labels = 1
WHERE requested_labels = 0
  AND period_id = (SELECT MAX(id_period) FROM periods)
  AND warehouse_id = 1
  AND EXISTS (
      SELECT 1 FROM inventory_stock inv
      WHERE inv.product_id = lr.product_id
        AND inv.warehouse_id = lr.warehouse_id
        AND inv.period_id = lr.period_id
  );
*/

-- 2.2 Opción B: Crear solicitudes faltantes para productos sin solicitud
-- (Para productos que existen en inventario pero NO tienen solicitud)
/*
INSERT INTO label_requests (
    product_id,
    warehouse_id,
    period_id,
    requested_labels,
    folios_generados,
    created_by,
    created_at
)
SELECT
    inv.product_id,
    inv.warehouse_id,
    inv.period_id,
    1,  -- Solicitar 1 folio por defecto
    0,  -- Aún no se han generado
    1,  -- Usuario sistema (ajustar según corresponda)
    NOW()
FROM inventory_stock inv
WHERE inv.period_id = (SELECT MAX(id_period) FROM periods)
  AND inv.warehouse_id = 1
  AND NOT EXISTS (
      SELECT 1 FROM label_requests lr
      WHERE lr.product_id = inv.product_id
        AND lr.warehouse_id = inv.warehouse_id
        AND lr.period_id = inv.period_id
  );
*/


-- PASO 3: VERIFICACIÓN POST-CORRECCIÓN
-- ============================================

-- 3.1 Verificar que ya no haya productos con 0 folios solicitados
SELECT
    COUNT(*) AS productos_pendientes
FROM products p
INNER JOIN inventory_stock inv ON inv.product_id = p.id_product
LEFT JOIN label_requests lr ON lr.product_id = p.id_product
    AND lr.period_id = inv.period_id
    AND lr.warehouse_id = inv.warehouse_id
WHERE inv.period_id = (SELECT MAX(id_period) FROM periods)
  AND inv.warehouse_id = 1
  AND (lr.requested_labels = 0 OR lr.requested_labels IS NULL);

-- 3.2 Ver resumen de solicitudes corregidas
SELECT
    p.product_key,
    p.description,
    COALESCE(inv.exist_qty, 0) AS existencias,
    lr.requested_labels AS folios_solicitados,
    lr.folios_generados,
    lr.requested_labels - lr.folios_generados AS pendientes_generar
FROM products p
INNER JOIN inventory_stock inv ON inv.product_id = p.id_product
INNER JOIN label_requests lr ON lr.product_id = p.id_product
    AND lr.period_id = inv.period_id
    AND lr.warehouse_id = inv.warehouse_id
WHERE inv.period_id = (SELECT MAX(id_period) FROM periods)
  AND inv.warehouse_id = 1
  AND lr.requested_labels > lr.folios_generados  -- Tienen folios pendientes de generar
ORDER BY p.product_key;


-- PASO 4: GENERAR FOLIOS FALTANTES
-- ============================================
-- NOTA: Después de corregir las solicitudes, debes usar la API para generar
-- los folios faltantes:
--
-- POST http://localhost:8080/api/sigmav2/labels/generate
-- {
--   "productId": <ID_DEL_PRODUCTO>,
--   "warehouseId": 1,
--   "periodId": <ID_DEL_PERIODO>,
--   "labelsToGenerate": <CANTIDAD_PENDIENTE>
-- }
--
-- O usar el endpoint batch si hay muchos productos


-- PASO 5: VERIFICACIÓN FINAL
-- ============================================

-- 5.1 Verificar que no haya huecos en la secuencia de folios
WITH folio_gaps AS (
    SELECT
        folio,
        LEAD(folio) OVER (ORDER BY folio) AS next_folio
    FROM labels
    WHERE period_id = (SELECT MAX(id_period) FROM periods)
      AND warehouse_id = 1
)
SELECT
    folio AS ultimo_antes_hueco,
    next_folio AS primero_despues_hueco,
    next_folio - folio - 1 AS cantidad_folios_faltantes
FROM folio_gaps
WHERE next_folio - folio > 1
ORDER BY folio;

-- Si esta query NO devuelve resultados, la secuencia es continua ✓

-- 5.2 Resumen final
SELECT
    COUNT(DISTINCT p.id_product) AS total_productos_inventario,
    COUNT(DISTINCT CASE WHEN lr.id_label_request IS NOT NULL THEN p.id_product END) AS con_solicitud,
    COUNT(DISTINCT CASE WHEN lr.requested_labels > 0 THEN p.id_product END) AS con_folios_solicitados,
    COUNT(DISTINCT CASE WHEN lr.folios_generados > 0 THEN p.id_product END) AS con_folios_generados,
    COUNT(DISTINCT l.folio) AS total_folios_generados
FROM products p
INNER JOIN inventory_stock inv ON inv.product_id = p.id_product
LEFT JOIN label_requests lr ON lr.product_id = p.id_product
    AND lr.period_id = inv.period_id
    AND lr.warehouse_id = inv.warehouse_id
LEFT JOIN labels l ON l.product_id = p.id_product
    AND l.period_id = inv.period_id
    AND l.warehouse_id = inv.warehouse_id
WHERE inv.period_id = (SELECT MAX(id_period) FROM periods)
  AND inv.warehouse_id = 1;

