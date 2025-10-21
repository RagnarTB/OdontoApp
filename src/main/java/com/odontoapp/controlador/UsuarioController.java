package com.odontoapp.controlador;

import com.odontoapp.dto.UsuarioDTO;
import com.odontoapp.entidad.Rol;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.servicio.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;

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
        // --- LÍNEA MODIFICADA ---
        List<Rol> rolesActivos = rolRepository.findAll().stream().filter(Rol::isEstaActivo)
                .collect(Collectors.toList());
        model.addAttribute("usuario", usuario);
        model.addAttribute("listaRoles", rolesActivos);
        return "modulos/usuarios/formulario";
    }

    // Procesa el guardado de un nuevo usuario
    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(@Valid @ModelAttribute("usuario") UsuarioDTO usuarioDTO,
            BindingResult result,
            Model model) {
        if (result.hasErrors()) {
            // Si hay errores de validación, volvemos a mostrar el formulario
            model.addAttribute("usuario", usuarioDTO);
            model.addAttribute("listaRoles", rolRepository.findAll());
            return "modulos/usuarios/formulario";
        }

        usuarioService.guardarUsuario(usuarioDTO);
        return "redirect:/usuarios";
    }
    // ... (métodos listarUsuarios, mostrarFormularioNuevoUsuario, guardarUsuario)

    // Muestra el formulario para editar un usuario existente
    @GetMapping("/usuarios/editar/{id}")
    public String mostrarFormularioEditarUsuario(@PathVariable Long id, Model model) {
        Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(id);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // Convertimos la entidad a DTO para el formulario
            UsuarioDTO usuarioDTO = new UsuarioDTO();
            usuarioDTO.setId(usuario.getId());
            usuarioDTO.setNombreCompleto(usuario.getNombreCompleto());
            usuarioDTO.setEmail(usuario.getEmail());
            // No enviamos la contraseña al formulario por seguridad
            usuarioDTO.setRoles(usuario.getRoles().stream()
                    .map(Rol::getId)
                    .collect(Collectors.toList()));

            // --- LÍNEA MODIFICADA ---
            // Solo lista los roles que están activos
            List<Rol> rolesActivos = rolRepository.findAll()
                    .stream()
                    .filter(Rol::isEstaActivo)
                    .collect(Collectors.toList());

            model.addAttribute("usuario", usuarioDTO);
            model.addAttribute("listaRoles", rolesActivos);
            return "modulos/usuarios/formulario"; // Reutilizamos la misma vista
        }

        return "redirect:/usuarios"; // Si no se encuentra, redirigir a la lista
    }

    // Procesa la actualización de un usuario (usa el mismo endpoint POST /guardar)
    // Spring diferencia si es nuevo o edición por la presencia del ID en el DTO

    // Elimina un usuario
    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Obtenemos el email del usuario que se quiere eliminar
        Optional<Usuario> usuarioParaEliminarOpt = usuarioService.buscarPorId(id);
        if (usuarioParaEliminarOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/usuarios";
        }
        String emailParaEliminar = usuarioParaEliminarOpt.get().getEmail();

        // Obtenemos el email del usuario logueado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUsuarioActual = authentication.getName();

        // REGLAS DE NEGOCIO
        if (emailParaEliminar.equals("admin@odontoapp.com")) {
            redirectAttributes.addFlashAttribute("error",
                    "No se puede eliminar al administrador principal del sistema.");
        } else if (emailParaEliminar.equals(emailUsuarioActual)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar tu propio usuario.");
        } else {
            usuarioService.eliminarUsuario(id);
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado con éxito.");
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

            usuarioService.cambiarEstadoUsuario(id);
            redirectAttributes.addFlashAttribute("success", "Estado del usuario cambiado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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

} // Fin de la clase