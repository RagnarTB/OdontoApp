package com.odontoapp.controlador;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.odontoapp.dto.RolDTO;
import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Rol;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.servicio.RolService;

import jakarta.validation.Valid;

@Controller
public class RolController {

    private final RolService rolService;
    private final PermisoRepository permisoRepository;

    public RolController(RolService rolService, PermisoRepository permisoRepository) {
        this.rolService = rolService;
        this.permisoRepository = permisoRepository;
    }

    @GetMapping("/roles")
    public String listarRoles(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Rol> paginaRoles = rolService.listarTodosLosRoles(keyword, pageable);

        model.addAttribute("paginaRoles", paginaRoles);
        model.addAttribute("keyword", keyword);
        return "modulos/roles/lista";
    }

    @GetMapping("/roles/nuevo")
    public String mostrarFormularioNuevoRol(Model model) {
        model.addAttribute("rol", new RolDTO());
        cargarPermisos(model);
        return "modulos/roles/formulario";
    }

    @PostMapping("/roles/guardar")
    public String guardarRol(@Valid @ModelAttribute("rol") RolDTO rolDTO,
            BindingResult result,
            Model model, // Usar Model para errores en el formulario
            RedirectAttributes redirectAttributes) {

        // Validación del DTO
        if (result.hasErrors()) {
            cargarPermisos(model); // Recargar permisos necesarios para la vista
            model.addAttribute("rol", rolDTO); // Devolver DTO con errores
            // Los errores de campo se mostrarán automáticamente por Thymeleaf
            return "modulos/roles/formulario";
        }

        try {
            rolService.guardarRol(rolDTO);
            redirectAttributes.addFlashAttribute("success", "Rol guardado con éxito.");
            return "redirect:/roles"; // Redirigir a la lista si todo OK

        } catch (DataIntegrityViolationException e) {
            // Error de duplicado u otro error de integridad capturado del servicio
            cargarPermisos(model);
            model.addAttribute("rol", rolDTO); // Devolver datos al formulario
            model.addAttribute("errorValidacion", e.getMessage()); // Pasar el mensaje de error a la vista
            return "modulos/roles/formulario"; // Volver al formulario

        } catch (Exception e) {
            // Otros errores inesperados
            cargarPermisos(model);
            model.addAttribute("rol", rolDTO);
            model.addAttribute("errorValidacion", "Ocurrió un error inesperado al guardar el rol.");
            System.err.println("Error inesperado al guardar rol: " + e.getMessage());
            e.printStackTrace();
            return "modulos/roles/formulario";
        }
    }

    @GetMapping("/roles/editar/{id}")
    public String mostrarFormularioEditarRol(@PathVariable Long id, Model model) {
        Rol rol = rolService.buscarRolPorId(id).orElse(null);
        if (rol != null) {
            RolDTO rolDTO = new RolDTO();
            rolDTO.setId(rol.getId());
            rolDTO.setNombre(rol.getNombre());
            if (rol.getPermisos() != null) {
                rolDTO.setPermisos(rol.getPermisos().stream().map(Permiso::getId).collect(Collectors.toList()));
            }

            model.addAttribute("rol", rolDTO);
            cargarPermisos(model);
            return "modulos/roles/formulario";
        }
        return "redirect:/roles";
    }

    @GetMapping("/roles/eliminar/{id}")
    public String eliminarRol(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rolService.eliminarRol(id); // Llama al método que hace soft delete
            redirectAttributes.addFlashAttribute("success", "Rol eliminado lógicamente con éxito."); // <-- Mensaje
                                                                                                     // actualizado
        } catch (UnsupportedOperationException | DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al intentar eliminar lógicamente el rol.");
            System.err.println("Error al eliminar (soft delete) rol: " + e.getMessage());
        }
        return "redirect:/roles";
    }

    @GetMapping("/roles/cambiar-estado/{id}")
    public String cambiarEstadoRol(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rolService.cambiarEstadoRol(id);
            redirectAttributes.addFlashAttribute("success", "Estado del rol cambiado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/roles";
    }

    private void cargarPermisos(Model model) {
        Map<String, List<Permiso>> permisosAgrupados = permisoRepository.findAll()
                .stream()
                .sorted((p1, p2) -> p1.getModulo().compareTo(p2.getModulo()))
                .collect(Collectors.groupingBy(Permiso::getModulo));
        model.addAttribute("permisosAgrupados", permisosAgrupados);
    }
}