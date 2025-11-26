package com.odontoapp.servicio.impl;

import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.EstadoCita;
import com.odontoapp.entidad.HorarioExcepcion;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.entidad.TratamientoPlanificado;
import com.odontoapp.entidad.TratamientoRealizado;
import com.odontoapp.entidad.MovimientoInventario;
import com.odontoapp.entidad.TipoMovimiento;
import com.odontoapp.entidad.MotivoMovimiento;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.EstadoCitaRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.repositorio.TratamientoPlanificadoRepository;
import com.odontoapp.repositorio.TratamientoRealizadoRepository;
import com.odontoapp.repositorio.MovimientoInventarioRepository;
import com.odontoapp.repositorio.TipoMovimientoRepository;
import com.odontoapp.repositorio.MotivoMovimientoRepository;
import com.odontoapp.servicio.CitaService;
import com.odontoapp.servicio.EmailService;
import com.odontoapp.servicio.InventarioService;
import com.odontoapp.servicio.FacturacionService;
import java.math.BigDecimal;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión de citas.
 * Maneja toda la lógica de negocio relacionada con citas dentales.
 */
@Service
@Transactional
public class CitaServiceImpl implements CitaService {

    // --- Constantes para nombres de estados ---
    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    private static final String ESTADO_CANCELADA_PACIENTE = "CANCELADA_PACIENTE";
    private static final String ESTADO_CANCELADA_CLINICA = "CANCELADA_CLINICA";
    private static final String ESTADO_ASISTIO = "ASISTIO";
    private static final String ESTADO_NO_ASISTIO = "NO_ASISTIO";
    private static final String ESTADO_REPROGRAMADA = "REPROGRAMADA";

    private static final String NO_LABORABLE = "NO_LABORABLE";
    private static final int INTERVALO_MINUTOS = 15; // Intervalos de 15 minutos (para permitir slots a las 8:00, 8:15,
                                                     // 8:30, 8:45, etc.)
    private static final int BUFFER_MINUTOS = 15; // Buffer de 15 minutos después de cada cita

    // --- Dependencias ---
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final EmailService emailService;
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
    private final InsumoRepository insumoRepository;
    private final TratamientoPlanificadoRepository tratamientoPlanificadoRepository;
    private final TratamientoRealizadoRepository tratamientoRealizadoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final InventarioService inventarioService;
    private final FacturacionService facturacionService;

    public CitaServiceImpl(CitaRepository citaRepository,
            UsuarioRepository usuarioRepository,
            ProcedimientoRepository procedimientoRepository,
            EstadoCitaRepository estadoCitaRepository,
            EmailService emailService,
            ProcedimientoInsumoRepository procedimientoInsumoRepository,
            InsumoRepository insumoRepository,
            TratamientoPlanificadoRepository tratamientoPlanificadoRepository,
            TratamientoRealizadoRepository tratamientoRealizadoRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            TipoMovimientoRepository tipoMovimientoRepository,
            MotivoMovimientoRepository motivoMovimientoRepository,
            InventarioService inventarioService,
            FacturacionService facturacionService) {
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.emailService = emailService;
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
        this.insumoRepository = insumoRepository;
        this.tratamientoPlanificadoRepository = tratamientoPlanificadoRepository;
        this.tratamientoRealizadoRepository = tratamientoRealizadoRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
        this.inventarioService = inventarioService;
        this.facturacionService = facturacionService;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buscarDisponibilidad(Long odontologoId, LocalDate fecha) {
        return buscarDisponibilidad(odontologoId, fecha, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buscarDisponibilidad(Long odontologoId, LocalDate fecha, Long citaIdExcluir) {
        return buscarDisponibilidad(odontologoId, fecha, INTERVALO_MINUTOS, citaIdExcluir);
    }

    /**
     * Buscar disponibilidad de horarios considerando la duración del procedimiento
     */
    @Transactional(readOnly = true)
    public Map<String, Object> buscarDisponibilidad(Long odontologoId, LocalDate fecha, Integer duracionMinutos,
            Long citaIdExcluir) {
        Usuario odontologo = usuarioRepository.findById(odontologoId)
                .orElseThrow(() -> new EntityNotFoundException("Odontólogo no encontrado con ID: " + odontologoId));

        // Usar duración por defecto si no se especifica o es inválida
        if (duracionMinutos == null || duracionMinutos <= 0) {
            duracionMinutos = INTERVALO_MINUTOS;
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("fecha", fecha);
        resultado.put("odontologoId", odontologoId);
        resultado.put("odontologoNombre", odontologo.getNombreCompleto());
        resultado.put("duracionMinutos", duracionMinutos);

        // Verificar si hay excepción de horario para esta fecha
        HorarioExcepcion excepcion = odontologo.getExcepcionesHorario().stream()
                .filter(e -> e.getFecha().equals(fecha))
                .findFirst()
                .orElse(null);

        if (excepcion != null) {
            if (NO_LABORABLE.equals(excepcion.getHoras())) {
                resultado.put("disponible", false);
                resultado.put("motivo", excepcion.getMotivo() != null ? excepcion.getMotivo() : "Día no laborable");
                resultado.put("horariosDisponibles", Collections.emptyList());
                return resultado;
            }
            // Usar horario de excepción
            resultado.put("disponible", true);
            resultado.put("esExcepcion", true);
            resultado.put("motivoExcepcion", excepcion.getMotivo());
            resultado.put("horariosDisponibles", calcularHorariosDisponibles(
                    odontologo, fecha, excepcion.getHoras(), duracionMinutos, citaIdExcluir));
            return resultado;
        }

        // Usar horario regular
        DayOfWeek diaSemana = fecha.getDayOfWeek();
        String horarioDelDia = odontologo.getHorarioRegular().get(diaSemana);

        if (horarioDelDia == null || horarioDelDia.isEmpty()) {
            resultado.put("disponible", false);
            resultado.put("motivo", "Sin horario regular configurado para este día");
            resultado.put("horariosDisponibles", Collections.emptyList());
            return resultado;
        }

        resultado.put("disponible", true);
        resultado.put("esExcepcion", false);
        resultado.put("horariosDisponibles", calcularHorariosDisponibles(
                odontologo, fecha, horarioDelDia, duracionMinutos, citaIdExcluir));
        return resultado;
    }

    /**
     * Calcula los horarios disponibles considerando las citas ya agendadas y la
     * duración del procedimiento.
     * 
     * @param duracionMinutos Duración del procedimiento en minutos
     * @param citaIdExcluir   ID de cita a excluir (puede ser null)
     */
    private List<Map<String, Object>> calcularHorariosDisponibles(Usuario odontologo, LocalDate fecha,
            String horarioStr, Integer duracionMinutos, Long citaIdExcluir) {
        List<Map<String, Object>> slots = new ArrayList<>();

        // Usar duración por defecto si no se especifica
        if (duracionMinutos == null || duracionMinutos <= 0) {
            duracionMinutos = INTERVALO_MINUTOS;
        }

        // Parsear los intervalos del horario (ej: "09:00-13:00,15:00-19:00")
        String[] intervalos = horarioStr.split(",");

        for (String intervalo : intervalos) {
            String[] partes = intervalo.trim().split("-");
            if (partes.length != 2)
                continue;

            LocalTime horaInicio = LocalTime.parse(partes[0].trim());
            LocalTime horaFin = LocalTime.parse(partes[1].trim());

            LocalDateTime inicioIntervalo = LocalDateTime.of(fecha, horaInicio);
            LocalDateTime finIntervalo = LocalDateTime.of(fecha, horaFin);

            // Obtener citas del odontólogo en este intervalo
            List<Cita> citasEnIntervalo = citaRepository.findConflictingCitas(
                    odontologo.getId(), inicioIntervalo, finIntervalo);

            // Filtrar solo citas activas (excluir canceladas, reprogramadas y la cita a
            // excluir)
            citasEnIntervalo = citasEnIntervalo.stream()
                    .filter(c -> {
                        String estado = c.getEstadoCita().getNombre();
                        boolean esActiva = !estado.startsWith("CANCELADA") && !estado.equals("REPROGRAMADA");

                        // Excluir la cita específica si se proporcionó un ID
                        if (citaIdExcluir != null && c.getId().equals(citaIdExcluir)) {
                            return false;
                        }

                        return esActiva;
                    })
                    .collect(Collectors.toList());

            // Generar slots de tiempo cada 30 minutos
            LocalDateTime slotActual = inicioIntervalo;
            while (slotActual.plusMinutes(INTERVALO_MINUTOS).isBefore(finIntervalo) ||
                    slotActual.plusMinutes(INTERVALO_MINUTOS).equals(finIntervalo)) {

                LocalDateTime finSlot = slotActual.plusMinutes(INTERVALO_MINUTOS);

                // Verificar si hay suficiente tiempo para el procedimiento completo
                // El slot debe tener espacio para la duración del procedimiento + buffer
                LocalDateTime finProcedimiento = slotActual.plusMinutes(duracionMinutos);

                // Verificar que el procedimiento no exceda el horario laboral
                boolean cabeEnHorario = !finProcedimiento.isAfter(finIntervalo);

                // Verificar que no esté ocupado (esto ya considera el buffer de 15 minutos)
                boolean ocupado = !cabeEnHorario || estaOcupado(slotActual, finProcedimiento, citasEnIntervalo);

                Map<String, Object> slot = new HashMap<>();
                slot.put("inicio", slotActual.format(DateTimeFormatter.ofPattern("HH:mm")));
                slot.put("fin", finSlot.format(DateTimeFormatter.ofPattern("HH:mm")));
                slot.put("disponible", !ocupado);

                slots.add(slot);
                slotActual = finSlot;
            }
        }

        return slots;
    }

    /**
     * Verifica si un slot de tiempo está ocupado por alguna cita.
     * Incluye un buffer de 15 minutos después de cada cita.
     */
    private boolean estaOcupado(LocalDateTime inicio, LocalDateTime fin, List<Cita> citas) {
        for (Cita cita : citas) {
            // Agregar buffer de 15 minutos al final de la cita
            LocalDateTime finConBuffer = cita.getFechaHoraFin().plusMinutes(BUFFER_MINUTOS);

            // Hay conflicto si los intervalos se solapan (considerando el buffer)
            if (inicio.isBefore(finConBuffer) && fin.isAfter(cita.getFechaHoraInicio())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si el horario de la cita está dentro del horario laboral del
     * odontólogo.
     * Considera tanto el horario regular como las excepciones.
     */
    private boolean estaEnHorarioLaboral(Usuario odontologo, LocalDateTime inicio, LocalDateTime fin) {
        LocalDate fecha = inicio.toLocalDate();
        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();

        // Verificar si hay excepción de horario
        HorarioExcepcion excepcion = odontologo.getExcepcionesHorario().stream()
                .filter(e -> e.getFecha().equals(fecha))
                .findFirst()
                .orElse(null);

        String horarioStr;
        if (excepcion != null) {
            // Si es día no laborable, no está disponible
            if (NO_LABORABLE.equals(excepcion.getHoras())) {
                return false;
            }
            horarioStr = excepcion.getHoras();
        } else {
            // Usar horario regular
            DayOfWeek diaSemana = fecha.getDayOfWeek();
            horarioStr = odontologo.getHorarioRegular().get(diaSemana);

            if (horarioStr == null || horarioStr.isEmpty()) {
                return false; // Sin horario configurado para este día
            }
        }

        // Parsear intervalos del horario (ej: "09:00-13:00,15:00-19:00")
        String[] intervalos = horarioStr.split(",");

        for (String intervalo : intervalos) {
            String[] partes = intervalo.trim().split("-");
            if (partes.length != 2)
                continue;

            LocalTime horarioInicioIntervalo = LocalTime.parse(partes[0].trim());
            LocalTime horarioFinIntervalo = LocalTime.parse(partes[1].trim());

            // Verificar si el horario de la cita está dentro de este intervalo
            if (!horaInicio.isBefore(horarioInicioIntervalo) && !horaFin.isAfter(horarioFinIntervalo)) {
                return true; // Está dentro del rango
            }
        }

        return false; // No está en ningún intervalo laboral
    }

    @Override
    public Cita agendarCita(Long pacienteId, Long odontologoId, Long procedimientoId,
            LocalDateTime fechaHoraInicio, String motivoConsulta, String notas) {

        // Validar que todos los parámetros requeridos estén presentes
        if (pacienteId == null || odontologoId == null || procedimientoId == null || fechaHoraInicio == null) {
            throw new IllegalArgumentException("Todos los parámetros requeridos deben estar presentes");
        }

        // Buscar entidades relacionadas
        Usuario paciente = usuarioRepository.findById(pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado con ID: " + pacienteId));

        Usuario odontologo = usuarioRepository.findById(odontologoId)
                .orElseThrow(() -> new EntityNotFoundException("Odontólogo no encontrado con ID: " + odontologoId));

        Procedimiento procedimiento = procedimientoRepository.findById(procedimientoId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Procedimiento no encontrado con ID: " + procedimientoId));

        // Calcular fecha de fin basada en la duración del procedimiento
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(procedimiento.getDuracionBaseMinutos());

        // Verificar que la fecha no sea en el pasado
        if (fechaHoraInicio.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("No se puede agendar una cita en el pasado");
        }

        // Verificar disponibilidad del odontólogo
        // NOTA: No agregamos el buffer aquí porque estaOcupado() ya lo considera
        // internamente
        List<Cita> citasConflictivas = citaRepository.findConflictingCitas(
                odontologoId, fechaHoraInicio, fechaHoraFin);

        // Filtrar solo citas activas (no canceladas ni reprogramadas)
        List<Cita> citasActivas = citasConflictivas.stream()
                .filter(c -> {
                    String estado = c.getEstadoCita().getNombre();
                    return !estado.startsWith("CANCELADA") && !estado.equals("REPROGRAMADA");
                })
                .collect(Collectors.toList());

        // Verificar conflictos usando el método que considera el buffer internamente
        if (estaOcupado(fechaHoraInicio, fechaHoraFin, citasActivas)) {
            throw new IllegalStateException(
                    "El odontólogo no está disponible en ese horario. " +
                            "Recuerde que se requiere un tiempo de buffer de " + BUFFER_MINUTOS +
                            " minutos después de cada cita.");
        }

        // Verificar que el PACIENTE no tenga otra cita en el mismo horario (con
        // cualquier odontólogo)
        List<Cita> citasPaciente = citaRepository.findConflictingCitas(
                pacienteId, fechaHoraInicio, fechaHoraFin);

        List<Cita> citasActivasPaciente = citasPaciente.stream()
                .filter(c -> {
                    String estado = c.getEstadoCita().getNombre();
                    return !estado.startsWith("CANCELADA") && !estado.equals("REPROGRAMADA");
                })
                .collect(Collectors.toList());

        if (!citasActivasPaciente.isEmpty()) {
            throw new IllegalStateException(
                    "El paciente ya tiene una cita agendada en este horario con otro odontólogo. " +
                            "Por favor seleccione otro horario.");
        }

        // Verificar que el paciente esté activo
        if (!paciente.isEstaActivo()) {
            throw new IllegalStateException(
                    "No se puede agendar una cita para un paciente inactivo. " +
                            "Por favor active el paciente primero.");
        }

        // Verificar que el horario esté dentro del horario laboral del odontólogo
        LocalDate fecha = fechaHoraInicio.toLocalDate();
        Map<String, Object> disponibilidad = buscarDisponibilidad(odontologoId, fecha);

        if (!(boolean) disponibilidad.get("disponible")) {
            throw new IllegalStateException("El odontólogo no está disponible en esta fecha: " +
                    disponibilidad.get("motivo"));
        }

        // Validar que el horario específico esté dentro de los rangos laborales
        if (!estaEnHorarioLaboral(odontologo, fechaHoraInicio, fechaHoraFin)) {
            throw new IllegalStateException(
                    "El horario seleccionado está fuera del horario laboral del odontólogo. " +
                            "Por favor seleccione un horario dentro de las horas de atención.");
        }

        // Crear la cita como CONFIRMADA (las citas presenciales se crean confirmadas)
        EstadoCita estadoConfirmada = estadoCitaRepository.findByNombre(ESTADO_CONFIRMADA)
                .orElseThrow(() -> new IllegalStateException("Estado CONFIRMADA no encontrado en la base de datos"));

        Cita nuevaCita = new Cita();
        nuevaCita.setPaciente(paciente);
        nuevaCita.setOdontologo(odontologo);
        nuevaCita.setProcedimiento(procedimiento);
        nuevaCita.setFechaHoraInicio(fechaHoraInicio);
        nuevaCita.setFechaHoraFin(fechaHoraFin);
        nuevaCita.setDuracionEstimadaMinutos(procedimiento.getDuracionBaseMinutos());
        nuevaCita.setEstadoCita(estadoConfirmada);
        nuevaCita.setMotivoConsulta(motivoConsulta);
        nuevaCita.setNotas(notas);

        return citaRepository.save(nuevaCita);
    }

    @Override
    public Cita agendarCitaPaciente(Long pacienteId, Long odontologoId, Long procedimientoId,
            LocalDateTime fechaHoraInicio, String motivoConsulta, String notas) {

        // Validar que todos los parámetros requeridos estén presentes
        if (pacienteId == null || odontologoId == null || procedimientoId == null || fechaHoraInicio == null) {
            throw new IllegalArgumentException("Todos los parámetros requeridos deben estar presentes");
        }

        // Buscar entidades relacionadas
        Usuario paciente = usuarioRepository.findById(pacienteId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado con ID: " + pacienteId));

        Usuario odontologo = usuarioRepository.findById(odontologoId)
                .orElseThrow(() -> new EntityNotFoundException("Odontólogo no encontrado con ID: " + odontologoId));

        Procedimiento procedimiento = procedimientoRepository.findById(procedimientoId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Procedimiento no encontrado con ID: " + procedimientoId));

        // Calcular fecha de fin basada en la duración del procedimiento
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(procedimiento.getDuracionBaseMinutos());

        // Verificar que la fecha no sea en el pasado
        if (fechaHoraInicio.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("No se puede agendar una cita en el pasado");
        }

        // Verificar disponibilidad del odontólogo
        List<Cita> citasConflictivas = citaRepository.findConflictingCitas(
                odontologoId, fechaHoraInicio, fechaHoraFin);

        // Filtrar solo citas activas (no canceladas ni reprogramadas)
        List<Cita> citasActivas = citasConflictivas.stream()
                .filter(c -> {
                    String estado = c.getEstadoCita().getNombre();
                    return !estado.startsWith("CANCELADA") && !estado.equals("REPROGRAMADA");
                })
                .collect(Collectors.toList());

        // Verificar conflictos usando el método que considera el buffer internamente
        if (estaOcupado(fechaHoraInicio, fechaHoraFin, citasActivas)) {
            throw new IllegalStateException(
                    "El odontólogo no está disponible en ese horario. " +
                            "Recuerde que se requiere un tiempo de buffer de " + BUFFER_MINUTOS +
                            " minutos después de cada cita.");
        }

        // Verificar que el PACIENTE no tenga otra cita en el mismo horario (con
        // cualquier odontólogo)
        List<Cita> citasPaciente = citaRepository.findConflictingCitas(
                pacienteId, fechaHoraInicio, fechaHoraFin);

        List<Cita> citasActivasPaciente = citasPaciente.stream()
                .filter(c -> {
                    String estado = c.getEstadoCita().getNombre();
                    return !estado.startsWith("CANCELADA") && !estado.equals("REPROGRAMADA");
                })
                .collect(Collectors.toList());

        if (!citasActivasPaciente.isEmpty()) {
            throw new IllegalStateException(
                    "Ya tienes una cita agendada en este horario. " +
                            "Por favor selecciona otro horario.");
        }

        // Verificar que el paciente esté activo
        if (!paciente.isEstaActivo()) {
            throw new IllegalStateException(
                    "No se puede agendar una cita para un paciente inactivo. " +
                            "Por favor contacta con la clínica.");
        }

        // Verificar que el horario esté dentro del horario laboral del odontólogo
        LocalDate fecha = fechaHoraInicio.toLocalDate();
        Map<String, Object> disponibilidad = buscarDisponibilidad(odontologoId, fecha);

        if (!(boolean) disponibilidad.get("disponible")) {
            throw new IllegalStateException("El odontólogo no está disponible en esta fecha: " +
                    disponibilidad.get("motivo"));
        }

        // Validar que el horario específico esté dentro de los rangos laborales
        if (!estaEnHorarioLaboral(odontologo, fechaHoraInicio, fechaHoraFin)) {
            throw new IllegalStateException(
                    "El horario seleccionado está fuera del horario laboral del odontólogo. " +
                            "Por favor selecciona un horario dentro de las horas de atención.");
        }

        // Crear la cita como PENDIENTE (las citas del portal de pacientes requieren
        // confirmación)
        EstadoCita estadoPendiente = estadoCitaRepository.findByNombre(ESTADO_PENDIENTE)
                .orElseThrow(() -> new IllegalStateException("Estado PENDIENTE no encontrado en la base de datos"));

        Cita nuevaCita = new Cita();
        nuevaCita.setPaciente(paciente);
        nuevaCita.setOdontologo(odontologo);
        nuevaCita.setProcedimiento(procedimiento);
        nuevaCita.setFechaHoraInicio(fechaHoraInicio);
        nuevaCita.setFechaHoraFin(fechaHoraFin);
        nuevaCita.setDuracionEstimadaMinutos(procedimiento.getDuracionBaseMinutos());
        nuevaCita.setEstadoCita(estadoPendiente);
        nuevaCita.setMotivoConsulta(motivoConsulta);
        nuevaCita.setNotas(notas);

        Cita citaGuardada = citaRepository.save(nuevaCita);

        // Enviar email de notificación al paciente
        try {
            emailService.enviarConfirmacionCita(citaGuardada);
        } catch (Exception e) {
            System.err.println("Error al enviar email de confirmación: " + e.getMessage());
            // No lanzar excepción, la cita ya fue creada exitosamente
        }

        return citaGuardada;
    }

    @Override
    public Cita reprogramarCita(Long citaId, Long nuevoOdontologoId, LocalDateTime nuevaFechaHoraInicio,
            String motivo) {
        Cita citaOriginal = citaRepository.findById(citaId)
                .orElseThrow(() -> new EntityNotFoundException("Cita no encontrada con ID: " + citaId));

        // Validar que la cita pueda ser reprogramada
        String estadoActual = citaOriginal.getEstadoCita().getNombre();
        if (estadoActual.equals(ESTADO_ASISTIO) || estadoActual.equals(ESTADO_NO_ASISTIO)) {
            throw new IllegalStateException("No se puede reprogramar una cita que ya fue atendida");
        }

        if (estadoActual.equals(ESTADO_REPROGRAMADA)) {
            throw new IllegalStateException("Esta cita ya fue reprogramada");
        }

        // Usar el nuevo odontólogo si se proporciona, de lo contrario mantener el
        // original
        Long odontologoIdFinal = (nuevoOdontologoId != null) ? nuevoOdontologoId : citaOriginal.getOdontologo().getId();

        // Crear nueva cita con los mismos datos pero nueva fecha y posiblemente nuevo
        // odontólogo
        Cita nuevaCita = agendarCita(
                citaOriginal.getPaciente().getId(),
                odontologoIdFinal,
                citaOriginal.getProcedimiento().getId(),
                nuevaFechaHoraInicio,
                citaOriginal.getMotivoConsulta(),
                "Reprogramación: " + (motivo != null ? motivo : "Sin motivo especificado"));

        // Marcar la cita original como REPROGRAMADA
        EstadoCita estadoReprogramada = estadoCitaRepository.findByNombre(ESTADO_REPROGRAMADA)
                .orElseThrow(() -> new IllegalStateException("Estado REPROGRAMADA no encontrado"));

        citaOriginal.setEstadoCita(estadoReprogramada);
        citaOriginal.setMotivoCancelacion(motivo);
        citaOriginal.setCitaReprogramada(nuevaCita);
        citaRepository.save(citaOriginal);

        // Enviar email de reprogramación al paciente
        try {
            emailService.enviarReprogramacionCita(citaOriginal, nuevaCita);
        } catch (Exception e) {
            System.err.println("Error al enviar email de reprogramación: " + e.getMessage());
        }

        return nuevaCita;
    }

    @Override
    public Cita cancelarCita(Long citaId, boolean esPaciente, String motivo) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new EntityNotFoundException("Cita no encontrada con ID: " + citaId));

        // Validar que la cita pueda ser cancelada
        String estadoActual = cita.getEstadoCita().getNombre();
        if (estadoActual.equals(ESTADO_ASISTIO) || estadoActual.equals(ESTADO_NO_ASISTIO)) {
            throw new IllegalStateException("No se puede cancelar una cita que ya fue atendida");
        }

        if (estadoActual.startsWith("CANCELADA")) {
            throw new IllegalStateException("Esta cita ya está cancelada");
        }

        if (estadoActual.equals(ESTADO_REPROGRAMADA)) {
            throw new IllegalStateException("Esta cita ya fue reprogramada");
        }

        // Determinar el estado de cancelación
        String nombreEstado = esPaciente ? ESTADO_CANCELADA_PACIENTE : ESTADO_CANCELADA_CLINICA;
        EstadoCita estadoCancelada = estadoCitaRepository.findByNombre(nombreEstado)
                .orElseThrow(() -> new IllegalStateException("Estado " + nombreEstado + " no encontrado"));

        cita.setEstadoCita(estadoCancelada);
        cita.setMotivoCancelacion(motivo);

        // Si hay tratamiento planificado asociado, volver a PLANIFICADO
        TratamientoPlanificado tratamientoPlanificado = tratamientoPlanificadoRepository.findByCitaAsociadaId(citaId);
        if (tratamientoPlanificado != null && "EN_CURSO".equals(tratamientoPlanificado.getEstado())) {
            tratamientoPlanificado.setEstado("PLANIFICADO");
            tratamientoPlanificado.setCitaAsociada(null); // Desvincular la cita cancelada
            tratamientoPlanificadoRepository.save(tratamientoPlanificado);
            System.out.println("⚠️ Cita cancelada - Tratamiento planificado vuelto a PLANIFICADO para cita: " + citaId);
        }

        Cita citaCancelada = citaRepository.save(cita);

        // Enviar email de cancelación al paciente
        try {
            emailService.enviarCancelacionCita(citaCancelada, motivo);
        } catch (Exception e) {
            System.err.println("Error al enviar email de cancelación: " + e.getMessage());
        }

        return citaCancelada;
    }

    @Override
    public Cita confirmarCita(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new EntityNotFoundException("Cita no encontrada con ID: " + citaId));

        // Validar que la cita esté en estado PENDIENTE
        if (!cita.getEstadoCita().getNombre().equals(ESTADO_PENDIENTE)) {
            throw new IllegalStateException("Solo se pueden confirmar citas en estado PENDIENTE");
        }

        EstadoCita estadoConfirmada = estadoCitaRepository.findByNombre(ESTADO_CONFIRMADA)
                .orElseThrow(() -> new IllegalStateException("Estado CONFIRMADA no encontrado"));

        cita.setEstadoCita(estadoConfirmada);
        Cita citaConfirmada = citaRepository.save(cita);

        // Enviar email de confirmación al paciente
        try {
            emailService.enviarConfirmacionCita(citaConfirmada);
        } catch (Exception e) {
            System.err.println("Error al enviar email de confirmación: " + e.getMessage());
            // No lanzar excepción, la cita ya fue confirmada exitosamente
        }

        return citaConfirmada;
    }

    @Override
    public Cita marcarAsistencia(Long citaId, boolean asistio, String notas) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new EntityNotFoundException("Cita no encontrada con ID: " + citaId));

        // Validar que la cita esté confirmada o pendiente
        String estadoActual = cita.getEstadoCita().getNombre();
        if (!estadoActual.equals(ESTADO_CONFIRMADA) && !estadoActual.equals(ESTADO_PENDIENTE)) {
            throw new IllegalStateException("Solo se puede marcar asistencia en citas confirmadas o pendientes");
        }

        // Determinar el estado de asistencia
        String nombreEstado = asistio ? ESTADO_ASISTIO : ESTADO_NO_ASISTIO;
        EstadoCita estadoAsistencia = estadoCitaRepository.findByNombre(nombreEstado)
                .orElseThrow(() -> new IllegalStateException("Estado " + nombreEstado + " no encontrado"));

        cita.setEstadoCita(estadoAsistencia);
        if (notas != null && !notas.isEmpty()) {
            String notasActuales = cita.getNotas() != null ? cita.getNotas() + "\n" : "";
            cita.setNotas(notasActuales + "Asistencia: " + notas);
        }

        // LÓGICA DE COHERENCIA: ASISTIO = REALIZADO + DESCUENTO DE STOCK
        // Si el paciente asistió, asegurar que exista un TratamientoRealizado MÍNIMO
        // y descontar inmediatamente el inventario asociado al procedimiento.

        if (asistio && cita.getProcedimiento() != null) {
            System.out.println("ℹ️ Paciente ASISTIÓ - Cita ID: " + citaId + ", Procedimiento: "
                    + cita.getProcedimiento().getNombre());

            // Verificar si ya existe un TratamientoRealizado para esta cita
            List<TratamientoRealizado> tratamientosExistentes = tratamientoRealizadoRepository.findByCitaId(citaId);

            if (tratamientosExistentes.isEmpty()) {
                // NO existe TratamientoRealizado → Crear uno MÍNIMO para coherencia
                TratamientoRealizado tratamientoMinimo = new TratamientoRealizado();
                tratamientoMinimo.setCita(cita);
                tratamientoMinimo.setProcedimiento(cita.getProcedimiento());
                tratamientoMinimo.setOdontologo(cita.getOdontologo());
                tratamientoMinimo.setPiezaDental(null);
                tratamientoMinimo.setDescripcionTrabajo("Tratamiento realizado en cita del " +
                        cita.getFechaHoraInicio().toLocalDate());
                tratamientoMinimo.setFechaRealizacion(cita.getFechaHoraInicio());

                TratamientoRealizado guardado = tratamientoRealizadoRepository.save(tratamientoMinimo);
                System.out.println("✅ TratamientoRealizado MÍNIMO creado - ID: " + guardado.getId() +
                        " (Coherencia: ASISTIO = REALIZADO)");

                // DESCUENTO DE STOCK INMEDIATO
                try {
                    String referenciaCita = "Cita #" + citaId;
                    inventarioService.descontarStockPorProcedimientoRealizado(
                            guardado.getProcedimiento().getId(),
                            null, // cantidadAjustada: null = usar cantidad por defecto
                            null, // insumoAjustadoId: null = descontar todos los insumos por defecto
                            referenciaCita);
                    System.out.println("✅ Stock descontado automáticamente para procedimiento: " +
                            guardado.getProcedimiento().getNombre() + " (Cita #" + citaId + ")");
                } catch (Exception e) {
                    System.err.println("⚠️ Error al descontar stock: " + e.getMessage());
                    // Log pero no fallar la cita - el descuento se puede hacer manualmente
                }
            } else {
                System.out.println("✓ Ya existe TratamientoRealizado para esta cita - ID: " +
                        tratamientosExistentes.get(0).getId() + " (stock ya procesado)");
            }
        }

        // Manejar tratamiento planificado asociado según asistencia
        TratamientoPlanificado tratamientoPlanificado = tratamientoPlanificadoRepository.findByCitaAsociadaId(citaId);

        if (tratamientoPlanificado != null) {
            String estadoActualTrat = tratamientoPlanificado.getEstado();
            System.out.println("🔍 [DEBUG] Tratamiento planificado encontrado:");
            System.out.println("   - ID: " + tratamientoPlanificado.getId());
            System.out.println("   - Estado actual: " + estadoActualTrat);
            System.out.println("   - Procedimiento: " + (tratamientoPlanificado.getProcedimiento() != null
                    ? tratamientoPlanificado.getProcedimiento().getNombre()
                    : "N/A"));
            System.out.println("   - Paciente ID: " + tratamientoPlanificado.getPaciente().getId());

            if (asistio) {
                // PACIENTE ASISTIÓ: Marcar tratamiento como COMPLETADO para excluirlo de
                // pendientes
                if ("EN_CURSO".equals(estadoActualTrat) || "PLANIFICADO".equals(estadoActualTrat)) {
                    tratamientoPlanificado.setEstado("COMPLETADO");
                    tratamientoPlanificadoRepository.save(tratamientoPlanificado);
                    tratamientoPlanificadoRepository.flush(); // Forzar escritura inmediata

                    System.out.println("✅ Tratamiento planificado marcado como COMPLETADO (Cita: " + citaId + ")");
                    System.out.println("   → ID: " + tratamientoPlanificado.getId());
                    System.out.println("   → Nuevo estado: COMPLETADO");
                    System.out
                            .println("   → El TratamientoRealizado se creará desde el Modal Avanzado de Tratamientos");
                } else {
                    System.out.println("⚠️ El tratamiento planificado ya está en estado: " + estadoActualTrat
                            + " (no se actualiza)");
                }
            } else {
                // PACIENTE NO ASISTIÓ: Volver a PLANIFICADO para poder reagendar
                if ("EN_CURSO".equals(estadoActualTrat)) {
                    tratamientoPlanificado.setEstado("PLANIFICADO");
                    tratamientoPlanificado.setCitaAsociada(null); // Desvincular la cita
                    tratamientoPlanificadoRepository.save(tratamientoPlanificado);
                    tratamientoPlanificadoRepository.flush(); // Forzar escritura inmediata
                    System.out
                            .println("⚠️ Paciente no asistió - Tratamiento planificado vuelto a PLANIFICADO para cita: "
                                    + citaId);
                }
            }
        } else {
            System.out.println("ℹ️ No se encontró tratamiento planificado asociado a la cita: " + citaId);
        }

        Cita citaActualizada = citaRepository.save(cita);

        // GENERAR COMPROBANTE AUTOMÁTICO cuando el paciente asiste
        if (asistio && cita.getProcedimiento() != null) {
            try {
                // Generar comprobante automáticamente (sin detalles adicionales)
                // Solo incluye el procedimiento de la cita con su precio base
                // Los insumos ya fueron descontados del inventario pero NO se cobran por
                // separado
                // El método generarComprobanteDesdeCita ya valida que no exista un comprobante
                // previo
                facturacionService.generarComprobanteDesdeCita(citaId, null);
                System.out.println("✅ Comprobante generado automáticamente para cita: " + citaId);
            } catch (IllegalStateException e) {
                // Si ya existe comprobante o hay otro error de estado, solo logueamos
                System.out.println("ℹ️ No se generó comprobante para cita " + citaId + ": " + e.getMessage());
            } catch (Exception e) {
                // Log del error pero no fallar la marcación de asistencia
                System.err.println(
                        "⚠️ Error al generar comprobante automático para cita " + citaId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return citaActualizada;
    }

    /**
     * Descuenta los insumos asociados a un procedimiento del inventario.
     * Se utiliza cuando un paciente asiste a una cita y se consume el
     * procedimiento.
     * Registra movimientos de inventario con motivo "Uso en procedimiento".
     *
     * @param procedimientoId ID del procedimiento
     * @param citaId          ID de la cita para referencia
     */
    private void descontarInsumosDelProcedimiento(Long procedimientoId, Long citaId) {
        // Obtener tipo y motivo de movimiento
        TipoMovimiento tipoSalida = tipoMovimientoRepository.findByCodigo("SALIDA")
                .orElseThrow(() -> new IllegalStateException("Tipo de movimiento SALIDA no encontrado"));
        MotivoMovimiento motivoUso = motivoMovimientoRepository.findByNombre("Uso en procedimiento")
                .orElseThrow(() -> new IllegalStateException("Motivo 'Uso en procedimiento' no encontrado"));

        // Obtener todos los insumos asociados al procedimiento
        List<ProcedimientoInsumo> procedimientoInsumos = procedimientoInsumoRepository
                .findByProcedimientoId(procedimientoId);

        for (ProcedimientoInsumo pi : procedimientoInsumos) {
            Insumo insumo = pi.getInsumo();
            BigDecimal cantidadADescontar = pi.getCantidadDefecto();
            BigDecimal stockAnterior = insumo.getStockActual();

            // Verificar que haya suficiente stock
            if (stockAnterior.compareTo(cantidadADescontar) < 0) {
                throw new IllegalStateException(
                        "Stock insuficiente del insumo: " + insumo.getNombre() +
                                " (Disponible: " + stockAnterior + ", Requerido: " + cantidadADescontar + ")");
            }

            // Descontar del stock
            BigDecimal stockNuevo = stockAnterior.subtract(cantidadADescontar);
            insumo.setStockActual(stockNuevo);
            insumoRepository.save(insumo);

            // Registrar movimiento de inventario
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setInsumo(insumo);
            movimiento.setTipoMovimiento(tipoSalida);
            movimiento.setMotivoMovimiento(motivoUso);
            movimiento.setCantidad(cantidadADescontar);
            movimiento.setStockAnterior(stockAnterior);
            movimiento.setStockNuevo(stockNuevo);
            movimiento.setReferencia("Cita #" + citaId);
            movimiento.setNotas("Uso de " + cantidadADescontar + " " + insumo.getUnidadMedida().getNombre() +
                    " de " + insumo.getNombre() + " en procedimiento");

            movimientoInventarioRepository.save(movimiento);
            System.out.println("✅ Movimiento de inventario registrado: " + insumo.getNombre() +
                    " - Cantidad: " + cantidadADescontar);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Cita buscarPorId(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cita no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cita> buscarCitasPorPaciente(Long pacienteId, Pageable pageable) {
        return citaRepository.findByPacienteId(pacienteId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cita> buscarCitasPorOdontologo(Long odontologoId, LocalDate fecha, Pageable pageable) {
        if (fecha == null) {
            // Sin filtro de fecha, retornar todas las citas del odontólogo
            return citaRepository.findByOdontologoId(odontologoId, pageable);
        } else {
            // Con filtro de fecha, buscar citas en ese día específico
            // Nota: Necesitaríamos un método adicional en el repositorio para esto
            // Por ahora, retornar todas las citas del odontólogo
            return citaRepository.findByOdontologoId(odontologoId, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cita> buscarCitasParaCalendario(LocalDate fechaInicio, LocalDate fechaFin, Long odontologoId) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        List<Cita> citas = citaRepository.findByFechaHoraInicioBetween(inicio, fin);

        // Filtrar por odontólogo si se especificó y excluir citas
        // canceladas/reprogramadas
        citas = citas.stream()
                .filter(c -> {
                    // Filtrar por odontólogo si se especificó
                    if (odontologoId != null && !c.getOdontologo().getId().equals(odontologoId)) {
                        return false;
                    }
                    // Excluir citas canceladas y reprogramadas del calendario
                    String estado = c.getEstadoCita().getNombre();
                    return !estado.startsWith("CANCELADA") && !estado.equals("REPROGRAMADA");
                })
                .collect(Collectors.toList());

        return citas;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Cita> listarCitasConFiltros(Long estadoId, Long odontologoId,
            LocalDate fechaDesde, LocalDate fechaHasta,
            Pageable pageable) {
        // Convertir fechas a LocalDateTime si están presentes
        LocalDateTime fechaDesdeTime = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fechaHastaTime = fechaHasta != null ? fechaHasta.atTime(23, 59, 59) : null;

        // Si no hay filtros, devolver todas las citas
        if (estadoId == null && odontologoId == null && fechaDesde == null && fechaHasta == null) {
            return citaRepository.findAll(pageable);
        }

        // Obtener todas las citas y filtrar manualmente (solución simple)
        // En producción se podría usar Specifications de JPA para filtros dinámicos
        List<Cita> todasCitas = citaRepository.findAll();

        List<Cita> citasFiltradas = todasCitas.stream()
                .filter(cita -> {
                    // Filtro por estado
                    if (estadoId != null && !cita.getEstadoCita().getId().equals(estadoId)) {
                        return false;
                    }
                    // Filtro por odontólogo
                    if (odontologoId != null && !cita.getOdontologo().getId().equals(odontologoId)) {
                        return false;
                    }
                    // Filtro por fecha desde
                    if (fechaDesdeTime != null && cita.getFechaHoraInicio().isBefore(fechaDesdeTime)) {
                        return false;
                    }
                    // Filtro por fecha hasta
                    if (fechaHastaTime != null && cita.getFechaHoraInicio().isAfter(fechaHastaTime)) {
                        return false;
                    }
                    return true;
                })
                .sorted((c1, c2) -> c2.getFechaHoraInicio().compareTo(c1.getFechaHoraInicio())) // Ordenar por fecha
                                                                                                // desc
                .collect(Collectors.toList());

        // Implementar paginación manual
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), citasFiltradas.size());

        List<Cita> paginaActual = start >= citasFiltradas.size() ? List.of() : citasFiltradas.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(
                paginaActual, pageable, citasFiltradas.size());
    }
}
