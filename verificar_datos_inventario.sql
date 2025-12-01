-- ============================================
-- Script de Verificación de Datos
-- Base de datos: SIGMAV2_2
-- Fecha: 2025-11-28
-- ============================================

-- ============================================
-- 1. VERIFICAR PERIODOS
-- ============================================
SELECT 'VERIFICANDO PERIODOS' as paso;
SELECT
    id_period,
    period as fecha_periodo,
    state as estado,
    comments as comentarios
FROM period
ORDER BY period DESC;

-- Contar periodos
SELECT COUNT(*) as total_periodos FROM period;

-- ============================================
-- 2. VERIFICAR ALMACENES
-- ============================================
SELECT 'VERIFICANDO ALMACENES' as paso;
SELECT
    id_warehouse,
    warehouse_key as clave,
    name_warehouse as nombre,
    observations
FROM warehouse
ORDER BY id_warehouse;

-- Contar almacenes
SELECT COUNT(*) as total_almacenes FROM warehouse;

-- ============================================
-- 3. VERIFICAR PRODUCTOS
-- ============================================
SELECT 'VERIFICANDO PRODUCTOS' as paso;
SELECT
    id_product,
    cve_art as clave,
    descr as descripcion,
    uni_med as unidad,
    status as estado
FROM products
LIMIT 10;

-- Contar productos
SELECT COUNT(*) as total_productos FROM products;

-- ============================================
-- 4. VERIFICAR EXISTENCIAS EN INVENTARIO
-- ============================================
SELECT 'VERIFICANDO EXISTENCIAS EN INVENTARIO' as paso;
SELECT
    ist.id_stock,
    w.warehouse_key as almacen,
    p.cve_art as clave_producto,
    p.descr as producto,
    ist.exist_qty as existencias,
    ist.status as estado,
    ist.updated_at as actualizado
FROM inventory_stock ist
INNER JOIN warehouse w ON ist.id_warehouse = w.id_warehouse
INNER JOIN products p ON ist.id_product = p.id_product
ORDER BY w.warehouse_key, p.cve_art
LIMIT 20;

-- Contar existencias por almacén
SELECT
    w.warehouse_key as almacen,
    w.name_warehouse as nombre_almacen,
    COUNT(*) as productos_con_existencias
FROM inventory_stock ist
INNER JOIN warehouse w ON ist.id_warehouse = w.id_warehouse
GROUP BY w.id_warehouse, w.warehouse_key, w.name_warehouse
ORDER BY w.warehouse_key;

-- ============================================
-- 5. VERIFICAR SOLICITUDES DE MARBETES
-- ============================================
SELECT 'VERIFICANDO SOLICITUDES DE MARBETES' as paso;
SELECT
    lr.id_label_request,
    p.cve_art as producto,
    w.warehouse_key as almacen,
    per.period as periodo,
    lr.requested_labels as solicitados,
    lr.folios_generados as generados,
    lr.created_at as fecha_solicitud
FROM label_requests lr
INNER JOIN products p ON lr.id_product = p.id_product
INNER JOIN warehouse w ON lr.id_warehouse = w.id_warehouse
INNER JOIN period per ON lr.id_period = per.id_period
ORDER BY lr.created_at DESC
LIMIT 10;

-- Contar solicitudes
SELECT COUNT(*) as total_solicitudes FROM label_requests;

-- ============================================
-- 6. VERIFICAR MARBETES GENERADOS
-- ============================================
SELECT 'VERIFICANDO MARBETES GENERADOS' as paso;
SELECT
    l.folio,
    p.cve_art as producto,
    w.warehouse_key as almacen,
    per.period as periodo,
    l.estado,
    l.created_at as fecha_creacion,
    l.impreso_at as fecha_impresion
FROM labels l
INNER JOIN products p ON l.id_product = p.id_product
INNER JOIN warehouse w ON l.id_warehouse = w.id_warehouse
INNER JOIN period per ON l.id_period = per.id_period
ORDER BY l.folio DESC
LIMIT 10;

-- Contar marbetes por estado
SELECT
    estado,
    COUNT(*) as cantidad
FROM labels
GROUP BY estado;

-- ============================================
-- 7. RESUMEN GENERAL
-- ============================================
SELECT 'RESUMEN GENERAL' as paso;
SELECT
    (SELECT COUNT(*) FROM period) as total_periodos,
    (SELECT COUNT(*) FROM warehouse) as total_almacenes,
    (SELECT COUNT(*) FROM products) as total_productos,
    (SELECT COUNT(*) FROM inventory_stock) as total_existencias,
    (SELECT COUNT(*) FROM label_requests) as total_solicitudes,
    (SELECT COUNT(*) FROM labels) as total_marbetes;

-- ============================================
-- 8. VERIFICAR PRIMER ALMACÉN Y ÚLTIMO PERIODO
-- (Los que usa el sistema por defecto)
-- ============================================
SELECT 'VERIFICANDO DEFAULTS DEL SISTEMA' as paso;

-- Primer almacén (el que usará el sistema por defecto)
SELECT
    'ALMACEN_DEFAULT' as tipo,
    id_warehouse as id,
    warehouse_key as clave,
    name_warehouse as nombre
FROM warehouse
ORDER BY id_warehouse ASC
LIMIT 1;

-- Último periodo (el que usará el sistema por defecto)
SELECT
    'PERIODO_DEFAULT' as tipo,
    id_period as id,
    period as fecha,
    state as estado
FROM period
ORDER BY period DESC
LIMIT 1;

-- ============================================
-- 9. CONSULTA COMPLETA SIMULANDO EL ENDPOINT
-- ============================================
SELECT 'SIMULANDO CONSULTA DEL ENDPOINT' as paso;

-- Productos del primer almacén con toda su información
SELECT
    p.id_product,
    p.cve_art as clave_producto,
    p.descr as nombre_producto,
    w.warehouse_key as clave_almacen,
    w.name_warehouse as nombre_almacen,
    COALESCE(lr.requested_labels, 0) as folios_solicitados,
    COALESCE(lcount.total, 0) as folios_existentes,
    ist.status as estado,
    ist.exist_qty as existencias
FROM inventory_stock ist
INNER JOIN products p ON ist.id_product = p.id_product
INNER JOIN warehouse w ON ist.id_warehouse = w.id_warehouse
LEFT JOIN label_requests lr ON lr.id_product = p.id_product
    AND lr.id_warehouse = w.id_warehouse
    AND lr.id_period = (SELECT id_period FROM period ORDER BY period DESC LIMIT 1)
LEFT JOIN (
    SELECT id_product, id_warehouse, id_period, COUNT(*) as total
    FROM labels
    GROUP BY id_product, id_warehouse, id_period
) lcount ON lcount.id_product = p.id_product
    AND lcount.id_warehouse = w.id_warehouse
    AND lcount.id_period = (SELECT id_period FROM period ORDER BY period DESC LIMIT 1)
WHERE w.id_warehouse = (SELECT id_warehouse FROM warehouse ORDER BY id_warehouse ASC LIMIT 1)
ORDER BY p.cve_art
LIMIT 10;

-- ============================================
-- 10. DIAGNÓSTICO: ¿Por qué devuelve []?
-- ============================================
SELECT 'DIAGNOSTICO' as paso;

-- Verificar si hay productos en el inventario del primer almacén
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN CONCAT('✓ HAY ', COUNT(*), ' PRODUCTOS en el primer almacén')
        ELSE '✗ NO HAY PRODUCTOS en el primer almacén'
    END as diagnostico
FROM inventory_stock
WHERE id_warehouse = (SELECT id_warehouse FROM warehouse ORDER BY id_warehouse ASC LIMIT 1);

-- Verificar si hay almacenes
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN CONCAT('✓ HAY ', COUNT(*), ' ALMACENES registrados')
        ELSE '✗ NO HAY ALMACENES registrados'
    END as diagnostico
FROM warehouse;

-- Verificar si hay periodos
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN CONCAT('✓ HAY ', COUNT(*), ' PERIODOS registrados')
        ELSE '✗ NO HAY PERIODOS registrados'
    END as diagnostico
FROM period;

-- ============================================
-- 11. SI NECESITAS CREAR DATOS DE PRUEBA
-- (Descomenta estas líneas si no hay datos)
-- ============================================

/*
-- Crear un periodo de prueba si no existe
INSERT INTO period (period, comments, state)
VALUES (CURDATE(), 'Periodo de prueba', 'ACTIVO')
ON DUPLICATE KEY UPDATE period=period;

-- Crear un almacén de prueba si no existe
INSERT INTO warehouse (warehouse_key, name_warehouse, observations)
VALUES ('ALM-TEST', 'Almacén de Prueba', 'Para testing')
ON DUPLICATE KEY UPDATE warehouse_key=warehouse_key;

-- Verificar que existan productos
SELECT COUNT(*) as total_productos FROM products;
-- Si no hay productos, deberás crearlos según tu lógica de negocio

-- Crear existencias de prueba (ajusta los IDs según tu BD)
INSERT INTO inventory_stock (id_product, id_warehouse, exist_qty, status, updated_at)
SELECT
    p.id_product,
    (SELECT id_warehouse FROM warehouse ORDER BY id_warehouse ASC LIMIT 1) as id_warehouse,
    100 as exist_qty,
    'ACTIVO' as status,
    NOW() as updated_at
FROM products p
LIMIT 10
ON DUPLICATE KEY UPDATE exist_qty=exist_qty;
*/

-- ============================================
-- FIN DEL SCRIPT
-- ============================================
SELECT '============================================' as fin;
SELECT 'SCRIPT DE VERIFICACION COMPLETADO' as mensaje;
SELECT '============================================' as fin;

