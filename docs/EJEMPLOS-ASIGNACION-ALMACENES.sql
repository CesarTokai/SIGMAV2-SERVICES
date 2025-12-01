-- Script de ejemplo para asignar almacenes a usuarios
-- Módulo de Marbetes - Control de Contexto Informativo

-- =====================================================
-- EJEMPLO 1: Asignar almacén 1 al usuario 5 (Almacenista)
-- =====================================================
INSERT INTO user_warehouse_assignments (id_user, id_warehouse, assigned_by, assigned_at, is_active)
VALUES (5, 1, 1, CURRENT_TIMESTAMP, TRUE);

-- =====================================================
-- EJEMPLO 2: Asignar múltiples almacenes a un usuario
-- =====================================================
-- Usuario 7 (Auxiliar de Conteo) tiene acceso a almacenes 1, 2 y 3
INSERT INTO user_warehouse_assignments (id_user, id_warehouse, assigned_by, assigned_at, is_active)
VALUES
    (7, 1, 1, CURRENT_TIMESTAMP, TRUE),
    (7, 2, 1, CURRENT_TIMESTAMP, TRUE),
    (7, 3, 1, CURRENT_TIMESTAMP, TRUE);

-- =====================================================
-- EJEMPLO 3: Consultar almacenes asignados a un usuario
-- =====================================================
SELECT
    uwa.id_user,
    u.username,
    uwa.id_warehouse,
    mw.warehouse_name,
    uwa.assigned_at,
    uwa.is_active
FROM user_warehouse_assignments uwa
INNER JOIN users u ON uwa.id_user = u.id_user
INNER JOIN main_warehouse mw ON uwa.id_warehouse = mw.id_warehouse
WHERE uwa.id_user = 5 AND uwa.is_active = TRUE;

-- =====================================================
-- EJEMPLO 4: Consultar usuarios con acceso a un almacén
-- =====================================================
SELECT
    uwa.id_warehouse,
    mw.warehouse_name,
    uwa.id_user,
    u.username,
    u.role,
    uwa.assigned_at
FROM user_warehouse_assignments uwa
INNER JOIN users u ON uwa.id_user = u.id_user
INNER JOIN main_warehouse mw ON uwa.id_warehouse = mw.id_warehouse
WHERE uwa.id_warehouse = 1 AND uwa.is_active = TRUE;

-- =====================================================
-- EJEMPLO 5: Desactivar una asignación (soft delete)
-- =====================================================
UPDATE user_warehouse_assignments
SET is_active = FALSE
WHERE id_user = 5 AND id_warehouse = 1;

-- =====================================================
-- EJEMPLO 6: Reactivar una asignación
-- =====================================================
UPDATE user_warehouse_assignments
SET is_active = TRUE
WHERE id_user = 5 AND id_warehouse = 1;

-- =====================================================
-- EJEMPLO 7: Eliminar permanentemente una asignación
-- =====================================================
DELETE FROM user_warehouse_assignments
WHERE id_user = 5 AND id_warehouse = 1;

-- =====================================================
-- EJEMPLO 8: Asignar todos los almacenes a un usuario
-- =====================================================
-- Útil para configurar un nuevo almacenista
INSERT INTO user_warehouse_assignments (id_user, id_warehouse, assigned_by, is_active)
SELECT
    10, -- ID del usuario
    id_warehouse,
    1, -- ID del administrador que asigna
    TRUE
FROM main_warehouse
WHERE is_active = TRUE; -- Solo almacenes activos

-- =====================================================
-- EJEMPLO 9: Verificar si un usuario tiene acceso a un almacén
-- =====================================================
SELECT EXISTS (
    SELECT 1
    FROM user_warehouse_assignments
    WHERE id_user = 5
    AND id_warehouse = 1
    AND is_active = TRUE
) AS tiene_acceso;

-- =====================================================
-- EJEMPLO 10: Reporte de asignaciones por rol
-- =====================================================
SELECT
    u.role,
    COUNT(DISTINCT uwa.id_user) AS usuarios_con_asignaciones,
    COUNT(*) AS total_asignaciones,
    COUNT(DISTINCT uwa.id_warehouse) AS almacenes_asignados
FROM user_warehouse_assignments uwa
INNER JOIN users u ON uwa.id_user = u.id_user
WHERE uwa.is_active = TRUE
GROUP BY u.role
ORDER BY u.role;

-- =====================================================
-- NOTAS IMPORTANTES:
-- =====================================================
-- 1. Los roles ADMINISTRADOR y AUXILIAR NO NECESITAN asignaciones
--    (tienen acceso a todos los almacenes por defecto)
--
-- 2. Solo se deben crear asignaciones para:
--    - ALMACENISTA
--    - AUXILIAR_DE_CONTEO
--
-- 3. El campo assigned_by puede ser NULL para asignaciones automáticas
--    o del sistema
--
-- 4. Usar is_active=FALSE en lugar de DELETE para mantener auditoría
--
-- 5. La tabla tiene restricción UNIQUE en (id_user, id_warehouse)
--    No se pueden duplicar asignaciones activas

