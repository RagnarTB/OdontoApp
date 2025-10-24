// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\controlador\RegistroController.java
package com.odontoapp.controlador;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.odontoapp.dto.RegistroPacienteDTO;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.EmailService;
import com.odontoapp.servicio.PacienteService;

import jakarta.validation.Valid;

@Controller
public class RegistroController {

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PacienteService pacienteService;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    public RegistroController(UsuarioRepository usuarioRepository, EmailService emailService,
            PacienteService pacienteService, TipoDocumentoRepository tipoDocumentoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.pacienteService = pacienteService;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    @GetMapping("/registro")
    public String mostrarRegistroEmail() {
        return "publico/registro-email";
    }

    @PostMapping("/registro/enviar-link")
    public String enviarLinkVerificacion(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {

        try {
            Usuario usuarioTemp = pacienteService.crearUsuarioTemporalParaRegistro(email);
            emailService.enviarEmailActivacion(usuarioTemp.getEmail(), usuarioTemp.getNombreCompleto(), // Usar nombre
                                                                                                        // temporal
                    usuarioTemp.getVerificationToken());
            return "redirect:/registro/confirmacion";

        } catch (IllegalStateException e) {
            String mensaje = e.getMessage();
            // --- MANEJO MEJORADO DEL ERROR DE DUPLICADO ---
            if (mensaje != null && mensaje.startsWith("EMAIL_ELIMINADO_REGISTRO:")) {
                String emailEliminado = mensaje.split(":")[1];
                redirectAttributes.addFlashAttribute("error",
                        "El email '" + emailEliminado
                                + "' pertenece a una cuenta que fue eliminada. Por favor, contacta con la clínica para recuperarla.");
            } else {
                // Otro error (ej. email ya en uso por cuenta activa)
                redirectAttributes.addFlashAttribute("error", mensaje);
            }
            // --- FIN MANEJO MEJORADO ---
            return "redirect:/registro"; // Volver al formulario de email
        } catch (Exception e) {
            // Otros errores inesperados
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error inesperado al procesar tu solicitud.");
            System.err.println("Error en enviarLinkVerificacion: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/registro";
        }
    }

    @GetMapping("/registro/confirmacion")
    public String mostrarConfirmacion() {
        return "publico/registro-confirmacion";
    }

    @GetMapping("/registro/completar")
    public String mostrarFormularioCompleto(@RequestParam("token") String token, Model model,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioRepository.findByVerificationToken(token).orElse(null);

        if (usuario == null || usuario.isEstaActivo()) {
            redirectAttributes.addFlashAttribute("mensajeError",
                    "El enlace de registro es inválido o ya ha sido utilizado.");
            return "redirect:/resultado-activacion?error=true"; // Redirigir a la vista de error
        }

        RegistroPacienteDTO dto = new RegistroPacienteDTO();
        dto.setEmail(usuario.getEmail());

        cargarDatosFormulario(model, dto, token);

        return "publico/registro-formulario";
    }

    @PostMapping("/registro/completar")
    public String completarRegistro(@Valid @ModelAttribute("pacienteDTO") RegistroPacienteDTO registroDTO,
            BindingResult result,
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes) {

        // ✅ Validación de contraseña robusta
        String errorValidacion = com.odontoapp.util.PasswordUtil.validarPasswordRobusta(password);
        if (errorValidacion != null) {
            model.addAttribute("error", errorValidacion);
            cargarDatosFormulario(model, registroDTO, token);
            return "publico/registro-formulario";
        }

        // ❌ Validación de coincidencia
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            cargarDatosFormulario(model, registroDTO, token);
            return "publico/registro-formulario";
        }

        // ❗ Validación de DTO
        if (result.hasErrors()) {
            cargarDatosFormulario(model, registroDTO, token);
            return "publico/registro-formulario";
        }

        // --- Fin Validaciones ---

        try {
            pacienteService.completarRegistroPaciente(registroDTO, token, password);
            redirectAttributes.addFlashAttribute("success",
                    "¡Tu cuenta ha sido registrada y activada! Ya puedes iniciar sesión.");
            return "redirect:/login";

        } catch (IllegalStateException e) {
            // Token inválido, expirado o paciente ya activo
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/resultado-activacion?error=true";

        } catch (DataIntegrityViolationException e) {
            // 🔥 Manejo Mejorado → error claro de duplicidad (DNI o Email)
            model.addAttribute("error", e.getMessage());
            cargarDatosFormulario(model, registroDTO, token);
            return "publico/registro-formulario";

        } catch (Exception e) {
            // Error inesperado
            model.addAttribute("error", "Ocurrió un error inesperado al completar el registro.");
            cargarDatosFormulario(model, registroDTO, token);
            System.err.println("Error inesperado en completarRegistro: " + e.getMessage());
            e.printStackTrace();
            return "publico/registro-formulario";
        }
    }

    private void cargarDatosFormulario(Model model, RegistroPacienteDTO dto, String token) {
        model.addAttribute("pacienteDTO", dto);
        model.addAttribute("token", token);
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
    }
}