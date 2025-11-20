package com.odontoapp.controlador;

import com.odontoapp.dto.PacienteDTO;
import com.odontoapp.entidad.*;
import com.odontoapp.repositorio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

/**
 * Controlador para el perfil e historial clínico del paciente.
 * Permite ver el historial completo y editar datos personales permitidos.
 */
@Controller
@RequestMapping("/paciente/perfil")
@RequiredArgsConstructor
public class PacientePerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final TratamientoRealizadoRepository tratamientoRealizadoRepository;
    private final TratamientoPlanificadoRepository tratamientoPlanificadoRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final OdontogramaDienteRepository odontogramaDienteRepository;

    /**
     * Helper: Obtiene el usuario paciente autenticado
     */
    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    /**
     * Muestra el perfil completo del paciente con historial clínico
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String verPerfil(
            @RequestParam(defaultValue = "0") int citasPageNum,
            @RequestParam(defaultValue = "10") int citasSize,
            @RequestParam(defaultValue = "0") int comprobantesPageNum,
            @RequestParam(defaultValue = "10") int comprobantesSize,
            @RequestParam(defaultValue = "0") int tratamientosPage,
            Model model) {

        // Obtener usuario autenticado
        Usuario usuario = obtenerUsuarioAutenticado();

        // Buscar el paciente asociado
        Paciente paciente = pacienteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        model.addAttribute("paciente", paciente);
        model.addAttribute("usuario", usuario);

        // Obtener citas del paciente con paginación
        Page<Cita> citasPage = citaRepository.findByPacienteId(
            usuario.getId(),
            PageRequest.of(citasPageNum, citasSize, Sort.by("fechaHoraInicio").descending())
        );
        model.addAttribute("citasPage", citasPage);

        // Obtener tratamientos realizados del paciente con paginación
        int tratamientosSize = 10;
        Page<TratamientoRealizado> tratamientosPageData =
                tratamientoRealizadoRepository.findByPacienteId(
                    usuario.getId(),
                    PageRequest.of(tratamientosPage, tratamientosSize, Sort.by("fechaRealizacion").descending())
                );
        model.addAttribute("tratamientosPage", tratamientosPageData);

        // Obtener tratamientos planificados del paciente (solo PLANIFICADO y EN_CURSO)
        List<TratamientoPlanificado> tratamientosPlanificados =
                tratamientoPlanificadoRepository.findTratamientosPendientes(usuario);
        model.addAttribute("tratamientosPlanificados", tratamientosPlanificados);

        // Obtener comprobantes del paciente con paginación
        Page<Comprobante> comprobantesPage =
                comprobanteRepository.findByPacienteIdOrderByFechaEmisionDesc(
                    usuario.getId(),
                    PageRequest.of(comprobantesPageNum, comprobantesSize)
                );
        model.addAttribute("comprobantesPage", comprobantesPage);

        // Obtener odontograma
        List<OdontogramaDiente> dientes = odontogramaDienteRepository.findByPaciente(usuario);
        model.addAttribute("odontograma", dientes);

        return "paciente/perfil/historial";
    }

    /**
     * Muestra el formulario de edición de datos personales
     */
    @GetMapping("/editar")
    public String mostrarFormularioEditar(Model model) {
        // Obtener usuario autenticado
        Usuario usuario = obtenerUsuarioAutenticado();

        // Buscar el paciente asociado
        Paciente paciente = pacienteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        // Crear DTO con datos editables
        PacienteDTO dto = new PacienteDTO();
        dto.setId(paciente.getId());
        dto.setEmail(paciente.getEmail());
        dto.setTelefono(paciente.getTelefono());
        dto.setDireccion(paciente.getDireccion());
        dto.setAlergias(paciente.getAlergias());
        dto.setAntecedentesMedicos(paciente.getAntecedentesMedicos());

        // Datos de solo lectura (para mostrar pero no editar)
        model.addAttribute("paciente", paciente);
        model.addAttribute("pacienteDTO", dto);

        return "paciente/perfil/editar";
    }

    /**
     * Guarda los cambios en los datos personales del paciente.
     * Solo permite editar: email, teléfono, dirección.
     */
    @PostMapping("/guardar")
    public String guardarDatosPersonales(
            @Valid @ModelAttribute("pacienteDTO") PacienteDTO pacienteDTO,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Obtener usuario autenticado
        Usuario usuario = obtenerUsuarioAutenticado();

        // Buscar el paciente asociado
        Paciente paciente = pacienteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        // Validación del DTO (solo campos permitidos)
        if (result.hasErrors()) {
            model.addAttribute("paciente", paciente);
            model.addAttribute("pacienteDTO", pacienteDTO);
            return "paciente/perfil/editar";
        }

        try {
            // Validar si el email cambió
            boolean emailCambio = !usuario.getEmail().equals(pacienteDTO.getEmail());

            if (emailCambio) {
                // Verificar que el nuevo email no esté en uso por otro usuario
                Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(pacienteDTO.getEmail());
                if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
                    model.addAttribute("paciente", paciente);
                    model.addAttribute("pacienteDTO", pacienteDTO);
                    model.addAttribute("error", "El email '" + pacienteDTO.getEmail() + "' ya está en uso por otro usuario.");
                    return "paciente/perfil/editar";
                }
            }

            // SOLO actualizar campos permitidos: email, teléfono, dirección, alergias, antecedentes
            paciente.setEmail(pacienteDTO.getEmail());
            paciente.setTelefono(pacienteDTO.getTelefono());
            paciente.setDireccion(pacienteDTO.getDireccion());
            paciente.setAlergias(pacienteDTO.getAlergias());
            paciente.setAntecedentesMedicos(pacienteDTO.getAntecedentesMedicos());

            // Guardar cambios en paciente
            pacienteRepository.save(paciente);

            // Si el email cambió, también actualizar el usuario
            if (emailCambio) {
                usuario.setEmail(pacienteDTO.getEmail());
                usuarioRepository.save(usuario);
                redirectAttributes.addFlashAttribute("info",
                    "Tu email ha sido actualizado. Usa el nuevo email para iniciar sesión la próxima vez.");
            }

            redirectAttributes.addFlashAttribute("success", "Datos personales actualizados con éxito.");
            return "redirect:/paciente/perfil";

        } catch (Exception e) {
            model.addAttribute("paciente", paciente);
            model.addAttribute("pacienteDTO", pacienteDTO);
            model.addAttribute("error", "Error al actualizar datos personales: " + e.getMessage());
            System.err.println("Error al actualizar perfil del paciente: " + e.getMessage());
            e.printStackTrace();
            return "paciente/perfil/editar";
        }
    }

    /**
     * Muestra el detalle de un comprobante del paciente
     */
    @GetMapping("/comprobantes/{id}")
    public String verDetalleComprobante(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        // Obtener usuario autenticado
        Usuario usuario = obtenerUsuarioAutenticado();

        // Buscar el comprobante
        Comprobante comprobante = comprobanteRepository.findById(id)
                .orElse(null);

        if (comprobante == null) {
            redirectAttributes.addFlashAttribute("error", "Comprobante no encontrado.");
            return "redirect:/paciente/perfil";
        }

        // Verificar que el comprobante pertenezca al paciente
        if (!comprobante.getPaciente().getId().equals(usuario.getId())) {
            redirectAttributes.addFlashAttribute("error", "No tiene permisos para ver este comprobante.");
            return "redirect:/paciente/perfil";
        }

        model.addAttribute("comprobante", comprobante);
        return "paciente/perfil/comprobante-detalle";
    }
}
