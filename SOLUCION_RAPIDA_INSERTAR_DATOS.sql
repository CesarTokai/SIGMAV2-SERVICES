-- ============================================
-- SOLUCIÓN RÁPIDA: Insertar Datos de Prueba
-- Para que el endpoint devuelva productos
-- ============================================

-- PASO 1: Verificar situación actual
SELECT
    'VERIFICANDO ALMACÉN 250' as paso,
    COUNT(*) as productos_en_almacen_250
FROM inventory_stock
WHERE id_warehouse = 250;

-- PASO 2: Ver cuántos productos hay disponibles
SELECT
    'PRODUCTOS DISPONIBLES' as paso,
    COUNT(*) as total_productos_activos
FROM products
WHERE status = 'ACTIVO';

-- ============================================
-- SOLUCIÓN RÁPIDA: Insertar 20 productos de prueba
-- ============================================

-- Eliminar datos existentes del almacén 250 (si los hay)
DELETE FROM inventory_stock WHERE id_warehouse = 250;

-- Insertar 20 productos con existencias en el almacén 250
INSERT INTO inventory_stock (id_product, id_warehouse, exist_qty, status, updated_at)
SELECT
    p.id_product,
    250 as id_warehouse,
    FLOOR(100 + (RAND() * 900)) as exist_qty,  -- Entre 100 y 1000
    'ACTIVO' as status,
    NOW() as updated_at
FROM products p
WHERE p.status = 'ACTIVO'
LIMIT 20;

-- PASO 3: Verificar que se insertaron correctamente
SELECT
    'RESULTADO' as paso,
    COUNT(*) as productos_insertados
FROM inventory_stock
WHERE id_warehouse = 250;

-- PASO 4: Ver los productos insertados
SELECT
    ist.id_stock,
    p.cve_art as clave,
    p.descr as producto,
    ist.exist_qty as existencias,
    ist.status as estado,
    w.warehouse_key as almacen
FROM inventory_stock ist
INNER JOIN products p ON ist.id_product = p.id_product
INNER JOIN warehouse w ON ist.id_warehouse = w.id_warehouse
WHERE ist.id_warehouse = 250
ORDER BY p.cve_art
LIMIT 20;

-- ============================================
-- LISTO! Ahora prueba el endpoint nuevamente
-- ============================================

/*
INSTRUCCIONES:

1. Copia y pega este script COMPLETO en tu cliente MySQL
2. Ejecuta todo el script
3. Verifica que dice "productos_insertados: 20"
4. Vuelve a hacer la petición al endpoint
5. Deberías ver 20 productos en lugar de []

PETICIÓN DE PRUEBA:
POST http://localhost:8080/api/sigmav2/labels/summary
{
  "warehouseId": 250,
  "periodId": 7,
  "page": 0,
  "size": 10
}

RESULTADO ESPERADO:
[
  {
    "productId": X,
    "claveProducto": "...",
    "nombreProducto": "...",
    "claveAlmacen": "AAA-JSP",
    "nombreAlmacen": "CEDIS TOKAI",
    "foliosSolicitados": 0,
    "foliosExistentes": 0,
    "estado": "ACTIVO",
    "existencias": 456
  },
  ...
]
*/

