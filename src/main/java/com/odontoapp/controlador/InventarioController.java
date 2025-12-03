package com.odontoapp.controlador;

import com.odontoapp.dto.MovimientoDTO;
import com.odontoapp.entidad.MovimientoInventario;
import com.odontoapp.repositorio.MotivoMovimientoRepository;
import com.odontoapp.repositorio.TipoMovimientoRepository;
import com.odontoapp.servicio.InventarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final InventarioService inventarioService;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;

    public InventarioController(InventarioService inventarioService, TipoMovimientoRepository tipoMovimientoRepository,
            MotivoMovimientoRepository motivoMovimientoRepository) {
        this.inventarioService = inventarioService;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
    }

    @GetMapping("/movimientos/nuevo/{insumoId}")
    public String getNuevoMovimientoForm(@PathVariable Long insumoId, Model model) {
        MovimientoDTO movimientoDTO = new MovimientoDTO();
        movimientoDTO.setInsumoId(insumoId);

        model.addAttribute("movimientoDTO", movimientoDTO);
        model.addAttribute("tiposMovimiento", tipoMovimientoRepository.findAll());

        // ✅ FILTRAR SOLO MOTIVOS MANUALES (esManual = true)
        // Combinar motivos de entrada y salida manuales
        List<com.odontoapp.entidad.MotivoMovimiento> motivosManuales = new ArrayList<>();
        motivosManuales.addAll(motivoMovimientoRepository.findByTipoMovimientoCodigoAndEsManual("ENTRADA", true));
        motivosManuales.addAll(motivoMovimientoRepository.findByTipoMovimientoCodigoAndEsManual("SALIDA", true));

        model.addAttribute("motivosMovimiento", motivosManuales);

        return "modulos/insumos/fragments :: modalMovimiento";
    }

    @PostMapping("/movimientos/registrar")
    public String registrarMovimiento(@Valid @ModelAttribute("movimientoDTO") MovimientoDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // Manejar errores de validación si es necesario, aunque el modal es simple
            redirectAttributes.addFlashAttribute("error",
                    "Datos inválidos. " + result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/insumos";
        }
        try {
            inventarioService.registrarMovimiento(dto);
            redirectAttributes.addFlashAttribute("success", "Movimiento registrado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar movimiento: " + e.getMessage());
        }
        return "redirect:/insumos";
    }

    // Endpoint para cargar la tabla de movimientos vía AJAX
    @GetMapping("/movimientos/historial/{insumoId}")
    public String getHistorialMovimientos(@PathVariable Long insumoId,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        try {
            Page<MovimientoInventario> paginaMovimientos = inventarioService.listarMovimientosPorInsumo(insumoId,
                    PageRequest.of(page, 10));

            // Si la página es null o vacía, pasar lista vacía para evitar error 500
            if (paginaMovimientos == null) {
                model.addAttribute("paginaMovimientos", Page.empty());
            } else {
                model.addAttribute("paginaMovimientos", paginaMovimientos);
            }

        } catch (Exception e) {
            System.err.println("❌ ERROR al cargar historial de movimientos para insumo ID " + insumoId + ":");
            System.err.println("   Mensaje: " + e.getMessage());
            System.err.println("   Tipo: " + e.getClass().getName());
            e.printStackTrace(); // ← Stack trace completo para debugging
            // En caso de error, pasar página vacía para que la vista pueda renderizar
            model.addAttribute("paginaMovimientos", Page.empty());
        }

        // Se devuelve la ruta al fragmento HTML, no los datos JSON
        return "modulos/insumos/fragments :: historialMovimientos";
    }
}
