package com.odontoapp.servicio.impl;

import com.odontoapp.dto.ReporteDTO;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.PagoRepository;
import com.odontoapp.repositorio.TratamientoRealizadoRepository;
import com.odontoapp.servicio.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private final PagoRepository pagoRepository;
    private final CitaRepository citaRepository;
    private final TratamientoRealizadoRepository tratamientoRealizadoRepository;
    private final PacienteRepository pacienteRepository;

    @Override
    public List<ReporteDTO> obtenerIngresosPorMetodoPago(LocalDate fechaInicio, LocalDate fechaFin) {
        return pagoRepository.obtenerIngresosPorMetodoPago(atStartOfDay(fechaInicio), atEndOfDay(fechaFin));
    }

    @Override
    public List<ReporteDTO> obtenerIngresosPorMes(LocalDate fechaInicio, LocalDate fechaFin) {
        return pagoRepository.obtenerIngresosPorMes(atStartOfDay(fechaInicio), atEndOfDay(fechaFin));
    }

    @Override
    public List<ReporteDTO> obtenerCitasPorEstado(LocalDate fechaInicio, LocalDate fechaFin, Long odontologoId) {
        return citaRepository.obtenerCitasPorEstado(atStartOfDay(fechaInicio), atEndOfDay(fechaFin), odontologoId);
    }

    @Override
    public List<ReporteDTO> obtenerTopTratamientos(LocalDate fechaInicio, LocalDate fechaFin, Long odontologoId) {
        return tratamientoRealizadoRepository.obtenerTopTratamientos(
                atStartOfDay(fechaInicio),
                atEndOfDay(fechaFin),
                odontologoId,
                PageRequest.of(0, 10)); // Top 10
    }

    @Override
    public List<ReporteDTO> obtenerNuevosPacientesPorMes(LocalDate fechaInicio, LocalDate fechaFin) {
        return pacienteRepository.obtenerNuevosPacientesPorMes(atStartOfDay(fechaInicio), atEndOfDay(fechaFin));
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : LocalDateTime.MIN;
    }

    private LocalDateTime atEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : LocalDateTime.MAX;
    }
}