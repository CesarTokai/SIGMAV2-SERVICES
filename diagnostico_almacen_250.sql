-- ============================================
-- DIAGNÓSTICO Y SOLUCIÓN: Lista Vacía en Labels
-- Basado en los logs de la aplicación
-- ============================================

-- PASO 1: Verificar el almacén que está usando (según logs: almacén 250)
SELECT
    id_warehouse,
    warehouse_key,
    name_warehouse,
    observations
FROM warehouse
WHERE id_warehouse = 250;

-- PASO 2: Verificar si hay productos en inventory_stock para ese almacén
SELECT COUNT(*) as total_productos_almacen_250
FROM inventory_stock
WHERE id_warehouse = 250;

-- PASO 3: Ver TODOS los almacenes que tienen productos en inventory_stock
SELECT
    w.id_warehouse,
    w.warehouse_key,
    w.name_warehouse,
    COUNT(ist.id_stock) as total_productos
FROM warehouse w
LEFT JOIN inventory_stock ist ON w.id_warehouse = ist.id_warehouse
GROUP BY w.id_warehouse, w.warehouse_key, w.name_warehouse
ORDER BY w.id_warehouse;

-- PASO 4: Ver cuántos productos hay en total en inventory_stock
SELECT
    COUNT(DISTINCT ist.id_warehouse) as almacenes_con_productos,
    COUNT(ist.id_stock) as total_registros,
    COUNT(DISTINCT ist.id_product) as productos_unicos
FROM inventory_stock ist;

-- PASO 5: Ver los primeros 10 registros de inventory_stock
SELECT
    ist.id_stock,
    ist.id_warehouse,
    w.warehouse_key,
    ist.id_product,
    p.cve_art,
    p.descr,
    ist.exist_qty,
    ist.status
FROM inventory_stock ist
LEFT JOIN warehouse w ON ist.id_warehouse = w.id_warehouse
LEFT JOIN products p ON ist.id_product = p.id_product
LIMIT 10;

-- ============================================
-- SOLUCIÓN 1: Si NO hay datos en inventory_stock para el almacén 250
-- Opción A: Insertar datos de prueba
-- ============================================

-- Primero, verificar si hay productos en la tabla products
SELECT COUNT(*) as total_productos FROM products;

-- Ver los primeros 10 productos disponibles
SELECT
    id_product,
    cve_art,
    descr,
    uni_med,
    status
FROM products
WHERE status = 'ACTIVO'
LIMIT 10;

-- ============================================
-- INSERTAR DATOS DE PRUEBA EN inventory_stock
-- (Ejecuta esto SOLO si no hay datos)
-- ============================================

/*
-- Inserta existencias para los primeros 10 productos en el almacén 250
INSERT INTO inventory_stock (id_product, id_warehouse, exist_qty, status, updated_at)
SELECT
    p.id_product,
    250 as id_warehouse,
    FLOOR(RAND() * 1000) + 100 as exist_qty,  -- Cantidad aleatoria entre 100 y 1100
    'ACTIVO' as status,
    NOW() as updated_at
FROM products p
WHERE p.status = 'ACTIVO'
LIMIT 10
ON DUPLICATE KEY UPDATE
    exist_qty = VALUES(exist_qty),
    updated_at = VALUES(updated_at);

-- Verificar que se insertaron
SELECT COUNT(*) as productos_insertados
FROM inventory_stock
WHERE id_warehouse = 250;
*/

-- ============================================
-- SOLUCIÓN 2: Si el almacén 250 NO debería usarse
-- Opción B: Usar otro almacén que SÍ tenga datos
-- ============================================

-- Encontrar el primer almacén que tiene productos en inventory_stock
SELECT
    w.id_warehouse,
    w.warehouse_key,
    w.name_warehouse,
    COUNT(ist.id_stock) as total_productos
FROM warehouse w
INNER JOIN inventory_stock ist ON w.id_warehouse = ist.id_warehouse
GROUP BY w.id_warehouse, w.warehouse_key, w.name_warehouse
ORDER BY w.id_warehouse ASC
LIMIT 1;

-- ============================================
-- SOLUCIÓN 3: Insertar datos para TODOS los almacenes
-- (Si quieres que todos los almacenes tengan productos)
-- ============================================

/*
-- Inserta 20 productos aleatorios para cada almacén
INSERT INTO inventory_stock (id_product, id_warehouse, exist_qty, status, updated_at)
SELECT
    p.id_product,
    w.id_warehouse,
    FLOOR(RAND() * 500) + 50 as exist_qty,  -- Cantidad aleatoria entre 50 y 550
    'ACTIVO' as status,
    NOW() as updated_at
FROM products p
CROSS JOIN warehouse w
WHERE p.status = 'ACTIVO'
LIMIT 1000  -- Ajusta según necesites
ON DUPLICATE KEY UPDATE
    exist_qty = VALUES(exist_qty),
    updated_at = VALUES(updated_at);
*/

-- ============================================
-- VERIFICACIÓN FINAL
-- ============================================

-- Ver el resultado que DEBERÍA devolver el endpoint
SELECT
    p.id_product,
    p.cve_art as clave_producto,
    p.descr as nombre_producto,
    w.warehouse_key as clave_almacen,
    w.name_warehouse as nombre_almacen,
    COALESCE(lr.requested_labels, 0) as folios_solicitados,
    0 as folios_existentes,  -- No hay marbetes generados aún
    ist.status as estado,
    ist.exist_qty as existencias
FROM inventory_stock ist
INNER JOIN products p ON ist.id_product = p.id_product
INNER JOIN warehouse w ON ist.id_warehouse = w.id_warehouse
LEFT JOIN label_requests lr ON lr.id_product = p.id_product
    AND lr.id_warehouse = w.id_warehouse
    AND lr.id_period = (SELECT id_period FROM period ORDER BY period DESC LIMIT 1)
WHERE w.id_warehouse = 250
ORDER BY p.cve_art
LIMIT 10;

-- ============================================
-- INFORMACIÓN ADICIONAL DEL SISTEMA
-- ============================================

-- Periodo que está usando (según logs: periodo 7)
SELECT
    id_period,
    period as fecha,
    state as estado,
    comments
FROM period
WHERE id_period = 7;

-- Verificar que el periodo 7 es el último
SELECT
    id_period,
    period as fecha,
    state as estado
FROM period
ORDER BY period DESC
LIMIT 5;

-- ============================================
-- RESUMEN DE ACCIONES
-- ============================================

/*
DIAGNÓSTICO:
✓ La aplicación está usando el almacén 250 (AAA-JSP - CEDIS TOKAI)
✓ La aplicación está usando el periodo 7
✗ NO hay productos en inventory_stock para el almacén 250

SOLUCIONES POSIBLES:

1. INSERTAR DATOS DE PRUEBA en inventory_stock para el almacén 250:
   - Descomenta la sección "INSERTAR DATOS DE PRUEBA" arriba
   - Ejecuta el INSERT
   - Vuelve a probar el endpoint

2. CAMBIAR EL ALMACÉN DEFAULT:
   - Encuentra un almacén que SÍ tenga datos
   - Usa ese warehouseId en la petición del endpoint
   - Ejemplo: { "warehouseId": X } donde X es un almacén con datos

3. POBLAR TODOS LOS ALMACENES:
   - Descomenta la sección "SOLUCIÓN 3"
   - Ejecuta el INSERT masivo
   - Esto creará datos para todos los almacenes

RECOMENDACIÓN:
Si estás en desarrollo/testing, ejecuta la SOLUCIÓN 1 o 3 para tener datos de prueba.
Si estás en producción, verifica por qué el almacén 250 no tiene datos en inventory_stock.
*/

-- ============================================
-- FIN DEL SCRIPT
-- ============================================

