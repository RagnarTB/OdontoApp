// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\dto\RegistroPacienteDTO.java
package com.odontoapp.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegistroPacienteDTO extends PacienteDTO {

    // El email ya viene de PacienteDTO

    @NotEmpty(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotEmpty(message = "Debe confirmar la contraseña")
    private String confirmPassword;

    // Campo para errores globales (usado en el formulario)
    private String global;
}