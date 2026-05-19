-- ✅ FIX: Hacer que status sea NOT NULL en tabla products e inventory_snapshot
-- Problema: En re-importaciones, status quedaba NULL

-- === TABLA PRODUCTS ===
-- Paso 1: Actualizar cualquier registro con status NULL a 'A' (valor por defecto)
UPDATE products SET status = 'A' WHERE status IS NULL;

-- Paso 2: Agregar constraint NOT NULL con default
ALTER TABLE products
MODIFY COLUMN status VARCHAR(1) NOT NULL DEFAULT 'A';

-- === TABLA INVENTORY_SNAPSHOT ===
-- Paso 3: Actualizar snapshots con status NULL usando el status del producto
UPDATE inventory_snapshot s
JOIN products p ON s.product_id = p.id_product
SET s.status = p.status
WHERE s.status IS NULL;

-- Paso 4: Si aún quedan NULL (producto sin match), poner 'A'
UPDATE inventory_snapshot SET status = 'A' WHERE status IS NULL;

-- Paso 5: Agregar constraint NOT NULL con default
ALTER TABLE inventory_snapshot
MODIFY COLUMN status VARCHAR(1) NOT NULL DEFAULT 'A';

