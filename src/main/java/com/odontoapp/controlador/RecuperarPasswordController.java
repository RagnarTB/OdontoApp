package com.odontoapp.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.EmailService;
import com.odontoapp.util.PasswordUtil;

import java.util.UUID;
import java.time.LocalDateTime;

@Controller
public class RecuperarPasswordController {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public RecuperarPasswordController(UsuarioRepository usuarioRepository,
                                     EmailService emailService,
                                     PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Muestra el formulario para ingresar email
     */
    @GetMapping("/recuperar-password")
    public String mostrarFormulario() {
        return "publico/recuperar-password";
    }

    /**
     * Envía el email con el link de recuperación
     */
    @PostMapping("/recuperar-password/enviar")
    public String enviarEmailRecuperacion(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            // Buscar usuario por email (solo usuarios activos y no eliminados)
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .filter(u -> u.isEstaActivo() && !u.isEliminado())
                    .orElse(null);

            // Por seguridad, siempre mostramos el mensaje de éxito aunque el email no exista
            if (usuario == null) {
                // No revelar que el email no existe (prevención de enumeración de usuarios)
                return "redirect:/recuperar-password/confirmacion";
            }

            // Generar token único
            String token = UUID.randomUUID().toString();
            usuario.setPasswordResetToken(token);
            usuario.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));
            usuarioRepository.save(usuario);

            // Enviar email con el token
            emailService.enviarEmailRecuperacionPassword(
                usuario.getEmail(),
                usuario.getNombreCompleto(),
                token
            );

            return "redirect:/recuperar-password/confirmacion";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Ocurrió un error al procesar tu solicitud. Inténtalo nuevamente.");
            return "redirect:/recuperar-password";
        }
    }

    /**
     * Muestra la confirmación de envío del email
     */
    @GetMapping("/recuperar-password/confirmacion")
    public String mostrarConfirmacion() {
        return "publico/recuperar-password-confirmacion";
    }

    /**
     * Muestra el formulario para establecer nueva contraseña
     */
    @GetMapping("/recuperar-password/restablecer")
    public String mostrarFormularioRestablecer(@RequestParam("token") String token,
                                             Model model,
                                             RedirectAttributes redirectAttributes) {
        // Validar token
        Usuario usuario = usuarioRepository.findByPasswordResetToken(token).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error",
                "El link de recuperación es inválido o ya ha sido utilizado.");
            return "redirect:/login?error=true";
        }

        // Verificar que el token no haya expirado
        if (usuario.getPasswordResetTokenExpiry() == null ||
            usuario.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error",
                "El link de recuperación ha expirado. Solicita uno nuevo.");
            return "redirect:/recuperar-password";
        }

        model.addAttribute("token", token);
        model.addAttribute("email", usuario.getEmail());
        return "publico/restablecer-password";
    }

    /**
     * Procesa el restablecimiento de contraseña
     */
    @PostMapping("/recuperar-password/restablecer")
    public String restablecerPassword(@RequestParam("token") String token,
                                    @RequestParam("password") String password,
                                    @RequestParam("confirmPassword") String confirmPassword,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        // Buscar usuario por token
        Usuario usuario = usuarioRepository.findByPasswordResetToken(token).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error",
                "El link de recuperación es inválido o ya ha sido utilizado.");
            return "redirect:/login?error=true";
        }

        // Verificar expiración
        if (usuario.getPasswordResetTokenExpiry() == null ||
            usuario.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("error",
                "El link de recuperación ha expirado. Solicita uno nuevo.");
            return "redirect:/recuperar-password";
        }

        // Validar contraseña robusta
        String errorValidacion = PasswordUtil.validarPasswordRobusta(password);
        if (errorValidacion != null) {
            model.addAttribute("error", errorValidacion);
            model.addAttribute("token", token);
            model.addAttribute("email", usuario.getEmail());
            return "publico/restablecer-password";
        }

        // Validar coincidencia
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("token", token);
            model.addAttribute("email", usuario.getEmail());
            return "publico/restablecer-password";
        }

        try {
            // Actualizar contraseña
            usuario.setPassword(passwordEncoder.encode(password));

            // Limpiar tokens de recuperación
            usuario.setPasswordResetToken(null);
            usuario.setPasswordResetTokenExpiry(null);

            // Si el usuario tenía que actualizar password, ya no es necesario
            usuario.setDebeActualizarPassword(false);

            usuarioRepository.save(usuario);

            redirectAttributes.addFlashAttribute("success",
                "¡Tu contraseña ha sido restablecida exitosamente! Ya puedes iniciar sesión.");
            return "redirect:/login?fromRegistro=true";

        } catch (Exception e) {
            model.addAttribute("error",
                "Ocurrió un error al restablecer tu contraseña. Inténtalo nuevamente.");
            model.addAttribute("token", token);
            model.addAttribute("email", usuario.getEmail());
            return "publico/restablecer-password";
        }
    }
}
