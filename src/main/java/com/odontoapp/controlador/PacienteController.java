// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\controlador\PacienteController.java
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
import com.odontoapp.entidad.TipoDocumento;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository; // NUEVO
import com.odontoapp.servicio.PacienteService;
import com.odontoapp.servicio.ReniecService;

import jakarta.validation.Valid;

@Controller
public class PacienteController {

    private final PacienteService pacienteService;
    private final ReniecService reniecService;
    private final PacienteRepository pacienteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository; // NUEVO

    public PacienteController(PacienteService pacienteService, ReniecService reniecService,
            PacienteRepository pacienteRepository, TipoDocumentoRepository tipoDocumentoRepository) {
        this.pacienteService = pacienteService;
        this.reniecService = reniecService;
        this.pacienteRepository = pacienteRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
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
        model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll()); // NUEVO
        return "modulos/pacientes/formulario";
    }

    @PostMapping("/pacientes/guardar")
    public String guardarPaciente(@Valid @ModelAttribute("pacienteDTO") PacienteDTO pacienteDTO,
            BindingResult result,
            Model model, // Necesario para recargar lista de tipos de documento
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll()); // Recargar tipos de doc
            return "modulos/pacientes/formulario";
        }
        try {
            pacienteService.guardarPaciente(pacienteDTO);
            redirectAttributes.addFlashAttribute("success", "Paciente guardado con éxito.");
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll()); // Recargar tipos de doc
            model.addAttribute("error", e.getMessage());
            return "modulos/pacientes/formulario"; // Regresar al formulario con el error
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error inesperado al guardar el paciente.");
            return "redirect:/pacientes/nuevo";
        }
        return "redirect:/pacientes";
    }

    @GetMapping("/pacientes/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        return pacienteService.buscarPorId(id).map(paciente -> {
            PacienteDTO dto = new PacienteDTO();
            dto.setId(paciente.getId());
            dto.setTipoDocumentoId(paciente.getTipoDocumento().getId()); // MODIFICADO
            dto.setNumeroDocumento(paciente.getNumeroDocumento()); // MODIFICADO
            dto.setNombreCompleto(paciente.getNombreCompleto());
            dto.setEmail(paciente.getEmail());
            dto.setTelefono(paciente.getTelefono());
            dto.setFechaNacimiento(paciente.getFechaNacimiento());
            dto.setDireccion(paciente.getDireccion());
            dto.setAlergias(paciente.getAlergias());
            dto.setAntecedentesMedicos(paciente.getAntecedentesMedicos());

            model.addAttribute("pacienteDTO", dto);
            model.addAttribute("tiposDocumento", tipoDocumentoRepository.findAll()); // NUEVO
            return "modulos/pacientes/formulario";
        }).orElse("redirect:/pacientes");
    }

    @GetMapping("/pacientes/eliminar/{id}")
    public String eliminarPaciente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pacienteService.eliminarPaciente(id);
            redirectAttributes.addFlashAttribute("success", "Paciente eliminado con éxito (desactivado).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el paciente: " + e.getMessage());
        }
        return "redirect:/pacientes";
    }

    // 🔥 MODIFICADO: Ahora recibe número y tipo de documento ID
    @GetMapping("/api/reniec")
    @ResponseBody
    public ResponseEntity<?> consultarReniec(@RequestParam("numDoc") String numDoc,
            @RequestParam("tipoDocId") Long tipoDocId) {

        // 1. Validar que el tipo de documento sea DNI (código 1 o el que definas)
        TipoDocumento tipoDocumento = tipoDocumentoRepository.findById(tipoDocId).orElse(null);
        if (tipoDocumento == null || !"DNI".equals(tipoDocumento.getCodigo())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La consulta Reniec solo está disponible para DNI."));
        }

        // 2. Buscar si ya existe (ignorando soft delete)
        Optional<Paciente> pacienteExistente = pacienteRepository.findByNumeroTipoDocumentoIgnorandoSoftDelete(numDoc,
                tipoDocId);

        if (pacienteExistente.isPresent()) {
            Paciente paciente = pacienteExistente.get();
            // 🔥 REGLA CLAVE: Si está eliminado, alertar al frontend para ofrecer
            // restauración
            if (paciente.isEliminado()) {
                return ResponseEntity.status(409).body(
                        Map.of("error", "El paciente existe, pero está eliminado lógicamente.",
                                "restaurar", true,
                                "pacienteId", paciente.getId())); // Devolver ID para restablecer
            } else {
                // Si existe y NO está eliminado, es un duplicado activo
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El documento ya se encuentra registrado y está activo."));
            }
        }

        // 3. Consultar Reniec (si no hay duplicado)
        ReniecResponseDTO response = reniecService.consultarDni(numDoc);
        if (response != null && response.getNombreCompleto() != null) {
            String nombreCalculado = response.getNombreCompleto();
            Map<String, String> resultadoJson = Map.of("nombreCompleto", nombreCalculado);
            return ResponseEntity.ok(resultadoJson);
        }

        return ResponseEntity.status(404).body(
                Map.of("error", "DNI no encontrado o datos incompletos. Verifique el número."));
    }

    @GetMapping("/pacientes/restablecer/{id}")
    public String restablecerPaciente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            pacienteService.restablecerPaciente(id);
            redirectAttributes.addFlashAttribute("success", "Paciente restablecido y activado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al restablecer el paciente: " + e.getMessage());
        }
        return "redirect:/pacientes";
    }

}