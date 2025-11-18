package com.odontoapp.servicio.impl;

import com.odontoapp.dto.CitaDTO;
import com.odontoapp.entidad.*;
import com.odontoapp.repositorio.*;
import com.odontoapp.servicio.PacienteDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de dashboard para el portal de pacientes
 */
@Service
@RequiredArgsConstructor
public class PacienteDashboardServiceImpl implements PacienteDashboardService {

    private final CitaRepository citaRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final TratamientoPlanificadoRepository tratamientoPlanificadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoPagoRepository estadoPagoRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticasPaciente(Long usuarioId) {
        Map<String, Object> estadisticas = new HashMap<>();

        estadisticas.put("proximasCitas", obtenerProximasCitas(usuarioId));
        estadisticas.put("ultimaCita", obtenerUltimaCitaRealizada(usuarioId));
        estadisticas.put("tratamientosEnCurso", obtenerTratamientosEnCurso(usuarioId));
        estadisticas.put("saldoPendiente", obtenerSaldoPendiente(usuarioId));
        estadisticas.put("citasCanceladas", obtenerCitasCanceladas(usuarioId));

        return estadisticas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitaDTO> obtenerProximasCitas(Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now();

        // Buscar todas las citas futuras del paciente
        List<Cita> citasFuturas = citaRepository.findByFechaHoraInicioAfterAndPacienteId(
            ahora, usuarioId, PageRequest.of(0, 10, Sort.by("fechaHoraInicio").ascending())
        ).getContent();

        // Filtrar solo CONFIRMADA y PENDIENTE
        return citasFuturas.stream()
            .filter(cita -> {
                String estado = cita.getEstadoCita().getNombre();
                return "CONFIRMADA".equals(estado) || "PENDIENTE".equals(estado);
            })
            .limit(3)
            .map(this::convertirACitaDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Cita obtenerUltimaCitaRealizada(Long usuarioId) {
        // Buscar citas pasadas con estado ASISTIO
        LocalDateTime ahora = LocalDateTime.now();

        List<Cita> citasPasadas = citaRepository.findByFechaHoraInicioBeforeAndPacienteId(
            ahora, usuarioId, PageRequest.of(0, 10, Sort.by("fechaHoraInicio").descending())
        ).getContent();

        // Buscar la primera con estado ASISTIO
        return citasPasadas.stream()
            .filter(cita -> "ASISTIO".equals(cita.getEstadoCita().getNombre()))
            .findFirst()
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TratamientoPlanificado> obtenerTratamientosEnCurso(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return Collections.emptyList();
        }

        // Obtener tratamientos pendientes (PLANIFICADO o EN_CURSO)
        return tratamientoPlanificadoRepository.findTratamientosPendientes(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldoPendiente(Long usuarioId) {
        // Obtener estados PENDIENTE y PAGADO_PARCIAL
        EstadoPago estadoPendiente = estadoPagoRepository.findByNombre("PENDIENTE").orElse(null);
        EstadoPago estadoParcial = estadoPagoRepository.findByNombre("PAGADO_PARCIAL").orElse(null);

        List<Comprobante> comprobantesPendientes = new ArrayList<>();

        if (estadoPendiente != null) {
            comprobantesPendientes.addAll(
                comprobanteRepository.findByPacienteIdAndEstadoPago(usuarioId, estadoPendiente)
            );
        }

        if (estadoParcial != null) {
            comprobantesPendientes.addAll(
                comprobanteRepository.findByPacienteIdAndEstadoPago(usuarioId, estadoParcial)
            );
        }

        // Sumar el monto pendiente de todos los comprobantes
        return comprobantesPendientes.stream()
            .map(Comprobante::getMontoPendiente)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public Long obtenerCitasCanceladas(Long usuarioId) {
        // Últimos 6 meses
        LocalDateTime hace6Meses = LocalDateTime.now().minusMonths(6);

        List<Cita> todasCitas = citaRepository.findByPacienteIdAndFechaHoraInicioBetween(
            usuarioId, hace6Meses, LocalDateTime.now()
        );

        // Contar las canceladas (CANCELADA_PACIENTE o CANCELADA_CLINICA)
        return todasCitas.stream()
            .filter(cita -> {
                String estado = cita.getEstadoCita().getNombre();
                return estado.startsWith("CANCELADA");
            })
            .count();
    }

    /**
     * Convierte una entidad Cita a CitaDTO
     */
    private CitaDTO convertirACitaDTO(Cita cita) {
        CitaDTO dto = new CitaDTO();
        dto.setId(cita.getId());
        dto.setFechaHoraInicio(cita.getFechaHoraInicio());
        dto.setFechaHoraFin(cita.getFechaHoraFin());
        dto.setMotivoConsulta(cita.getMotivoConsulta());
        dto.setDuracionEstimadaMinutos(cita.getDuracionEstimadaMinutos());

        if (cita.getEstadoCita() != null) {
            dto.setEstadoCitaNombre(cita.getEstadoCita().getNombre());
        }

        if (cita.getPaciente() != null) {
            dto.setPacienteNombre(cita.getPaciente().getNombreCompleto());
            dto.setPacienteUsuarioId(cita.getPaciente().getId());
        }

        if (cita.getOdontologo() != null) {
            dto.setOdontologoNombre(cita.getOdontologo().getNombreCompleto());
            dto.setOdontologoUsuarioId(cita.getOdontologo().getId());
        }

        if (cita.getProcedimiento() != null) {
            dto.setProcedimientoNombre(cita.getProcedimiento().getNombre());
            dto.setProcedimientoId(cita.getProcedimiento().getId());
        }

        return dto;
    }
}
