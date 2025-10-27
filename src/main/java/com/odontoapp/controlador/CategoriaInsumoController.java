package com.odontoapp.controlador;

import com.odontoapp.entidad.CategoriaInsumo;
import com.odontoapp.servicio.CategoriaInsumoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categorias-insumo")
public class CategoriaInsumoController {

    private final CategoriaInsumoService categoriaService;

    public CategoriaInsumoController(CategoriaInsumoService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping("/gestion")
    public String obtenerModalGestion(Model model) {
        model.addAttribute("listaCategorias", categoriaService.listarTodasOrdenadasPorNombre());
        return "modulos/categorias_insumo/fragments :: gestionCategorias";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("categoria", new CategoriaInsumo());
        return "modulos/categorias_insumo/fragments :: formularioCategoria";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        return categoriaService.buscarPorId(id).map(cat -> {
            model.addAttribute("categoria", cat);
            return "modulos/categorias_insumo/fragments :: formularioCategoria";
        }).orElse("modulos/categorias_insumo/fragments :: errorCategoria");
    }

    @PostMapping("/guardar")
    public String guardarCategoria(@Valid @ModelAttribute("categoria") CategoriaInsumo categoria,
            BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Error: El nombre no puede estar vacío.");
            return "redirect:/insumos";
        }
        try {
            categoriaService.guardar(categoria);
            redirectAttributes.addFlashAttribute("success", "Categoría guardada con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/insumos";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarCategoria(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Categoría eliminada con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/insumos";
    }

    @GetMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoriaService.cambiarEstado(id);
            redirectAttributes.addFlashAttribute("success", "Estado cambiado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/insumos";
    }
}
