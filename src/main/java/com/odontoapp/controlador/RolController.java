package com.odontoapp.controlador;

import com.odontoapp.dto.RolDTO;
import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.servicio.RolService;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.DefaultValue;

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
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("rol", rolDTO);
            cargarPermisos(model);
            return "modulos/roles/formulario";
        }

        try {
            rolService.guardarRol(rolDTO);
            redirectAttributes.addFlashAttribute("success", "Rol guardado con éxito.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (rolDTO.getId() != null) {
                return "redirect:/roles/editar/" + rolDTO.getId();
            } else {
                return "redirect:/roles/nuevo";
            }
        }

        return "redirect:/roles";
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
            rolService.eliminarRol(id);
            redirectAttributes.addFlashAttribute("success", "Rol eliminado con éxito.");
        } catch (UnsupportedOperationException | DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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