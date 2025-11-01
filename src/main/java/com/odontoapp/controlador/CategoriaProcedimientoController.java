package com.odontoapp.controlador;

import com.odontoapp.entidad.CategoriaProcedimiento;
import com.odontoapp.servicio.CategoriaProcedimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador para gestionar categorías de procedimientos/servicios
 */
@Controller
@RequestMapping("/categorias-procedimiento")
@RequiredArgsConstructor
public class CategoriaProcedimientoController {

    private final CategoriaProcedimientoService categoriaService;

    /**
     * Endpoint para obtener el modal de gestión de categorías (AJAX)
     */
    @GetMapping("/gestion")
    public String obtenerModalGestion(Model model) {
        model.addAttribute("listaCategorias", categoriaService.listarTodasOrdenadasPorNombre());
        return "modulos/categorias_procedimiento/fragments :: gestionCategorias";
    }

    /**
     * Endpoint para mostrar formulario de nueva categoría (AJAX)
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("categoria", new CategoriaProcedimiento());
        return "modulos/categorias_procedimiento/fragments :: formularioCategoria";
    }

    /**
     * Endpoint para mostrar formulario de edición de categoría (AJAX)
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        return categoriaService.buscarPorId(id).map(cat -> {
            model.addAttribute("categoria", cat);
            return "modulos/categorias_procedimiento/fragments :: formularioCategoria";
        }).orElse("modulos/categorias_procedimiento/fragments :: errorCategoria");
    }

    /**
     * Guardar o actualizar categoría
     */
    @PostMapping("/guardar")
    public String guardarCategoria(
            @Valid @ModelAttribute("categoria") CategoriaProcedimiento categoria,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Error: El nombre no puede estar vacío.");
            return "redirect:/servicios";
        }

        try {
            categoriaService.guardar(categoria);
            redirectAttributes.addFlashAttribute("success", "Categoría guardada con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/servicios";
    }

    /**
     * Eliminar categoría (soft delete)
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarCategoria(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Categoría eliminada con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/servicios";
    }

    /**
     * Cambiar estado activo/inactivo de categoría
     */
    @GetMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.cambiarEstado(id);
            redirectAttributes.addFlashAttribute("success", "Estado cambiado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/servicios";
    }
}
