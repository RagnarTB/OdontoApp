package com.odontoapp.controlador;

import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.odontoapp.dto.PacienteDTO;
import com.odontoapp.dto.ReniecResponseDTO;
import com.odontoapp.entidad.Paciente;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.servicio.PacienteService;
import com.odontoapp.servicio.ReniecService;

import jakarta.validation.Valid;

@Controller
public class PacienteController {

    private final PacienteService pacienteService;
    private final ReniecService reniecService;
    private final PacienteRepository pacienteRepository;

    public PacienteController(PacienteService pacienteService, ReniecService reniecService,
            PacienteRepository pacienteRepository) {
        this.pacienteService = pacienteService;
        this.reniecService = reniecService;
        this.pacienteRepository = pacienteRepository;
    }

    @GetMapping("/pacientes")
    public String listarPacientes(Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Paciente> paginaPacientes = pacienteService.listarTodosLosPacientes(keyword, pageable);
        model.addAttribute("paginaPacientes", paginaPacientes);
        model.addAttribute("keyword", keyword);
        return "modulos/pacientes/lista";
    }

    @GetMapping("/pacientes/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("pacienteDTO", new PacienteDTO());
        return "modulos/pacientes/formulario";
    }

    @PostMapping("/pacientes/guardar")
    public String guardarPaciente(@Valid @ModelAttribute("pacienteDTO") PacienteDTO pacienteDTO,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "modulos/pacientes/formulario";
        }
        try {
            pacienteService.guardarPaciente(pacienteDTO);
            redirectAttributes.addFlashAttribute("success", "Paciente guardado con éxito.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (pacienteDTO.getId() != null) {
                return "redirect:/pacientes/editar/" + pacienteDTO.getId();
            } else {
                return "redirect:/pacientes/nuevo";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error inesperado.");
            return "redirect:/pacientes/nuevo";
        }
        return "redirect:/pacientes";
    }

    @GetMapping("/pacientes/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        return pacienteService.buscarPorId(id).map(paciente -> {
            PacienteDTO dto = new PacienteDTO();
            dto.setId(paciente.getId());
            dto.setDni(paciente.getDni());
            dto.setNombreCompleto(paciente.getNombreCompleto());
            dto.setEmail(paciente.getEmail());
            dto.setTelefono(paciente.getTelefono());
            dto.setFechaNacimiento(paciente.getFechaNacimiento());
            dto.setDireccion(paciente.getDireccion());
            dto.setAlergias(paciente.getAlergias());
            dto.setAntecedentesMedicos(paciente.getAntecedentesMedicos());

            model.addAttribute("pacienteDTO", dto);
            return "modulos/pacientes/formulario";
        }).orElse("redirect:/pacientes");
    }

    @GetMapping("/pacientes/eliminar/{id}")
    public String eliminarPaciente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pacienteService.eliminarPaciente(id);
            redirectAttributes.addFlashAttribute("success", "Paciente eliminado con éxito (desactivado).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el paciente.");
        }
        return "redirect:/pacientes";
    }

    @GetMapping("/api/reniec/{dni}")
    @ResponseBody
    public ResponseEntity<?> consultarReniec(@PathVariable String dni) {
        // --- VALIDACIÓN MEJORADA (IGNORA SOFT DELETE) ---
        Optional<Paciente> pacienteExistente = pacienteRepository.findByDniIgnorandoSoftDelete(dni);
        if (pacienteExistente.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El DNI ya se encuentra registrado."));
        }

        // En PacienteController, dentro de consultarReniec
        ReniecResponseDTO response = reniecService.consultarDni(dni);
        System.out.println(">>> ReniecService devolvió: " + response);

        if (response != null) {
            String nombreCalculado = response.getNombreCompleto(); // Llama al getter modificado
            System.out.println(">>> DTO.getNombreCompleto() devuelve: " + nombreCalculado);

            if (nombreCalculado != null) {
                // Crear un objeto simple para enviar solo el nombre
                Map<String, String> resultadoJson = Map.of("nombreCompleto", nombreCalculado);
                return ResponseEntity.ok(resultadoJson); // Devolver el Map
            }
        }

        // Si response es null o nombreCalculado es null
        return ResponseEntity.status(404).body(
                Map.of("error", "DNI no encontrado o datos incompletos. Verifique el número."));
    }

}