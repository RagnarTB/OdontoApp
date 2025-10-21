package com.odontoapp.controlador;

import com.odontoapp.dto.ReniecResponseDTO;
import com.odontoapp.entidad.Paciente;
import com.odontoapp.servicio.PacienteService;
import com.odontoapp.servicio.ReniecService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@Controller
public class PacienteController {

    private final PacienteService pacienteService;
    private final ReniecService reniecService;

    public PacienteController(PacienteService pacienteService, ReniecService reniecService) {
        this.pacienteService = pacienteService;
        this.reniecService = reniecService;
    }

    @GetMapping("/pacientes")
    public String listarPacientes(Model model) {
        model.addAttribute("pacientes", pacienteService.listarTodosLosPacientes());
        return "modulos/pacientes/lista";
    }

    @GetMapping("/pacientes/nuevo")
    public String mostrarFormularioNuevo(Model model) {
        model.addAttribute("paciente", new Paciente());
        return "modulos/pacientes/formulario";
    }

    @PostMapping("/pacientes/guardar")
    public String guardarPaciente(@Valid @ModelAttribute("paciente") Paciente paciente, BindingResult result) {
        if (result.hasErrors()) {
            return "modulos/pacientes/formulario";
        }
        pacienteService.guardarPaciente(paciente);
        return "redirect:/pacientes";
    }

    @GetMapping("/pacientes/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model) {
        Optional<Paciente> paciente = pacienteService.buscarPorId(id);
        if (paciente.isPresent()) {
            model.addAttribute("paciente", paciente.get());
            return "modulos/pacientes/formulario";
        }
        return "redirect:/pacientes";
    }

    @GetMapping("/pacientes/eliminar/{id}")
    public String eliminarPaciente(@PathVariable Long id) {
        pacienteService.eliminarPaciente(id);
        return "redirect:/pacientes";
    }

    // --- ENDPOINT PARA LA API DE RENIEC ---
    @GetMapping("/api/reniec/{dni}")
    @ResponseBody // <-- Importante: Devuelve datos (JSON), no una vista.
    public ResponseEntity<?> consultarReniec(@PathVariable String dni) {
        // Primero, buscamos en nuestra BD (nuestro caché)
        Optional<Paciente> pacienteExistente = pacienteService.buscarPorDni(dni);
        if (pacienteExistente.isPresent()) {
            // Si ya existe, devolvemos un error para evitar duplicados
            return ResponseEntity.badRequest().body("El DNI ya se encuentra registrado.");
        }

        // Si no existe, consultamos la API externa
        ReniecResponseDTO response = reniecService.consultarDni(dni);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}