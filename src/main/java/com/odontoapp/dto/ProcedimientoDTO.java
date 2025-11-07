package com.odontoapp.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProcedimientoDTO {

    private Long id;

    @NotEmpty(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede tener más de 50 caracteres")
    private String codigo;

    @NotEmpty(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede tener más de 150 caracteres")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @PositiveOrZero(message = "El precio no puede ser negativo")
    private BigDecimal precioBase;

    @NotNull(message = "La duración es obligatoria")
    @Min(value = 1, message = "La duración debe ser de al menos 1 minuto")
    private int duracionBaseMinutos;

    @NotNull(message = "Debe seleccionar una categoría")
    private Long categoriaId;

    // Lista de insumos asociados al procedimiento
    private List<InsumoItemDTO> insumos = new ArrayList<>();

    /**
     * DTO interno para representar un insumo en el formulario
     */
    @Data
    public static class InsumoItemDTO {
        private Long insumoId;
        private BigDecimal cantidad;
        private String unidad;
        private boolean esObligatorio = true;
    }
}
