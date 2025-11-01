package com.odontoapp.servicio;

import com.odontoapp.dto.ProcedimientoInsumoDTO;
import com.odontoapp.entidad.ProcedimientoInsumo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio para la gestión de la relación entre procedimientos e insumos.
 * Define qué insumos se utilizan en cada procedimiento y en qué cantidad.
 */
public interface ProcedimientoInsumoService {

    /**
     * Asigna un insumo a un procedimiento con una cantidad por defecto.
     *
     * @param dto Datos de la asignación
     * @return La relación creada
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentran el procedimiento o insumo
     * @throws org.springframework.dao.DataIntegrityViolationException si ya existe la relación
     */
    ProcedimientoInsumo asignarInsumoAProcedimiento(ProcedimientoInsumoDTO dto);

    /**
     * Actualiza la cantidad por defecto de un insumo en un procedimiento.
     *
     * @param procedimientoInsumoId ID de la relación
     * @param nuevaCantidad Nueva cantidad por defecto
     * @return La relación actualizada
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra la relación
     */
    ProcedimientoInsumo actualizarCantidadInsumo(Long procedimientoInsumoId, BigDecimal nuevaCantidad);

    /**
     * Elimina la relación entre un procedimiento y un insumo.
     *
     * @param procedimientoInsumoId ID de la relación a eliminar
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra la relación
     */
    void quitarInsumoDeProcedimiento(Long procedimientoInsumoId);

    /**
     * Busca todos los insumos asociados a un procedimiento.
     *
     * @param procedimientoId ID del procedimiento
     * @return Lista de relaciones procedimiento-insumo
     */
    List<ProcedimientoInsumo> buscarInsumosPorProcedimiento(Long procedimientoId);

    /**
     * Busca todos los procedimientos que utilizan un insumo específico.
     *
     * @param insumoId ID del insumo
     * @return Lista de relaciones procedimiento-insumo
     */
    List<ProcedimientoInsumo> buscarProcedimientosPorInsumo(Long insumoId);

    /**
     * Busca una relación específica entre procedimiento e insumo.
     *
     * @param procedimientoId ID del procedimiento
     * @param insumoId ID del insumo
     * @return La relación si existe
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra la relación
     */
    ProcedimientoInsumo buscarRelacion(Long procedimientoId, Long insumoId);
}
