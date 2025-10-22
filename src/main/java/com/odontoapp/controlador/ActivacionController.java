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
            @RequestParam("confirmPassword") String confirmPassword, // Añadido
            RedirectAttributes redirectAttributes,
            Model model) { // Añadido Model
        Usuario usuario = usuarioRepository.findByVerificationToken(token).orElse(null);

        // Validar si el token es válido ANTES de otras validaciones
        if (usuario == null || usuario.isEstaActivo()) {
            redirectAttributes.addFlashAttribute("error",
                    "El enlace de activación es inválido o ya ha sido utilizado.");
            // Redirige a una vista que muestre el error, no directamente al login
            return "redirect:/resultado-activacion?error=true";
        }

        // Validación 1: Contraseñas coinciden
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token); // Volver a pasar el token a la vista
            model.addAttribute("error", "Las contraseñas no coinciden."); // Mensaje para el div de alerta
            return "publico/establecer-password"; // Volver al formulario
        }

        // Validación 2: Complejidad de la contraseña (Ejemplo: mínimo 8 caracteres)
        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            return "publico/establecer-password";
        }
        // Aquí puedes añadir más validaciones de complejidad si quieres (regex, etc.)

        // Si todo es válido, proceder a guardar
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setEstaActivo(true);
        usuario.setVerificationToken(null); // Invalidar el token [cite: 1871]
        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("success", "¡Tu cuenta ha sido activada! Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    // --- NUEVO MÉTODO GET PARA MOSTRAR RESULTADO ---
    @GetMapping("/resultado-activacion")
    public String mostrarResultadoActivacion(Model model) {
        // Este método solo muestra la vista. El mensaje viene por RedirectAttributes.
        // Añadimos un atributo por si se accede directamente sin error/success
        if (!model.containsAttribute("error") && !model.containsAttribute("success")) {
            model.addAttribute("info", "Página de resultado de activación.");
        }
        return "publico/resultado-activacion";
    }
}