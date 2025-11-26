package com.odontoapp.controlador;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.util.PasswordUtil;

@Controller
@RequestMapping("/usuarios/cambiar-password")
public class CambioPasswordVoluntarioController {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CambioPasswordVoluntarioController(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String mostrarFormulario(Authentication authentication, Model model) {
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(username).orElse(null);
        model.addAttribute("usuario", usuario);
        return "seguridad/cambio-password-voluntario";
    }

    @PostMapping
    public String cambiarPassword(
            Authentication authentication,
            @RequestParam("passwordActual") String passwordActual,
            @RequestParam("nuevaPassword") String nuevaPassword,
            @RequestParam("confirmarPassword") String confirmarPassword,
            RedirectAttributes redirectAttributes,
            Model model) {
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(username).orElse(null);
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/paciente/perfil";
        }
        // Validar contraseña actual
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            model.addAttribute("error", "La contraseña actual es incorrecta.");
            model.addAttribute("usuario", usuario);
            return "seguridad/cambio-password-voluntario";
        }
        // Validar que las nuevas contraseñas coincidan
        if (!nuevaPassword.equals(confirmarPassword)) {
            model.addAttribute("error", "Las nuevas contraseñas no coinciden.");
            model.addAttribute("usuario", usuario);
            return "seguridad/cambio-password-voluntario";
        }
        // Validar robustez de la nueva contraseña
        String errorValidacion = PasswordUtil.validarPasswordRobusta(nuevaPassword);
        if (errorValidacion != null) {
            model.addAttribute("error", errorValidacion);
            model.addAttribute("usuario", usuario);
            return "seguridad/cambio-password-voluntario";
        }
        // Validar que la nueva contraseña no sea igual a la actual
        if (passwordEncoder.matches(nuevaPassword, usuario.getPassword())) {
            model.addAttribute("error", "La nueva contraseña no puede ser igual a la contraseña actual.");
            model.addAttribute("usuario", usuario);
            return "seguridad/cambio-password-voluntario";
        }
        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("success", "¡Contraseña actualizada correctamente!");

        // Redirigir según el rol activo
        String rolActivo = (String) authentication.getPrincipal();
        if ("PACIENTE".equals(rolActivo)) {
            return "redirect:/paciente/perfil";
        }
        return "redirect:/dashboard";
    }
}