package com.odontoapp.servicio;

import com.odontoapp.dto.ReporteDTO;
import java.util.List;

public interface ReporteService {
        List<ReporteDTO> obtenerIngresosPorMetodoPago(java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin);

        List<ReporteDTO> obtenerIngresosPorMes(java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin);

        List<ReporteDTO> obtenerCitasPorEstado(java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin,
                        Long odontologoId);

        List<ReporteDTO> obtenerTopTratamientos(java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin,
                        Long odontologoId);

        List<ReporteDTO> obtenerNuevosPacientesPorMes(java.time.LocalDate fechaInicio, java.time.LocalDate fechaFin);

        
}
