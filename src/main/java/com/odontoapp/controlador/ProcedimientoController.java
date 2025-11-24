package com.odontoapp.controlador;

import com.odontoapp.dto.ProcedimientoDTO;
import com.odontoapp.entidad.CategoriaProcedimiento;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.repositorio.CategoriaProcedimientoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.servicio.ProcedimientoService;
import com.odontoapp.util.Permisos;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.odontoapp.dto.CategoriaServicioDTO;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;
import com.odontoapp.entidad.ProcedimientoInsumo;
import org.springframework.http.ResponseEntity;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Controller
@RequestMapping("/servicios")
public class ProcedimientoController {

    private final ProcedimientoService procedimientoService;
    private final CategoriaProcedimientoRepository categoriaRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
    private final InsumoRepository insumoRepository;

    public ProcedimientoController(ProcedimientoService procedimientoService,
            CategoriaProcedimientoRepository categoriaRepository,
            ProcedimientoRepository procedimientoRepository,
            ProcedimientoInsumoRepository procedimientoInsumoRepository,
            InsumoRepository insumoRepository) {
        this.procedimientoService = procedimientoService;
        this.categoriaRepository = categoriaRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
        this.insumoRepository = insumoRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_SERVICIOS)")
    public String listarProcedimientos(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoriaId) {

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

        // Cargar lista paginada de servicios con filtro de categoría
        Pageable pageable = PageRequest.of(page, size);
        Page<Procedimiento> paginaProcedimientos;

        if (categoriaId != null) {
            // Filtrar por categoría específica
            if (keyword != null && !keyword.isBlank()) {
                // Buscar por keyword dentro de la categoría
                paginaProcedimientos = procedimientoRepository.findByCategoriaIdAndKeyword(categoriaId, keyword, pageable);
            } else {
                // Solo filtrar por categoría
                paginaProcedimientos = procedimientoRepository.findByCategoriaId(categoriaId, pageable);
            }
        } else {
            // Sin filtro de categoría, buscar por keyword o listar todos
            paginaProcedimientos = procedimientoService.listarTodos(keyword, pageable);
        }

        model.addAttribute("paginaProcedimientos", paginaProcedimientos);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("mostrarEliminados", false);

        // Cargar todas las categorías para el filtro dropdown
        model.addAttribute("todasLasCategorias", categoriaRepository.findAll());

        return "modulos/servicios/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_SERVICIOS)")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("procedimientoDTO", new ProcedimientoDTO());
        cargarDatosFormulario(model);
        return "modulos/servicios/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority(T(com.odontoapp.util.Permisos).CREAR_SERVICIOS, T(com.odontoapp.util.Permisos).EDITAR_SERVICIOS)")
    public String guardarProcedimiento(@Valid @ModelAttribute("procedimientoDTO") ProcedimientoDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            cargarDatosFormulario(model);
            return "modulos/servicios/formulario";
        }
        try {
            procedimientoService.guardar(dto);
            redirectAttributes.addFlashAttribute("success", "Servicio guardado con éxito.");
            return "redirect:/servicios";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            cargarDatosFormulario(model);
            return "modulos/servicios/formulario";
        }
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_SERVICIOS)")
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

            // Cargar insumos con sus relaciones
            List<Insumo> insumos = insumoRepository.findAllWithRelations();
            model.addAttribute("insumos", insumos);

            return "modulos/servicios/formulario";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Servicio no encontrado.");
            return "redirect:/servicios";
        });
    }

    @GetMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_SERVICIOS)")
    public String eliminarProcedimiento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            procedimientoService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Servicio eliminado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el servicio: " + e.getMessage());
        }
        return "redirect:/servicios";
    }

    /**
     * Endpoint para obtener insumos de un procedimiento (usado por el formulario)
     */
    @GetMapping("/{id}/insumos")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_DETALLE_SERVICIOS)")
    @ResponseBody
    public ResponseEntity<?> obtenerInsumosDeProcedimiento(@PathVariable Long id) {
        try {
            List<ProcedimientoInsumo> insumos = procedimientoInsumoRepository.findByProcedimientoId(id);

            List<Map<String, Object>> resultado = insumos.stream()
                .map(pi -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("insumoId", pi.getInsumo().getId());
                    map.put("insumoNombre", pi.getInsumo().getNombre() + " (" + pi.getInsumo().getCodigo() + ")");
                    map.put("cantidadDefecto", pi.getCantidadDefecto());
                    map.put("unidad", pi.getUnidad());
                    map.put("esObligatorio", pi.isEsObligatorio());
                    return map;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/restablecer/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).RESTAURAR_SERVICIOS)")
    public String restablecerProcedimiento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            procedimientoService.restablecer(id);
            redirectAttributes.addFlashAttribute("success", "Servicio restablecido con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al restablecer el servicio: " + e.getMessage());
        }
        return "redirect:/servicios";
    }

    private void cargarDatosFormulario(Model model) {
        // Cargar solo categorías activas para formularios
        List<CategoriaProcedimiento> categorias = categoriaRepository.findAll().stream()
                .filter(CategoriaProcedimiento::isEstaActiva)
                .collect(Collectors.toList());
        model.addAttribute("categorias", categorias);

        // Cargar todos los insumos activos con sus relaciones (categoria y unidadMedida)
        List<Insumo> insumos = insumoRepository.findAllWithRelations();
        model.addAttribute("insumos", insumos);
    }
}
