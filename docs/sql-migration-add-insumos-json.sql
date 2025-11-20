-- =====================================================
-- Migración: Agregar campo insumosJson a TratamientoPlanificado
-- Fecha: 2025-11-20
-- Descripción: Permite guardar insumos modificados/adicionales
--              en tratamientos planificados
-- =====================================================

-- NOTA: Esta migración NO es necesaria ejecutarla manualmente
-- porque Hibernate está configurado con ddl-auto=update
-- y creará automáticamente la columna al iniciar la aplicación.

-- Este archivo es solo para documentación y referencia.

-- Si deseas ejecutar manualmente (opcional):
ALTER TABLE tratamientos_planificados
ADD COLUMN insumos_json TEXT NULL
COMMENT 'JSON con insumos modificados: [{"insumoId": 1, "cantidad": "2.5"}, ...]';

-- Verificar que la columna se creó correctamente:
-- DESCRIBE tratamientos_planificados;
