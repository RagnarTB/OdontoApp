package com.odontoapp.controlador;

import com.odontoapp.servicio.OdontogramaDienteService;
import com.odontoapp.repositorio.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
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
     * Vista principal del odontograma de un paciente
     */
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA')")
    public String verOdontograma(
            @PathVariable Long pacienteId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
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

            return "modulos/odontograma/vista";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Error al cargar el odontograma: " + e.getMessage());
            return "redirect:/pacientes/historial/" + pacienteId;
        }
    }
}
