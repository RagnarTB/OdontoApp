package com.odontoapp.controlador;

import java.util.List;
import java.util.Optional;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.odontoapp.dto.UsuarioDTO;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.UsuarioService;

import jakarta.validation.Valid;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository,
            RolRepository rolRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    // Muestra la lista de usuarios
    @GetMapping("/usuarios")
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

    // Muestra el formulario para crear un nuevo usuario
    @GetMapping("/usuarios/nuevo")
    public String mostrarFormularioNuevoUsuario(Model model) {
        UsuarioDTO usuario = new UsuarioDTO();
        model.addAttribute("usuario", usuario);
        cargarRolesActivos(model); // Usar método helper
        return "modulos/usuarios/formulario";
    }

    // Procesa el guardado de un nuevo usuario
    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@Valid @ModelAttribute("usuario") UsuarioDTO usuarioDTO,
            BindingResult result,
            Model model, // Añadir Model
            RedirectAttributes redirectAttributes) { // Añadir RedirectAttributes
        if (result.hasErrors()) {
            cargarRolesActivos(model); // Recargar roles si hay errores de DTO
            model.addAttribute("usuario", usuarioDTO); // Devolver el DTO con errores
            return "modulos/usuarios/formulario";
        }

        try {
            usuarioService.guardarUsuario(usuarioDTO);
            redirectAttributes.addFlashAttribute("success", "Usuario guardado con éxito.");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException | DataIntegrityViolationException e) {
            // Capturar errores de lógica de negocio o BD (email duplicado, etc.)
            cargarRolesActivos(model);
            model.addAttribute("usuario", usuarioDTO);
            model.addAttribute("errorValidacion", e.getMessage()); // Pasar mensaje de error específico
            return "modulos/usuarios/formulario";
        } catch (Exception e) {
            // Capturar otros errores inesperados
            cargarRolesActivos(model);
            model.addAttribute("usuario", usuarioDTO);
            model.addAttribute("errorValidacion", "Ocurrió un error inesperado al guardar el usuario.");
            System.err.println("Error al guardar usuario: " + e.getMessage()); // Loggear el error real
            e.printStackTrace(); // Imprimir stack trace para depuración
            return "modulos/usuarios/formulario";
        }
    }

    // Muestra el formulario para editar un usuario existentes
    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditarUsuario(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            UsuarioDTO usuarioDTO = new UsuarioDTO();
            usuarioDTO.setId(usuario.getId());
            usuarioDTO.setNombreCompleto(usuario.getNombreCompleto());
            usuarioDTO.setEmail(usuario.getEmail());
            // *** NO SE PASA LA CONTRASEÑA ***
            usuarioDTO.setRoles(usuario.getRoles().stream()
                    .map(Rol::getId)
                    .collect(Collectors.toList()));

            model.addAttribute("usuario", usuarioDTO);
            cargarRolesActivos(model); // Usar método helper
            return "modulos/usuarios/formulario";
        }
        redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
        return "redirect:/usuarios";
    }

    // Procesa la actualización de un usuario (usa el mismo endpoint POST /guardar)
    // Spring diferencia si es nuevo o edición por la presencia del ID en el DTO

    // Elimina un usuario
    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Obtenemos el email del usuario logueado para la validación de
        // auto-eliminación
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuarioActual = authentication.getName();

        Optional<Usuario> usuarioParaModificarOpt = usuarioService.buscarPorId(id);
        if (usuarioParaModificarOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/usuarios";
        }
        Usuario usuarioParaModificar = usuarioParaModificarOpt.get();

        // REGLAS DE NEGOCIO (ya están en el servicio, pero reforzamos en controlador)
        if (usuarioParaModificar.getEmail().equals(emailUsuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No puedes desactivar tu propio usuario.");
        } else {
            try {
                // Llama al servicio que ahora hace SOFT DELETE
                usuarioService.eliminarUsuario(id);
                redirectAttributes.addFlashAttribute("success", "Usuario eliminado lógicamente con éxito."); // <--
                                                                                                             // Mensaje
                                                                                                             // actualizado
            } catch (UnsupportedOperationException | DataIntegrityViolationException e) { // Añadir
                                                                                          // DataIntegrityViolationException
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Error al intentar eliminar lógicamente el usuario.");
                System.err.println("Error al eliminar (soft delete) usuario: " + e.getMessage());
            }
        }

        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/cambiar-estado/{id}")
    public String cambiarEstadoUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Regla de negocio: No puedes desactivarte a ti mismo
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Usuario usuarioActual = usuarioRepository.findByEmail(authentication.getName()).orElse(null);

            if (usuarioActual != null && usuarioActual.getId().equals(id)) {
                redirectAttributes.addFlashAttribute("error", "No puedes cambiar tu propio estado.");
                return "redirect:/usuarios";
            }

            // Llama al método que alterna el estado
            usuarioService.cambiarEstadoUsuario(id);
            // Determinar si se activó o desactivó para el mensaje
            Usuario usuarioCambiado = usuarioService.buscarPorId(id).orElse(null); // Volver a buscar para estado actual
            String accion = (usuarioCambiado != null && usuarioCambiado.isEstaActivo()) ? "activado" : "desactivado";
            redirectAttributes.addFlashAttribute("success", "Usuario " + accion + " con éxito.");

        } catch (UnsupportedOperationException | IllegalStateException e) { // Capturar excepciones específicas
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cambiar estado del usuario.");
            System.err.println("Error al cambiar estado usuario: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    // ... (dentro de la clase UsuarioController)

    @GetMapping("/usuarios/desbloquear/{id}")
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

    // --- MÉTODO HELPER ---
    private void cargarRolesActivos(Model model) {
        List<Rol> rolesActivos = rolRepository.findAll()
                .stream()
                .filter(Rol::isEstaActivo) // Asegúrate que Rol tenga el getter isEstaActivo()
                .collect(Collectors.toList());
        model.addAttribute("listaRoles", rolesActivos);
    }

} // Fin de la clase