package com.odontoapp.servicio.impl;

import com.odontoapp.dto.CitaDTO;
import com.odontoapp.dto.InsumoDTO;
import com.odontoapp.entidad.*;
import com.odontoapp.repositorio.*;
import com.odontoapp.servicio.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Dashboard
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final InsumoRepository insumoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final PagoRepository pagoRepository;

    @Override
    public Map<String, Object> obtenerEstadisticasGenerales() {
        Map<String, Object> estadisticas = new HashMap<>();

        estadisticas.put("citasDelDia", obtenerCitasDelDia());
        estadisticas.put("pacientesNuevos", obtenerPacientesNuevosDelMes());
        estadisticas.put("ingresosPendientes", obtenerIngresosPendientes());
        estadisticas.put("ingresosDelMes", obtenerIngresosDelMes());
        estadisticas.put("totalPacientes", obtenerTotalPacientes());
        estadisticas.put("proximasCitas", obtenerProximasCitas());
        estadisticas.put("insumosStockBajo", obtenerInsumosStockBajo());

        return estadisticas;
    }

    @Override
    public Long obtenerCitasDelDia() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDelDia = hoy.atStartOfDay();
        LocalDateTime finDelDia = hoy.atTime(LocalTime.MAX);

        return citaRepository.countByFechaHoraInicioBetween(inicioDelDia, finDelDia);
    }

    @Override
    public Long obtenerPacientesNuevosDelMes() {
        YearMonth mesActual = YearMonth.now();
        LocalDate inicioDelMes = mesActual.atDay(1);
        LocalDate finDelMes = mesActual.atEndOfMonth();

        LocalDateTime inicioDateTime = inicioDelMes.atStartOfDay();
        LocalDateTime finDateTime = finDelMes.atTime(LocalTime.MAX);

        return pacienteRepository.countByFechaCreacionBetween(inicioDateTime, finDateTime);
    }

    @Override
    public BigDecimal obtenerIngresosPendientes() {
        // Obtener el estado "Pendiente"
        EstadoPago estadoPendiente = estadoPagoRepository.findByNombre("Pendiente")
                .orElse(null);

        if (estadoPendiente == null) {
            return BigDecimal.ZERO;
        }

        // Obtener comprobantes pendientes de pago
        List<Comprobante> comprobantesPendientes = comprobanteRepository
                .findByEstadoPagoAndEliminadoFalse(estadoPendiente);

        return comprobantesPendientes.stream()
                .map(Comprobante::getMontoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal obtenerIngresosDelMes() {
        YearMonth mesActual = YearMonth.now();
        LocalDate inicioDelMes = mesActual.atDay(1);
        LocalDate finDelMes = mesActual.atEndOfMonth();

        LocalDateTime inicioDateTime = inicioDelMes.atStartOfDay();
        LocalDateTime finDateTime = finDelMes.atTime(LocalTime.MAX);

        // Sumar todos los pagos realizados en el mes actual
        return pagoRepository.sumMontoByFechaPagoBetween(inicioDateTime, finDateTime);
    }

    @Override
    public Long obtenerTotalPacientes() {
        return pacienteRepository.countByEliminadoFalse();
    }

    @Override
    public List<CitaDTO> obtenerProximasCitas() {
        LocalDate hoy = LocalDate.now();
        LocalDate pasadoManana = hoy.plusDays(2);

        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fin = pasadoManana.atTime(LocalTime.MAX);

        List<Cita> citas = citaRepository.findByFechaHoraInicioBetween(inicio, fin);

        // Limitar a las próximas 5 citas
        return citas.stream()
                .limit(5)
                .map(this::convertirACitaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InsumoDTO> obtenerInsumosStockBajo() {
        List<Insumo> insumos = insumoRepository.findInsumosConStockBajo();

        // Limitar a los primeros 5
        return insumos.stream()
                .limit(5)
                .map(this::convertirAInsumoDTO)
                .collect(Collectors.toList());
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

    /**
     * Convierte una entidad Insumo a InsumoDTO
     */
    private InsumoDTO convertirAInsumoDTO(Insumo insumo) {
        InsumoDTO dto = new InsumoDTO();
        dto.setId(insumo.getId());
        dto.setNombre(insumo.getNombre());
        dto.setStockActual(insumo.getStockActual());
        dto.setStockMinimo(insumo.getStockMinimo());

        if (insumo.getUnidadMedida() != null) {
            dto.setUnidadMedidaNombre(insumo.getUnidadMedida().getNombre());
        }

        return dto;
    }
}
