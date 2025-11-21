package com.odontoapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.odontoapp.validacion.FechaVencimientoValida;

@Data
public class InsumoDTO {
    private Long id;

    @NotEmpty(message = "El código es obligatorio")
    private String codigo;

    @NotEmpty(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;
    private String marca;
    private String ubicacion;
    private String lote;

    @FechaVencimientoValida
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

    // Este campo lo requiere el DashboardServiceImpl (Error 2)
    private BigDecimal stockActual;

    // Este campo lo requiere el DashboardServiceImpl (Error 3)
    private String unidadMedidaNombre;

}
