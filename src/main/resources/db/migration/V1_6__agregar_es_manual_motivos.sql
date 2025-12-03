-- Agregar campo es_manual a la tabla motivos_movimiento
ALTER TABLE motivos_movimiento 
ADD COLUMN es_manual BOOLEAN DEFAULT TRUE;

-- Marcar como NO manuales los motivos que solo se usan automáticamente
UPDATE motivos_movimiento 
SET es_manual = FALSE 
WHERE nombre IN ('Uso en procedimiento', 'Venta Directa', 'Anulación de Venta');

-- Verificar los cambios
SELECT id, nombre, es_manual FROM motivos_movimiento ORDER BY es_manual DESC, nombre;
