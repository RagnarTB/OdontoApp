package com.odontoapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MovimientoDTO {
    @NotNull
    private Long insumoId;

    @NotNull(message = "Debe seleccionar un tipo de movimiento.")
    private Long tipoMovimientoId;

    @NotNull(message = "Debe seleccionar un motivo.")
    private Long motivoMovimientoId;

    @NotNull(message = "La cantidad es obligatoria.")
    @Positive(message = "La cantidad debe ser positiva.")
    private BigDecimal cantidad;

    private String notas;
    private String referencia;
}
