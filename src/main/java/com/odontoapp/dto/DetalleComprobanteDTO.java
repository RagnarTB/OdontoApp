package com.odontoapp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para representar líneas de detalle en un comprobante.
 * Cada detalle puede ser un procedimiento o un insumo vendido.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleComprobanteDTO {

    private Long id;

    @NotEmpty(message = "El tipo de ítem es obligatorio")
    private String tipoItem; // 'PROCEDIMIENTO' o 'INSUMO'

    @NotNull(message = "El ID del ítem es obligatorio")
    private Long itemId;

    @NotEmpty(message = "La descripción del ítem es obligatoria")
    private String descripcionItem;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser positiva")
    private BigDecimal cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @PositiveOrZero(message = "El precio unitario no puede ser negativo")
    private BigDecimal precioUnitario;

    @NotNull(message = "El subtotal es obligatorio")
    @PositiveOrZero(message = "El subtotal no puede ser negativo")
    private BigDecimal subtotal;

    private String notas;
}
