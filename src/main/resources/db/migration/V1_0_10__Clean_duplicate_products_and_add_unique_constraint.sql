-- Limpiar duplicados de cve_art
-- Esta migración elimina registros duplicados, manteniendo el ID más bajo

-- 1. Primero, eliminar los snapshots asociados a los productos duplicados
DELETE FROM inventory_snapshot
WHERE product_id IN (
    SELECT id_product FROM products p1
    WHERE cve_art IN (
        SELECT cve_art FROM products
        GROUP BY cve_art
        HAVING COUNT(*) > 1
    )
    AND id_product NOT IN (
        SELECT MIN(id_product) FROM products p2
        WHERE p2.cve_art = p1.cve_art
    )
);

-- 2. Eliminar los productos duplicados
DELETE FROM products
WHERE id_product IN (
    SELECT id_product FROM (
        SELECT id_product FROM products p1
        WHERE cve_art IN (
            SELECT cve_art FROM products
            GROUP BY cve_art
            HAVING COUNT(*) > 1
        )
        AND id_product NOT IN (
            SELECT MIN(id_product) FROM products p2
            WHERE p2.cve_art = p1.cve_art
        )
    ) AS duplicates_to_delete
);

-- 3. Agregar constraint UNIQUE a cve_art (si aún no existe)
-- Se ignora el error si el constraint ya existe
ALTER TABLE products
ADD CONSTRAINT uk_products_cve_art UNIQUE (cve_art);



