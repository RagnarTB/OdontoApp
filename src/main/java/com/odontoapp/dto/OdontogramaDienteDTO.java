package com.odontoapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO para transferir datos de dientes del odontograma
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OdontogramaDienteDTO {

    private Long id;

    /**
     * Número del diente según sistema FDI (11-48 para adultos)
     */
    private String numeroDiente;

    /**
     * Estado actual: SANO, CARIES, RESTAURADO, ENDODONCIA, CORONA, EXTRACCION, IMPLANTE, AUSENTE, FRACTURADO
     */
    private String estado;

    /**
     * Superficies afectadas separadas por coma: "Oclusal,Vestibular,Mesial"
     */
    private String superficiesAfectadas;

    /**
     * Notas u observaciones sobre el diente
     */
    private String notas;

    /**
     * Fecha de última modificación
     */
    private LocalDateTime fechaUltimaModificacion;

    // Campos adicionales para el frontend

    /**
     * Cuadrante (1-4)
     */
    private String cuadrante;

    /**
     * Posición dentro del cuadrante (1-8)
     */
    private String posicion;

    /**
     * Nombre descriptivo del diente (Incisivo Central, Molar, etc.)
     */
    private String nombreDiente;
}
