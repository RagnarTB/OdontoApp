package com.odontoapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InsumoDTO {
    private Long id;

    private String codigo;

    @NotEmpty(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;
    private String marca;
    private String ubicacion;
    private String lote;
    private LocalDate fechaVencimiento;

    @NotNull(message = "El stock mínimo es obligatorio")
    @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
    private BigDecimal stockMinimo;

    @NotNull(message = "El precio es obligatorio")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private BigDecimal precioUnitario;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    @NotNull(message = "La unidad de medida es obligatoria")
    private Long unidadMedidaId;
}
