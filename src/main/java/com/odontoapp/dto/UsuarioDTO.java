package com.odontoapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class UsuarioDTO {
    private Long id;

    @NotEmpty(message = "El nombre no puede estar vacío")
    private String nombreCompleto;

    @NotEmpty(message = "El email no puede estar vacío")
    @Email
    private String email;

    // La contraseña ya no es @NotEmpty aquí. La validaremos en el servicio.
    private String password;

    // ¡NUEVO! Validación para roles.
    @NotEmpty(message = "Debe seleccionar al menos un rol")
    private List<Long> roles;
}