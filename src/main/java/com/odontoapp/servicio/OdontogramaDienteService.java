package com.odontoapp.servicio;

import com.odontoapp.dto.OdontogramaDienteDTO;
import com.odontoapp.entidad.OdontogramaHistorial;
import java.util.List;
import java.util.Map;

/**
 * Servicio para la gestión del odontograma usando tablas normalizadas.
 * Maneja el estado de cada diente individual y su historial.
 */
public interface OdontogramaDienteService {

    /**
     * Obtiene el odontograma completo de un paciente.
     * Si no existe, lo inicializa automáticamente con 32 dientes en estado SANO.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @return Lista de DTOs con todos los dientes
     */
    List<OdontogramaDienteDTO> obtenerOdontogramaCompleto(Long pacienteUsuarioId);

    /**
     * Actualiza el estado de un diente específico.
     * Registra automáticamente el cambio en OdontogramaHistorial.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @param numeroDiente Número FDI del diente (ej: "16", "21")
     * @param nuevoEstado Nuevo estado del diente
     * @param superficiesAfectadas Superficies afectadas (opcional)
     * @param notas Notas u observaciones (opcional)
     * @param tratamientoRealizadoId ID del tratamiento que originó el cambio (opcional)
     * @return DTO del diente actualizado
     */
    OdontogramaDienteDTO actualizarEstadoDiente(
        Long pacienteUsuarioId,
        String numeroDiente,
        String nuevoEstado,
        String superficiesAfectadas,
        String notas,
        Long tratamientoRealizadoId
    );

    /**
     * Actualiza múltiples dientes a la vez.
     * Útil para guardar cambios masivos desde el frontend.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @param cambios Lista de DTOs con los cambios
     * @return Lista de DTOs actualizados
     */
    List<OdontogramaDienteDTO> actualizarMultiplesDientes(
        Long pacienteUsuarioId,
        List<OdontogramaDienteDTO> cambios
    );

    /**
     * Inicializa el odontograma de un paciente con los 32 dientes permanentes
     * en estado SANO.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @return Lista de DTOs de los dientes creados
     */
    List<OdontogramaDienteDTO> inicializarOdontograma(Long pacienteUsuarioId);

    /**
     * Obtiene el historial completo de cambios de un diente específico.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @param numeroDiente Número FDI del diente
     * @return Lista de entradas del historial ordenadas por fecha descendente
     */
    List<OdontogramaHistorial> obtenerHistorialDiente(
        Long pacienteUsuarioId,
        String numeroDiente
    );

    /**
     * Obtiene estadísticas del odontograma del paciente.
     * Incluye contadores de dientes por estado.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @return Mapa con estadísticas (total, sanos, caries, restaurados, etc.)
     */
    Map<String, Integer> obtenerEstadisticas(Long pacienteUsuarioId);

    /**
     * Actualiza automáticamente el odontograma basándose en un tratamiento realizado.
     * Mapea el tipo de procedimiento al estado correspondiente del diente.
     *
     * @param tratamientoRealizadoId ID del tratamiento realizado
     */
    void actualizarDesdeTratamiento(Long tratamientoRealizadoId);
}
