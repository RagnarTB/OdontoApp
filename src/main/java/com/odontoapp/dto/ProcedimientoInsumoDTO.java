package com.odontoapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para gestionar la relación entre procedimientos e insumos.
 * Define qué insumos se utilizan por defecto en cada procedimiento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcedimientoInsumoDTO {

    private Long id;

    @NotNull(message = "El ID del procedimiento es obligatorio")
    private Long procedimientoId;

    @NotNull(message = "El ID del insumo es obligatorio")
    private Long insumoId;

    @NotNull(message = "La cantidad por defecto es obligatoria")
    @Positive(message = "La cantidad por defecto debe ser positiva")
    private BigDecimal cantidadDefault;

    // --- Campos de solo lectura (para mostrar información) ---

    private String procedimientoNombre;

    private String procedimientoCodigo;

    private String insumoNombre;

    private String insumoCodigo;

    private String insumoUnidadMedida;

    private BigDecimal insumoStockActual;
}
