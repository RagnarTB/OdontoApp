package com.odontoapp.controlador;

import com.odontoapp.servicio.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Controlador para el Dashboard principal
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({ "/", "/dashboard" })
    public String verDashboard(Model model) {
        // Obtener todas las estadísticas del dashboard
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
    public String verDashboardPaciente() {
        return "paciente/dashboard";
    }
}
