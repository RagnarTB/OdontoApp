package com.odontoapp.controlador;

import com.odontoapp.dto.CitaDTO;
import com.odontoapp.dto.FullCalendarEventDTO;
import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.EstadoCita;
import com.odontoapp.repositorio.EstadoCitaRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.CitaService;
import com.odontoapp.servicio.FacturacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
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
 * Controlador para la gestión de citas de la clínica dental.
 * Proporciona vistas web y endpoints API para el calendario de citas.
 */
@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;
    private final FacturacionService facturacionService;
    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final InsumoRepository insumoRepository;
    private final EstadoCitaRepository estadoCitaRepository;

    public CitaController(CitaService citaService,
                         FacturacionService facturacionService,
                         RolRepository rolRepository,
                         UsuarioRepository usuarioRepository,
                         PacienteRepository pacienteRepository,
                         ProcedimientoRepository procedimientoRepository,
                         InsumoRepository insumoRepository,
                         EstadoCitaRepository estadoCitaRepository) {
        this.citaService = citaService;
        this.facturacionService = facturacionService;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.insumoRepository = insumoRepository;
        this.estadoCitaRepository = estadoCitaRepository;
    }

    /**
     * Muestra la vista del calendario de citas.
     *
     * @param model Modelo de Spring MVC
     * @return Vista del calendario
     */
    @GetMapping
    public String verCalendario(Model model) {
        try {
            // Buscar usuarios con rol ODONTOLOGO
            var rolOdontologo = rolRepository.findByNombre("ODONTOLOGO");
            var listaOdontologos = rolOdontologo.isPresent()
                    ? usuarioRepository.findByRolesNombre("ODONTOLOGO")
                    : List.of();

            // Buscar todos los pacientes
            var listaPacientes = pacienteRepository.findAll();

            // Buscar todos los procedimientos con sus relaciones cargadas
            var listaProcedimientos = procedimientoRepository.findAllWithRelations();

            // Buscar todos los insumos con sus relaciones cargadas (para el modal de registrar tratamiento)
            var listaInsumos = insumoRepository.findAllWithRelations();

            // Añadir al modelo
            model.addAttribute("listaOdontologos", listaOdontologos);
            model.addAttribute("listaPacientes", listaPacientes);
            model.addAttribute("listaProcedimientos", listaProcedimientos);
            model.addAttribute("listaInsumos", listaInsumos);
            model.addAttribute("citaDTO", new CitaDTO());

            return "modulos/citas/calendario";
        } catch (Exception e) {
            System.err.println("Error al cargar calendario de citas: " + e.getMessage());
            e.printStackTrace();

            // Inicializar listas vacías para evitar errores en la vista
            model.addAttribute("listaOdontologos", List.of());
            model.addAttribute("listaPacientes", List.of());
            model.addAttribute("listaProcedimientos", List.of());
            model.addAttribute("listaInsumos", List.of());
            model.addAttribute("citaDTO", new CitaDTO());
            model.addAttribute("error", "Error al cargar los datos del calendario. Por favor, contacte al administrador.");

            return "modulos/citas/calendario";
        }
    }

    /**
     * Muestra la vista de lista de citas con filtros y paginación.
     *
     * @param model Modelo de Spring MVC
     * @param page Número de página (default 0)
     * @param size Tamaño de página (default 20)
     * @param estadoId ID del estado para filtrar (opcional)
     * @param odontologoId ID del odontólogo para filtrar (opcional)
     * @param fechaDesde Fecha desde para filtrar (opcional)
     * @param fechaHasta Fecha hasta para filtrar (opcional)
     * @return Vista de lista de citas
     */
    @GetMapping("/lista")
    public String verListaCitas(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) Long estadoId,
                               @RequestParam(required = false) Long odontologoId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {

        // Crear paginación ordenada por fecha descendente
        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaHoraInicio").descending());

        // Obtener citas según filtros
        Page<Cita> paginaCitas = citaService.listarCitasConFiltros(
            estadoId, odontologoId, fechaDesde, fechaHasta, pageable);

        // Cargar listas para los filtros
        var listaEstados = estadoCitaRepository.findAll();
        var rolOdontologo = rolRepository.findByNombre("ODONTOLOGO");
        var listaOdontologos = rolOdontologo.isPresent()
                ? usuarioRepository.findByRolesNombre("ODONTOLOGO")
                : List.of();

        // Añadir al modelo
        model.addAttribute("paginaCitas", paginaCitas);
        model.addAttribute("listaEstados", listaEstados);
        model.addAttribute("listaOdontologos", listaOdontologos);
        model.addAttribute("estadoIdFiltro", estadoId);
        model.addAttribute("odontologoIdFiltro", odontologoId);
        model.addAttribute("fechaDesdeFiltro", fechaDesde);
        model.addAttribute("fechaHastaFiltro", fechaHasta);

        return "modulos/citas/lista";
    }

    /**
     * API REST para obtener eventos del calendario.
     * Compatible con FullCalendar.js.
     *
     * @param start Fecha de inicio en formato ISO 8601
     * @param end Fecha de fin en formato ISO 8601
     * @param odontologoId ID del odontólogo (opcional)
     * @return Lista de eventos en formato FullCalendar
     */
    @GetMapping("/api/eventos")
    @ResponseBody
    public List<FullCalendarEventDTO> getEventos(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) Long odontologoId) {

        // Parsear fechas de ISO 8601 a LocalDate
        LocalDate fechaInicio = LocalDate.parse(start.substring(0, 10));
        LocalDate fechaFin = LocalDate.parse(end.substring(0, 10));

        // Buscar citas en el rango de fechas
        List<Cita> citas = citaService.buscarCitasParaCalendario(fechaInicio, fechaFin, odontologoId);

        // Mapear a FullCalendarEventDTO
        return citas.stream()
                .map(this::mapearCitaAEvento)
                .collect(Collectors.toList());
    }

    /**
     * API REST para obtener la disponibilidad de un odontólogo en una fecha.
     *
     * @param odontologoId ID del odontólogo
     * @param fecha Fecha para verificar disponibilidad
     * @return Mapa con horarios disponibles y ocupados
     */
    @GetMapping("/api/disponibilidad")
    @ResponseBody
    public Map<String, Object> getDisponibilidad(
            @RequestParam Long odontologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        return citaService.buscarDisponibilidad(odontologoId, fecha);
    }

    /**
     * Agenda una nueva cita.
     *
     * @param dto DTO con datos de la cita
     * @param attributes Atributos para mensajes flash
     * @return Redirección al calendario
     */
    @PostMapping("/agendar")
    public String agendarCita(@ModelAttribute CitaDTO dto, RedirectAttributes attributes) {
        try {
            citaService.agendarCita(
                    dto.getPacienteUsuarioId(),
                    dto.getOdontologoUsuarioId(),
                    dto.getProcedimientoId(),
                    dto.getFechaHoraInicio(),
                    dto.getMotivoConsulta(),
                    dto.getNotasInternas()
            );
            attributes.addFlashAttribute("success", "Cita agendada con éxito.");
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error", "Error al agendar: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Error inesperado al agendar la cita: " + e.getMessage());
        }

        return "redirect:/citas";
    }

    /**
     * Reprograma una cita existente.
     *
     * @param citaId ID de la cita a reprogramar
     * @param nuevaFechaHoraInicio Nueva fecha y hora de inicio
     * @param motivo Motivo de la reprogramación
     * @param attributes Atributos para mensajes flash
     * @return Redirección al calendario
     */
    @PostMapping("/reprogramar")
    public String reprogramarCita(
            @RequestParam Long citaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nuevaFechaHoraInicio,
            @RequestParam(required = false, defaultValue = "Reprogramado por usuario") String motivo,
            RedirectAttributes attributes) {
        try {
            citaService.reprogramarCita(citaId, nuevaFechaHoraInicio, motivo);
            attributes.addFlashAttribute("success", "Cita reprogramada con éxito.");
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error", "Error al reprogramar: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Error inesperado al reprogramar la cita: " + e.getMessage());
        }

        return "redirect:/citas";
    }

    /**
     * Cancela una cita.
     *
     * @param citaId ID de la cita a cancelar
     * @param motivo Motivo de la cancelación
     * @param attributes Atributos para mensajes flash
     * @return Redirección al calendario
     */
    @PostMapping("/cancelar")
    public String cancelarCita(
            @RequestParam Long citaId,
            @RequestParam String motivo,
            RedirectAttributes attributes) {
        try {
            // Por defecto, asumimos que la cancelación es por la clínica (false)
            citaService.cancelarCita(citaId, false, motivo);
            attributes.addFlashAttribute("success", "Cita cancelada con éxito.");
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error", "Error al cancelar: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Error inesperado al cancelar la cita: " + e.getMessage());
        }

        return "redirect:/citas";
    }

    /**
     * Confirma una cita pendiente.
     *
     * @param citaId ID de la cita a confirmar
     * @param attributes Atributos para mensajes flash
     * @return Redirección al calendario
     */
    @PostMapping("/confirmar")
    public String confirmarCita(@RequestParam Long citaId, RedirectAttributes attributes) {
        try {
            citaService.confirmarCita(citaId);
            attributes.addFlashAttribute("success", "Cita confirmada con éxito.");
        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error", "Error al confirmar: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Error inesperado al confirmar la cita: " + e.getMessage());
        }

        return "redirect:/citas";
    }

    /**
     * Marca la asistencia de un paciente a una cita.
     * Si el paciente asistió, genera automáticamente el comprobante.
     *
     * @param citaId ID de la cita
     * @param asistio true si el paciente asistió, false si no asistió
     * @param attributes Atributos para mensajes flash
     * @return Redirección al calendario
     */
    @PostMapping("/marcar-asistencia")
    public String marcarAsistencia(
            @RequestParam Long citaId,
            @RequestParam boolean asistio,
            RedirectAttributes attributes) {
        try {
            citaService.marcarAsistencia(citaId, asistio, null);

            // Si el paciente asistió, generar comprobante automáticamente
            if (asistio) {
                try {
                    facturacionService.generarComprobanteDesdeCita(citaId, null);
                    attributes.addFlashAttribute("success",
                            "Asistencia registrada y comprobante generado con éxito.");
                } catch (Exception e) {
                    // Si falla la generación del comprobante, informar pero mantener la asistencia
                    attributes.addFlashAttribute("warning",
                            "Asistencia registrada, pero hubo un error al generar el comprobante: "
                                    + e.getMessage());
                }
            } else {
                attributes.addFlashAttribute("success", "Inasistencia registrada con éxito.");
            }

        } catch (IllegalStateException e) {
            attributes.addFlashAttribute("error", "Error al marcar asistencia: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error inesperado al marcar asistencia: " + e.getMessage());
        }

        return "redirect:/citas";
    }

    /**
     * Mapea una entidad Cita a un DTO FullCalendarEvent.
     *
     * @param cita Cita a mapear
     * @return DTO compatible con FullCalendar
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
        extendedProps.put("notasInternas", cita.getNotasInternas());

        // Obtener color del estado
        String color = obtenerColorPorEstado(cita.getEstadoCita());

        return new FullCalendarEventDTO(
                cita.getId().toString(),
                cita.getPaciente().getNombreCompleto(),
                cita.getFechaHoraInicio().toString(),
                cita.getFechaHoraFin().toString(),
                color,
                color,
                extendedProps
        );
    }

    /**
     * Obtiene el color asociado a un estado de cita.
     *
     * @param estadoCita Estado de la cita
     * @return Color en formato hexadecimal o nombre CSS
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
