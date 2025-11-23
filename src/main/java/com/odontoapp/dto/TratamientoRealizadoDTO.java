package com.odontoapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para registrar tratamientos realizados durante una cita.
 * Incluye información del procedimiento, insumos ajustados y detalles del trabajo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TratamientoRealizadoDTO {

    private Long id;

    @NotNull(message = "El ID de la cita es obligatorio")
    private Long citaId;

    @NotNull(message = "El ID del procedimiento es obligatorio")
    private Long procedimientoId;

    @NotNull(message = "El ID del odontólogo es obligatorio")
    private Long odontologoUsuarioId;

    @Size(max = 50, message = "La pieza dental no puede exceder 50 caracteres")
    private String piezaDental;

    @Size(max = 2000, message = "La descripción del trabajo no puede exceder 2000 caracteres")
    private String descripcionTrabajo;

    @NotNull(message = "La fecha de realización es obligatoria")
    private LocalDateTime fechaRealizacion;

    @Positive(message = "La cantidad del insumo ajustada debe ser positiva")
    private BigDecimal cantidadInsumoAjustada;

    private Long insumoAjustadoId;

    // --- Campos de solo lectura (para mostrar información) ---

    private String procedimientoNombre;

    private String odontologoNombre;

    private String insumoAjustadoNombre;
}
