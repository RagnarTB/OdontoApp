package com.odontoapp.controlador;

import com.odontoapp.dto.OdontogramaDienteDTO;
import com.odontoapp.servicio.OdontogramaDienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión del odontograma.
 * Proporciona endpoints para obtener, actualizar y consultar el estado dental de pacientes.
 */
@RestController
@RequestMapping("/api/odontograma")
public class OdontogramaRestController {

    private final OdontogramaDienteService odontogramaService;

    public OdontogramaRestController(OdontogramaDienteService odontogramaService) {
        this.odontogramaService = odontogramaService;
    }

    /**
     * Obtiene el odontograma completo de un paciente
     */
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<OdontogramaDienteDTO>> obtenerOdontograma(
            @PathVariable Long pacienteId) {
        try {
            List<OdontogramaDienteDTO> odontograma = odontogramaService.obtenerOdontogramaCompleto(pacienteId);
            return ResponseEntity.ok(odontograma);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Actualiza el estado de un diente específico
     */
    @PostMapping("/paciente/{pacienteId}/diente")
    @PreAuthorize("hasRole('ODONTOLOGO') or hasRole('ADMIN')")
    public ResponseEntity<?> actualizarDiente(
            @PathVariable Long pacienteId,
            @RequestBody OdontogramaDienteDTO dienteDTO) {
        try {
            OdontogramaDienteDTO resultado = odontogramaService.actualizarEstadoDiente(
                pacienteId,
                dienteDTO.getNumeroDiente(),
                dienteDTO.getEstado(),
                dienteDTO.getSuperficiesAfectadas(),
                dienteDTO.getNotas(),
                null
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Diente actualizado correctamente");
            response.put("diente", resultado);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("mensaje", "Error al actualizar diente: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Actualiza múltiples dientes a la vez
     */
    @PostMapping("/paciente/{pacienteId}/actualizar-multiple")
    @PreAuthorize("hasRole('ODONTOLOGO') or hasRole('ADMIN')")
    public ResponseEntity<?> actualizarMultiple(
            @PathVariable Long pacienteId,
            @RequestBody List<OdontogramaDienteDTO> cambios) {
        try {
            List<OdontogramaDienteDTO> resultados =
                odontogramaService.actualizarMultiplesDientes(pacienteId, cambios);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Dientes actualizados correctamente");
            response.put("dientes", resultados);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("mensaje", "Error al actualizar dientes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Obtiene estadísticas del odontograma
     */
    @GetMapping("/paciente/{pacienteId}/estadisticas")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<Map<String, Integer>> obtenerEstadisticas(
            @PathVariable Long pacienteId) {
        try {
            Map<String, Integer> estadisticas = odontogramaService.obtenerEstadisticas(pacienteId);
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene el historial de cambios de un diente específico
     */
    @GetMapping("/paciente/{pacienteId}/diente/{numeroDiente}/historial")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> obtenerHistorialDiente(
            @PathVariable Long pacienteId,
            @PathVariable String numeroDiente) {
        try {
            var historial = odontogramaService.obtenerHistorialDiente(pacienteId, numeroDiente);

            Map<String, Object> response = new HashMap<>();
            response.put("numeroDiente", numeroDiente);
            response.put("historial", historial);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al obtener historial: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Inicializa el odontograma de un paciente
     */
    @PostMapping("/paciente/{pacienteId}/inicializar")
    @PreAuthorize("hasRole('ODONTOLOGO') or hasRole('ADMIN')")
    public ResponseEntity<?> inicializarOdontograma(@PathVariable Long pacienteId) {
        try {
            List<OdontogramaDienteDTO> odontograma = odontogramaService.inicializarOdontograma(pacienteId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Odontograma inicializado con 32 dientes");
            response.put("dientes", odontograma);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("mensaje", "Error al inicializar odontograma: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
