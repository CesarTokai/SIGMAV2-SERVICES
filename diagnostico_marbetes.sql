-- Script de diagnóstico para verificar marbetes generados
-- Ejecutar este script en MySQL/MariaDB para diagnosticar el problema

-- ====================================
-- 1. VERIFICAR SOLICITUDES DE FOLIOS
-- ====================================
SELECT
    'SOLICITUDES DE FOLIOS' as tipo,
    lr.id_label_request,
    lr.id_product,
    p.cve_art AS clave_producto,
    p.descr AS nombre_producto,
    lr.id_warehouse,
    w.warehouse_key AS clave_almacen,
    lr.id_period,
    lr.requested_labels AS solicitados,
    lr.folios_generados AS generados,
    lr.created_at AS fecha_solicitud
FROM label_request lr
LEFT JOIN products p ON lr.id_product = p.id_product
LEFT JOIN warehouse w ON lr.id_warehouse = w.id_warehouse
ORDER BY lr.created_at DESC;

-- ====================================
-- 2. VERIFICAR MARBETES GENERADOS
-- ====================================
SELECT
    'MARBETES GENERADOS' as tipo,
    COUNT(*) AS total_marbetes,
    l.id_period,
    l.id_warehouse,
    l.id_product,
    p.cve_art AS clave_producto,
    p.descr AS nombre_producto,
    l.estado,
    MIN(l.folio) AS primer_folio,
    MAX(l.folio) AS ultimo_folio
FROM labels l
LEFT JOIN products p ON l.id_product = p.id_product
GROUP BY l.id_period, l.id_warehouse, l.id_product, p.cve_art, p.descr, l.estado
ORDER BY l.id_period, l.id_warehouse, l.id_product;

-- ====================================
-- 3. VERIFICAR LOTES DE GENERACIÓN
-- ====================================
SELECT
    'LOTES DE GENERACIÓN' as tipo,
    lgb.id_generation_batch,
    lgb.id_label_request,
    lgb.id_period,
    lgb.id_warehouse,
    lgb.primer_folio,
    lgb.ultimo_folio,
    lgb.total_generados,
    lgb.generado_at AS fecha_generacion,
    lgb.generado_por AS usuario_id
FROM label_generation_batch lgb
ORDER BY lgb.generado_at DESC;

-- ====================================
-- 4. VERIFICAR SECUENCIA DE FOLIOS
-- ====================================
SELECT
    'SECUENCIA DE FOLIOS' as tipo,
    lfs.id_period,
    lfs.ultimo_folio
FROM label_folio_sequence lfs
ORDER BY lfs.id_period;

-- ====================================
-- 5. VERIFICAR INCONSISTENCIAS
-- ====================================
-- Solicitudes con folios generados pero sin marbetes en la tabla labels
SELECT
    'INCONSISTENCIA: Solicitud sin marbetes' as problema,
    lr.id_label_request,
    lr.id_product,
    p.cve_art AS clave_producto,
    lr.id_warehouse,
    lr.id_period,
    lr.requested_labels AS solicitados,
    lr.folios_generados AS reportados_generados,
    COALESCE(COUNT(l.folio), 0) AS marbetes_reales
FROM label_request lr
LEFT JOIN products p ON lr.id_product = p.id_product
LEFT JOIN labels l ON l.id_label_request = lr.id_label_request
WHERE lr.folios_generados > 0
GROUP BY lr.id_label_request, lr.id_product, p.cve_art, lr.id_warehouse, lr.id_period, lr.requested_labels, lr.folios_generados
HAVING marbetes_reales != lr.folios_generados;

-- ====================================
-- 6. CONTEO GENERAL POR PERIODO Y ALMACÉN
-- ====================================
SELECT
    'CONTEO POR PERIODO Y ALMACÉN' as tipo,
    l.id_period,
    l.id_warehouse,
    w.warehouse_key AS clave_almacen,
    w.name_warehouse AS nombre_almacen,
    COUNT(*) AS total_marbetes,
    COUNT(CASE WHEN l.estado = 'GENERADO' THEN 1 END) AS generados,
    COUNT(CASE WHEN l.estado = 'IMPRESO' THEN 1 END) AS impresos,
    COUNT(CASE WHEN l.estado = 'CANCELADO' THEN 1 END) AS cancelados
FROM labels l
LEFT JOIN warehouse w ON l.id_warehouse = w.id_warehouse
GROUP BY l.id_period, l.id_warehouse, w.warehouse_key, w.name_warehouse
ORDER BY l.id_period, l.id_warehouse;

-- ====================================
-- 7. ÚLTIMOS 20 MARBETES GENERADOS
-- ====================================
SELECT
    'ÚLTIMOS MARBETES' as tipo,
    l.folio,
    l.id_period,
    l.id_warehouse,
    l.id_product,
    p.cve_art AS clave_producto,
    p.descr AS nombre_producto,
    l.estado,
    l.created_at AS fecha_creacion,
    l.impreso_at AS fecha_impresion
FROM labels l
LEFT JOIN products p ON l.id_product = p.id_product
ORDER BY l.created_at DESC
LIMIT 20;

-- ====================================
-- 8. RESUMEN EJECUTIVO
-- ====================================
SELECT
    'RESUMEN EJECUTIVO' as reporte,
    (SELECT COUNT(*) FROM label_request) AS total_solicitudes,
    (SELECT SUM(requested_labels) FROM label_request) AS folios_solicitados_total,
    (SELECT SUM(folios_generados) FROM label_request) AS folios_reportados_total,
    (SELECT COUNT(*) FROM labels) AS marbetes_reales_total,
    (SELECT COUNT(*) FROM label_generation_batch) AS lotes_generacion_total;

