// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\controlador\ActivacionController.java
package com.odontoapp.controlador;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;

@Controller
public class ActivacionController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ActivacionController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Este endpoint es para usuarios creados por el administrador (Personal)
    @GetMapping("/activar-cuenta")
    public String mostrarFormularioActivacion(@RequestParam("token") String token, Model model) {
        Usuario usuario = usuarioRepository.findByVerificationToken(token).orElse(null);
        if (usuario == null || usuario.isEstaActivo()) {
            model.addAttribute("mensajeError", "El enlace de activación es inválido o ya ha sido utilizado.");
            return "publico/resultado-activacion";
        }
        model.addAttribute("token", token);
        return "publico/establecer-password";
    }

    @PostMapping("/establecer-password")
    public String establecerPassword(@RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes,
            Model model) {
        Usuario usuario = usuarioRepository.findByVerificationToken(token).orElse(null);

        if (usuario == null || usuario.isEstaActivo()) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "El enlace de activación es inválido o ya ha sido utilizado.");
            return "redirect:/resultado-activacion?error=true";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "publico/establecer-password";
        }

        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            return "publico/establecer-password";
        }

        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setEstaActivo(true);
        usuario.setVerificationToken(null);
        usuarioRepository.save(usuario);
        redirectAttributes.addFlashAttribute("success", "¡Tu cuenta ha sido activada! Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    @GetMapping("/resultado-activacion")
    public String mostrarResultadoActivacion(Model model) {
        if (!model.containsAttribute("error") && !model.containsAttribute("success")
                && !model.containsAttribute("mensajeError")) {
            model.addAttribute("info", "Página de resultado de activación.");
        }
        return "publico/resultado-activacion";
    }
}