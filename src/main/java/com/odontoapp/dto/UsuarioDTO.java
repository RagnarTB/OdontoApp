package com.odontoapp.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioDTO {
    private Long id;

    @NotNull(message = "El tipo de documento es obligatorio")
    private Long tipoDocumentoId;

    @NotEmpty(message = "El número de documento no puede estar vacío")
    @Size(min = 8, max = 20, message = "El documento debe tener entre 8 y 20 caracteres")
    private String numeroDocumento;

    @NotEmpty(message = "El nombre no puede estar vacío")
    private String nombreCompleto;

    @NotEmpty(message = "El email no puede estar vacío")
    @Email
    private String email;

    @Pattern(regexp = "^[0-9]{9}$", message = "El teléfono debe tener 9 dígitos")
    private String telefono;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento;

    private String direccion;

    @NotNull(message = "La fecha de contratación es obligatoria")
    private LocalDate fechaContratacion;

    private LocalDate fechaVigencia; // Fecha de vigencia del usuario
    private LocalDateTime ultimoAcceso; // Generalmente no se envía desde el form, se muestra

    @NotEmpty(message = "Debe seleccionar al menos un rol")
    private List<Long> roles;

    private String alergias;
    private String antecedentesMedicos;
    private String tratamientosActuales;

    // --- CAMPOS PARA HORARIO ---

    /**
     * Mapa para recibir/enviar el horario regular desde el formulario.
     * La clave será DayOfWeek y el valor el string con intervalos
     * "HH:mm-HH:mm,HH:mm-HH:mm".
     */
    private Map<DayOfWeek, @Pattern(regexp = "^$|([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9](,([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9])*$", message = "Formato de horas inválido. Use HH:mm-HH:mm (ej. 09:00-13:00). Separe múltiples intervalos con comas o deje vacío si no trabaja.") String> horarioRegular = new EnumMap<>(
            DayOfWeek.class);

    /**
     * Lista para recibir/enviar las excepciones de horario.
     * 
     * @Valid permite que las validaciones dentro de HorarioExcepcionDTO se
     *        ejecuten.
     */
    @Valid
    private List<HorarioExcepcionDTO> excepcionesHorario = new ArrayList<>();
}
