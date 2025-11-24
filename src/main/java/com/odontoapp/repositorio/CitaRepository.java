package com.odontoapp.repositorio;

import com.odontoapp.entidad.Cita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    /**
     * Cuenta cuántas citas están asociadas a un procedimiento específico.
     * Útil para validar si un procedimiento puede ser eliminado.
     * @param procedimientoId El ID del Procedimiento.
     * @return El número de citas asociadas a ese procedimiento.
     */
    long countByProcedimientoId(Long procedimientoId);

    /**
     * Busca todas las citas de un paciente específico.
     * @param pacienteId El ID del usuario paciente
     * @param pageable Paginación
     * @return Página de citas del paciente
     */
    @Query("SELECT c FROM Cita c WHERE c.paciente.id = :pacienteId ORDER BY c.fechaHoraInicio DESC")
    Page<Cita> findByPacienteId(@Param("pacienteId") Long pacienteId, Pageable pageable);

    /**
     * Busca todas las citas de un odontólogo específico.
     * @param odontologoId El ID del usuario odontólogo
     * @param pageable Paginación
     * @return Página de citas del odontólogo
     */
    @Query("SELECT c FROM Cita c WHERE c.odontologo.id = :odontologoId ORDER BY c.fechaHoraInicio DESC")
    Page<Cita> findByOdontologoId(@Param("odontologoId") Long odontologoId, Pageable pageable);

    /**
     * Busca citas en un rango de fechas.
     * @param inicio Fecha y hora de inicio del rango
     * @param fin Fecha y hora de fin del rango
     * @return Lista de citas en el rango
     */
    @Query("SELECT c FROM Cita c WHERE c.fechaHoraInicio >= :inicio AND c.fechaHoraInicio <= :fin ORDER BY c.fechaHoraInicio")
    List<Cita> findByFechaHoraInicioBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    /**
     * Busca citas de un odontólogo en un rango de fechas.
     * Útil para verificar disponibilidad y conflictos de horario.
     * @param odontologoId El ID del usuario odontólogo
     * @param inicio Fecha y hora de inicio del rango
     * @param fin Fecha y hora de fin del rango
     * @return Lista de citas del odontólogo en el rango
     */
    @Query("SELECT c FROM Cita c WHERE c.odontologo.id = :odontologoId " +
           "AND c.fechaHoraInicio < :fin AND c.fechaHoraFin > :inicio")
    List<Cita> findConflictingCitas(@Param("odontologoId") Long odontologoId,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fin") LocalDateTime fin);

    /**
     * Busca citas por estado.
     * @param estadoCitaId El ID del estado de cita
     * @param pageable Paginación
     * @return Página de citas con ese estado
     */
    Page<Cita> findByEstadoCitaId(Long estadoCitaId, Pageable pageable);

    /**
     * Cuenta las citas en un rango de fechas (para estadísticas del dashboard)
     * @param inicio Fecha y hora de inicio
     * @param fin Fecha y hora de fin
     * @return Número de citas en el rango
     */
    Long countByFechaHoraInicioBetween(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Busca citas futuras de un paciente específico
     * @param fechaHora Fecha y hora de referencia
     * @param pacienteId ID del usuario paciente
     * @param pageable Paginación
     * @return Página de citas futuras del paciente
     */
    @Query("SELECT c FROM Cita c WHERE c.fechaHoraInicio > :fechaHora AND c.paciente.id = :pacienteId")
    Page<Cita> findByFechaHoraInicioAfterAndPacienteId(
        @Param("fechaHora") LocalDateTime fechaHora,
        @Param("pacienteId") Long pacienteId,
        Pageable pageable
    );

    /**
     * Busca citas pasadas de un paciente específico
     * @param fechaHora Fecha y hora de referencia
     * @param pacienteId ID del usuario paciente
     * @param pageable Paginación
     * @return Página de citas pasadas del paciente
     */
    @Query("SELECT c FROM Cita c WHERE c.fechaHoraInicio < :fechaHora AND c.paciente.id = :pacienteId")
    Page<Cita> findByFechaHoraInicioBeforeAndPacienteId(
        @Param("fechaHora") LocalDateTime fechaHora,
        @Param("pacienteId") Long pacienteId,
        Pageable pageable
    );

    /**
     * Busca citas de un paciente en un rango de fechas
     * @param pacienteId ID del usuario paciente
     * @param inicio Fecha y hora de inicio del rango
     * @param fin Fecha y hora de fin del rango
     * @return Lista de citas del paciente en el rango
     */
    @Query("SELECT c FROM Cita c WHERE c.paciente.id = :pacienteId " +
           "AND c.fechaHoraInicio >= :inicio AND c.fechaHoraInicio <= :fin")
    List<Cita> findByPacienteIdAndFechaHoraInicioBetween(
        @Param("pacienteId") Long pacienteId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    /**
     * Busca citas de un paciente con filtros opcionales (estado, fechas)
     * @param pacienteId ID del usuario paciente
     * @param estadoId ID del estado de cita (opcional)
     * @param fechaDesde Fecha de inicio del rango (opcional)
     * @param fechaHasta Fecha de fin del rango (opcional)
     * @param pageable Paginación
     * @return Página de citas del paciente con filtros aplicados
     */
    @Query("SELECT c FROM Cita c WHERE c.paciente.id = :pacienteId " +
           "AND (:estadoId IS NULL OR c.estadoCita.id = :estadoId) " +
           "AND (:fechaDesde IS NULL OR c.fechaHoraInicio >= :fechaDesde) " +
           "AND (:fechaHasta IS NULL OR c.fechaHoraInicio <= :fechaHasta)")
    Page<Cita> findByPacienteIdWithFilters(
        @Param("pacienteId") Long pacienteId,
        @Param("estadoId") Long estadoId,
        @Param("fechaDesde") LocalDateTime fechaDesde,
        @Param("fechaHasta") LocalDateTime fechaHasta,
        Pageable pageable
    );

    /**
     * Busca la cita generada por un tratamiento desde una cita origen.
     * Permite verificar si una cita ya generó otra cita de tratamiento.
     * @param citaOrigenId ID de la cita origen
     * @return Cita generada por tratamiento, o null si no existe
     */
    @Query("SELECT c FROM Cita c WHERE c.citaGeneradaPorTratamiento.id = :citaOrigenId")
    Cita findByCitaGeneradaPorTratamientoId(@Param("citaOrigenId") Long citaOrigenId);

    /**
     * Cuenta las citas activas (no canceladas ni reprogramadas) de un paciente.
     * Útil para validar si un paciente puede ser eliminado.
     * @param pacienteId ID del usuario paciente
     * @return Número de citas activas del paciente
     */
    @Query("SELECT COUNT(c) FROM Cita c WHERE c.paciente.id = :pacienteId " +
           "AND c.estadoCita.nombre NOT LIKE 'CANCELADA%' " +
           "AND c.estadoCita.nombre != 'REPROGRAMADA' " +
           "AND c.estadoCita.nombre != 'NO_ASISTIO'")
    long countCitasActivas(@Param("pacienteId") Long pacienteId);

    /**
     * Cuenta las citas activas (no canceladas ni reprogramadas) de un odontólogo.
     * Útil para validar si un odontólogo puede ser eliminado o desactivado.
     * @param odontologoId ID del usuario odontólogo
     * @return Número de citas activas del odontólogo
     */
    @Query("SELECT COUNT(c) FROM Cita c WHERE c.odontologo.id = :odontologoId " +
           "AND c.estadoCita.nombre NOT LIKE 'CANCELADA%' " +
           "AND c.estadoCita.nombre != 'REPROGRAMADA' " +
           "AND c.estadoCita.nombre != 'NO_ASISTIO'")
    long countCitasActivasByOdontologo(@Param("odontologoId") Long odontologoId);
}
