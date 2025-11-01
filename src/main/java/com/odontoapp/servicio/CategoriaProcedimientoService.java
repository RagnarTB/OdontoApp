package com.odontoapp.servicio;

import java.util.List;
import java.util.Optional;

import com.odontoapp.entidad.CategoriaProcedimiento;

/**
 * Servicio para gestionar categorías de procedimientos/servicios
 */
public interface CategoriaProcedimientoService {

    /**
     * Lista todas las categorías ordenadas por nombre
     */
    List<CategoriaProcedimiento> listarTodasOrdenadasPorNombre();

    /**
     * Lista solo las categorías activas
     */
    List<CategoriaProcedimiento> listarCategoriasActivas();

    /**
     * Busca una categoría por ID
     */
    Optional<CategoriaProcedimiento> buscarPorId(Long id);

    /**
     * Guarda o actualiza una categoría
     */
    void guardar(CategoriaProcedimiento categoria);

    /**
     * Elimina (soft delete) una categoría si no tiene procedimientos asociados
     */
    void eliminar(Long id);

    /**
     * Cambia el estado activo/inactivo de una categoría
     */
    void cambiarEstado(Long id);
}
