package com.odontoapp.servicio;

import com.odontoapp.entidad.Cita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Servicio para la gestión de citas en la clínica dental.
 * Maneja la disponibilidad, agendamiento, reprogramación, cancelación y seguimiento de citas.
 */
public interface CitaService {

    /**
     * Busca los horarios disponibles de un odontólogo en una fecha específica.
     * Considera el horario regular del odontólogo y las excepciones de horario.
     *
     * @param odontologoId ID del odontólogo
     * @param fecha Fecha para buscar disponibilidad
     * @return Mapa con los horarios disponibles y ocupados
     */
    Map<String, Object> buscarDisponibilidad(Long odontologoId, LocalDate fecha);

    /**
     * Busca los horarios disponibles de un odontólogo en una fecha específica,
     * excluyendo una cita específica (útil para reprogramación).
     *
     * @param odontologoId ID del odontólogo
     * @param fecha Fecha para buscar disponibilidad
     * @param citaIdExcluir ID de la cita a excluir (puede ser null)
     * @return Mapa con los horarios disponibles y ocupados
     */
    Map<String, Object> buscarDisponibilidad(Long odontologoId, LocalDate fecha, Long citaIdExcluir);

    /**
     * Busca los horarios disponibles de un odontólogo en una fecha específica,
     * considerando la duración del procedimiento y excluyendo una cita específica.
     *
     * @param odontologoId ID del odontólogo
     * @param fecha Fecha para buscar disponibilidad
     * @param duracionMinutos Duración del procedimiento en minutos
     * @param citaIdExcluir ID de la cita a excluir (puede ser null)
     * @return Mapa con los horarios disponibles y ocupados
     */
    Map<String, Object> buscarDisponibilidad(Long odontologoId, LocalDate fecha, Integer duracionMinutos, Long citaIdExcluir);

    /**
     * Agenda una nueva cita para un paciente.
     * Valida la disponibilidad del odontólogo y detecta conflictos de horario.
     *
     * @param pacienteId ID del paciente
     * @param odontologoId ID del odontólogo
     * @param procedimientoId ID del procedimiento a realizar
     * @param fechaHoraInicio Fecha y hora de inicio de la cita
     * @param motivoConsulta Motivo de la consulta
     * @param notas Notas adicionales (opcional)
     * @return La cita creada con estado PENDIENTE
     * @throws IllegalStateException si hay conflicto de horarios
     * @throws IllegalArgumentException si los parámetros no son válidos
     */
    Cita agendarCita(Long pacienteId, Long odontologoId, Long procedimientoId,
                     LocalDateTime fechaHoraInicio, String motivoConsulta, String notas);

    /**
     * Reprograma una cita existente a un nuevo horario.
     * Crea una nueva cita y marca la anterior como REPROGRAMADA.
     *
     * @param citaId ID de la cita a reprogramar
     * @param nuevaFechaHoraInicio Nueva fecha y hora de inicio
     * @param motivo Motivo de la reprogramación
     * @return La nueva cita creada
     * @throws IllegalStateException si la cita no puede ser reprogramada
     */
    Cita reprogramarCita(Long citaId, LocalDateTime nuevaFechaHoraInicio, String motivo);

    /**
     * Cancela una cita existente.
     * Marca la cita como CANCELADA_PACIENTE o CANCELADA_CLINICA según corresponda.
     *
     * @param citaId ID de la cita a cancelar
     * @param esPaciente true si la cancelación es por el paciente, false si es por la clínica
     * @param motivo Motivo de la cancelación
     * @return La cita cancelada
     * @throws IllegalStateException si la cita no puede ser cancelada
     */
    Cita cancelarCita(Long citaId, boolean esPaciente, String motivo);

    /**
     * Confirma una cita pendiente.
     * Cambia el estado de PENDIENTE a CONFIRMADA.
     *
     * @param citaId ID de la cita a confirmar
     * @return La cita confirmada
     * @throws IllegalStateException si la cita no está en estado PENDIENTE
     */
    Cita confirmarCita(Long citaId);

    /**
     * Marca la asistencia de un paciente a una cita.
     * Cambia el estado a ASISTIO o NO_ASISTIO.
     *
     * @param citaId ID de la cita
     * @param asistio true si el paciente asistió, false si no asistió
     * @param notas Notas adicionales sobre la asistencia
     * @return La cita con el estado de asistencia actualizado
     * @throws IllegalStateException si la cita no está confirmada o si ya se marcó asistencia
     */
    Cita marcarAsistencia(Long citaId, boolean asistio, String notas);

    /**
     * Busca una cita por su ID.
     *
     * @param id ID de la cita
     * @return La cita encontrada
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra la cita
     */
    Cita buscarPorId(Long id);

    /**
     * Busca todas las citas de un paciente específico.
     *
     * @param pacienteId ID del paciente
     * @param pageable Configuración de paginación
     * @return Página de citas del paciente
     */
    Page<Cita> buscarCitasPorPaciente(Long pacienteId, Pageable pageable);

    /**
     * Busca las citas de un odontólogo en una fecha específica.
     *
     * @param odontologoId ID del odontólogo
     * @param fecha Fecha para buscar citas (opcional, si es null busca todas)
     * @param pageable Configuración de paginación
     * @return Página de citas del odontólogo
     */
    Page<Cita> buscarCitasPorOdontologo(Long odontologoId, LocalDate fecha, Pageable pageable);

    /**
     * Busca todas las citas en un rango de fechas para mostrar en el calendario.
     * Útil para vistas de calendario mensual o semanal.
     *
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @param odontologoId ID del odontólogo (opcional, para filtrar por odontólogo)
     * @return Lista de citas en el rango de fechas
     */
    List<Cita> buscarCitasParaCalendario(LocalDate fechaInicio, LocalDate fechaFin, Long odontologoId);

    /**
     * Lista todas las citas con filtros opcionales y paginación.
     *
     * @param estadoId ID del estado para filtrar (opcional)
     * @param odontologoId ID del odontólogo para filtrar (opcional)
     * @param fechaDesde Fecha desde para filtrar (opcional)
     * @param fechaHasta Fecha hasta para filtrar (opcional)
     * @param pageable Configuración de paginación y ordenamiento
     * @return Página de citas filtradas
     */
    Page<Cita> listarCitasConFiltros(Long estadoId, Long odontologoId,
                                      LocalDate fechaDesde, LocalDate fechaHasta,
                                      Pageable pageable);
}
