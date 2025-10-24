// Archivo: src/main/java/com/odontoapp/dto/UsuarioDTO.java
package com.odontoapp.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UsuarioDTO {
    private Long id;

    @NotEmpty(message = "El nombre no puede estar vacío")
    private String nombreCompleto;

    @NotEmpty(message = "El email no puede estar vacío")
    @Email
    private String email;

    // ELIMINADO: private String password;

    @NotEmpty(message = "Debe seleccionar al menos un rol")
    private List<Long> roles;
}