-- =====================================================
-- Migración: Agregar campo cita_generada_por_tratamiento_id
-- Fecha: 2025-11-20
-- Descripción: Permite crear cadenas de citas para tratamientos secuenciales
-- =====================================================

-- Agregar columna para vincular citas en cadena
ALTER TABLE citas
ADD COLUMN cita_generada_por_tratamiento_id BIGINT NULL
COMMENT 'ID de la cita que generó esta cita por un tratamiento realizado';

-- Agregar foreign key
ALTER TABLE citas
ADD CONSTRAINT fk_cita_generada_por_tratamiento
FOREIGN KEY (cita_generada_por_tratamiento_id)
REFERENCES citas(id)
ON DELETE SET NULL;

-- Crear índice para mejorar performance
CREATE INDEX idx_cita_generada_por_tratamiento
ON citas(cita_generada_por_tratamiento_id);

-- Verificar la estructura
DESCRIBE citas;
