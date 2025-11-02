package com.odontoapp.controlador;

import com.odontoapp.dto.ProcedimientoDTO;
import com.odontoapp.entidad.CategoriaProcedimiento;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.repositorio.CategoriaProcedimientoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.servicio.ProcedimientoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.odontoapp.dto.CategoriaServicioDTO;
import java.util.stream.Collectors;

import java.util.List;

@Controller
@RequestMapping("/servicios")
public class ProcedimientoController {

    private final ProcedimientoService procedimientoService;
    private final CategoriaProcedimientoRepository categoriaRepository;
    private final ProcedimientoRepository procedimientoRepository;

    public ProcedimientoController(ProcedimientoService procedimientoService,
            CategoriaProcedimientoRepository categoriaRepository,
            ProcedimientoRepository procedimientoRepository) {
        this.procedimientoService = procedimientoService;
        this.categoriaRepository = categoriaRepository;
        this.procedimientoRepository = procedimientoRepository;
    }

    @GetMapping
    public String listarProcedimientos(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        // Cargar resumen de categorías
        List<CategoriaServicioDTO> categoriasConteo = categoriaRepository.findAll().stream()
                .map(cat -> new CategoriaServicioDTO(
                        cat.getId(),
                        cat.getNombre(),
                        cat.getIcono(),
                        cat.getColor(),
                        procedimientoRepository.countByCategoriaId(cat.getId()) // Necesitarás este método en el repo
                )).collect(Collectors.toList());
        model.addAttribute("categoriasConteo", categoriasConteo);

        // Cargar lista paginada de servicios (esto se mantiene)
        Pageable pageable = PageRequest.of(page, size);
        Page<Procedimiento> paginaProcedimientos = procedimientoService.listarTodos(keyword, pageable);
        model.addAttribute("paginaProcedimientos", paginaProcedimientos);
        model.addAttribute("keyword", keyword);

        // Cargar todas las categorías para el filtro dropdown
        model.addAttribute("todasLasCategorias", categoriaRepository.findAll());

        return "modulos/servicios/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("procedimientoDTO", new ProcedimientoDTO());
        cargarCategorias(model);
        return "modulos/servicios/formulario";
    }

    @PostMapping("/guardar")
    public String guardarProcedimiento(@Valid @ModelAttribute("procedimientoDTO") ProcedimientoDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            cargarCategorias(model);
            return "modulos/servicios/formulario";
        }
        try {
            procedimientoService.guardar(dto);
            redirectAttributes.addFlashAttribute("success", "Servicio guardado con éxito.");
            return "redirect:/servicios";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            cargarCategorias(model);
            return "modulos/servicios/formulario";
        }
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return procedimientoService.buscarPorId(id).map(proc -> {
            ProcedimientoDTO dto = new ProcedimientoDTO();
            dto.setId(proc.getId());
            dto.setCodigo(proc.getCodigo());
            dto.setNombre(proc.getNombre());
            dto.setDescripcion(proc.getDescripcion());
            dto.setPrecioBase(proc.getPrecioBase());
            dto.setDuracionBaseMinutos(proc.getDuracionBaseMinutos());
            dto.setCategoriaId(proc.getCategoria().getId());

            model.addAttribute("procedimientoDTO", dto);

            // Para edición, cargar todas las categorías activas
            // más la categoría actual del procedimiento (aunque esté inactiva)
            List<CategoriaProcedimiento> categorias = categoriaRepository.findAll().stream()
                    .filter(cat -> cat.isEstaActiva() || cat.getId().equals(proc.getCategoria().getId()))
                    .collect(Collectors.toList());
            model.addAttribute("categorias", categorias);

            return "modulos/servicios/formulario";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Servicio no encontrado.");
            return "redirect:/servicios";
        });
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProcedimiento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            procedimientoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Servicio eliminado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el servicio: " + e.getMessage());
        }
        return "redirect:/servicios";
    }

    private void cargarCategorias(Model model) {
        // Cargar solo categorías activas para formularios
        List<CategoriaProcedimiento> categorias = categoriaRepository.findAll().stream()
                .filter(CategoriaProcedimiento::isEstaActiva)
                .collect(Collectors.toList());
        model.addAttribute("categorias", categorias);
    }
}
