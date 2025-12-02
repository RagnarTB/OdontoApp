package com.odontoapp.servicio.impl;

import com.odontoapp.dto.ReporteDTO;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.PagoRepository;
import com.odontoapp.repositorio.TratamientoRealizadoRepository;
import com.odontoapp.servicio.ReporteService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    @Override
    public byte[] generarReporteExcel(LocalDate fechaInicio, LocalDate fechaFin, Long odontologoId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Hoja 1: Resumen Financiero
            Sheet sheetFinanciero = workbook.createSheet("Financiero");
            createHeader(sheetFinanciero, headerStyle, "Concepto", "Valor");

            int rowNum = 1;
            Row rowTitle = sheetFinanciero.createRow(rowNum++);
            rowTitle.createCell(0).setCellValue("Ingresos por Método de Pago");

            List<ReporteDTO> ingresosMetodo = obtenerIngresosPorMetodoPago(fechaInicio, fechaFin);
            for (ReporteDTO dato : ingresosMetodo) {
                Row row = sheetFinanciero.createRow(rowNum++);
                row.createCell(0).setCellValue(dato.getLabel());
                row.createCell(1).setCellValue(dato.getValue().doubleValue());
            }

            rowNum++;
            Row rowTitle2 = sheetFinanciero.createRow(rowNum++);
            rowTitle2.createCell(0).setCellValue("Ingresos por Mes");

            List<ReporteDTO> ingresosMes = obtenerIngresosPorMes(fechaInicio, fechaFin);
            for (ReporteDTO dato : ingresosMes) {
                Row row = sheetFinanciero.createRow(rowNum++);
                row.createCell(0).setCellValue(dato.getLabel());
                row.createCell(1).setCellValue(dato.getValue().doubleValue());
            }

            sheetFinanciero.autoSizeColumn(0);
            sheetFinanciero.autoSizeColumn(1);

            // Hoja 2: Operativo
            Sheet sheetOperativo = workbook.createSheet("Operativo");
            createHeader(sheetOperativo, headerStyle, "Concepto", "Cantidad");

            rowNum = 1;
            Row rowTitleOp = sheetOperativo.createRow(rowNum++);
            rowTitleOp.createCell(0).setCellValue("Estado de Citas");

            List<ReporteDTO> citasEstado = obtenerCitasPorEstado(fechaInicio, fechaFin, odontologoId);
            for (ReporteDTO dato : citasEstado) {
                Row row = sheetOperativo.createRow(rowNum++);
                row.createCell(0).setCellValue(dato.getLabel());
                row.createCell(1).setCellValue(dato.getValue().doubleValue());
            }

            rowNum++;
            Row rowTitleOp2 = sheetOperativo.createRow(rowNum++);
            rowTitleOp2.createCell(0).setCellValue("Top Tratamientos");

            List<ReporteDTO> topTratamientos = obtenerTopTratamientos(fechaInicio, fechaFin, odontologoId);
            for (ReporteDTO dato : topTratamientos) {
                Row row = sheetOperativo.createRow(rowNum++);
                row.createCell(0).setCellValue(dato.getLabel());
                row.createCell(1).setCellValue(dato.getValue().doubleValue());
            }

            sheetOperativo.autoSizeColumn(0);
            sheetOperativo.autoSizeColumn(1);

            // Hoja 3: Pacientes
            Sheet sheetPacientes = workbook.createSheet("Pacientes");
            createHeader(sheetPacientes, headerStyle, "Mes", "Nuevos Pacientes");

            rowNum = 1;
            List<ReporteDTO> nuevosPacientes = obtenerNuevosPacientesPorMes(fechaInicio, fechaFin);
            for (ReporteDTO dato : nuevosPacientes) {
                Row row = sheetPacientes.createRow(rowNum++);
                row.createCell(0).setCellValue(dato.getLabel());
                row.createCell(1).setCellValue(dato.getValue().doubleValue());
            }

            sheetPacientes.autoSizeColumn(0);
            sheetPacientes.autoSizeColumn(1);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createHeader(Sheet sheet, CellStyle style, String... headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : LocalDateTime.MIN;
    }

    private LocalDateTime atEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : LocalDateTime.MAX;
    }
}
