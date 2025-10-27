package com.odontoapp.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO para representar una excepción de horario en el formulario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioExcepcionDTO {

    @NotNull(message = "La fecha de la excepción es obligatoria.")
    @FutureOrPresent(message = "La fecha de la excepción no puede ser en el pasado.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // Asegura el formato correcto desde el formulario
    private LocalDate fecha;

    /**
     * Horario específico para esta fecha.
     * Puede ser "NO_LABORABLE" o uno o más intervalos HH:mm-HH:mm separados por
     * comas.
     * Ejemplo: "09:00-13:00", "09:00-12:00,14:00-17:00", "NO_LABORABLE"
     */
    @NotEmpty(message = "Debe especificar las horas o indicar 'NO_LABORABLE'.")
    @Pattern(regexp = "^(NO_LABORABLE|([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9](,([0-1]?[0-9]|2[0-3]):[0-5][0-9]-([0-1]?[0-9]|2[0-3]):[0-5][0-9])*)$", message = "Formato de horas inválido. Use HH:mm-HH:mm (ej. 09:00-13:00) o NO_LABORABLE. Separe múltiples intervalos con comas.")
    private String horas;

    private String motivo; // Opcional
}
