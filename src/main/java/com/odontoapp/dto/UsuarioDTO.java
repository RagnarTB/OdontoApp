package com.odontoapp.dto;

import java.time.DayOfWeek; // NUEVO import
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList; // NUEVO import
import java.util.EnumMap; // NUEVO import (mejor para DayOfWeek como clave)
import java.util.List;
import java.util.Map; // NUEVO import

import jakarta.validation.Valid; // NUEVO import para validar la lista anidada
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern; // NUEVO import
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioDTO {
    private Long id;

    @NotNull(message = "El tipo de documento es obligatorio")
    private Long tipoDocumentoId;

    @NotEmpty(message = "El nÃºmero de documento no puede estar vacÃ­o")
    @Size(min = 8, max = 20, message = "El documento debe tener entre 8 y 20 caracteres")
    private String numeroDocumento;

    @NotEmpty(message = "El nombre no puede estar vacÃ­o")
    private String nombreCompleto;

    @NotEmpty(message = "El email no puede estar vacÃ­o")
    @Email
    private String email;

    private String telefono;
    private LocalDate fechaNacimiento;
    private String direccion;
    private LocalDate fechaContratacion;
    private LocalDate fechaVigencia; // Fecha de vigencia del usuario
    private LocalDateTime ultimoAcceso; // Generalmente no se envía desde el form, se muestra

    @NotEmpty(message = "Debe seleccionar al menos un rol")
    private List<Long> roles;

    // --- NUEVOS CAMPOS PARA HORARIO ---

    /**
     * Mapa para recibir/enviar el horario regular desde el formulario.
     * La clave será el nombre del día (String) o DayOfWeek. Usaremos DayOfWeek.
     * El valor será el string con los intervalos "HH:mm-HH:mm,HH:mm-HH:mm".
     * Usamos EnumMap porque es más eficiente con enums como clave.
     */
    private Map<DayOfWeek, @Pattern(regexp = "^$|([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9](,([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9])*$", message = "Formato de horas inválido. Use HH:mm-HH:mm (ej. 09:00-13:00). Separe múltiples intervalos con comas o deje vacío si no trabaja.") String> horarioRegular = new EnumMap<>(
            DayOfWeek.class);

    /**
     * Lista para recibir/enviar las excepciones de horario.
     * 
     * @Valid permite que las validaciones dentro de HorarioExcepcionDTO se
     *        ejecuten.
     */
    @Valid // IMPORTANTE: para validar los DTOs dentro de la lista
    private List<HorarioExcepcionDTO> excepcionesHorario = new ArrayList<>();

    // --- FIN NUEVOS CAMPOS PARA HORARIO ---

    // Constructor para inicializar el mapa si es necesario (Lombok @Data ya lo
    // hace)
    // public UsuarioDTO() {
    // horarioRegular = new EnumMap<>(DayOfWeek.class);
    // excepcionesHorario = new ArrayList<>();
    // }
}
