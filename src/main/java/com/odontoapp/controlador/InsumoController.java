package com.odontoapp.controlador;

import com.odontoapp.dto.InsumoDTO;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.repositorio.CategoriaInsumoRepository;
import com.odontoapp.repositorio.UnidadMedidaRepository;
import com.odontoapp.servicio.InsumoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/insumos")
public class InsumoController {

    private final InsumoService insumoService;
    private final CategoriaInsumoRepository categoriaInsumoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    public InsumoController(InsumoService insumoService, CategoriaInsumoRepository categoriaInsumoRepository,
            UnidadMedidaRepository unidadMedidaRepository) {
        this.insumoService = insumoService;
        this.categoriaInsumoRepository = categoriaInsumoRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
    }

    @GetMapping
    public String listarInsumos(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword) {

        // 🔥 LÍNEA CLAVE: Carga las alertas de stock bajo y las envía a la vista
        model.addAttribute("alertasStockBajo", insumoService.listarConStockBajo());

        // Carga la tabla principal (esto ya lo tenías)
        Pageable pageable = PageRequest.of(page, size);
        Page<Insumo> paginaInsumos = insumoService.listarTodos(keyword, pageable);
        model.addAttribute("paginaInsumos", paginaInsumos);
        model.addAttribute("keyword", keyword);

        // Carga las categorías para el filtro (esto también lo tenías)
        model.addAttribute("todasLasCategorias", categoriaInsumoRepository.findAll());

        return "modulos/insumos/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("insumoDTO", new InsumoDTO());
        cargarCatalogos(model);
        return "modulos/insumos/formulario";
    }

    @PostMapping("/guardar")
    public String guardarInsumo(@Valid @ModelAttribute("insumoDTO") InsumoDTO dto,
            BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            cargarCatalogos(model);
            return "modulos/insumos/formulario";
        }
        try {
            insumoService.guardar(dto);
            redirectAttributes.addFlashAttribute("success", "Artículo guardado con éxito.");
            return "redirect:/insumos";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            cargarCatalogos(model);
            return "modulos/insumos/formulario";
        }
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return insumoService.buscarPorId(id).map(insumo -> {
            InsumoDTO dto = new InsumoDTO();
            dto.setId(insumo.getId());
            dto.setCodigo(insumo.getCodigo());
            dto.setNombre(insumo.getNombre());
            dto.setDescripcion(insumo.getDescripcion());
            dto.setMarca(insumo.getMarca());
            dto.setUbicacion(insumo.getUbicacion());
            dto.setLote(insumo.getLote());
            dto.setFechaVencimiento(insumo.getFechaVencimiento());
            dto.setStockMinimo(insumo.getStockMinimo());
            dto.setPrecioUnitario(insumo.getPrecioUnitario());
            dto.setCategoriaId(insumo.getCategoria().getId());
            dto.setUnidadMedidaId(insumo.getUnidadMedida().getId());

            model.addAttribute("insumoDTO", dto);
            cargarCatalogos(model);
            return "modulos/insumos/formulario";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Artículo no encontrado.");
            return "redirect:/insumos";
        });
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarInsumo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            insumoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Artículo eliminado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el artículo: " + e.getMessage());
        }
        return "redirect:/insumos";
    }

    private void cargarCatalogos(Model model) {
        model.addAttribute("categorias", categoriaInsumoRepository.findAll());
        model.addAttribute("unidadesMedida", unidadMedidaRepository.findAll());
    }
}


