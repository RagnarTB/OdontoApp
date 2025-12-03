package com.odontoapp.controlador;

import com.odontoapp.servicio.ReporteService;
import com.odontoapp.servicio.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReporteControlador {

    private final ReporteService reporteService;
    private final UsuarioService usuarioService;

    @GetMapping
    public String index(Model model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long odontologoId,
            RedirectAttributes redirectAttributes) {

        // Valores por defecto: Último mes si no se especifica
        if (fechaInicio == null) {
            fechaInicio = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        }
        if (fechaFin == null) {
            fechaFin = LocalDate.now();
        }

        // ===== VALIDACIONES =====
        LocalDate hoy = LocalDate.now();

        // Validar que las fechas no sean futuras
        if (fechaInicio.isAfter(hoy)) {
            redirectAttributes.addFlashAttribute("error", "La fecha de inicio no puede ser futura.");
            return "redirect:/reportes";
        }

        if (fechaFin.isAfter(hoy)) {
            redirectAttributes.addFlashAttribute("error", "La fecha de fin no puede ser futura.");
            return "redirect:/reportes";
        }

        // Validar que fechaInicio <= fechaFin
        if (fechaInicio.isAfter(fechaFin)) {
            redirectAttributes.addFlashAttribute("error",
                    "La fecha de inicio no puede ser posterior a la fecha de fin.");
            return "redirect:/reportes";
        }

        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("odontologoId", odontologoId);

        // Cargar listas para filtros
        model.addAttribute("odontologos", usuarioService.listarPorRol("ODONTOLOGO"));

        // Cargar datos de reportes con filtros
        model.addAttribute("ingresosPorMetodo", reporteService.obtenerIngresosPorMetodoPago(fechaInicio, fechaFin));
        model.addAttribute("ingresosPorMes", reporteService.obtenerIngresosPorMes(fechaInicio, fechaFin));
        model.addAttribute("citasPorEstado", reporteService.obtenerCitasPorEstado(fechaInicio, fechaFin, odontologoId));
        model.addAttribute("topTratamientos",
                reporteService.obtenerTopTratamientos(fechaInicio, fechaFin, odontologoId));
        model.addAttribute("nuevosPacientes", reporteService.obtenerNuevosPacientesPorMes(fechaInicio, fechaFin));

        return "modulos/reportes/index";
    }

    @GetMapping("/exportar/excel")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EXPORTAR_REPORTES)")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) Long odontologoId) throws IOException {

        if (fechaInicio == null)
            fechaInicio = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        if (fechaFin == null)
            fechaFin = LocalDate.now();

        byte[] excelContent = reporteService.generarReporteExcel(fechaInicio, fechaFin, odontologoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "reporte_general.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelContent);
    }
}
