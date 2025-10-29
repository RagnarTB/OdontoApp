package com.odontoapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para registrar pagos realizados sobre comprobantes.
 * Soporta pagos mixtos (efectivo + Yape).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {

    private Long id;

    @NotNull(message = "El ID del comprobante es obligatorio")
    private Long comprobanteId;

    @NotNull(message = "La fecha de pago es obligatoria")
    private LocalDateTime fechaPago;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal monto;

    @NotNull(message = "El ID del método de pago es obligatorio")
    private Long metodoPagoId;

    private String referenciaYape;

    @PositiveOrZero(message = "El monto en efectivo no puede ser negativo")
    private BigDecimal montoEfectivo;

    @PositiveOrZero(message = "El monto Yape no puede ser negativo")
    private BigDecimal montoYape;

    private String notas;

    // --- Campos de solo lectura (para mostrar información) ---

    private String metodoPagoNombre;

    private String comprobanteNumero;
}
