package com.odontoapp.servicio;

import com.odontoapp.dto.TratamientoRealizadoDTO;
import com.odontoapp.entidad.TratamientoRealizado;

import java.util.List;

/**
 * Servicio para la gestión de tratamientos realizados durante las citas.
 * Maneja el registro, consulta y eliminación de tratamientos.
 */
public interface TratamientoRealizadoService {

    /**
     * Registra un nuevo tratamiento realizado durante una cita.
     * Si se especifica un insumo ajustado, se descuenta del inventario.
     *
     * @param dto Datos del tratamiento a registrar
     * @return El tratamiento registrado
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentran las entidades relacionadas
     */
    TratamientoRealizado registrarTratamiento(TratamientoRealizadoDTO dto);

    /**
     * Busca todos los tratamientos realizados en una cita específica.
     *
     * @param citaId ID de la cita
     * @return Lista de tratamientos realizados en la cita
     */
    List<TratamientoRealizado> buscarPorCita(Long citaId);

    /**
     * Busca un tratamiento por su ID.
     *
     * @param id ID del tratamiento
     * @return El tratamiento encontrado
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el tratamiento
     */
    TratamientoRealizado buscarPorId(Long id);

    /**
     * Elimina (soft delete) un tratamiento realizado.
     * Nota: Los tratamientos no tienen soft delete por diseño (registro de auditoría),
     * pero este método podría usarse para validaciones futuras.
     *
     * @param tratamientoId ID del tratamiento a eliminar
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el tratamiento
     */
    void eliminarTratamiento(Long tratamientoId);
}
