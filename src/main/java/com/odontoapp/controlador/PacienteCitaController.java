package com.odontoapp.controlador;

import com.odontoapp.dto.CitaDTO;
import com.odontoapp.dto.FullCalendarEventDTO;
import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.EstadoCita;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.*;
import com.odontoapp.servicio.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para gestión de citas del portal de pacientes.
 * Todas las operaciones están filtradas por el paciente autenticado.
 */
@Controller
@RequestMapping("/paciente/citas")
@RequiredArgsConstructor
public class PacienteCitaController {

    private final CitaService citaService;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final CitaRepository citaRepository;

    /**
     * Helper: Obtiene el usuario paciente autenticado
     */
    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    /**
     * Muestra el calendario de citas del paciente
     */
    @GetMapping
    public String verCalendario(Model model) {
        try {
            // Buscar usuarios con rol ODONTOLOGO
            var rolOdontologo = rolRepository.findByNombre("ODONTOLOGO");
            var listaOdontologos = rolOdontologo.isPresent()
                    ? usuarioRepository.findByRolesNombre("ODONTOLOGO")
                    : List.of();

            // Buscar procedimientos con relaciones cargadas
            var listaProcedimientos = procedimientoRepository.findAllWithRelations();

            // Buscar todos los estados de cita (para mostrar la leyenda)
            var listaEstadosCita = estadoCitaRepository.findAll();

            // Añadir al modelo
            model.addAttribute("listaOdontologos", listaOdontologos);
            model.addAttribute("listaProcedimientos", listaProcedimientos);
            model.addAttribute("listaEstadosCita", listaEstadosCita);
            model.addAttribute("citaDTO", new CitaDTO());

            return "paciente/citas/calendario";
        } catch (Exception e) {
            System.err.println("Error al cargar calendario de citas (paciente): " + e.getMessage());
            e.printStackTrace();

            // Inicializar listas vacías para evitar errores en la vista
            model.addAttribute("listaOdontologos", List.of());
            model.addAttribute("listaProcedimientos", List.of());
            model.addAttribute("listaEstadosCita", List.of());
            model.addAttribute("citaDTO", new CitaDTO());
            model.addAttribute("error", "Error al cargar los datos del calendario.");

            return "paciente/citas/calendario";
        }
    }

    /**
     * Muestra la vista de lista de citas del paciente con filtros y paginación
     */
    @GetMapping("/lista")
    @Transactional(readOnly = true)
    public String verListaCitas(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size,
                                @RequestParam(required = false) Long estadoId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        // Obtener usuario autenticado
        Usuario usuario = obtenerUsuarioAutenticado();

        // Crear paginación ordenada por fecha descendente
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaHoraInicio").descending());

        // Convertir fechas a LocalDateTime si es necesario
        LocalDateTime fechaDesdeDateTime = (fechaDesde != null) ? fechaDesde.atStartOfDay() : null;
        LocalDateTime fechaHastaDateTime = (fechaHasta != null) ? fechaHasta.atTime(23, 59, 59) : null;

        // Obtener citas del paciente según filtros
        Page<Cita> paginaCitas;

        if (estadoId != null || fechaDesdeDateTime != null || fechaHastaDateTime != null) {
            // Aplicar filtros
            paginaCitas = citaRepository.findByPacienteIdWithFilters(
                usuario.getId(), estadoId, fechaDesdeDateTime, fechaHastaDateTime, pageable);
        } else {
            // Sin filtros, solo por pacienteId
            paginaCitas = citaRepository.findByPacienteId(usuario.getId(), pageable);
        }

        // Cargar listas para los filtros
        var listaEstados = estadoCitaRepository.findAll();

        // Añadir al modelo
        model.addAttribute("paginaCitas", paginaCitas);
        model.addAttribute("listaEstados", listaEstados);
        model.addAttribute("estadoIdFiltro", estadoId);
        model.addAttribute("fechaDesdeFiltro", fechaDesde);
        model.addAttribute("fechaHastaFiltro", fechaHasta);

        return "paciente/citas/lista";
    }

    /**
     * API REST para obtener eventos del calendario del paciente.
     * Compatible con FullCalendar.js.
     */
    @GetMapping("/api/eventos")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<FullCalendarEventDTO> getEventos(
            @RequestParam String start,
            @RequestParam String end) {

        // Obtener usuario autenticado
        Usuario usuario = obtenerUsuarioAutenticado();

        // Parsear fechas de ISO 8601 a LocalDate
        LocalDate fechaInicio = LocalDate.parse(start.substring(0, 10));
        LocalDate fechaFin = LocalDate.parse(end.substring(0, 10));

        LocalDateTime inicioDateTime = fechaInicio.atStartOfDay();
        LocalDateTime finDateTime = fechaFin.atTime(23, 59, 59);

        // Buscar SOLO citas del paciente autenticado en el rango de fechas
        List<Cita> citas = citaRepository.findByPacienteIdAndFechaHoraInicioBetween(
            usuario.getId(), inicioDateTime, finDateTime);

        // Mapear a FullCalendarEventDTO
        return citas.stream()
                .map(this::mapearCitaAEvento)
                .collect(Collectors.toList());
    }

    /**
     * API REST para obtener la disponibilidad de un odontólogo en una fecha
     */
    @GetMapping("/api/disponibilidad")
    @ResponseBody
    public Map<String, Object> getDisponibilidad(
            @RequestParam Long odontologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        return citaService.buscarDisponibilidad(odontologoId, fecha);
    }

    /**
     * API REST para obtener horarios disponibles de un odontólogo
     */
    @GetMapping("/api/horarios-disponibles")
    @ResponseBody
    public Map<String, Object> obtenerHorariosDisponibles(
            @RequestParam Long odontologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false, defaultValue = "30") Integer duracion,
            @RequestParam(required = false) Long citaIdExcluir) {

        try {
            // Llamar al servicio para obtener disponibilidad
            Map<String, Object> disponibilidad = citaService.buscarDisponibilidad(odontologoId, fecha, duracion, citaIdExcluir);

            // Obtener horarios disponibles
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> horariosDisponibles =
                    (List<Map<String, Object>>) disponibilidad.get("horariosDisponibles");

            // Filtrar para el día de hoy
            if (fecha.equals(LocalDate.now())) {
                LocalDateTime minimoAdelante = LocalDateTime.now().plusMinutes(30);

                List<Map<String, Object>> horariosFiltrados = horariosDisponibles.stream()
                        .filter(slot -> {
                            String horaInicio = (String) slot.get("inicio");
                            LocalDateTime fechaHoraSlot = LocalDateTime.of(fecha,
                                    java.time.LocalTime.parse(horaInicio));
                            return fechaHoraSlot.isAfter(minimoAdelante) ||
                                   fechaHoraSlot.equals(minimoAdelante);
                        })
                        .collect(Collectors.toList());

                if (!horariosFiltrados.isEmpty()) {
                    horariosDisponibles = horariosFiltrados;
                } else {
                    long horariosDisponiblesOriginales = horariosDisponibles.stream()
                            .filter(slot -> (Boolean) slot.get("disponible"))
                            .count();

                    if (horariosDisponiblesOriginales > 0) {
                        disponibilidad.put("disponible", false);
                        disponibilidad.put("motivo", "Los horarios disponibles para hoy requieren al menos 30 minutos de anticipación.");
                    }
                    horariosDisponibles = horariosFiltrados;
                }
            }

            disponibilidad.put("horariosDisponibles", horariosDisponibles);
            disponibilidad.put("duracionProcedimiento", duracion);
            disponibilidad.put("minimoAnticipacion", "30 minutos");

            return disponibilidad;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("mensaje", "Error al obtener horarios disponibles: " + e.getMessage());
            return error;
        }
    }

    /**
     * Agenda una nueva cita para el paciente autenticado.
     * El pacienteUsuarioId se establece automáticamente.
     */
    @PostMapping("/agendar")
    public String agendarCita(@ModelAttribute CitaDTO dto, RedirectAttributes attributes) {
        try {
            // Obtener usuario autenticado
            Usuario usuario = obtenerUsuarioAutenticado();

            // Forzar pacienteUsuarioId al ID del usuario autenticado
            citaService.agendarCita(
                    usuario.getId(), // Siempre el usuario autenticado
                    dto.getOdontologoUsuarioId(),
                    dto.getProcedimientoId(),
                    dto.getFechaHoraInicio(),
                    dto.getMotivoConsulta(),
                    null // notasInternas no permitidas para pacientes
            );
            attributes.addFlashAttribute("success", "Cita agendada con éxito. Pendiente de confirmación.");
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error", "Error al agendar: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Error inesperado al agendar la cita: " + e.getMessage());
        }

        return "redirect:/paciente/citas";
    }

    /**
     * Reprograma una cita existente del paciente.
     * Solo si la cita pertenece al paciente y está en estado PENDIENTE o CONFIRMADA.
     */
    @PostMapping("/reprogramar")
    public String reprogramarCita(
            @RequestParam Long citaId,
            @RequestParam(required = false) Long odontologoUsuarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nuevaFechaHoraInicio,
            @RequestParam(required = false, defaultValue = "Reprogramado por paciente") String motivo,
            RedirectAttributes attributes) {

        try {
            // Obtener usuario autenticado
            Usuario usuario = obtenerUsuarioAutenticado();

            // Verificar que la cita pertenezca al paciente
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new IllegalStateException("Cita no encontrada"));

            if (!cita.getPaciente().getId().equals(usuario.getId())) {
                throw new IllegalStateException("No tiene permisos para reprogramar esta cita");
            }

            // Verificar que el estado permita reprogramación
            String estadoActual = cita.getEstadoCita().getNombre();
            if (!"PENDIENTE".equals(estadoActual) && !"CONFIRMADA".equals(estadoActual)) {
                throw new IllegalStateException("Solo se pueden reprogramar citas en estado PENDIENTE o CONFIRMADA");
            }

            citaService.reprogramarCita(citaId, odontologoUsuarioId, nuevaFechaHoraInicio, motivo);
            attributes.addFlashAttribute("success", "Cita reprogramada con éxito. Pendiente de confirmación.");
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error", "Error al reprogramar: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Error inesperado al reprogramar la cita: " + e.getMessage());
        }

        return "redirect:/paciente/citas";
    }

    /**
     * Cancela una cita del paciente.
     * Solo si la cita pertenece al paciente y está en estado PENDIENTE o CONFIRMADA.
     */
    @PostMapping("/cancelar")
    public String cancelarCita(
            @RequestParam Long citaId,
            @RequestParam String motivo,
            RedirectAttributes attributes) {
        try {
            // Obtener usuario autenticado
            Usuario usuario = obtenerUsuarioAutenticado();

            // Verificar que la cita pertenezca al paciente
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new IllegalStateException("Cita no encontrada"));

            if (!cita.getPaciente().getId().equals(usuario.getId())) {
                throw new IllegalStateException("No tiene permisos para cancelar esta cita");
            }

            // Verificar que el estado permita cancelación
            String estadoActual = cita.getEstadoCita().getNombre();
            if (!"PENDIENTE".equals(estadoActual) && !"CONFIRMADA".equals(estadoActual)) {
                throw new IllegalStateException("Solo se pueden cancelar citas en estado PENDIENTE o CONFIRMADA");
            }

            // Cancelar con flag canceladoPorPaciente = true
            citaService.cancelarCita(citaId, true, motivo);
            attributes.addFlashAttribute("success", "Cita cancelada con éxito.");
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error", "Error al cancelar: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Error inesperado al cancelar la cita: " + e.getMessage());
        }

        return "redirect:/paciente/citas";
    }

    /**
     * Mapea una entidad Cita a un DTO FullCalendarEvent
     */
    private FullCalendarEventDTO mapearCitaAEvento(Cita cita) {
        // Preparar propiedades extendidas
        Map<String, Object> extendedProps = new HashMap<>();
        extendedProps.put("pacienteId", cita.getPaciente().getId());
        extendedProps.put("odontologoId", cita.getOdontologo().getId());
        extendedProps.put("odontologoNombre", cita.getOdontologo().getNombreCompleto());
        extendedProps.put("estadoNombre", cita.getEstadoCita().getNombre());

        if (cita.getProcedimiento() != null) {
            extendedProps.put("procedimientoId", cita.getProcedimiento().getId());
            extendedProps.put("procedimientoNombre", cita.getProcedimiento().getNombre());
        }

        extendedProps.put("motivoConsulta", cita.getMotivoConsulta());

        // Obtener color del estado
        String color = obtenerColorPorEstado(cita.getEstadoCita());

        return new FullCalendarEventDTO(
                cita.getId().toString(),
                cita.getOdontologo().getNombreCompleto() + " - " +
                (cita.getProcedimiento() != null ? cita.getProcedimiento().getNombre() : "Consulta"),
                cita.getFechaHoraInicio().toString(),
                cita.getFechaHoraFin().toString(),
                color,
                color,
                extendedProps
        );
    }

    /**
     * Obtiene el color asociado a un estado de cita
     */
    private String obtenerColorPorEstado(EstadoCita estadoCita) {
        return switch (estadoCita.getNombre()) {
            case "PENDIENTE" -> "#ffc107"; // Amarillo
            case "CONFIRMADA" -> "#17a2b8"; // Azul
            case "ASISTIO" -> "#28a745"; // Verde
            case "NO_ASISTIO" -> "#dc3545"; // Rojo
            case "CANCELADA_PACIENTE", "CANCELADA_CLINICA" -> "#6c757d"; // Gris
            case "REPROGRAMADA" -> "#fd7e14"; // Naranja
            default -> "#007bff"; // Azul por defecto
        };
    }
}
