package com.odontoapp.repositorio;

import com.odontoapp.entidad.TratamientoRealizado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TratamientoRealizadoRepository extends JpaRepository<TratamientoRealizado, Long> {

    /**
     * Busca todos los tratamientos realizados asociados a una cita específica.
     * 
     * @param citaId El ID de la cita
     * @return Lista de tratamientos realizados ordenados por fecha de realización
     *         descendente
     */
    List<TratamientoRealizado> findByCitaIdOrderByFechaRealizacionDesc(Long citaId);

    /**
     * Busca todos los tratamientos realizados asociados a una cita específica.
     * Método simplificado sin ordenamiento específico.
     * 
     * @param citaId El ID de la cita
     * @return Lista de tratamientos realizados
     */
    List<TratamientoRealizado> findByCitaId(Long citaId);

    /**
     * Busca todos los tratamientos realizados de un paciente con paginación.
     * 
     * @param pacienteId El ID del paciente
     * @param pageable   Información de paginación y ordenamiento
     * @return Página de tratamientos realizados
     */
    @Query("SELECT t FROM TratamientoRealizado t WHERE t.cita.paciente.id = :pacienteId")
    Page<TratamientoRealizado> findByPacienteId(@Param("pacienteId") Long pacienteId, Pageable pageable);
    // --- Consultas para Reportes ---

    @Query("SELECT new com.odontoapp.dto.ReporteDTO(t.procedimiento.nombre, COUNT(t)) " +
            "FROM TratamientoRealizado t " +
            "WHERE t.fechaRealizacion BETWEEN :fechaInicio AND :fechaFin " +
            "AND (:odontologoId IS NULL OR t.odontologo.id = :odontologoId) " +
            "GROUP BY t.procedimiento.nombre " +
            "ORDER BY COUNT(t) DESC")
    List<com.odontoapp.dto.ReporteDTO> obtenerTopTratamientos(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("odontologoId") Long odontologoId,
            Pageable pageable);
}
