// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\dto\PacienteDTO.java
package com.odontoapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import com.odontoapp.validacion.FechaNacimientoValida;

@Data
public class PacienteDTO {
    private Long id;

    @NotNull(message = "El tipo de documento es obligatorio")
    private Long tipoDocumentoId; // Nuevo

    @NotEmpty(message = "El número de documento no puede estar vacío")
    @Size(min = 8, max = 20, message = "El documento debe tener entre 8 y 20 caracteres")
    private String numeroDocumento; // Reemplaza dni

    @NotEmpty(message = "El nombre no puede estar vacío")
    private String nombreCompleto;

    @Email(message = "Debe ser un email válido")
    @NotEmpty(message = "El email no puede estar vacío")
    private String email;

    private String telefono;

    @FechaNacimientoValida
    private LocalDate fechaNacimiento;

    private String direccion;
    private String alergias;
    private String antecedentesMedicos;
}