package com.odontoapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaServicioDTO {
    private Long id;
    private String nombre;
    private String icono;
    private String color;
    private long cantidadServicios;
}
