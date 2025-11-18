package com.odontoapp.servicio.impl;

import com.odontoapp.dto.OdontogramaDienteDTO;
import com.odontoapp.entidad.*;
import com.odontoapp.repositorio.*;
import com.odontoapp.servicio.OdontogramaDienteService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión del odontograma.
 */
@Service
@Transactional
public class OdontogramaDienteServiceImpl implements OdontogramaDienteService {

    private final OdontogramaDienteRepository odontogramaDienteRepository;
    private final OdontogramaHistorialRepository historialRepository;
    private final UsuarioRepository usuarioRepository;
    private final TratamientoRealizadoRepository tratamientoRepository;

    // Mapa de nombres de dientes según posición FDI
    private static final Map<String, String> NOMBRES_DIENTES = Map.ofEntries(
        Map.entry("1", "Incisivo Central"),
        Map.entry("2", "Incisivo Lateral"),
        Map.entry("3", "Canino"),
        Map.entry("4", "Primer Premolar"),
        Map.entry("5", "Segundo Premolar"),
        Map.entry("6", "Primer Molar"),
        Map.entry("7", "Segundo Molar"),
        Map.entry("8", "Tercer Molar")
    );

    public OdontogramaDienteServiceImpl(
            OdontogramaDienteRepository odontogramaDienteRepository,
            OdontogramaHistorialRepository historialRepository,
            UsuarioRepository usuarioRepository,
            TratamientoRealizadoRepository tratamientoRepository) {
        this.odontogramaDienteRepository = odontogramaDienteRepository;
        this.historialRepository = historialRepository;
        this.usuarioRepository = usuarioRepository;
        this.tratamientoRepository = tratamientoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdontogramaDienteDTO> obtenerOdontogramaCompleto(Long pacienteUsuarioId) {
        Usuario paciente = buscarPaciente(pacienteUsuarioId);

        List<OdontogramaDiente> dientes = odontogramaDienteRepository.findByPaciente(paciente);

        // Si no tiene odontograma, inicializarlo automáticamente
        if (dientes.isEmpty()) {
            return inicializarOdontograma(pacienteUsuarioId);
        }

        return dientes.stream()
            .map(this::convertirADTO)
            .sorted(Comparator.comparing(OdontogramaDienteDTO::getNumeroDiente))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OdontogramaDienteDTO actualizarEstadoDiente(
            Long pacienteUsuarioId,
            String numeroDiente,
            String nuevoEstado,
            String superficiesAfectadas,
            String notas,
            Long tratamientoRealizadoId) {

        Usuario paciente = buscarPaciente(pacienteUsuarioId);

        // Buscar o crear el diente
        OdontogramaDiente diente = odontogramaDienteRepository
            .findByPacienteAndNumeroDiente(paciente, numeroDiente)
            .orElseGet(() -> {
                OdontogramaDiente nuevo = new OdontogramaDiente();
                nuevo.setPaciente(paciente);
                nuevo.setNumeroDiente(numeroDiente);
                nuevo.setEstado("SANO");
                return nuevo;
            });

        // Guardar estado anterior para historial
        String estadoAnterior = diente.getEstado();

        // Actualizar estado
        diente.setEstado(nuevoEstado);
        diente.setSuperficiesAfectadas(superficiesAfectadas);
        diente.setNotas(notas);
        diente.setFechaUltimaModificacion(LocalDateTime.now());

        OdontogramaDiente dienteGuardado = odontogramaDienteRepository.save(diente);

        // Registrar en historial solo si cambió el estado
        if (!estadoAnterior.equals(nuevoEstado)) {
            registrarEnHistorial(paciente, numeroDiente, estadoAnterior, nuevoEstado,
                               notas, tratamientoRealizadoId);
        }

        return convertirADTO(dienteGuardado);
    }

    @Override
    @Transactional
    public List<OdontogramaDienteDTO> actualizarMultiplesDientes(
            Long pacienteUsuarioId,
            List<OdontogramaDienteDTO> cambios) {

        List<OdontogramaDienteDTO> resultados = new ArrayList<>();

        for (OdontogramaDienteDTO cambio : cambios) {
            OdontogramaDienteDTO resultado = actualizarEstadoDiente(
                pacienteUsuarioId,
                cambio.getNumeroDiente(),
                cambio.getEstado(),
                cambio.getSuperficiesAfectadas(),
                cambio.getNotas(),
                null
            );
            resultados.add(resultado);
        }

        return resultados;
    }

    @Override
    @Transactional
    public List<OdontogramaDienteDTO> inicializarOdontograma(Long pacienteUsuarioId) {
        Usuario paciente = buscarPaciente(pacienteUsuarioId);

        List<OdontogramaDiente> dientes = new ArrayList<>();

        // Crear los 32 dientes permanentes (cuadrantes 1-4, dientes 1-8)
        for (int cuadrante = 1; cuadrante <= 4; cuadrante++) {
            for (int posicion = 1; posicion <= 8; posicion++) {
                String numeroDiente = String.valueOf(cuadrante) + posicion;

                // Verificar que no exista ya
                if (!odontogramaDienteRepository
                        .existsByPacienteAndNumeroDiente(paciente, numeroDiente)) {

                    OdontogramaDiente diente = new OdontogramaDiente();
                    diente.setPaciente(paciente);
                    diente.setNumeroDiente(numeroDiente);
                    diente.setEstado("SANO");
                    diente.setFechaUltimaModificacion(LocalDateTime.now());
                    dientes.add(diente);
                }
            }
        }

        List<OdontogramaDiente> guardados = odontogramaDienteRepository.saveAll(dientes);

        return guardados.stream()
            .map(this::convertirADTO)
            .sorted(Comparator.comparing(OdontogramaDienteDTO::getNumeroDiente))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdontogramaHistorial> obtenerHistorialDiente(
            Long pacienteUsuarioId, String numeroDiente) {
        Usuario paciente = buscarPaciente(pacienteUsuarioId);
        return historialRepository
            .findByPacienteAndNumeroDienteOrderByFechaCambioDesc(paciente, numeroDiente);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> obtenerEstadisticas(Long pacienteUsuarioId) {
        Usuario paciente = buscarPaciente(pacienteUsuarioId);
        List<OdontogramaDiente> dientes = odontogramaDienteRepository.findByPaciente(paciente);

        Map<String, Integer> estadisticas = new HashMap<>();
        estadisticas.put("total", dientes.size());
        estadisticas.put("sanos", (int) dientes.stream()
            .filter(d -> "SANO".equals(d.getEstado())).count());
        estadisticas.put("caries", (int) dientes.stream()
            .filter(d -> "CARIES".equals(d.getEstado())).count());
        estadisticas.put("restaurados", (int) dientes.stream()
            .filter(d -> "RESTAURADO".equals(d.getEstado())).count());
        estadisticas.put("endodoncia", (int) dientes.stream()
            .filter(d -> "ENDODONCIA".equals(d.getEstado())).count());
        estadisticas.put("coronas", (int) dientes.stream()
            .filter(d -> "CORONA".equals(d.getEstado())).count());
        estadisticas.put("extraidos", (int) dientes.stream()
            .filter(d -> "EXTRACCION".equals(d.getEstado())).count());
        estadisticas.put("implantes", (int) dientes.stream()
            .filter(d -> "IMPLANTE".equals(d.getEstado())).count());
        estadisticas.put("ausentes", (int) dientes.stream()
            .filter(d -> "AUSENTE".equals(d.getEstado())).count());
        estadisticas.put("fracturados", (int) dientes.stream()
            .filter(d -> "FRACTURADO".equals(d.getEstado())).count());

        return estadisticas;
    }

    @Override
    @Transactional
    public void actualizarDesdeTratamiento(Long tratamientoRealizadoId) {
        TratamientoRealizado tratamiento = tratamientoRepository.findById(tratamientoRealizadoId)
            .orElseThrow(() -> new RuntimeException("Tratamiento no encontrado"));

        String piezaDental = tratamiento.getPiezaDental();
        if (piezaDental == null || piezaDental.isEmpty()) {
            return; // No hay pieza dental específica
        }

        // Puede tener múltiples dientes separados por coma
        String[] dientes = piezaDental.split(",");

        for (String diente : dientes) {
            String numeroDiente = diente.trim();

            // Mapear procedimiento a estado del diente
            String procedimientoNombre = tratamiento.getProcedimiento().getNombre().toUpperCase();
            String nuevoEstado = mapearProcedimientoAEstado(procedimientoNombre);

            if (nuevoEstado != null) {
                try {
                    actualizarEstadoDiente(
                        tratamiento.getCita().getPaciente().getId(),
                        numeroDiente,
                        nuevoEstado,
                        null,
                        "Actualizado automáticamente desde tratamiento: " + tratamiento.getProcedimiento().getNombre(),
                        tratamientoRealizadoId
                    );
                } catch (Exception e) {
                    // Log pero continuar con otros dientes
                    System.err.println("Error al actualizar diente " + numeroDiente + ": " + e.getMessage());
                }
            }
        }
    }

    // =============== MÉTODOS PRIVADOS ===============

    private Usuario buscarPaciente(Long pacienteUsuarioId) {
        return usuarioRepository.findById(pacienteUsuarioId)
            .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + pacienteUsuarioId));
    }

    private void registrarEnHistorial(Usuario paciente, String numeroDiente,
                                     String estadoAnterior, String estadoNuevo,
                                     String notas, Long tratamientoRealizadoId) {

        OdontogramaHistorial historial = new OdontogramaHistorial();
        historial.setPaciente(paciente);
        historial.setNumeroDiente(numeroDiente);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setNotas(notas);
        historial.setFechaCambio(LocalDateTime.now());

        // Intentar obtener el usuario actual del contexto de seguridad
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario usuario = usuarioRepository.findByEmail(username).orElse(paciente);
            historial.setUsuario(usuario);
        } catch (Exception e) {
            // Si no hay contexto de seguridad, usar el paciente
            historial.setUsuario(paciente);
        }

        if (tratamientoRealizadoId != null) {
            TratamientoRealizado tratamiento = tratamientoRepository
                .findById(tratamientoRealizadoId).orElse(null);
            historial.setTratamientoRealizado(tratamiento);
        }

        historialRepository.save(historial);
    }

    private OdontogramaDienteDTO convertirADTO(OdontogramaDiente diente) {
        OdontogramaDienteDTO dto = new OdontogramaDienteDTO();
        dto.setId(diente.getId());
        dto.setNumeroDiente(diente.getNumeroDiente());
        dto.setEstado(diente.getEstado());
        dto.setSuperficiesAfectadas(diente.getSuperficiesAfectadas());
        dto.setNotas(diente.getNotas());
        dto.setFechaUltimaModificacion(diente.getFechaUltimaModificacion());

        // Extraer cuadrante y posición
        String numero = diente.getNumeroDiente();
        if (numero != null && numero.length() == 2) {
            dto.setCuadrante(numero.substring(0, 1));
            dto.setPosicion(numero.substring(1));
            dto.setNombreDiente(NOMBRES_DIENTES.getOrDefault(dto.getPosicion(), "Diente"));
        }

        return dto;
    }

    /**
     * Mapea el nombre de un procedimiento al estado correspondiente del diente
     */
    private String mapearProcedimientoAEstado(String procedimiento) {
        if (procedimiento.contains("OBTURACIÓN") || procedimiento.contains("RESTAURACIÓN") ||
            procedimiento.contains("RESINA") || procedimiento.contains("AMALGAMA")) {
            return "RESTAURADO";
        }
        if (procedimiento.contains("ENDODONCIA") || procedimiento.contains("CONDUCTO")) {
            return "ENDODONCIA";
        }
        if (procedimiento.contains("CORONA") || procedimiento.contains("PRÓTESIS FIJA")) {
            return "CORONA";
        }
        if (procedimiento.contains("EXTRACCIÓN") || procedimiento.contains("EXODONCIA")) {
            return "EXTRACCION";
        }
        if (procedimiento.contains("IMPLANTE")) {
            return "IMPLANTE";
        }
        // No se puede mapear automáticamente
        return null;
    }
}
