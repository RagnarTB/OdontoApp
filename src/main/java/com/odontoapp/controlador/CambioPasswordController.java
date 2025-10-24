package com.odontoapp.controlador;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.util.PasswordUtil;

@Controller
public class CambioPasswordController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CambioPasswordController(UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/cambiar-password-obligatorio")
    public String mostrarFormularioCambio(Authentication authentication, Model model) {
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(username).orElse(null);

        if (usuario == null || !usuario.isDebeActualizarPassword()) {
            return "redirect:/dashboard";
        }

        model.addAttribute("usuario", usuario);
        return "seguridad/cambio-password-obligatorio";
    }

    @PostMapping("/cambiar-password-obligatorio")
    public String cambiarPassword(
            Authentication authentication,
            @RequestParam("passwordActual") String passwordActual,
            @RequestParam("nuevaPassword") String nuevaPassword,
            @RequestParam("confirmarPassword") String confirmarPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(username).orElse(null);

        if (usuario == null || !usuario.isDebeActualizarPassword()) {
            return "redirect:/dashboard";
        }

        // Validar contraseña actual
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
            model.addAttribute("error", "La contraseña actual es incorrecta.");
            model.addAttribute("usuario", usuario);
            return "seguridad/cambio-password-obligatorio";
        }

        // Validar que las nuevas contraseñas coincidan
        if (!nuevaPassword.equals(confirmarPassword)) {
            model.addAttribute("error", "Las nuevas contraseñas no coinciden.");
            model.addAttribute("usuario", usuario);
            return "seguridad/cambio-password-obligatorio";
        }

        // 🔥 Validar robustez de la nueva contraseña
        String errorValidacion = PasswordUtil.validarPasswordRobusta(nuevaPassword);
        if (errorValidacion != null) {
            model.addAttribute("error", errorValidacion);
            model.addAttribute("usuario", usuario);
            return "seguridad/cambio-password-obligatorio";
        }

        // ---- NUEVA VALIDACIÓN ----
        // Validar que la nueva contraseña no sea igual a la actual (temporal)
        if (passwordEncoder.matches(nuevaPassword, usuario.getPassword())) {
            model.addAttribute("error", "La nueva contraseña no puede ser igual a la contraseña temporal actual.");
            model.addAttribute("usuario", usuario);
            return "seguridad/cambio-password-obligatorio";
        }

        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setDebeActualizarPassword(false);
        usuario.setPasswordTemporal(null);
        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("success",
                "¡Contraseña actualizada correctamente! Ya puedes usar el sistema.");

        return "redirect:/dashboard";
    }
}