package com.odontoapp.controlador;

import com.odontoapp.servicio.OdontogramaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para la gestión del odontograma (carta dental).
 * Expone endpoints para obtener, actualizar e inicializar el estado dental de pacientes.
 */
@RestController
@RequestMapping("/api/odontograma")
public class OdontogramaController {

    private final OdontogramaService odontogramaService;

    public OdontogramaController(OdontogramaService odontogramaService) {
        this.odontogramaService = odontogramaService;
    }

    /**
     * Obtiene el estado actual del odontograma de un paciente.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @return ResponseEntity con el JSON del odontograma o 404 si no existe
     */
    @GetMapping("/{pacienteUsuarioId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<?> obtenerOdontograma(@PathVariable Long pacienteUsuarioId) {
        try {
            String estadoJson = odontogramaService.obtenerEstadoOdontograma(pacienteUsuarioId);

            if (estadoJson == null || estadoJson.trim().isEmpty()) {
                // Si no tiene odontograma, retornar 404 con mensaje
                Map<String, Object> response = new HashMap<>();
                response.put("mensaje", "El paciente no tiene un odontograma registrado");
                response.put("inicializado", false);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Retornar el JSON directamente
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(estadoJson);

        } catch (EntityNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Paciente no encontrado");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error de estado");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Actualiza el estado del odontograma de un paciente.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @param estadoJson JSON con el estado del odontograma
     * @return ResponseEntity con mensaje de éxito o error
     */
    @PostMapping("/{pacienteUsuarioId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> actualizarOdontograma(
            @PathVariable Long pacienteUsuarioId,
            @RequestBody String estadoJson) {

        try {
            odontogramaService.actualizarEstadoOdontograma(pacienteUsuarioId, estadoJson);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Odontograma actualizado exitosamente");
            response.put("pacienteUsuarioId", pacienteUsuarioId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Error de validación del JSON
            Map<String, String> error = new HashMap<>();
            error.put("error", "Datos inválidos");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (EntityNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Paciente no encontrado");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error de estado");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Inicializa un odontograma vacío para un paciente.
     * Crea la estructura JSON base con los 32 dientes permanentes.
     *
     * @param pacienteUsuarioId ID del usuario paciente
     * @return ResponseEntity con el JSON del odontograma inicializado
     */
    @PostMapping("/{pacienteUsuarioId}/inicializar")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> inicializarOdontograma(@PathVariable Long pacienteUsuarioId) {
        try {
            String odontogramaBase = odontogramaService.inicializarOdontograma(pacienteUsuarioId);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Odontograma inicializado exitosamente");
            response.put("pacienteUsuarioId", pacienteUsuarioId);
            response.put("odontograma", odontogramaBase);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (EntityNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Paciente no encontrado");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error de estado");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Valida si un JSON de odontograma tiene el formato correcto.
     *
     * @param estadoJson JSON a validar
     * @return ResponseEntity con el resultado de la validación
     */
    @PostMapping("/validar")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> validarOdontograma(@RequestBody String estadoJson) {
        try {
            boolean esValido = odontogramaService.validarFormatoOdontograma(estadoJson);

            Map<String, Object> response = new HashMap<>();
            response.put("valido", esValido);

            if (esValido) {
                response.put("mensaje", "El formato del odontograma es válido");
                return ResponseEntity.ok(response);
            } else {
                response.put("mensaje", "El formato del odontograma no es válido");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al validar");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
