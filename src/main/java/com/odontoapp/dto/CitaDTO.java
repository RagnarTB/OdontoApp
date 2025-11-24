package com.odontoapp.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para transferir datos de citas entre capas de la aplicación.
 * Incluye campos para recepción y envío de datos de citas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitaDTO {

    private Long id;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteUsuarioId;

    @NotNull(message = "El ID del odontólogo es obligatorio")
    private Long odontologoUsuarioId;

    private Long procedimientoId;

    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de la cita debe ser presente o futura")
    private LocalDateTime fechaHoraInicio;

    @Size(max = 500, message = "El motivo de consulta no puede exceder 500 caracteres")
    private String motivoConsulta;

    @Size(max = 1000, message = "Las notas internas no pueden exceder 1000 caracteres")
    private String notasInternas;

    private String estadoCitaNombre;

    // --- Campos de solo lectura (para mostrar información) ---

    private String pacienteNombre;

    private String odontologoNombre;

    private String procedimientoNombre;

    private LocalDateTime fechaHoraFin;

    private Integer duracionEstimadaMinutos;

    private String motivoCancelacion;

    private BigDecimal precioBase;
}
