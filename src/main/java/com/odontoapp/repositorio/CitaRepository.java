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
}
