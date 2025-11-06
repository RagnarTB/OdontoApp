package com.odontoapp.controlador;

import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller para endpoints de API de Procedimientos
 * Usado por modales y componentes JavaScript
 */
@RestController
@RequestMapping("/procedimientos")
public class ProcedimientoRestController {

    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;

    public ProcedimientoRestController(ProcedimientoInsumoRepository procedimientoInsumoRepository) {
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
    }

    /**
     * Obtener los insumos asociados a un procedimiento
     * Usado por el modal de Registrar Tratamiento para mostrar insumos por defecto
     *
     * @param id ID del procedimiento
     * @return Lista de insumos con cantidad, unidad y tipo (obligatorio/opcional)
     */
    @GetMapping("/{id}/insumos")
    public ResponseEntity<List<Map<String, Object>>> obtenerInsumosProcedimiento(@PathVariable Long id) {
        List<ProcedimientoInsumo> insumos = procedimientoInsumoRepository.findByProcedimientoId(id);

        if (insumos.isEmpty()) {
            return ResponseEntity.ok(List.of()); // Retornar lista vacía si no hay insumos
        }

        List<Map<String, Object>> resultado = insumos.stream()
                .map(pi -> {
                    Map<String, Object> insumoData = new HashMap<>();
                    insumoData.put("insumoNombre", pi.getInsumo().getNombre());
                    insumoData.put("cantidadDefecto", pi.getCantidadDefecto());
                    insumoData.put("unidad", pi.getUnidad());
                    insumoData.put("esObligatorio", pi.isEsObligatorio());
                    insumoData.put("notas", pi.getNotas());
                    return insumoData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(resultado);
    }
}
