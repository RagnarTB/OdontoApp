package com.odontoapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PacienteDTO {
    private Long id;

    @NotEmpty(message = "El DNI no puede estar vacío")
    @Size(min = 8, max = 8, message = "El DNI debe tener 8 dígitos")
    private String dni;

    @NotEmpty(message = "El nombre no puede estar vacío")
    private String nombreCompleto;

    @Email(message = "Debe ser un email válido")
    private String email;

    private String telefono;
    private LocalDate fechaNacimiento;
    private String direccion;
    private String alergias;
    private String antecedentesMedicos;
}