package com.odontoapp.controlador;

// --- Imports necesarios ---
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap; // Import Arrays
import java.util.List;
import java.util.Locale;
import java.util.Optional; // Import Locale
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.odontoapp.dto.HorarioExcepcionDTO;
import com.odontoapp.dto.UsuarioDTO;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.ReniecService;
import com.odontoapp.servicio.UsuarioService;
import com.odontoapp.dto.ReniecResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasAuthority('ADMIN')") // ✅ Solo ADMIN puede gestionar usuarios
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final ReniecService reniecService;

    // Constante para los días ordenados
    private static final List<DayOfWeek> DIAS_SEMANA_ORDENADOS = Arrays.asList(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    public UsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository,
            RolRepository rolRepository, TipoDocumentoRepository tipoDocumentoRepository,
            ReniecService reniecService) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.reniecService = reniecService;
    }

    @GetMapping
    public String listarUsuarios(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Usuario> paginaUsuarios = usuarioService.listarTodosLosUsuarios(keyword, pageable);
        model.addAttribute("paginaUsuarios", paginaUsuarios);
        model.addAttribute("keyword", keyword);
        return "modulos/usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoUsuario(Model model) {
        UsuarioDTO usuario = new UsuarioDTO();
        usuario.setHorarioRegular(new EnumMap<>(DayOfWeek.class));
        usuario.setExcepcionesHorario(new ArrayList<>());
        // Inicializar horario regular vacío para la vista
        for (DayOfWeek dia : DIAS_SEMANA_ORDENADOS) {
            usuario.getHorarioRegular().put(dia, "");
        }

        model.addAttribute("usuario", usuario);
        cargarRolesYTiposDoc(model);
        model.addAttribute("diasSemana", DIAS_SEMANA_ORDENADOS); // <-- Añadir lista al modelo
        model.addAttribute("localeEs", new Locale("es", "ES")); // <-- Locale para nombres de días
        return "modulos/usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@Valid @ModelAttribute("usuario") UsuarioDTO usuarioDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            cargarRolesYTiposDoc(model);
            model.addAttribute("diasSemana", DIAS_SEMANA_ORDENADOS); // <-- Añadir también en caso de error
            model.addAttribute("localeEs", new Locale("es", "ES")); // <-- Añadir también en caso de error
            // Asegurarse de que el mapa horarioRegular tenga todos los días si hay error de
            // validación
            if (usuarioDTO.getHorarioRegular() == null) {
                usuarioDTO.setHorarioRegular(new EnumMap<>(DayOfWeek.class));
            }
            for (DayOfWeek dia : DIAS_SEMANA_ORDENADOS) {
                usuarioDTO.getHorarioRegular().putIfAbsent(dia, "");
            }
            return "modulos/usuarios/formulario";
        }

        try {
            usuarioService.guardarUsuario(usuarioDTO);
            redirectAttributes.addFlashAttribute("success", "Usuario guardado con éxito.");
            return "redirect:/usuarios";

        } catch (DataIntegrityViolationException | IllegalArgumentException e) {
            cargarRolesYTiposDoc(model);
            model.addAttribute("usuario", usuarioDTO); // Devolver DTO con datos ingresados
            model.addAttribute("diasSemana", DIAS_SEMANA_ORDENADOS); // <-- Añadir también en caso de error
            model.addAttribute("localeEs", new Locale("es", "ES")); // <-- Añadir también en caso de error
            // Asegurarse de que el mapa horarioRegular tenga todos los días si hay error de
            // guardado
            if (usuarioDTO.getHorarioRegular() == null) {
                usuarioDTO.setHorarioRegular(new EnumMap<>(DayOfWeek.class));
            }
            for (DayOfWeek dia : DIAS_SEMANA_ORDENADOS) {
                usuarioDTO.getHorarioRegular().putIfAbsent(dia, "");
            }

            if (e instanceof DataIntegrityViolationException && e.getMessage() != null
                    && e.getMessage().startsWith("EMAIL_ELIMINADO:")) {
                try {
                    String[] parts = e.getMessage().split(":");
                    Long idUsuarioEliminado = Long.parseLong(parts[1]);
                    String emailEliminado = parts[2];
                    model.addAttribute("errorRestauracion",
                            "El email '" + emailEliminado + "' pertenece a un usuario eliminado.");
                    model.addAttribute("idUsuarioParaRestaurar", idUsuarioEliminado);
                } catch (Exception parseEx) {
                    model.addAttribute("errorValidacion", "El email ya existe (usuario eliminado, error al procesar).");
                }
            } else {
                model.addAttribute("errorValidacion", e.getMessage());
            }
            return "modulos/usuarios/formulario";

        } catch (Exception e) {
            cargarRolesYTiposDoc(model);
            model.addAttribute("usuario", usuarioDTO);
            model.addAttribute("diasSemana", DIAS_SEMANA_ORDENADOS); // <-- Añadir también en caso de error
            model.addAttribute("localeEs", new Locale("es", "ES")); // <-- Añadir también en caso de error
            // Asegurarse de que el mapa horarioRegular tenga todos los días si hay error
            // inesperado
            if (usuarioDTO.getHorarioRegular() == null) {
                usuarioDTO.setHorarioRegular(new EnumMap<>(DayOfWeek.class));
            }
            for (DayOfWeek dia : DIAS_SEMANA_ORDENADOS) {
                usuarioDTO.getHorarioRegular().putIfAbsent(dia, "");
            }
            model.addAttribute("errorValidacion", "Ocurrió un error inesperado. Contacte al administrador.");
            System.err.println("Error INESPERADO al guardar usuario: " + e.getMessage());
            e.printStackTrace();
            return "modulos/usuarios/formulario";
        }
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarUsuario(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            UsuarioDTO usuarioDTO = new UsuarioDTO();

            // --- Mapear campos básicos ---
            usuarioDTO.setId(usuario.getId());
            if (usuario.getTipoDocumento() != null) {
                usuarioDTO.setTipoDocumentoId(usuario.getTipoDocumento().getId());
            }
            usuarioDTO.setNumeroDocumento(usuario.getNumeroDocumento());
            usuarioDTO.setNombreCompleto(usuario.getNombreCompleto());
            usuarioDTO.setEmail(usuario.getEmail());
            usuarioDTO.setTelefono(usuario.getTelefono());
            usuarioDTO.setFechaNacimiento(usuario.getFechaNacimiento());
            usuarioDTO.setDireccion(usuario.getDireccion());
            usuarioDTO.setFechaContratacion(usuario.getFechaContratacion());
            usuarioDTO.setFechaVigencia(usuario.getFechaVigencia());
            usuarioDTO.setUltimoAcceso(usuario.getUltimoAcceso());
            usuarioDTO.setRoles(usuario.getRoles().stream()
                    .map(Rol::getId)
                    .collect(Collectors.toList()));

            // --- Mapear Horarios de Entidad a DTO ---
            usuarioDTO.setHorarioRegular(new EnumMap<>(DayOfWeek.class));
            // Asegurar que existan entradas para todos los días
            for (DayOfWeek dia : DIAS_SEMANA_ORDENADOS) {
                usuarioDTO.getHorarioRegular().put(dia,
                        (usuario.getHorarioRegular() != null && usuario.getHorarioRegular().containsKey(dia))
                                ? usuario.getHorarioRegular().get(dia)
                                : "");
            }

            usuarioDTO.setExcepcionesHorario(new ArrayList<>());
            if (usuario.getExcepcionesHorario() != null) {
                List<HorarioExcepcionDTO> excepcionesDTO = usuario.getExcepcionesHorario().stream()
                        .map(ex -> new HorarioExcepcionDTO(ex.getFecha(), ex.getHoras(), ex.getMotivo()))
                        .sorted((e1, e2) -> e1.getFecha().compareTo(e2.getFecha())) // Ordenar por fecha
                        .collect(Collectors.toList());
                usuarioDTO.setExcepcionesHorario(excepcionesDTO);
            }

            model.addAttribute("usuario", usuarioDTO);
            cargarRolesYTiposDoc(model);
            model.addAttribute("diasSemana", DIAS_SEMANA_ORDENADOS); // <-- Añadir lista al modelo
            model.addAttribute("localeEs", new Locale("es", "ES")); // <-- Locale para nombres de días
            return "modulos/usuarios/formulario";
        }
        redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
        return "redirect:/usuarios";
    }

    // --- Otros métodos (eliminar, cambiarEstado, desbloquear, restablecer) sin
    // cambios relevantes ---
    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuarioActual = authentication.getName();

        Optional<Usuario> usuarioParaModificarOpt = usuarioService.buscarPorId(id);
        if (usuarioParaModificarOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/usuarios";
        }
        Usuario usuarioParaModificar = usuarioParaModificarOpt.get();

        if (usuarioParaModificar.getEmail().equals(emailUsuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar tu propio usuario.");
        } else {
            try {
                usuarioService.eliminarUsuario(id);
                redirectAttributes.addFlashAttribute("success", "Usuario eliminado lógicamente con éxito.");
            } catch (UnsupportedOperationException | DataIntegrityViolationException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error al intentar eliminar lógicamente el usuario.");
                System.err.println("Error al eliminar (soft delete) usuario: " + e.getMessage());
            }
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/cambiar-estado/{id}")
    public String cambiarEstadoUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuarioActual = usuarioRepository.findByEmail(authentication.getName()).orElse(null);

            if (usuarioActual != null && usuarioActual.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "No puedes cambiar tu propio estado.");
                return "redirect:/usuarios";
            }

            usuarioService.cambiarEstadoUsuario(id);
            Usuario usuarioCambiado = usuarioService.buscarPorId(id).orElse(null);
            String accion = (usuarioCambiado != null && usuarioCambiado.isEstaActivo()) ? "activado" : "desactivado";
            redirectAttributes.addFlashAttribute("success", "Usuario " + accion + " con éxito.");

        } catch (UnsupportedOperationException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar estado del usuario.");
            System.err.println("Error al cambiar estado usuario: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/desbloquear/{id}")
    public String desbloquearUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(id);
        if (usuarioOpt.isPresent()) {
            usuarioService.resetearIntentosFallidos(usuarioOpt.get().getEmail());
            redirectAttributes.addFlashAttribute("success", "Usuario desbloqueado con éxito.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/restablecer/{id}")
    public String restablecerUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.restablecerUsuario(id);
            redirectAttributes.addFlashAttribute("success",
                    "Usuario restablecido con éxito. Se ha enviado un email con una nueva contraseña temporal.");
        } catch (RuntimeException e) { // Captura RuntimeException directamente
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Causa del error al restablecer: " + e.getCause().getMessage());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error inesperado al restablecer el usuario: " + e.getMessage());
            System.err.println("Error al restablecer usuario: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/usuarios";
    }

    // --- API REST PARA BÚSQUEDA DE DNI ---
    /**
     * Endpoint REST para consultar datos de una persona por DNI (RENIEC)
     * Compatible con el mismo formato que usa PacienteController
     *
     * @param numDoc Número de documento a buscar
     * @param tipoDocId ID del tipo de documento
     * @return ResponseEntity con datos de RENIEC o error
     */
    @GetMapping("/api/buscar-dni")
    @ResponseBody
    public ResponseEntity<?> buscarPorDni(
            @RequestParam("numDoc") String numDoc,
            @RequestParam("tipoDocId") Long tipoDocId) {
        try {
            // 1. Validar que el tipo de documento sea DNI
            var tipoDocumento = tipoDocumentoRepository.findById(tipoDocId).orElse(null);
            if (tipoDocumento == null || !"DNI".equals(tipoDocumento.getCodigo())) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "La consulta Reniec solo está disponible para DNI."));
            }

            // 2. Validar formato básico de DNI (8 dígitos)
            if (numDoc == null || !numDoc.matches("\\d{8}")) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "DNI debe tener 8 dígitos numéricos"));
            }

            // 3. Verificar si ya existe un usuario con ese DNI (activo)
            Optional<Usuario> usuarioExistente = usuarioRepository
                    .findByNumeroDocumentoAndTipoDocumento_Id(numDoc, tipoDocId);

            if (usuarioExistente.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("error", "Ya existe un usuario registrado con ese DNI."));
            }

            // 4. Consultar servicio RENIEC
            ReniecResponseDTO persona = reniecService.consultarDni(numDoc);

            if (persona != null && persona.getNombreCompleto() != null) {
                return ResponseEntity.ok(java.util.Map.of("nombreCompleto", persona.getNombreCompleto()));
            } else {
                return ResponseEntity.status(404)
                        .body(java.util.Map.of("error", "DNI no encontrado o datos incompletos. Verifique el número."));
            }

        } catch (Exception e) {
            System.err.println("Error en endpoint buscar-dni de usuarios: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("error", "Error al consultar el servicio de RENIEC"));
        }
    }

    // --- MÉTODO HELPER REFACTORIZADO ---
    private void cargarRolesYTiposDoc(Model model) {
        List<Rol> rolesActivos = rolRepository.findAll()
                .stream()
                .filter(Rol::isEstaActivo)
                // Excluimos PACIENTE de la lista de roles asignables manualmente aquí
                .filter(rol -> !"PACIENTE".equals(rol.getNombre()))
                .collect(Collectors.toList());
        model.addAttribute("listaRoles", rolesActivos);
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
    }

}
