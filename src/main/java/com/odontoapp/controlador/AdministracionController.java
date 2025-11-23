package com.odontoapp.controlador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.util.Permisos;

@Controller
@RequestMapping("/administracion")
public class AdministracionController {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final RolRepository rolRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final InsumoRepository insumoRepository;

    public AdministracionController(UsuarioRepository usuarioRepository,
                                    PacienteRepository pacienteRepository,
                                    RolRepository rolRepository,
                                    ProcedimientoRepository procedimientoRepository,
                                    InsumoRepository insumoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.rolRepository = rolRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.insumoRepository = insumoRepository;
    }

    /**
     * Vista centralizada de todos los registros eliminados del sistema.
     * Solo accesible para usuarios con permisos de RESTAURAR en al menos un módulo.
     */
    @GetMapping("/eliminados")
    @PreAuthorize("hasAnyAuthority(" +
            "T(com.odontoapp.util.Permisos).RESTAURAR_USUARIOS, " +
            "T(com.odontoapp.util.Permisos).RESTAURAR_PACIENTES, " +
            "T(com.odontoapp.util.Permisos).RESTAURAR_ROLES, " +
            "T(com.odontoapp.util.Permisos).RESTAURAR_SERVICIOS, " +
            "T(com.odontoapp.util.Permisos).RESTAURAR_INVENTARIO)")
    public String mostrarRegistrosEliminados(
            @RequestParam(defaultValue = "usuarios") String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size);

        // Cargar los registros eliminados según el tipo seleccionado
        switch (tipo) {
            case "usuarios":
                Page<Usuario> usuarios = usuarioRepository.findEliminados(pageable);
                model.addAttribute("usuarios", usuarios);
                model.addAttribute("totalUsuarios", usuarios.getTotalElements());
                break;

            case "pacientes":
                Page<Paciente> pacientes = pacienteRepository.findEliminados(pageable);
                model.addAttribute("pacientes", pacientes);
                model.addAttribute("totalPacientes", pacientes.getTotalElements());
                break;

            case "roles":
                Page<Rol> roles = rolRepository.findEliminados(pageable);
                model.addAttribute("roles", roles);
                model.addAttribute("totalRoles", roles.getTotalElements());
                break;

            case "servicios":
                Page<Procedimiento> servicios = procedimientoRepository.findEliminados(pageable);
                model.addAttribute("servicios", servicios);
                model.addAttribute("totalServicios", servicios.getTotalElements());
                break;

            case "insumos":
                Page<Insumo> insumos = insumoRepository.findEliminados(pageable);
                model.addAttribute("insumos", insumos);
                model.addAttribute("totalInsumos", insumos.getTotalElements());
                break;

            default:
                // Por defecto mostrar usuarios
                Page<Usuario> usuariosDefault = usuarioRepository.findEliminados(pageable);
                model.addAttribute("usuarios", usuariosDefault);
                model.addAttribute("totalUsuarios", usuariosDefault.getTotalElements());
                tipo = "usuarios";
        }

        // Cargar contadores de todos los tipos (para mostrar en los tabs)
        model.addAttribute("totalUsuariosGlobal", usuarioRepository.findEliminados(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("totalPacientesGlobal", pacienteRepository.findEliminados(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("totalRolesGlobal", rolRepository.findEliminados(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("totalServiciosGlobal", procedimientoRepository.findEliminados(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("totalInsumosGlobal", insumoRepository.findEliminados(PageRequest.of(0, 1)).getTotalElements());

        model.addAttribute("tipoActivo", tipo);

        return "modulos/administracion/eliminados";
    }
}
