// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\controlador\PacienteController.java
package com.odontoapp.controlador;

import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.odontoapp.dto.PacienteDTO;
import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.Comprobante;
import com.odontoapp.entidad.OdontogramaDiente;
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.TipoDocumento;
import com.odontoapp.entidad.TratamientoPlanificado;
import com.odontoapp.entidad.TratamientoRealizado;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.ComprobanteRepository;
import com.odontoapp.repositorio.OdontogramaDienteRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.TratamientoPlanificadoRepository;
import com.odontoapp.repositorio.TratamientoRealizadoRepository;
import com.odontoapp.servicio.PacienteService;
import com.odontoapp.util.Permisos;

import java.time.format.DateTimeFormatter;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final PacienteRepository pacienteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final CitaRepository citaRepository;
    private final OdontogramaDienteRepository odontogramaDienteRepository;
    private final TratamientoRealizadoRepository tratamientoRealizadoRepository;
    private final TratamientoPlanificadoRepository tratamientoPlanificadoRepository;
    private final ComprobanteRepository comprobanteRepository;

    public PacienteController(PacienteService pacienteService,
            PacienteRepository pacienteRepository, TipoDocumentoRepository tipoDocumentoRepository,
            CitaRepository citaRepository, OdontogramaDienteRepository odontogramaDienteRepository,
            TratamientoRealizadoRepository tratamientoRealizadoRepository,
            TratamientoPlanificadoRepository tratamientoPlanificadoRepository,
            ComprobanteRepository comprobanteRepository) {
        this.pacienteService = pacienteService;
        this.pacienteRepository = pacienteRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.citaRepository = citaRepository;
        this.odontogramaDienteRepository = odontogramaDienteRepository;
        this.tratamientoRealizadoRepository = tratamientoRealizadoRepository;
        this.tratamientoPlanificadoRepository = tratamientoPlanificadoRepository;
        this.comprobanteRepository = comprobanteRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_PACIENTES)")
    public String listarPacientes(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Paciente> paginaPacientes = pacienteService.listarTodosLosPacientes(keyword, pageable);
        model.addAttribute("paginaPacientes", paginaPacientes);
        model.addAttribute("keyword", keyword);
        model.addAttribute("mostrarEliminados", false);
        return "modulos/pacientes/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_PACIENTES)")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("pacienteDTO", new PacienteDTO());
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll()); // NUEVO
        return "modulos/pacientes/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority(T(com.odontoapp.util.Permisos).CREAR_PACIENTES, T(com.odontoapp.util.Permisos).EDITAR_PACIENTES)")
    public String guardarPaciente(@Valid @ModelAttribute("pacienteDTO") PacienteDTO pacienteDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // 1. Validación del DTO
        if (result.hasErrors()) {
            // Agregar explícitamente el pacienteDTO al modelo para que persistan los datos
            model.addAttribute("pacienteDTO", pacienteDTO);
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
            return "modulos/pacientes/formulario";
        }

        // 2. Intentar guardar
        try {
            pacienteService.guardarPaciente(pacienteDTO);
            redirectAttributes.addFlashAttribute("success", "Paciente guardado con éxito.");
            return "redirect:/pacientes";

        } catch (DataIntegrityViolationException e) {
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
            model.addAttribute("pacienteDTO", pacienteDTO); // Devolver datos al formulario

            String mensajeServicio = e.getMessage();

            // Verificar si es nuestro mensaje personalizado para restaurar
            if (mensajeServicio != null && mensajeServicio.startsWith("EMAIL_ELIMINADO:")) {
                try {
                    String[] parts = mensajeServicio.split(":");
                    Long idUsuarioEliminado = Long.parseLong(parts[1]);
                    String emailEliminado = parts[2];
                    model.addAttribute("errorRestauracion", // Atributo específico para la vista
                            "El email '" + emailEliminado + "' pertenece a un usuario eliminado.");
                    model.addAttribute("idUsuarioParaRestaurar", idUsuarioEliminado); // Pasar ID a la vista
                } catch (Exception parseEx) {
                    model.addAttribute("error", "El email ya existe (usuario eliminado, error al procesar).");
                }
            } else {
                // Otro error (duplicado de DNI, email activo, etc.)
                model.addAttribute("error",
                        mensajeServicio != null ? mensajeServicio : "Error de integridad de datos.");
            }
            return "modulos/pacientes/formulario"; // Regresar al formulario con el error

        } catch (Exception e) {
            // Otros errores inesperados
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
            model.addAttribute("pacienteDTO", pacienteDTO);
            model.addAttribute("error", "Ocurrió un error inesperado al guardar el paciente.");
            System.err.println("Error INESPERADO al guardar paciente: " + e.getMessage());
            e.printStackTrace();
            return "modulos/pacientes/formulario";
        }
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_PACIENTES)")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        return pacienteService.buscarPorId(id).map(paciente -> {
            PacienteDTO dto = new PacienteDTO();
            dto.setId(paciente.getId());
            dto.setTipoDocumentoId(paciente.getTipoDocumento().getId()); // MODIFICADO
            dto.setNumeroDocumento(paciente.getNumeroDocumento()); // MODIFICADO
            dto.setNombreCompleto(paciente.getNombreCompleto());
            dto.setEmail(paciente.getEmail());
            dto.setTelefono(paciente.getTelefono());
            dto.setFechaNacimiento(paciente.getFechaNacimiento());
            dto.setDireccion(paciente.getDireccion());
            dto.setAlergias(paciente.getAlergias());
            dto.setAntecedentesMedicos(paciente.getAntecedentesMedicos());

            model.addAttribute("pacienteDTO", dto);
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll()); // NUEVO
            return "modulos/pacientes/formulario";
        }).orElse("redirect:/pacientes");
    }

    @GetMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_PACIENTES)")
    public String eliminarPaciente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pacienteService.eliminarPaciente(id);
            redirectAttributes.addFlashAttribute("success", "Paciente eliminado con éxito (desactivado).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el paciente: " + e.getMessage());
        }
        return "redirect:/pacientes";
    }

    @GetMapping("/restablecer/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).RESTAURAR_PACIENTES)")
    public String restablecerPaciente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pacienteService.restablecerPaciente(id);
            redirectAttributes.addFlashAttribute("success", "Paciente restablecido y activado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al restablecer el paciente: " + e.getMessage());
        }
        return "redirect:/pacientes";
    }

    /**
     * Muestra el historial clínico completo de un paciente
     * Incluye: datos básicos, tratamientos, citas, odontograma, comprobantes
     * Con paginación para citas y comprobantes
     */
    @GetMapping("/historial/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_DETALLE_PACIENTES)")
    public String verHistorial(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int citasPageNum,
            @RequestParam(defaultValue = "10") int citasSize,
            @RequestParam(defaultValue = "0") int comprobantesPageNum,
            @RequestParam(defaultValue = "10") int comprobantesSize,
            @RequestParam(defaultValue = "0") int tratamientosPage,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Paciente> pacienteOpt = pacienteService.buscarPorId(id);

        if (pacienteOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Paciente no encontrado.");
            return "redirect:/pacientes";
        }

        Paciente paciente = pacienteOpt.get();
        model.addAttribute("paciente", paciente);

        // Cargar datos del historial del paciente
        if (paciente.getUsuario() != null) {
            Long usuarioId = paciente.getUsuario().getId();

            // Obtener citas del paciente con paginación
            org.springframework.data.domain.Page<Cita> citasPage = citaRepository.findByPacienteId(
                usuarioId,
                PageRequest.of(citasPageNum, citasSize, Sort.by("fechaHoraInicio").descending())
            );
            model.addAttribute("citasPage", citasPage);

            // Obtener tratamientos realizados del paciente con paginación
            int tratamientosSize = 10; // 10 tratamientos por página

            org.springframework.data.domain.Page<TratamientoRealizado> tratamientosPageData =
                    tratamientoRealizadoRepository.findByPacienteId(
                        usuarioId, // ← FIX: Usar usuarioId en lugar de paciente.getId()
                        PageRequest.of(tratamientosPage, tratamientosSize, Sort.by("fechaRealizacion").descending())
                    );
            model.addAttribute("tratamientosPage", tratamientosPageData);

            // Obtener tratamientos planificados del paciente (solo PLANIFICADO y EN_CURSO, no los COMPLETADOS)
            java.util.List<TratamientoPlanificado> tratamientosPlanificados =
                    tratamientoPlanificadoRepository.findTratamientosPendientes(paciente.getUsuario());
            model.addAttribute("tratamientosPlanificados", tratamientosPlanificados);

            // Obtener comprobantes del paciente con paginación
            org.springframework.data.domain.Page<Comprobante> comprobantesPage =
                    comprobanteRepository.findByPacienteIdOrderByFechaEmisionDesc(
                        usuarioId,
                        PageRequest.of(comprobantesPageNum, comprobantesSize)
                    );
            model.addAttribute("comprobantesPage", comprobantesPage);
        }

        return "modulos/pacientes/historial";
    }

    /**
     * API REST para obtener el detalle completo de un paciente
     * Usado por el modal "Ver Detalle" en la lista de pacientes
     */
    @GetMapping("/api/detalle/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_DETALLE_PACIENTES)")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerDetallePaciente(@PathVariable Long id) {
        Optional<Paciente> pacienteOpt = pacienteService.buscarPorId(id);

        if (pacienteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Paciente paciente = pacienteOpt.get();
        Map<String, Object> detalle = new java.util.HashMap<>();

        // Datos personales
        detalle.put("nombreCompleto", paciente.getNombreCompleto());
        detalle.put("tipoDocumento", paciente.getTipoDocumento().getNombre());
        detalle.put("numeroDocumento", paciente.getNumeroDocumento());
        detalle.put("email", paciente.getEmail());
        detalle.put("telefono", paciente.getTelefono());
        detalle.put("fechaNacimiento", paciente.getFechaNacimiento() != null ?
            paciente.getFechaNacimiento().toString() : null);
        detalle.put("direccion", paciente.getDireccion());

        // Información médica
        detalle.put("alergias", paciente.getAlergias());
        detalle.put("antecedentes", paciente.getAntecedentesMedicos());
        detalle.put("tratamientosActuales", paciente.getTratamientosActuales());

        // Historial de citas (cargar desde CitaRepository)
        java.util.List<Map<String, Object>> historialCitas = new java.util.ArrayList<>();
        if (paciente.getUsuario() != null) {
            java.util.List<Cita> citas = citaRepository.findByPacienteId(paciente.getUsuario().getId(),
                    PageRequest.of(0, 10)).getContent(); // Últimas 10 citas
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Cita cita : citas) {
                Map<String, Object> citaMap = new java.util.HashMap<>();
                citaMap.put("fecha", cita.getFechaHoraInicio().format(formatter));
                citaMap.put("odontologo", cita.getOdontologo() != null ? cita.getOdontologo().getNombreCompleto() : "N/A");
                citaMap.put("procedimiento", cita.getProcedimiento() != null ? cita.getProcedimiento().getNombre() : "N/A");
                citaMap.put("estado", cita.getEstadoCita() != null ? cita.getEstadoCita().getNombre() : "PENDIENTE");
                citaMap.put("motivo", cita.getMotivoConsulta());
                historialCitas.add(citaMap);
            }
        }
        detalle.put("historialCitas", historialCitas);

        // Odontograma (cargar desde OdontogramaDienteRepository)
        java.util.List<Map<String, Object>> odontograma = new java.util.ArrayList<>();
        if (paciente.getUsuario() != null) {
            java.util.List<OdontogramaDiente> dientes = odontogramaDienteRepository.findByPaciente(paciente.getUsuario());

            for (OdontogramaDiente diente : dientes) {
                Map<String, Object> dienteMap = new java.util.HashMap<>();
                dienteMap.put("numero", diente.getNumeroDiente());
                dienteMap.put("estado", diente.getEstado());
                dienteMap.put("notas", diente.getNotas());
                odontograma.add(dienteMap);
            }
        }

        // Si no hay registros de odontograma, retornar todos los dientes como SANO
        if (odontograma.isEmpty()) {
            for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
                int inicio = cuadrante == 1 ? 11 : cuadrante == 2 ? 21 : cuadrante == 3 ? 31 : 41;
                int fin = cuadrante == 1 ? 18 : cuadrante == 2 ? 28 : cuadrante == 3 ? 38 : 48;

                for (int num = inicio; num <= fin; num++) {
                    Map<String, Object> diente = new java.util.HashMap<>();
                    diente.put("numero", String.valueOf(num));
                    diente.put("estado", "SANO");
                    diente.put("notas", "");
                    odontograma.add(diente);
                }
            }
        }
        detalle.put("odontograma", odontograma);

        return ResponseEntity.ok(detalle);
    }

}