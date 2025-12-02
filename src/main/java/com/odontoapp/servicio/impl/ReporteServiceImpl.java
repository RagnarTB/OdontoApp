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

            // --- ESTILOS ---
            // Estilo Título Principal
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // Estilo Subtítulo
            CellStyle subtitleStyle = workbook.createCellStyle();
            Font subtitleFont = workbook.createFont();
            subtitleFont.setBold(true);
            subtitleFont.setFontHeightInPoints((short) 12);
            subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitleStyle.setFont(subtitleFont);
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

            // Estilo Cabecera de Tabla
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Estilo Datos
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setAlignment(HorizontalAlignment.LEFT);

            // Estilo Números
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            // --- HOJA 1: RESUMEN GENERAL ---
            Sheet sheet = workbook.createSheet("Resumen General");

            // Encabezado Corporativo
            Row rowTitle = sheet.createRow(0);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("ODONTOAPP - REPORTE DE GESTIÓN");
            cellTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            Row rowSubtitle = sheet.createRow(1);
            Cell cellSubtitle = rowSubtitle.createCell(0);
            cellSubtitle.setCellValue("Generado el: "
                    + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            cellSubtitle.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 4));

            Row rowRange = sheet.createRow(2);
            Cell cellRange = rowRange.createCell(0);
            cellRange.setCellValue("Período: " + fechaInicio + " al " + fechaFin);
            cellRange.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 0, 4));

            int rowNum = 4;

            // Sección 1: Ingresos
            rowNum = createSectionHeader(sheet, rowNum, "INGRESOS POR MÉTODO DE PAGO", headerStyle);
            List<ReporteDTO> ingresosMetodo = obtenerIngresosPorMetodoPago(fechaInicio, fechaFin);
            rowNum = createDataTable(sheet, rowNum, ingresosMetodo, dataStyle, numberStyle, "Método", "Monto (S/.)");

            rowNum += 2;
            rowNum = createSectionHeader(sheet, rowNum, "INGRESOS POR MES", headerStyle);
            List<ReporteDTO> ingresosMes = obtenerIngresosPorMes(fechaInicio, fechaFin);
            rowNum = createDataTable(sheet, rowNum, ingresosMes, dataStyle, numberStyle, "Mes", "Monto (S/.)");

            // Sección 2: Operativo
            rowNum += 2;
            rowNum = createSectionHeader(sheet, rowNum, "ESTADO DE CITAS", headerStyle);
            List<ReporteDTO> citasEstado = obtenerCitasPorEstado(fechaInicio, fechaFin, odontologoId);
            rowNum = createDataTable(sheet, rowNum, citasEstado, dataStyle, numberStyle, "Estado", "Cantidad");

            rowNum += 2;
            rowNum = createSectionHeader(sheet, rowNum, "TOP TRATAMIENTOS", headerStyle);
            List<ReporteDTO> topTratamientos = obtenerTopTratamientos(fechaInicio, fechaFin, odontologoId);
            rowNum = createDataTable(sheet, rowNum, topTratamientos, dataStyle, numberStyle, "Tratamiento", "Cantidad");

            // Sección 3: Pacientes
            rowNum += 2;
            rowNum = createSectionHeader(sheet, rowNum, "NUEVOS PACIENTES", headerStyle);
            List<ReporteDTO> nuevosPacientes = obtenerNuevosPacientesPorMes(fechaInicio, fechaFin);
            rowNum = createDataTable(sheet, rowNum, nuevosPacientes, dataStyle, numberStyle, "Mes", "Cantidad");

            // Autoajustar columnas
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.setColumnWidth(0, 8000); // Ancho fijo para la primera columna para mejor estética
            sheet.setColumnWidth(1, 4000);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private int createSectionHeader(Sheet sheet, int rowNum, String title, CellStyle style) {
        Row row = sheet.createRow(rowNum++);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        // Fusionar celdas para el título de sección
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum - 1, rowNum - 1, 0, 1));
        return rowNum;
    }

    private int createDataTable(Sheet sheet, int rowNum, List<ReporteDTO> data, CellStyle textStyle, CellStyle numStyle,
            String col1, String col2) {
        // Sub-cabeceras
        Row headerRow = sheet.createRow(rowNum++);
        Cell h1 = headerRow.createCell(0);
        h1.setCellValue(col1);
        h1.setCellStyle(textStyle);
        Cell h2 = headerRow.createCell(1);
        h2.setCellValue(col2);
        h2.setCellStyle(textStyle);

        // Datos
        for (ReporteDTO dto : data) {
            Row row = sheet.createRow(rowNum++);
            Cell c1 = row.createCell(0);
            c1.setCellValue(dto.getLabel());
            c1.setCellStyle(textStyle);
            Cell c2 = row.createCell(1);
            c2.setCellValue(dto.getValue().doubleValue());
            c2.setCellStyle(numStyle);
        }
        return rowNum;
    }

    private LocalDateTime atStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : LocalDateTime.MIN;
    }

    private LocalDateTime atEndOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : LocalDateTime.MAX;
    }
}
