package com.odontoapp.dto;

import java.time.LocalDate; // Importar
import java.util.List;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull; // Importar
import jakarta.validation.constraints.Size; // Importar
import lombok.Data;

@Data
public class UsuarioDTO {
    private Long id;

    @NotNull(message = "El tipo de documento es obligatorio") // Nuevo
    private Long tipoDocumentoId; // Nuevo

    @NotEmpty(message = "El número de documento no puede estar vacío") // Nuevo
    @Size(min = 8, max = 20, message = "El documento debe tener entre 8 y 20 caracteres") // Nuevo
    private String numeroDocumento; // Nuevo

    @NotEmpty(message = "El nombre no puede estar vacío")
    private String nombreCompleto;

    @NotEmpty(message = "El email no puede estar vacío")
    @Email
    private String email;

    private String telefono; // Nuevo
    private LocalDate fechaNacimiento; // Nuevo
    private String direccion; // Nuevo
    private LocalDate fechaContratacion;
    private LocalDateTime ultimoAcceso;

    @NotEmpty(message = "Debe seleccionar al menos un rol")
    private List<Long> roles;
}