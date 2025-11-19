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
import com.odontoapp.repositorio.TratamientoPlanificadoRepository;
import com.odontoapp.entidad.TratamientoPlanificado;
import com.odontoapp.servicio.CitaService;
import com.odontoapp.servicio.FacturacionService;
import org.springframework.http.ResponseEntity;
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
    private final TratamientoPlanificadoRepository tratamientoPlanificadoRepository;

    public CitaController(CitaService citaService,
                         FacturacionService facturacionService,
                         RolRepository rolRepository,
                         UsuarioRepository usuarioRepository,
                         PacienteRepository pacienteRepository,
                         ProcedimientoRepository procedimientoRepository,
                         InsumoRepository insumoRepository,
                         EstadoCitaRepository estadoCitaRepository,
                         TratamientoPlanificadoRepository tratamientoPlanificadoRepository) {
        this.citaService = citaService;
        this.facturacionService = facturacionService;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.insumoRepository = insumoRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.tratamientoPlanificadoRepository = tratamientoPlanificadoRepository;
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

            // Buscar todos los estados de cita (para filtros)
            var listaEstadosCita = estadoCitaRepository.findAll();

            // Añadir al modelo
            model.addAttribute("listaOdontologos", listaOdontologos);
            model.addAttribute("listaPacientes", listaPacientes);
            model.addAttribute("listaProcedimientos", listaProcedimientos);
            model.addAttribute("listaInsumos", listaInsumos);
            model.addAttribute("listaEstadosCita", listaEstadosCita);
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
            model.addAttribute("listaEstadosCita", List.of());
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
     * @param odontologoUsuarioId ID del nuevo odontólogo (opcional, null mantiene el original)
     * @param nuevaFechaHoraInicio Nueva fecha y hora de inicio
     * @param motivo Motivo de la reprogramación
     * @param attributes Atributos para mensajes flash
     * @return Redirección al calendario
     */
    @PostMapping("/reprogramar")
    public String reprogramarCita(
            @RequestParam Long citaId,
            @RequestParam(required = false) Long odontologoUsuarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime nuevaFechaHoraInicio,
            @RequestParam(required = false, defaultValue = "Reprogramado por usuario") String motivo,
            RedirectAttributes attributes) {

        System.out.println("=== RECIBIENDO REPROGRAMACIÓN ===");
        System.out.println("Cita ID: " + citaId);
        System.out.println("Odontólogo Usuario ID: " + odontologoUsuarioId);
        System.out.println("Nueva Fecha/Hora: " + nuevaFechaHoraInicio);
        System.out.println("Motivo: " + motivo);

        try {
            citaService.reprogramarCita(citaId, odontologoUsuarioId, nuevaFechaHoraInicio, motivo);
            attributes.addFlashAttribute("success", "Cita reprogramada con éxito.");
            System.out.println("✓ Cita reprogramada exitosamente");
        } catch (IllegalStateException e) {
            System.err.println("❌ Error de estado al reprogramar: " + e.getMessage());
            e.printStackTrace();
            attributes.addFlashAttribute("error", "Error al reprogramar: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error inesperado al reprogramar: " + e.getMessage());
            e.printStackTrace();
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
     * El comprobante se genera automáticamente en CitaServiceImpl.marcarAsistencia()
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
            // El servicio maneja tanto la asistencia como la generación del comprobante
            citaService.marcarAsistencia(citaId, asistio, null);

            if (asistio) {
                attributes.addFlashAttribute("success",
                        "Asistencia registrada y comprobante generado con éxito.");
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
     * API REST: Obtiene los horarios disponibles para un odontólogo en una fecha específica.
     * Considera el horario del odontólogo, citas existentes, y reglas de agendamiento.
     *
     * @param odontologoId ID del odontólogo
     * @param fecha Fecha para consultar disponibilidad (formato: yyyy-MM-dd)
     * @param duracion Duración estimada del procedimiento en minutos (opcional, default: 30)
     * @return JSON con horarios disponibles
     */
    @GetMapping("/api/horarios-disponibles")
    @ResponseBody
    public Map<String, Object> obtenerHorariosDisponibles(
            @RequestParam Long odontologoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false, defaultValue = "30") Integer duracion,
            @RequestParam(required = false) Long citaIdExcluir) {

        try {
            // Llamar al servicio para obtener disponibilidad (excluir cita si se proporciona ID)
            // Pasar la duración del procedimiento para filtrar horarios adecuadamente
            Map<String, Object> disponibilidad = citaService.buscarDisponibilidad(odontologoId, fecha, duracion, citaIdExcluir);

            // Obtener horarios disponibles
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> horariosDisponibles =
                    (List<Map<String, Object>>) disponibilidad.get("horariosDisponibles");

            // Solo aplicar filtro para el día de hoy - mostrar desde el siguiente turno disponible
            if (fecha.equals(LocalDate.now())) {
                // Permitir reservar desde 30 minutos adelante (un turno de anticipación)
                LocalDateTime minimoAdelante = LocalDateTime.now().plusMinutes(30);

                List<Map<String, Object>> horariosFiltrados = horariosDisponibles.stream()
                        .filter(slot -> {
                            String horaInicio = (String) slot.get("inicio");
                            LocalDateTime fechaHoraSlot = LocalDateTime.of(fecha,
                                    java.time.LocalTime.parse(horaInicio));
                            // Incluir slots que inician en o después de 30 minutos desde ahora
                            return fechaHoraSlot.isAfter(minimoAdelante) ||
                                   fechaHoraSlot.equals(minimoAdelante);
                        })
                        .collect(Collectors.toList());

                // Si después del filtrado quedan horarios, usarlos
                // Si no quedan horarios, verificar si había alguno disponible originalmente
                if (!horariosFiltrados.isEmpty()) {
                    horariosDisponibles = horariosFiltrados;
                } else {
                    // Verificar si había horarios disponibles antes del filtro de tiempo
                    long horariosDisponiblesOriginales = horariosDisponibles.stream()
                            .filter(slot -> (Boolean) slot.get("disponible"))
                            .count();

                    if (horariosDisponiblesOriginales > 0) {
                        // Había horarios pero todos están en el pasado o muy cercanos
                        disponibilidad.put("disponible", false);
                        disponibilidad.put("motivo", "Los horarios disponibles para hoy requieren al menos 30 minutos de anticipación. Por favor seleccione otro horario u otra fecha.");
                    }
                    // Si no había horarios disponibles originalmente, mantener la lista vacía
                    horariosDisponibles = horariosFiltrados;
                }
            }

            // Actualizar la lista filtrada en el resultado
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
     * API REST para obtener lista de citas con paginación y filtros.
     * Usado por la vista de lista de citas en el calendario.
     *
     * @param page Número de página (0-indexed)
     * @param size Cantidad de elementos por página
     * @param keyword Palabra clave para buscar
     * @param fechaDesde Fecha desde para filtro de rango (formato: yyyy-MM-dd)
     * @param fechaHasta Fecha hasta para filtro de rango (formato: yyyy-MM-dd)
     * @param estadoId ID del estado de cita para filtrar
     * @param odontologoId ID del odontólogo para filtrar
     * @return JSON con citas paginadas
     */
    @GetMapping("/api/lista")
    @ResponseBody
    public Map<String, Object> obtenerListaCitas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) Long odontologoId) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("fechaHoraInicio").descending());

            // Listar citas con filtros aplicados
            // Nota: El servicio convierte internamente LocalDate a LocalDateTime
            Page<Cita> paginaCitas = citaService.listarCitasConFiltros(
                    estadoId, odontologoId, fechaDesde, fechaHasta, pageable);

            // Convertir a formato para la tabla
            List<Map<String, Object>> citasDTO = paginaCitas.getContent().stream()
                .map(cita -> {
                    Map<String, Object> citaMap = new HashMap<>();
                    citaMap.put("id", cita.getId());
                    citaMap.put("fechaHoraInicio", cita.getFechaHoraInicio().toString());
                    citaMap.put("fechaHoraFin", cita.getFechaHoraFin().toString());
                    citaMap.put("pacienteNombre", cita.getPaciente().getNombreCompleto());
                    citaMap.put("odontologoNombre", cita.getOdontologo().getNombreCompleto());
                    citaMap.put("odontologoId", cita.getOdontologo().getId());
                    citaMap.put("procedimientoNombre", cita.getProcedimiento() != null ?
                        cita.getProcedimiento().getNombre() : "Sin procedimiento");
                    citaMap.put("procedimientoId", cita.getProcedimiento() != null ?
                        cita.getProcedimiento().getId() : null);
                    citaMap.put("duracion", cita.getDuracionEstimadaMinutos());
                    citaMap.put("estadoNombre", cita.getEstadoCita().getNombre());
                    citaMap.put("estadoColor", obtenerColorPorEstado(cita.getEstadoCita()));
                    citaMap.put("motivoConsulta", cita.getMotivoConsulta());
                    citaMap.put("notasInternas", cita.getNotas());
                    return citaMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("citas", citasDTO);
            response.put("currentPage", paginaCitas.getNumber());
            response.put("totalPages", paginaCitas.getTotalPages());
            response.put("totalElements", paginaCitas.getTotalElements());
            response.put("size", paginaCitas.getSize());

            return response;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("mensaje", "Error al obtener lista de citas: " + e.getMessage());
            return error;
        }
    }

    /**
     * Obtiene el tratamiento planificado asociado a una cita.
     *
     * @param citaId ID de la cita
     * @return ResponseEntity con los datos del tratamiento planificado o vacío
     */
    @GetMapping("/api/tratamiento-planificado/{citaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerTratamientoPlanificado(@PathVariable Long citaId) {
        try {
            TratamientoPlanificado tratamiento = tratamientoPlanificadoRepository.findByCitaAsociadaId(citaId);

            if (tratamiento == null) {
                return ResponseEntity.ok(Map.of("existe", false));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("existe", true);
            response.put("id", tratamiento.getId());
            response.put("procedimiento", tratamiento.getProcedimiento().getNombre());
            response.put("piezasDentales", tratamiento.getPiezasDentales() != null ? tratamiento.getPiezasDentales() : "N/A");
            response.put("descripcion", tratamiento.getDescripcion() != null ? tratamiento.getDescripcion() : "");
            response.put("estado", tratamiento.getEstado());
            response.put("odontologo", tratamiento.getOdontologo().getNombreCompleto());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("existe", false));
        }
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
