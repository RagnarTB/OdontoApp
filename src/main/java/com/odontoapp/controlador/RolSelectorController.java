package com.odontoapp.controlador;

import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Controlador para gestionar la selección de rol cuando un usuario tiene
 * múltiples roles.
 * Permite seleccionar el rol activo y cambiar entre roles sin cerrar sesión.
 */
@Controller
@RequestMapping("/seleccionar-rol")
public class RolSelectorController {

    private final UsuarioRepository usuarioRepository;

    public RolSelectorController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Muestra la página de selección de rol para usuarios con múltiples roles
     */
    @GetMapping
    public String mostrarSelectorRol(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener TODOS los roles del usuario (incluyendo PACIENTE)
        Set<Rol> rolesUsuario = usuario.getRoles();

        // Si solo tiene un rol, redirigir directamente
        if (rolesUsuario.size() == 1) {
            Rol rolUnico = rolesUsuario.iterator().next();
            session.setAttribute("rolActivo", rolUnico.getNombre());
            redirectAttributes.addFlashAttribute("info",
                    "Has iniciado sesión con el rol: " + rolUnico.getNombre());

            // Redirigir según el tipo de rol
            if ("PACIENTE".equals(rolUnico.getNombre())) {
                return "redirect:/paciente/dashboard";
            }
            return "redirect:/dashboard";
        }

        // Si no tiene roles, error
        if (rolesUsuario.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Tu cuenta no tiene roles activos. Contacta al administrador.");
            return "redirect:/login?logout";
        }

        // Verificar si ya tiene un rol activo seleccionado
        String rolActivo = (String) session.getAttribute("rolActivo");
        if (rolActivo != null) {
            // Si ya seleccionó un rol, mostrar opción de cambiar
            model.addAttribute("rolActual", rolActivo);
            model.addAttribute("modoSeleccion", false);
        } else {
            // Primer login, debe seleccionar
            model.addAttribute("modoSeleccion", true);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", rolesUsuario);

        return "publico/seleccionar-rol";
    }

    /**
     * Procesa la selección de rol y actualiza la sesión
     */
    @PostMapping("/seleccionar")
    public String seleccionarRol(
            @RequestParam("rolId") Long rolId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que el rol seleccionado pertenece al usuario
        Rol rolSeleccionado = usuario.getRoles().stream()
                .filter(rol -> rol.getId().equals(rolId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol no válido para este usuario"));

        // Guardar el rol activo en la sesión
        session.setAttribute("rolActivo", rolSeleccionado.getNombre());
        session.setAttribute("rolActivoId", rolSeleccionado.getId());

        // Actualizar las authorities del contexto de seguridad con el rol seleccionado
        actualizarAuthoritiesConRolSeleccionado(authentication, rolSeleccionado, usuario);

        redirectAttributes.addFlashAttribute("success",
                "Has seleccionado el rol: " + rolSeleccionado.getNombre());

        // Redirigir según el tipo de rol
        if ("PACIENTE".equals(rolSeleccionado.getNombre())) {
            return "redirect:/paciente/dashboard";
        }
        return "redirect:/dashboard";
    }

    /**
     * Permite cambiar de rol sin cerrar sesión
     */
    @GetMapping("/cambiar")
    public String cambiarRol(HttpSession session, Model model) {
        // Limpiar el rol activo de la sesión para forzar nueva selección
        session.removeAttribute("rolActivo");
        session.removeAttribute("rolActivoId");

        return "redirect:/seleccionar-rol";
    }

    /**
     * Actualiza las authorities del usuario en el contexto de seguridad
     * para reflejar solo el rol seleccionado (manteniendo permisos granulares)
     */
    private void actualizarAuthoritiesConRolSeleccionado(
            Authentication currentAuth,
            Rol rolSeleccionado,
            Usuario usuario) {

        List<GrantedAuthority> authorities = new ArrayList<>();

        // Agregar el rol seleccionado
        authorities.add(new SimpleGrantedAuthority("ROLE_" + rolSeleccionado.getNombre()));

        // Agregar todos los permisos asociados al rol seleccionado
        if (rolSeleccionado.getPermisos() != null) {
            rolSeleccionado.getPermisos().forEach(permiso -> {
                authorities.add(new SimpleGrantedAuthority(
                        permiso.getAccion() + "_" + permiso.getModulo()));
            });
        }

        // Si el usuario también es PACIENTE, mantener ese rol
        boolean esPaciente = usuario.getRoles().stream()
                .anyMatch(rol -> "PACIENTE".equals(rol.getNombre()));
        if (esPaciente) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PACIENTE"));
        }

        // Crear nueva autenticación con las authorities actualizadas
        PreAuthenticatedAuthenticationToken newAuth = new PreAuthenticatedAuthenticationToken(
                currentAuth.getPrincipal(),
                currentAuth.getCredentials(),
                authorities);

        // Actualizar el contexto de seguridad
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }
}
