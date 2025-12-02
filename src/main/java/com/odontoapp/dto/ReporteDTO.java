package com.odontoapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteDTO {
    private String label;
    private Number value;

    public ReporteDTO(String label, Long value) {
        this.label = label;
        this.value = value;
    }

    public ReporteDTO(String label, java.math.BigDecimal value) {
        this.label = label;
        this.value = value;
    }

    // Constructor genérico para atrapar cualquier discrepancia de tipos (Object,
    // Object)
    public ReporteDTO(Object label, Object value) {
        this.label = label != null ? label.toString() : "Sin etiqueta";

        if (value instanceof Number) {
            this.value = (Number) value;
        } else if (value != null) {
            // Intentar parsear si viene como String u otro
            try {
                this.value = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                this.value = 0;
            }
        } else {
            this.value = 0;
        }
    }
}
