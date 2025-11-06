package com.odontoapp.controlador;

import com.odontoapp.dto.ProcedimientoInsumoDTO;
import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.servicio.ProcedimientoInsumoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para gestionar la relación entre procedimientos e insumos.
 * Define qué insumos se utilizan en cada procedimiento dental.
 */
@RestController
@RequestMapping("/api/procedimiento-insumos")
public class ProcedimientoInsumoController {

    private final ProcedimientoInsumoService procedimientoInsumoService;

    public ProcedimientoInsumoController(ProcedimientoInsumoService procedimientoInsumoService) {
        this.procedimientoInsumoService = procedimientoInsumoService;
    }

    /**
     * Obtiene todos los insumos asociados a un procedimiento específico.
     *
     * @param procedimientoId ID del procedimiento
     * @return Lista de DTOs con información de los insumos asociados
     */
    @GetMapping("/procedimiento/{procedimientoId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<?> obtenerInsumosPorProcedimiento(@PathVariable Long procedimientoId) {
        try {
            List<ProcedimientoInsumo> relaciones =
                procedimientoInsumoService.buscarInsumosPorProcedimiento(procedimientoId);

            // Convertir entidades a DTOs para evitar problemas de serialización
            List<ProcedimientoInsumoDTO> dtos = relaciones.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Parámetros inválidos");
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
     * Obtiene todos los procedimientos que utilizan un insumo específico.
     *
     * @param insumoId ID del insumo
     * @return Lista de DTOs con información de los procedimientos asociados
     */
    @GetMapping("/insumo/{insumoId}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<?> obtenerProcedimientosPorInsumo(@PathVariable Long insumoId) {
        try {
            List<ProcedimientoInsumo> relaciones =
                procedimientoInsumoService.buscarProcedimientosPorInsumo(insumoId);

            // Convertir entidades a DTOs
            List<ProcedimientoInsumoDTO> dtos = relaciones.stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Parámetros inválidos");
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
     * Busca una relación específica entre procedimiento e insumo.
     *
     * @param procedimientoId ID del procedimiento
     * @param insumoId ID del insumo
     * @return DTO con la información de la relación
     */
    @GetMapping("/relacion")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> buscarRelacion(
            @RequestParam Long procedimientoId,
            @RequestParam Long insumoId) {
        try {
            ProcedimientoInsumo relacion =
                procedimientoInsumoService.buscarRelacion(procedimientoId, insumoId);

            ProcedimientoInsumoDTO dto = convertirADto(relacion);

            return ResponseEntity.ok(dto);

        } catch (EntityNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Relación no encontrada");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Parámetros inválidos");
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
     * Asigna un insumo a un procedimiento con una cantidad por defecto.
     *
     * @param dto Datos de la asignación (validados con Bean Validation)
     * @return ResponseEntity con el DTO de la relación creada
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> asignarInsumo(@RequestBody @Valid ProcedimientoInsumoDTO dto) {
        try {
            ProcedimientoInsumo nuevaRelacion =
                procedimientoInsumoService.asignarInsumoAProcedimiento(dto);

            ProcedimientoInsumoDTO responseDto = convertirADto(nuevaRelacion);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Insumo asignado al procedimiento exitosamente");
            response.put("relacion", responseDto);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Datos inválidos");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Relación duplicada");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);

        } catch (EntityNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Entidad no encontrada");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Actualiza la cantidad por defecto de un insumo en un procedimiento.
     *
     * @param id ID de la relación procedimiento-insumo
     * @param requestBody Objeto con la nueva cantidad
     * @return ResponseEntity con el DTO actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> actualizarCantidad(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> requestBody) {
        try {
            BigDecimal nuevaCantidad = requestBody.get("cantidadDefault");

            if (nuevaCantidad == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Parámetro faltante");
                error.put("mensaje", "Debe proporcionar el campo 'cantidadDefault'");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            ProcedimientoInsumo relacionActualizada =
                procedimientoInsumoService.actualizarCantidadInsumo(id, nuevaCantidad);

            ProcedimientoInsumoDTO dto = convertirADto(relacionActualizada);

            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Cantidad actualizada exitosamente");
            response.put("relacion", dto);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Datos inválidos");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (EntityNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Relación no encontrada");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Elimina la relación entre un procedimiento y un insumo.
     *
     * @param id ID de la relación a eliminar
     * @return ResponseEntity con mensaje de éxito
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<?> eliminarAsignacion(@PathVariable Long id) {
        try {
            procedimientoInsumoService.quitarInsumoDeProcedimiento(id);

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Asignación eliminada exitosamente");

            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Relación no encontrada");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error interno del servidor");
            error.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Convierte una entidad ProcedimientoInsumo a DTO.
     * Evita problemas de serialización JSON con relaciones LAZY de JPA.
     *
     * @param entidad Entidad a convertir
     * @return DTO con toda la información
     */
    private ProcedimientoInsumoDTO convertirADto(ProcedimientoInsumo entidad) {
        ProcedimientoInsumoDTO dto = new ProcedimientoInsumoDTO();

        dto.setId(entidad.getId());
        dto.setProcedimientoId(entidad.getProcedimiento().getId());
        dto.setInsumoId(entidad.getInsumo().getId());
        dto.setCantidadDefault(entidad.getCantidadDefecto());

        // Campos de solo lectura (información adicional)
        dto.setProcedimientoNombre(entidad.getProcedimiento().getNombre());
        dto.setProcedimientoCodigo(entidad.getProcedimiento().getCodigo());
        dto.setInsumoNombre(entidad.getInsumo().getNombre());
        dto.setInsumoCodigo(entidad.getInsumo().getCodigo());
        dto.setInsumoUnidadMedida(entidad.getInsumo().getUnidadMedida().getNombre());
        dto.setInsumoStockActual(entidad.getInsumo().getStockActual());

        return dto;
    }
}
