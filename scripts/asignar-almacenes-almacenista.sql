-- ============================================================
-- SCRIPT: Asignar Almacenes a Almacenistas
-- Propósito: Permitir que los almacenistas busquen folios
-- ============================================================

-- 1. Verificar qué almacenistas existen sin asignaciones
SELECT
    u.id,
    u.username,
    u.rol,
    COUNT(uw.id) as almacenes_asignados
FROM users u
LEFT JOIN user_warehouse_assignments uw ON u.id = uw.user_id
WHERE u.rol = 'ALMACENISTA'
    AND u.deleted_at IS NULL
GROUP BY u.id, u.username, u.rol
ORDER BY almacenes_asignados ASC;

-- 2. Verificar qué almacenes existen activos
SELECT
    id,
    warehouse_key,
    name_warehouse,
    created_at
FROM warehouses
WHERE deleted_at IS NULL
ORDER BY id;

-- 3. ASIGNAR TODOS LOS ALMACENES ACTIVOS A TODOS LOS ALMACENISTAS
-- (Ejecutar después de revisar los resultados de las queries anteriores)
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, created_at)
SELECT DISTINCT
    u.id as user_id,
    w.id as warehouse_id,
    NOW() as created_at
FROM users u
CROSS JOIN warehouses w
WHERE u.rol = 'ALMACENISTA'
    AND u.deleted_at IS NULL
    AND w.deleted_at IS NULL
    AND NOT EXISTS (
        -- Evitar duplicados: si ya existe la asignación, no insertar
        SELECT 1 FROM user_warehouse_assignments uw2
        WHERE uw2.user_id = u.id
        AND uw2.warehouse_id = w.id
    )
ON CONFLICT (user_id, warehouse_id) DO NOTHING;

-- 4. Verificar que se asignaron correctamente
SELECT
    u.id,
    u.username,
    u.rol,
    COUNT(uw.warehouse_id) as almacenes_asignados
FROM users u
LEFT JOIN user_warehouse_assignments uw ON u.id = uw.user_id
WHERE u.rol = 'ALMACENISTA'
    AND u.deleted_at IS NULL
GROUP BY u.id, u.username, u.rol
ORDER BY almacenes_asignados DESC;

-- 5. Verificar asignaciones específicas
SELECT
    u.id as usuario_id,
    u.username,
    w.id as almacen_id,
    w.warehouse_key,
    w.name_warehouse,
    uw.created_at
FROM users u
JOIN user_warehouse_assignments uw ON u.id = uw.user_id
JOIN warehouses w ON uw.warehouse_id = w.id
WHERE u.rol = 'ALMACENISTA'
    AND u.deleted_at IS NULL
    AND w.deleted_at IS NULL
ORDER BY u.username, w.warehouse_key;

