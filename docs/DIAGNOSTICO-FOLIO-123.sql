-- =====================================================
-- Script de Diagnóstico: Folio 123 - Conteo C2
-- =====================================================

-- 1. Verificar si el folio existe
SELECT
    '=== INFORMACIÓN DEL FOLIO ===' AS seccion;

SELECT
    l.folio,
    l.estado,
    l.id_product,
    l.id_warehouse,
    l.id_period,
    l.created_at AS folio_creado,
    l.impreso_at AS folio_impreso,
    p.cve_art AS codigo_producto,
    p.descr AS nombre_producto,
    w.warehouse_key AS codigo_almacen,
    w.name_warehouse AS nombre_almacen
FROM labels l
LEFT JOIN products p ON l.id_product = p.id_product
LEFT JOIN warehouse w ON l.id_warehouse = w.id_warehouse
WHERE l.folio = 123;

-- 2. Verificar conteos registrados
SELECT
    '=== CONTEOS REGISTRADOS ===' AS seccion;

SELECT
    lce.id_count_event,
    lce.folio,
    lce.count_number AS conteo,
    lce.counted_value AS valor,
    lce.created_at AS fecha_registro,
    lce.role_at_time AS rol_usuario,
    lce.user_id,
    lce.is_final,
    u.email AS usuario_email
FROM label_count_events lce
LEFT JOIN users u ON lce.user_id = u.user_id
WHERE lce.folio = 123
ORDER BY lce.count_number, lce.created_at;

-- 3. Resumen de conteos
SELECT
    '=== RESUMEN ===' AS seccion;

SELECT
    l.folio,
    l.estado AS estado_marbete,

    -- Conteo C1
    (SELECT counted_value FROM label_count_events
     WHERE folio = l.folio AND count_number = 1
     ORDER BY created_at DESC LIMIT 1) AS c1_ultimo_valor,

    (SELECT COUNT(*) FROM label_count_events
     WHERE folio = l.folio AND count_number = 1) AS c1_total_registros,

    -- Conteo C2
    (SELECT counted_value FROM label_count_events
     WHERE folio = l.folio AND count_number = 2
     ORDER BY created_at DESC LIMIT 1) AS c2_ultimo_valor,

    (SELECT COUNT(*) FROM label_count_events
     WHERE folio = l.folio AND count_number = 2) AS c2_total_registros,

    -- Total de eventos
    (SELECT COUNT(*) FROM label_count_events
     WHERE folio = l.folio) AS total_eventos_conteo,

    -- Diagnóstico
    CASE
        WHEN NOT EXISTS (SELECT 1 FROM label_count_events WHERE folio = l.folio AND count_number = 1) THEN 'SIN CONTEOS - Registrar C1 primero'
        WHEN NOT EXISTS (SELECT 1 FROM label_count_events WHERE folio = l.folio AND count_number = 2) THEN 'SOLO C1 - Debe REGISTRAR C2 con POST'
        ELSE 'C1 y C2 COMPLETOS - Puede ACTUALIZAR C2 con PUT'
    END AS diagnostico

FROM labels l
WHERE l.folio = 123;

-- 4. Verificar si se puede registrar C2
SELECT
    '=== VALIDACIONES PARA REGISTRAR C2 ===' AS seccion;

SELECT
    l.folio,

    -- Validaciones
    CASE WHEN l.estado = 'IMPRESO' THEN '✅ Válido' ELSE '❌ Debe estar IMPRESO' END AS estado_valido,

    CASE WHEN l.estado = 'CANCELADO' THEN '❌ Está CANCELADO' ELSE '✅ No cancelado' END AS no_cancelado,

    CASE WHEN EXISTS (SELECT 1 FROM label_count_events WHERE folio = l.folio AND count_number = 1)
         THEN '✅ C1 existe'
         ELSE '❌ Debe registrar C1 primero'
    END AS tiene_c1,

    CASE WHEN EXISTS (SELECT 1 FROM label_count_events WHERE folio = l.folio AND count_number = 2)
         THEN '❌ C2 ya existe (usar PUT para actualizar)'
         ELSE '✅ Puede registrar C2'
    END AS puede_registrar_c2,

    -- Acción recomendada
    CASE
        WHEN l.estado = 'CANCELADO' THEN 'IMPOSIBLE: Folio cancelado'
        WHEN l.estado != 'IMPRESO' THEN 'ESPERAR: Folio debe estar impreso'
        WHEN NOT EXISTS (SELECT 1 FROM label_count_events WHERE folio = l.folio AND count_number = 1)
             THEN 'POST /counts/c1 - Registrar C1 primero'
        WHEN EXISTS (SELECT 1 FROM label_count_events WHERE folio = l.folio AND count_number = 2)
             THEN 'PUT /counts/c2 - Actualizar C2 existente'
        ELSE 'POST /counts/c2 - Registrar nuevo C2'
    END AS accion_recomendada

FROM labels l
WHERE l.folio = 123;

-- 5. Historial completo de cambios del folio
SELECT
    '=== HISTORIAL COMPLETO ===' AS seccion;

SELECT
    'CREACIÓN' AS evento,
    l.created_at AS fecha,
    l.created_by AS usuario_id,
    u1.email AS usuario_email,
    'Folio generado' AS detalles
FROM labels l
LEFT JOIN users u1 ON l.created_by = u1.user_id
WHERE l.folio = 123

UNION ALL

SELECT
    'IMPRESIÓN' AS evento,
    l.impreso_at AS fecha,
    NULL AS usuario_id,
    NULL AS usuario_email,
    'Folio impreso' AS detalles
FROM labels l
WHERE l.folio = 123 AND l.impreso_at IS NOT NULL

UNION ALL

SELECT
    CONCAT('CONTEO C', lce.count_number) AS evento,
    lce.created_at AS fecha,
    lce.user_id AS usuario_id,
    u2.email AS usuario_email,
    CONCAT('Valor: ', lce.counted_value, ' - Rol: ', lce.role_at_time) AS detalles
FROM label_count_events lce
LEFT JOIN users u2 ON lce.user_id = u2.user_id
WHERE lce.folio = 123

ORDER BY fecha;

-- =====================================================
-- FIN DEL DIAGNÓSTICO
-- =====================================================

-- RESULTADOS ESPERADOS:
--
-- Si c2_ultimo_valor = NULL y diagnostico = 'SOLO C1 - Debe REGISTRAR C2 con POST'
-- Entonces debes usar: POST /api/sigmav2/labels/counts/c2
--
-- Si c2_ultimo_valor != NULL y diagnostico = 'C1 y C2 COMPLETOS - Puede ACTUALIZAR C2 con PUT'
-- Entonces puedes usar: PUT /api/sigmav2/labels/counts/c2
