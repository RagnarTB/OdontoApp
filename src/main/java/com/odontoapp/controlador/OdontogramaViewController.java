package com.odontoapp.controlador;

import com.odontoapp.servicio.OdontogramaDienteService;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.entidad.Usuario;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador para las vistas del odontograma.
 */
@Controller
@RequestMapping("/odontograma")
public class OdontogramaViewController {

    private final OdontogramaDienteService odontogramaService;
    private final UsuarioRepository usuarioRepository;

    public OdontogramaViewController(
            OdontogramaDienteService odontogramaService,
            UsuarioRepository usuarioRepository) {
        this.odontogramaService = odontogramaService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Vista principal del odontograma de un paciente.
     *
     * Permite:
     * - Personal clínico (ODONTOLOGO, ADMIN, RECEPCIONISTA): Ver cualquier odontograma en modo edición
     * - Pacientes (PACIENTE): Ver SOLO su propio odontograma en modo solo lectura
     */
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    public String verOdontograma(
            @PathVariable Long pacienteId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            // Obtener usuario autenticado
            String emailUsuarioActual = authentication.getName();
            Usuario usuarioActual = usuarioRepository.findByEmail(emailUsuarioActual)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

            // Verificar si el usuario es paciente
            boolean esPaciente = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_PACIENTE"));

            // Si es paciente, solo puede ver su propio odontograma
            if (esPaciente) {
                // Verificar que el pacienteId corresponde al usuario autenticado
                if (!usuarioActual.getId().equals(pacienteId)) {
                    redirectAttributes.addFlashAttribute("error",
                        "No tienes permiso para ver el odontograma de otro paciente.");
                    return "redirect:/paciente/perfil";
                }
            }

            // Verificar que el paciente existe
            var paciente = usuarioRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

            // Obtener odontograma (se inicializa automáticamente si no existe)
            var dientes = odontogramaService.obtenerOdontogramaCompleto(pacienteId);
            var estadisticas = odontogramaService.obtenerEstadisticas(pacienteId);

            model.addAttribute("pacienteId", pacienteId);
            model.addAttribute("paciente", paciente);
            model.addAttribute("dientes", dientes);
            model.addAttribute("estadisticas", estadisticas);
            model.addAttribute("soloLectura", esPaciente); // ✅ Indica si es modo solo lectura

            return "modulos/odontograma/vista";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Error al cargar el odontograma: " + e.getMessage());

            // Redirigir según el tipo de usuario
            boolean esPaciente = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_PACIENTE"));

            if (esPaciente) {
                return "redirect:/paciente/perfil";
            } else {
                return "redirect:/pacientes/historial/" + pacienteId;
            }
        }
    }
}
