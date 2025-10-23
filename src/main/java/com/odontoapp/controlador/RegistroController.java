// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\controlador\RegistroController.java
package com.odontoapp.controlador;

import com.odontoapp.dto.RegistroPacienteDTO;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.EmailService;
import com.odontoapp.servicio.PacienteService;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

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

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmailIgnorandoSoftDelete(email);
        if (usuarioExistente.isPresent() && usuarioExistente.get().isEstaActivo()
                && !usuarioExistente.get().isEliminado()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe una cuenta activa con este email.");
            return "redirect:/registro";
        }

        try {
            Usuario usuarioTemp = pacienteService.crearUsuarioTemporalParaRegistro(email);
            emailService.enviarEmailActivacion(usuarioTemp.getEmail(), usuarioTemp.getNombreCompleto(),
                    usuarioTemp.getVerificationToken());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/registro";
        }

        return "redirect:/registro/confirmacion";
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

        // Validaciones de Contraseña (las hacemos aquí porque el DTO extiende
        // PacienteDTO y no UsuarioDTO)
        if (!password.equals(confirmPassword)) {
            result.rejectValue("global", "error.global", "Las contraseñas no coinciden.");
        }
        if (password.length() < 8) {
            result.rejectValue("global", "error.global", "La contraseña debe tener al menos 8 caracteres.");
        }

        if (result.hasErrors()) {
            cargarDatosFormulario(model, registroDTO, token);
            return "publico/registro-formulario";
        }

        try {
            pacienteService.completarRegistroPaciente(registroDTO, token, password);
            redirectAttributes.addFlashAttribute("success",
                    "¡Tu cuenta ha sido registrada y activada! Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (IllegalStateException e) {
            // Error de token inválido o ya activo
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/resultado-activacion?error=true";
        } catch (Exception e) {
            // Error de duplicidad DNI, email, etc.
            model.addAttribute("error", e.getMessage());
            cargarDatosFormulario(model, registroDTO, token);
            return "publico/registro-formulario";
        }
    }

    private void cargarDatosFormulario(Model model, RegistroPacienteDTO dto, String token) {
        model.addAttribute("pacienteDTO", dto);
        model.addAttribute("token", token);
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll());
    }
}