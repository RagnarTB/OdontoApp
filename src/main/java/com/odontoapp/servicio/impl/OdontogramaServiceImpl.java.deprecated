package com.odontoapp.servicio.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.OdontogramaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de gestión del odontograma.
 * Maneja el estado dental de los pacientes en formato JSON.
 */
@Service
@Transactional
public class OdontogramaServiceImpl implements OdontogramaService {

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final ObjectMapper objectMapper;

    public OdontogramaServiceImpl(UsuarioRepository usuarioRepository,
                                 PacienteRepository pacienteRepository,
                                 ObjectMapper objectMapper) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void actualizarEstadoOdontograma(Long pacienteUsuarioId, String estadoJson) {
        // Validar que el JSON no sea nulo o vacío
        if (estadoJson == null || estadoJson.trim().isEmpty()) {
            throw new IllegalArgumentException("El estado del odontograma no puede estar vacío");
        }

        // Validar formato JSON
        if (!validarFormatoOdontograma(estadoJson)) {
            throw new IllegalArgumentException("El formato del odontograma no es un JSON válido");
        }

        // Buscar el usuario paciente
        Usuario usuario = usuarioRepository.findById(pacienteUsuarioId)
            .orElseThrow(() -> new EntityNotFoundException(
                "No se encontró el usuario con ID: " + pacienteUsuarioId));

        // Obtener el paciente asociado
        Paciente paciente = usuario.getPaciente();
        if (paciente == null) {
            throw new IllegalStateException(
                "El usuario con ID " + pacienteUsuarioId + " no tiene un perfil de paciente asociado");
        }

        // Actualizar el estado del odontograma
        paciente.setEstadoOdontograma(estadoJson);
        pacienteRepository.save(paciente);
    }

    @Override
    @Transactional(readOnly = true)
    public String obtenerEstadoOdontograma(Long pacienteUsuarioId) {
        // Buscar el usuario paciente
        Usuario usuario = usuarioRepository.findById(pacienteUsuarioId)
            .orElseThrow(() -> new EntityNotFoundException(
                "No se encontró el usuario con ID: " + pacienteUsuarioId));

        // Obtener el paciente asociado
        Paciente paciente = usuario.getPaciente();
        if (paciente == null) {
            throw new IllegalStateException(
                "El usuario con ID " + pacienteUsuarioId + " no tiene un perfil de paciente asociado");
        }

        return paciente.getEstadoOdontograma();
    }

    @Override
    public boolean validarFormatoOdontograma(String estadoJson) {
        if (estadoJson == null || estadoJson.trim().isEmpty()) {
            return false;
        }

        try {
            // Intentar parsear el JSON
            JsonNode jsonNode = objectMapper.readTree(estadoJson);

            // El JSON debe ser un objeto o un array
            return jsonNode.isObject() || jsonNode.isArray();
        } catch (Exception e) {
            // Si hay cualquier error al parsear, no es un JSON válido
            return false;
        }
    }

    @Override
    @Transactional
    public String inicializarOdontograma(Long pacienteUsuarioId) {
        // Buscar el usuario paciente
        Usuario usuario = usuarioRepository.findById(pacienteUsuarioId)
            .orElseThrow(() -> new EntityNotFoundException(
                "No se encontró el usuario con ID: " + pacienteUsuarioId));

        // Obtener el paciente asociado
        Paciente paciente = usuario.getPaciente();
        if (paciente == null) {
            throw new IllegalStateException(
                "El usuario con ID " + pacienteUsuarioId + " no tiene un perfil de paciente asociado");
        }

        // Crear estructura JSON base del odontograma
        // Formato estándar ISO 3950 (FDI World Dental Federation)
        // Adultos: 32 dientes (cuadrantes 1-4, dientes 1-8 por cuadrante)
        // Estructura: { "dientes": { "11": {...}, "12": {...}, ..., "48": {...} } }
        String odontogramaBase = generarOdontogramaBase();

        // Guardar el odontograma inicializado
        paciente.setEstadoOdontograma(odontogramaBase);
        pacienteRepository.save(paciente);

        return odontogramaBase;
    }

    /**
     * Genera la estructura JSON base de un odontograma vacío.
     * Incluye los 32 dientes permanentes según notación FDI.
     *
     * Cuadrantes:
     * - 1: Superior derecho (11-18)
     * - 2: Superior izquierdo (21-28)
     * - 3: Inferior izquierdo (31-38)
     * - 4: Inferior derecho (41-48)
     *
     * Estados posibles por diente:
     * - sano: Diente sano sin tratamientos
     * - caries: Presencia de caries
     * - obturado: Diente con obturación/restauración
     * - endodoncia: Tratamiento de conducto
     * - corona: Corona dental
     * - ausente: Diente faltante
     * - extraccion_indicada: Marcado para extracción
     *
     * @return JSON string con la estructura base del odontograma
     */
    private String generarOdontogramaBase() {
        StringBuilder json = new StringBuilder();
        json.append("{\"dientes\":{");

        boolean primero = true;
        // Generar los 32 dientes (4 cuadrantes × 8 dientes)
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            for (int diente = 1; diente <= 8; diente++) {
                if (!primero) {
                    json.append(",");
                }
                primero = false;

                String numeroDiente = String.valueOf(cuadrante) + diente;
                json.append("\"").append(numeroDiente).append("\":{");
                json.append("\"numero\":\"").append(numeroDiente).append("\",");
                json.append("\"estado\":\"sano\",");
                json.append("\"observaciones\":\"\",");
                json.append("\"tratamientos\":[]");
                json.append("}");
            }
        }

        json.append("},");
        json.append("\"fechaActualizacion\":\"").append(java.time.LocalDateTime.now()).append("\",");
        json.append("\"notas\":\"\"");
        json.append("}");

        return json.toString();
    }
}
