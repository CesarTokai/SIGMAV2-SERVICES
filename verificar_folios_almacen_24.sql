-- =====================================================
-- VERIFICACIÓN RÁPIDA: Folios Almacén 24, Periodo 20
-- =====================================================

-- 1. VER TODOS LOS FOLIOS REALES
SELECT
    l.folio,
    p.cve_art as clave_producto,
    p.descr as nombre_producto,
    l.estado,
    l.created_at
FROM labels l
INNER JOIN product p ON l.product_id = p.id_product
WHERE l.warehouse_id = 420
  AND l.period_id = 20
ORDER BY l.folio;

-- 2. RESUMEN POR PRODUCTO
SELECT
    p.cve_art as producto,
    MIN(l.folio) as primer_folio,
    MAX(l.folio) as ultimo_folio,
    COUNT(*) as total_folios,
    l.estado
FROM labels l
INNER JOIN product p ON l.product_id = p.id_product
WHERE l.warehouse_id = 420
  AND l.period_id = 20
GROUP BY p.cve_art, l.estado
ORDER BY MIN(l.folio);

-- 3. VERIFICAR SI EXISTE FOLIO 5
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN CONCAT('✅ FOLIO 5 EXISTE - Producto: ', MAX(p.cve_art))
        ELSE '❌ FOLIO 5 NO EXISTE'
    END as resultado
FROM labels l
LEFT JOIN product p ON l.product_id = p.id_product
WHERE l.folio = 5
  AND l.warehouse_id = 420
  AND l.period_id = 20;

-- 4. VERIFICAR SI EXISTE FOLIO 247
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN CONCAT('✅ FOLIO 247 EXISTE - Producto: ', MAX(p.cve_art))
        ELSE '❌ FOLIO 247 NO EXISTE'
    END as resultado
FROM labels l
LEFT JOIN product p ON l.product_id = p.id_product
WHERE l.folio = 247
  AND l.warehouse_id = 420
  AND l.period_id = 20;

-- 5. LISTA DE FOLIOS PARA COPIAR/PEGAR EN CONTEO
SELECT
    GROUP_CONCAT(l.folio ORDER BY l.folio SEPARATOR ', ') as folios_disponibles
FROM labels l
WHERE l.warehouse_id = 420
  AND l.period_id = 20
  AND l.estado = 'IMPRESO';

