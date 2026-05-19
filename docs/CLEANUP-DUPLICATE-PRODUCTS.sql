-- Script de limpieza de productos duplicados
-- Este script elimina todos los productos duplicados, manteniendo el ID más bajo

-- 1. Primero, eliminar los snapshots asociados a los productos que serán eliminados
DELETE FROM inventory_snapshot
WHERE product_id IN (
    SELECT id_product FROM (
        SELECT id_product
        FROM products
        WHERE cve_art IN (
            SELECT cve_art
            FROM products
            GROUP BY cve_art
            HAVING COUNT(*) > 1
        )
        AND id_product NOT IN (
            SELECT MIN(id_product)
            FROM products
            GROUP BY cve_art
            HAVING COUNT(*) > 1
        )
    ) AS duplicates
);

-- 2. Eliminar los productos duplicados (mantener el ID más bajo)
DELETE FROM products
WHERE cve_art IN (
    SELECT cve_art
    FROM products
    GROUP BY cve_art
    HAVING COUNT(*) > 1
)
AND id_product NOT IN (
    SELECT MIN(id_product)
    FROM products
    GROUP BY cve_art
    HAVING COUNT(*) > 1
);

-- 3. Verificar que no hay más duplicados
SELECT cve_art, COUNT(*) as cantidad
FROM products
GROUP BY cve_art
HAVING COUNT(*) > 1;

-- Si la consulta anterior devuelve resultados, hay un problema
-- Si no devuelve nada, todos los duplicados fueron eliminados exitosamente

