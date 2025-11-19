package com.odontoapp.repositorio;

import com.odontoapp.entidad.TratamientoPlanificado;
import com.odontoapp.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TratamientoPlanificadoRepository extends JpaRepository<TratamientoPlanificado, Long> {

    /**
     * Obtener todos los tratamientos planificados de un paciente por ID
     */
    List<TratamientoPlanificado> findByPacienteId(Long pacienteId);

    /**
     * Obtener todos los tratamientos planificados de un paciente
     */
    List<TratamientoPlanificado> findByPacienteOrderByFechaPlanificadaDesc(Usuario paciente);

    /**
     * Obtener tratamientos planificados por estado
     */
    List<TratamientoPlanificado> findByPacienteAndEstadoOrderByFechaPlanificadaAsc(Usuario paciente, String estado);

    /**
     * Obtener tratamientos PLANIFICADOS y EN_CURSO de un paciente.
     * IMPORTANTE: Este query EXCLUYE tratamientos con estado 'COMPLETADO',
     * evitando duplicación con los tratamientos realizados.
     * Estados válidos: PLANIFICADO, EN_CURSO
     * Estados excluidos: COMPLETADO, CANCELADO, etc.
     */
    @Query("SELECT t FROM TratamientoPlanificado t WHERE t.paciente = :paciente " +
           "AND t.estado IN ('PLANIFICADO', 'EN_CURSO') " +
           "ORDER BY t.fechaPlanificada ASC")
    List<TratamientoPlanificado> findTratamientosPendientes(@Param("paciente") Usuario paciente);

    /**
     * Obtener tratamientos planificados de un odontólogo
     */
    List<TratamientoPlanificado> findByOdontologoAndEstadoOrderByFechaPlanificadaAsc(Usuario odontologo, String estado);

    /**
     * Obtener tratamiento planificado asociado a una cita
     */
    @Query("SELECT t FROM TratamientoPlanificado t WHERE t.citaAsociada.id = :citaId")
    TratamientoPlanificado findByCitaAsociadaId(@Param("citaId") Long citaId);

    /**
     * Buscar tratamientos planificados por paciente, procedimiento y estado
     * Útil para encontrar tratamientos pendientes y marcarlos como completados
     */
    @Query("SELECT t FROM TratamientoPlanificado t WHERE t.paciente = :paciente " +
           "AND t.procedimiento = :procedimiento AND t.estado = :estado " +
           "ORDER BY t.fechaCreacion DESC")
    List<TratamientoPlanificado> findByPacienteAndProcedimientoAndEstado(
            @Param("paciente") Usuario paciente,
            @Param("procedimiento") com.odontoapp.entidad.Procedimiento procedimiento,
            @Param("estado") String estado);
}
