-- =====================================================
-- Script de Corrección: Asignar Motivos a Movimientos NULL
-- Fecha: 2025-11-20
-- Descripción: Corrige registros antiguos en movimientos_inventario
--              que tienen motivo_movimiento_id en NULL
-- =====================================================

-- PASO 1: Verificar cuántos registros tienen motivo NULL
SELECT
    COUNT(*) as total_sin_motivo,
    tipo_movimiento_id,
    tm.nombre as tipo_nombre
FROM movimientos_inventario mi
LEFT JOIN tipos_movimiento tm ON mi.tipo_movimiento_id = tm.id
WHERE mi.motivo_movimiento_id IS NULL
GROUP BY tipo_movimiento_id, tm.nombre;

-- PASO 2: Ver ejemplos de registros con NULL
SELECT
    id,
    referencia,
    tipo_movimiento_id,
    motivo_movimiento_id,
    fecha_creacion
FROM movimientos_inventario
WHERE motivo_movimiento_id IS NULL
ORDER BY fecha_creacion DESC
LIMIT 10;

-- PASO 3: Obtener el ID del motivo "Uso en procedimiento"
-- (Necesitamos este ID para la actualización)
SELECT id, nombre, tipo_movimiento_id
FROM motivos_movimiento
WHERE nombre LIKE '%procedimiento%' OR nombre LIKE '%tratamiento%';

-- PASO 4: Actualizar registros de SALIDA (tipo_movimiento_id = 2)
--         que tienen NULL en motivo_movimiento_id
--
-- ⚠️ IMPORTANTE: Reemplaza el '3' con el ID correcto del motivo que obtuviste en el PASO 3
--
-- EJEMPLO: Si el motivo "Uso en procedimiento" tiene ID = 5, usa 5 en vez de 3

UPDATE movimientos_inventario
SET motivo_movimiento_id = (
    SELECT id
    FROM motivos_movimiento
    WHERE nombre = 'Uso en procedimiento'
    LIMIT 1
)
WHERE motivo_movimiento_id IS NULL
  AND tipo_movimiento_id = (SELECT id FROM tipos_movimiento WHERE codigo = 'SALIDA')
  AND referencia LIKE 'Cita #%';

-- PASO 5: Verificar la corrección
SELECT
    COUNT(*) as total_corregidos,
    mm.nombre as motivo_nombre
FROM movimientos_inventario mi
JOIN motivos_movimiento mm ON mi.motivo_movimiento_id = mm.id
WHERE mi.referencia LIKE 'Cita #%'
  AND mi.tipo_movimiento_id = (SELECT id FROM tipos_movimiento WHERE codigo = 'SALIDA')
GROUP BY mm.nombre;

-- PASO 6: Verificar si aún quedan registros sin motivo
SELECT COUNT(*) as total_aun_sin_motivo
FROM movimientos_inventario
WHERE motivo_movimiento_id IS NULL;

-- =====================================================
-- NOTAS IMPORTANTES:
-- =====================================================
-- 1. Este script NO creará el motivo si no existe.
--    Si no existe "Uso en procedimiento", primero créalo manualmente:
--
--    INSERT INTO motivos_movimiento (nombre, tipo_movimiento_id)
--    VALUES ('Uso en procedimiento', (SELECT id FROM tipos_movimiento WHERE codigo = 'SALIDA'));
--
-- 2. Ejecuta cada paso UNO POR UNO para verificar los resultados.
--
-- 3. Si tienes otros tipos de movimientos con NULL, ajusta el PASO 4
--    para incluirlos con el motivo correspondiente.
--
-- 4. Haz un BACKUP de la base de datos ANTES de ejecutar el UPDATE.
-- =====================================================
