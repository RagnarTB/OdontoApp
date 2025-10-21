package com.odontoapp.controlador;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ActivacionController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ActivacionController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
            RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioRepository.findByVerificationToken(token).orElse(null);
        if (usuario == null || usuario.isEstaActivo()) {
            redirectAttributes.addFlashAttribute("error", "Enlace inválido.");
            return "redirect:/login";
        }

        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setEstaActivo(true);
        usuario.setVerificationToken(null); // Invalidar el token
        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("success", "¡Tu cuenta ha sido activada! Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}