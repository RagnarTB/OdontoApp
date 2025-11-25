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
import org.springframework.web.bind.annotation.RequestMapping; // Añadir RequestMapping
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.odontoapp.dto.RolDTO;
import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Rol;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.servicio.RolService;
import com.odontoapp.util.Permisos;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/roles")
public class RolController {

    private final RolService rolService;
    private final PermisoRepository permisoRepository;

    // Nombres de roles protegidos
    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_PACIENTE = "PACIENTE";
    private static final String ROL_ODONTOLOGO = "ODONTOLOGO";

    public RolController(RolService rolService, PermisoRepository permisoRepository) {
        this.rolService = rolService;
        this.permisoRepository = permisoRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).VER_LISTA_ROLES)")
    public String listarRoles(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        // Búsqueda insensible a mayúsculas si los nombres se guardan en mayúsculas
        String keywordUpper = (keyword != null) ? keyword.toUpperCase() : null;
        Page<Rol> paginaRoles = rolService.listarTodosLosRoles(keywordUpper, pageable);

        model.addAttribute("paginaRoles", paginaRoles);
        model.addAttribute("keyword", keyword); // Mantener keyword original para mostrar en input de búsqueda
        model.addAttribute("mostrarEliminados", false);
        return "modulos/roles/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).CREAR_ROLES)")
    public String mostrarFormularioNuevoRol(Model model) {
        model.addAttribute("rol", new RolDTO());
        cargarPermisos(model);
        return "modulos/roles/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyAuthority(T(com.odontoapp.util.Permisos).CREAR_ROLES, T(com.odontoapp.util.Permisos).EDITAR_ROLES)")
    public String guardarRol(@Valid @ModelAttribute("rol") RolDTO rolDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            cargarPermisos(model);
            model.addAttribute("rol", rolDTO);
            return "modulos/roles/formulario";
        }

        try {
            rolService.guardarRol(rolDTO);
            redirectAttributes.addFlashAttribute("success", "Rol guardado con éxito.");
            return "redirect:/roles";

        } catch (DataIntegrityViolationException | UnsupportedOperationException | IllegalStateException e) { // Capturar
                                                                                                              // más
                                                                                                              // excepciones
            cargarPermisos(model);
            model.addAttribute("rol", rolDTO);
            model.addAttribute("errorValidacion", e.getMessage()); // Usar mensaje de la excepción
            return "modulos/roles/formulario";

        } catch (Exception e) {
            cargarPermisos(model);
            model.addAttribute("rol", rolDTO);
            model.addAttribute("errorValidacion", "Ocurrió un error inesperado al guardar el rol.");
            System.err.println("Error inesperado al guardar rol: " + e.getMessage());
            e.printStackTrace();
            return "modulos/roles/formulario";
        }
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_ROLES)")
    public String mostrarFormularioEditarRol(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) { // Añadir RedirectAttributes
        Rol rol = rolService.buscarRolPorId(id).orElse(null);
        if (rol != null) {
            RolDTO rolDTO = new RolDTO();
            rolDTO.setId(rol.getId());
            rolDTO.setNombre(rol.getNombre());
            if (rol.getPermisos() != null) {
                rolDTO.setPermisos(rol.getPermisos().stream().map(Permiso::getId).collect(Collectors.toList()));
            }

            model.addAttribute("rol", rolDTO);
            model.addAttribute("esRolSistema", rol.isEsRolSistema()); // Pasar info al formulario
            cargarPermisos(model);
            return "modulos/roles/formulario";
        }
        redirectAttributes.addFlashAttribute("error", "Rol no encontrado."); // Mensaje si no existe
        return "redirect:/roles";
    }

    @GetMapping("/eliminar/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).ELIMINAR_ROLES)")
    public String eliminarRol(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rolService.eliminarRol(id);
            redirectAttributes.addFlashAttribute("success", "Rol eliminado lógicamente con éxito.");
        } catch (UnsupportedOperationException | DataIntegrityViolationException | IllegalStateException e) { // Capturar
                                                                                                              // más
                                                                                                              // excepciones
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al intentar eliminar lógicamente el rol.");
            System.err.println("Error al eliminar (soft delete) rol: " + e.getMessage());
        }
        return "redirect:/roles";
    }

    @GetMapping("/cambiar-estado/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).EDITAR_ROLES)")
    public String cambiarEstadoRol(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rolService.cambiarEstadoRol(id);
            // Mensaje más específico
            Rol rolActualizado = rolService.buscarRolPorId(id).orElse(null);
            String estado = (rolActualizado != null && rolActualizado.isEstaActivo()) ? "activado" : "desactivado";
            redirectAttributes.addFlashAttribute("success", "Rol " + estado + " con éxito.");
        } catch (UnsupportedOperationException | DataIntegrityViolationException | IllegalStateException e) { // Capturar
                                                                                                              // más
                                                                                                              // excepciones
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error inesperado al cambiar estado del rol.");
            System.err.println("Error al cambiar estado rol: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/roles";
    }

    @GetMapping("/restablecer/{id}")
    @PreAuthorize("hasAuthority(T(com.odontoapp.util.Permisos).RESTAURAR_ROLES)")
    public String restablecerRol(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            rolService.restablecerRol(id);
            redirectAttributes.addFlashAttribute("success", "Rol restablecido con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al restablecer el rol: " + e.getMessage());
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

    /**
     * Obtiene roles de personal (todos excepto PACIENTE)
     * Para usar en el modal de promoción de pacientes
     */
    @GetMapping("/api/roles-personal")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<List<Map<String, Object>>> obtenerRolesPersonal() {
        try {
            List<Rol> rolesPersonal = rolService.listarTodosLosRoles(null,
                    PageRequest.of(0, 100)).getContent()
                    .stream()
                    .filter(Rol::isEstaActivo)
                    .filter(rol -> !"PACIENTE".equals(rol.getNombre()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> resultado = rolesPersonal.stream()
                    .map(rol -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", rol.getId());
                        map.put("nombre", rol.getNombre());
                        return map;
                    })
                    .collect(Collectors.toList());

            return org.springframework.http.ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }
}
