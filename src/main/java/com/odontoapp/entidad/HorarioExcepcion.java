package com.odontoapp.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Clase embebida para representar excepciones al horario regular de un usuario
 * (Odontólogo).
 * Una instancia de esta clase representa un día específico con un horario
 * diferente
 * o un día no laborable.
 */
@Embeddable // Indica que esta clase se embeberá en otra entidad (Usuario)
@Data // Lombok para getters, setters, etc.
@NoArgsConstructor // Constructor sin argumentos requerido por JPA
@AllArgsConstructor // Constructor con todos los argumentos (opcional, útil para crear instancias)
public class HorarioExcepcion {

    @Column(name = "fecha_excepcion", nullable = false)
    private LocalDate fecha;

    /**
     * Define el horario específico para esta fecha o indica si no es laborable.
     * Ejemplos:
     * "09:00-13:00" (Solo trabaja en la mañana)
     * "09:00-13:00,15:00-18:00" (Horario partido diferente al regular)
     * "NO_LABORABLE" (No trabaja ese día)
     * null o vacío podría interpretarse como que se aplica el horario regular
     * (aunque es mejor ser explícito).
     */
    @Column(name = "horas_excepcion", length = 100)
    private String horas; // Puede ser null si la excepción es solo un motivo informativo sin cambio de
                          // hora

    @Column(name = "motivo_excepcion", length = 255)
    private String motivo; // Ej: "Vacaciones", "Conferencia", "Cita médica"

}
