package com.odontoapp.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RolDTO {
    private Long id;
    @NotEmpty(message = "El nombre del rol es obligatorio")
    private String nombre;
    private List<Long> permisos;
}