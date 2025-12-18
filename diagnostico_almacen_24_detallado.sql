-- =====================================================
-- DIAGNÓSTICO: Marbetes sin existencias - Almacén 24
-- =====================================================
-- Fecha: 2025-12-18
-- Propósito: Diagnosticar por qué no aparecen marbetes
--           pendientes de impresión en Almacén 24
-- =====================================================

-- 1. VERIFICAR TODOS LOS MARBETES DEL ALMACÉN 24, PERIODO 20
-- ============================================================
SELECT
    '1. MARBETES GENERADOS - ALMACÉN 24' as seccion;

SELECT
    l.folio,
    l.estado,
    l.product_id,
    p.cve_art as clave_producto,
    p.descr as nombre_producto,
    l.warehouse_id,
    w.warehouse_key as clave_almacen,
    w.name_warehouse as nombre_almacen,
    COALESCE(s.exist_qty, 0) as existencias_teoricas,
    l.created_at as fecha_generacion,
    lp.printed_at as fecha_impresion
FROM labels l
LEFT JOIN product p ON l.product_id = p.id_product
LEFT JOIN warehouse w ON l.warehouse_id = w.id_warehouse
LEFT JOIN inventory_stock s ON s.product_id = l.product_id
    AND s.warehouse_id = l.warehouse_id
    AND s.period_id = l.period_id
LEFT JOIN label_prints lp ON lp.folio_inicial <= l.folio
    AND lp.folio_final >= l.folio
    AND lp.period_id = l.period_id
WHERE l.warehouse_id = 420  -- Almacén 24
  AND l.period_id = 20
ORDER BY l.folio;

-- 2. RESUMEN POR ESTADO
-- ======================
SELECT
    '2. RESUMEN POR ESTADO' as seccion;

SELECT
    estado,
    COUNT(*) as total_marbetes,
    MIN(folio) as folio_inicial,
    MAX(folio) as folio_final
FROM labels
WHERE warehouse_id = 420
  AND period_id = 20
GROUP BY estado;

-- 3. MARBETES PENDIENTES DE IMPRESIÓN (Estado GENERADO)
-- ======================================================
SELECT
    '3. MARBETES PENDIENTES (GENERADO)' as seccion;

SELECT
    l.folio,
    p.cve_art,
    p.descr,
    COALESCE(s.exist_qty, 0) as existencias
FROM labels l
LEFT JOIN product p ON l.product_id = p.id_product
LEFT JOIN inventory_stock s ON s.product_id = l.product_id
    AND s.warehouse_id = l.warehouse_id
    AND s.period_id = l.period_id
WHERE l.warehouse_id = 420
  AND l.period_id = 20
  AND l.estado = 'GENERADO'
ORDER BY l.folio;

-- 4. MARBETES YA IMPRESOS
-- ========================
SELECT
    '4. MARBETES YA IMPRESOS' as seccion;

SELECT
    l.folio,
    p.cve_art,
    p.descr,
    COALESCE(s.exist_qty, 0) as existencias,
    lp.printed_at as fecha_impresion,
    lp.printed_by as impreso_por
FROM labels l
LEFT JOIN product p ON l.product_id = p.id_product
LEFT JOIN inventory_stock s ON s.product_id = l.product_id
    AND s.warehouse_id = l.warehouse_id
    AND s.period_id = l.period_id
LEFT JOIN label_prints lp ON lp.folio_inicial <= l.folio
    AND lp.folio_final >= l.folio
    AND lp.period_id = l.period_id
WHERE l.warehouse_id = 420
  AND l.period_id = 20
  AND l.estado = 'IMPRESO'
ORDER BY l.folio;

-- 5. MARBETES CANCELADOS
-- =======================
SELECT
    '5. MARBETES CANCELADOS' as seccion;

SELECT
    l.folio,
    p.cve_art,
    p.descr,
    lc.cancelled_at as fecha_cancelacion,
    lc.cancelled_by as cancelado_por,
    lc.reason as motivo
FROM labels l
LEFT JOIN product p ON l.product_id = p.id_product
LEFT JOIN labels_cancelled lc ON lc.folio = l.folio
    AND lc.period_id = l.period_id
WHERE l.warehouse_id = 420
  AND l.period_id = 20
  AND l.estado = 'CANCELADO'
ORDER BY l.folio;

-- 6. VERIFICAR EXISTENCIAS DE PRODUCTOS EN ALMACÉN 24
-- ====================================================
SELECT
    '6. EXISTENCIAS DE PRODUCTOS - ALMACÉN 24' as seccion;

SELECT
    p.id_product,
    p.cve_art,
    p.descr,
    s.exist_qty as existencias,
    s.period_id,
    per.start_date as periodo_inicio
FROM inventory_stock s
INNER JOIN product p ON s.product_id = p.id_product
LEFT JOIN periods per ON s.period_id = per.id_period
WHERE s.warehouse_id = 420
  AND s.period_id = 20
ORDER BY s.exist_qty DESC, p.cve_art;

-- 7. PRODUCTOS SIN EXISTENCIAS CON MARBETES GENERADOS
-- ====================================================
SELECT
    '7. PRODUCTOS SIN EXISTENCIAS CON MARBETES' as seccion;

SELECT
    l.folio,
    l.estado,
    p.cve_art,
    p.descr,
    COALESCE(s.exist_qty, 0) as existencias,
    COUNT(l.folio) OVER (PARTITION BY l.product_id) as total_marbetes_producto
FROM labels l
INNER JOIN product p ON l.product_id = p.id_product
LEFT JOIN inventory_stock s ON s.product_id = l.product_id
    AND s.warehouse_id = l.warehouse_id
    AND s.period_id = l.period_id
WHERE l.warehouse_id = 420
  AND l.period_id = 20
  AND COALESCE(s.exist_qty, 0) = 0
ORDER BY l.folio;

-- 8. COMPARATIVA CON ALMACÉN 1 (que sí funciona)
-- ===============================================
SELECT
    '8. COMPARATIVA ALMACÉN 1 vs ALMACÉN 24' as seccion;

SELECT
    w.warehouse_key as almacen,
    w.name_warehouse as nombre,
    l.estado,
    COUNT(*) as total_marbetes,
    COUNT(DISTINCT l.product_id) as productos_distintos,
    SUM(CASE WHEN COALESCE(s.exist_qty, 0) = 0 THEN 1 ELSE 0 END) as marbetes_sin_existencias,
    SUM(CASE WHEN COALESCE(s.exist_qty, 0) > 0 THEN 1 ELSE 0 END) as marbetes_con_existencias
FROM labels l
INNER JOIN warehouse w ON l.warehouse_id = w.id_warehouse
LEFT JOIN inventory_stock s ON s.product_id = l.product_id
    AND s.warehouse_id = l.warehouse_id
    AND s.period_id = l.period_id
WHERE l.warehouse_id IN (415, 420)  -- Almacén 1 y Almacén 24
  AND l.period_id = 20
GROUP BY w.warehouse_key, w.name_warehouse, l.estado
ORDER BY almacen, l.estado;

-- 9. HISTORIAL DE IMPRESIONES
-- ============================
SELECT
    '9. HISTORIAL DE IMPRESIONES' as seccion;

SELECT
    lp.id_label_print,
    lp.period_id,
    lp.warehouse_id,
    w.name_warehouse,
    lp.folio_inicial,
    lp.folio_final,
    lp.cantidad_impresa,
    lp.printed_at,
    lp.printed_by,
    u.username as usuario
FROM label_prints lp
INNER JOIN warehouse w ON lp.warehouse_id = w.id_warehouse
LEFT JOIN user u ON lp.printed_by = u.id_user
WHERE lp.warehouse_id = 420
  AND lp.period_id = 20
ORDER BY lp.printed_at DESC;

-- 10. DIAGNÓSTICO FINAL: ¿POR QUÉ NO HAY PENDIENTES?
-- ===================================================
SELECT
    '10. DIAGNÓSTICO FINAL' as seccion;

SELECT
    CASE
        WHEN COUNT(*) = 0 THEN 'NO EXISTEN MARBETES GENERADOS'
        WHEN SUM(CASE WHEN estado = 'GENERADO' THEN 1 ELSE 0 END) = 0
            THEN 'TODOS LOS MARBETES YA FUERON IMPRESOS O CANCELADOS'
        WHEN SUM(CASE WHEN estado = 'GENERADO' THEN 1 ELSE 0 END) > 0
            THEN CONCAT('HAY ', SUM(CASE WHEN estado = 'GENERADO' THEN 1 ELSE 0 END), ' MARBETES PENDIENTES')
        ELSE 'ESTADO DESCONOCIDO'
    END as diagnostico,
    COUNT(*) as total_marbetes,
    SUM(CASE WHEN estado = 'GENERADO' THEN 1 ELSE 0 END) as pendientes,
    SUM(CASE WHEN estado = 'IMPRESO' THEN 1 ELSE 0 END) as impresos,
    SUM(CASE WHEN estado = 'CANCELADO' THEN 1 ELSE 0 END) as cancelados
FROM labels
WHERE warehouse_id = 420
  AND period_id = 20;

-- =====================================================
-- INSTRUCCIONES DE USO:
-- =====================================================
-- 1. Ejecutar este script completo en MySQL Workbench
-- 2. Revisar cada sección numerada
-- 3. La sección 10 da el diagnóstico final
--
-- POSIBLES RESULTADOS:
-- - "NO EXISTEN MARBETES GENERADOS"
--   → Generar marbetes primero
--
-- - "TODOS LOS MARBETES YA FUERON IMPRESOS O CANCELADOS"
--   → Usar forceReprint=true para reimprimir
--
-- - "HAY X MARBETES PENDIENTES"
--   → Verificar por qué la API no los retorna
-- =====================================================

