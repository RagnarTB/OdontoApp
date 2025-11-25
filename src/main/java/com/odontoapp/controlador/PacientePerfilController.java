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
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            // Obtener usuario autenticado
            Usuario usuario = obtenerUsuarioAutenticado();

            System.out.println("🔍 DEBUG - Buscando paciente para usuario:");
            System.out.println("   - ID: " + usuario.getId());
            System.out.println("   - Email: " + usuario.getEmail());
            System.out.println("   - Nombre: " + usuario.getNombreCompleto());
            // Buscar el paciente asociado
            Paciente paciente = pacienteRepository.findByUsuario(usuario)
                    .orElseThrow(() -> {
                        System.err.println("❌ ERROR - No se encontró paciente para usuario ID: " + usuario.getId());
                        System.err.println("   Ejecute el script SQL de verificación en la guía (CAMBIO #3)");
                        return new RuntimeException(
                                "No se encontró un registro de paciente asociado a su usuario. " +
                                        "Por favor, contacte al administrador del sistema.");
                    });
            System.out.println("✅ Paciente encontrado:");
            System.out.println("   - ID: " + paciente.getId());
            System.out.println("   - Nombre: " + paciente.getNombreCompleto());
            System.out.println(
                    "   - Documento: " + paciente.getTipoDocumento().getCodigo() + " " + paciente.getNumeroDocumento());
            model.addAttribute("paciente", paciente);
            model.addAttribute("usuario", usuario);
            // Obtener citas del paciente con paginación
            Page<Cita> citasPage = citaRepository.findByPacienteId(
                    usuario.getId(),
                    PageRequest.of(citasPageNum, citasSize, Sort.by("fechaHoraInicio").descending()));
            model.addAttribute("citasPage", citasPage);
            System.out.println("   - Citas encontradas: " + citasPage.getTotalElements());
            // Obtener tratamientos realizados del paciente con paginación
            int tratamientosSize = 10;
            Page<TratamientoRealizado> tratamientosPageData = tratamientoRealizadoRepository.findByPacienteId(
                    usuario.getId(),
                    PageRequest.of(tratamientosPage, tratamientosSize, Sort.by("fechaRealizacion").descending()));
            model.addAttribute("tratamientosPage", tratamientosPageData);
            System.out.println("   - Tratamientos realizados: " + tratamientosPageData.getTotalElements());
            // Obtener tratamientos planificados del paciente (solo PLANIFICADO y EN_CURSO)
            List<TratamientoPlanificado> tratamientosPlanificados = tratamientoPlanificadoRepository
                    .findTratamientosPendientes(usuario);
            model.addAttribute("tratamientosPlanificados", tratamientosPlanificados);
            System.out.println("   - Tratamientos planificados: " + tratamientosPlanificados.size());
            // Obtener comprobantes del paciente con paginación
            Page<Comprobante> comprobantesPage = comprobanteRepository.findByPacienteIdOrderByFechaEmisionDesc(
                    usuario.getId(),
                    PageRequest.of(comprobantesPageNum, comprobantesSize));
            model.addAttribute("comprobantesPage", comprobantesPage);
            System.out.println("   - Comprobantes: " + comprobantesPage.getTotalElements());
            // Obtener odontograma
            List<OdontogramaDiente> dientes = odontogramaDienteRepository.findByPaciente(usuario);
            model.addAttribute("odontograma", dientes);
            System.out.println("   - Dientes en odontograma: " + dientes.size());
            return "paciente/perfil/historial";

        } catch (RuntimeException e) {
            System.err.println("❌ ERROR en verPerfil: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/paciente/dashboard";
        }
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
     * Solo permite editar: teléfono, dirección, alergias y antecedentes médicos.
     * NOTA: El email NO es editable por seguridad.
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
            // SOLO actualizar campos permitidos: teléfono, dirección, alergias,
            // antecedentes
            // NOTA: El email NO es editable por seguridad
            paciente.setTelefono(pacienteDTO.getTelefono());
            paciente.setDireccion(pacienteDTO.getDireccion());
            paciente.setAlergias(pacienteDTO.getAlergias());
            paciente.setAntecedentesMedicos(pacienteDTO.getAntecedentesMedicos());

            // Guardar cambios en paciente
            pacienteRepository.save(paciente);

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
