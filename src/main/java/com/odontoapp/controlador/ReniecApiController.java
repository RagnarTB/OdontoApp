package com.odontoapp.controlador;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.odontoapp.dto.ReniecResponseDTO;
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.TipoDocumento;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.servicio.ReniecService;

/**
 * Controlador REST dedicado para consultas a RENIEC.
 * Proporciona un endpoint público para validar documentos de identidad.
 */
@RestController
@RequestMapping("/api")
public class ReniecApiController {

    private final ReniecService reniecService;
    private final PacienteRepository pacienteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;

    public ReniecApiController(ReniecService reniecService,
                              PacienteRepository pacienteRepository,
                              TipoDocumentoRepository tipoDocumentoRepository) {
        this.reniecService = reniecService;
        this.pacienteRepository = pacienteRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
    }

    /**
     * Consulta datos de RENIEC por número de documento.
     *
     * @param numDoc Número de documento (DNI)
     * @param tipoDocId ID del tipo de documento
     * @return ResponseEntity con los datos de RENIEC o error
     */
    @GetMapping("/reniec")
    public ResponseEntity<?> consultarReniec(@RequestParam("numDoc") String numDoc,
            @RequestParam("tipoDocId") Long tipoDocId) {

        // 1. Validar que el tipo de documento sea DNI
        TipoDocumento tipoDocumento = tipoDocumentoRepository.findById(tipoDocId).orElse(null);
        if (tipoDocumento == null || !"DNI".equals(tipoDocumento.getCodigo())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La consulta Reniec solo está disponible para DNI."));
        }

        // 2. Buscar si ya existe un paciente con ese documento (ignorando soft delete)
        Optional<Paciente> pacienteExistente = pacienteRepository.findByNumeroTipoDocumentoIgnorandoSoftDelete(numDoc,
                tipoDocId);

        if (pacienteExistente.isPresent()) {
            Paciente paciente = pacienteExistente.get();

            // Si el paciente está eliminado, ofrecer restauración
            if (paciente.isEliminado()) {
                return ResponseEntity.status(409).body(
                    Map.of(
                        "error", "El paciente existe, pero está eliminado lógicamente.",
                        "restaurar", true,
                        "pacienteId", paciente.getId()
                    )
                );
            }

            // Si el paciente existe y está activo, es un duplicado
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El documento ya se encuentra registrado y está activo."));
        }

        // 3. Si no existe, consultar RENIEC
        ReniecResponseDTO response = reniecService.consultarDni(numDoc);
        if (response != null && response.getNombreCompleto() != null) {
            String nombreCalculado = response.getNombreCompleto();
            Map<String, String> resultadoJson = Map.of("nombreCompleto", nombreCalculado);
            return ResponseEntity.ok(resultadoJson);
        }

        return ResponseEntity.status(404).body(
                Map.of("error", "DNI no encontrado o datos incompletos. Verifique el número."));
    }
}
