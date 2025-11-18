package com.odontoapp.controlador;

import com.odontoapp.servicio.DashboardService;
import com.odontoapp.servicio.PacienteDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;

import java.util.Map;

/**
 * Controlador para el Dashboard principal
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final PacienteDashboardService pacienteDashboardService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping({ "/", "/dashboard" })
    public String verDashboard(Model model) {
        // Obtener todas las estadísticas del dashboard (para personal)
        Map<String, Object> estadisticas = dashboardService.obtenerEstadisticasGenerales();

        // Agregar las estadísticas al modelo
        model.addAttribute("citasDelDia", estadisticas.get("citasDelDia"));
        model.addAttribute("pacientesNuevos", estadisticas.get("pacientesNuevos"));
        model.addAttribute("ingresosPendientes", estadisticas.get("ingresosPendientes"));
        model.addAttribute("ingresosDelMes", estadisticas.get("ingresosDelMes"));
        model.addAttribute("totalPacientes", estadisticas.get("totalPacientes"));
        model.addAttribute("proximasCitas", estadisticas.get("proximasCitas"));
        model.addAttribute("insumosStockBajo", estadisticas.get("insumosStockBajo"));

        return "dashboard";
    }

    @GetMapping("/paciente/dashboard")
    public String verDashboardPaciente(Model model) {
        // Obtener el usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Buscar el usuario por email
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Obtener estadísticas del paciente
        Map<String, Object> estadisticas = pacienteDashboardService.obtenerEstadisticasPaciente(usuario.getId());

        // Agregar al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("proximasCitas", estadisticas.get("proximasCitas"));
        model.addAttribute("ultimaCita", estadisticas.get("ultimaCita"));
        model.addAttribute("tratamientosEnCurso", estadisticas.get("tratamientosEnCurso"));
        model.addAttribute("saldoPendiente", estadisticas.get("saldoPendiente"));
        model.addAttribute("citasCanceladas", estadisticas.get("citasCanceladas"));

        return "paciente/dashboard";
    }
}
