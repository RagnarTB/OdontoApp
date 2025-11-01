package com.odontoapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * DTO para eventos de calendario compatibles con FullCalendar.js.
 * Representa una cita en el formato esperado por la librería FullCalendar.
 */
@Data
@AllArgsConstructor
public class FullCalendarEventDTO {

    /**
     * ID único del evento (ID de la cita).
     */
    private String id;

    /**
     * Título del evento (nombre del paciente).
     */
    private String title;

    /**
     * Fecha/hora de inicio en formato ISO 8601 (ej: "2025-10-31T10:30:00").
     */
    private String start;

    /**
     * Fecha/hora de fin en formato ISO 8601 (ej: "2025-10-31T11:00:00").
     */
    private String end;

    /**
     * Color de fondo del evento según el estado de la cita.
     */
    private String color;

    /**
     * Color del borde del evento según el estado de la cita.
     */
    private String borderColor;

    /**
     * Propiedades extendidas para almacenar información adicional.
     * Incluye: pacienteId, odontologoId, procedimientoId, estadoNombre, etc.
     */
    private Map<String, Object> extendedProps;
}
