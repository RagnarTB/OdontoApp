package com.odontoapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para transferir datos de comprobantes de pago (boletas, facturas).
 * Incluye detalles del comprobante y pagos asociados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteDTO {

    private Long id;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteUsuarioId;

    private LocalDateTime fechaEmision;

    private String serieNumero;

    @NotNull(message = "El total es obligatorio")
    @PositiveOrZero(message = "El total no puede ser negativo")
    private BigDecimal total;

    private BigDecimal saldoPendiente;

    private String estadoPagoNombre;

    private String observaciones;

    private String tipoComprobante; // 'BOLETA', 'FACTURA', 'TICKET'

    @Valid
    private List<DetalleComprobanteDTO> detalles = new ArrayList<>();

    // --- Campos de solo lectura (para mostrar información) ---

    private List<PagoDTO> pagos = new ArrayList<>();

    private String pacienteNombre;

    private String pacienteDocumento;

    private BigDecimal montoTotal;

    private BigDecimal montoPagado;

    private BigDecimal montoPendiente;
}
