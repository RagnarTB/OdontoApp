package com.odontoapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para transferir datos de archivos adjuntos (radiografías, documentos, etc.).
 * Incluye metadatos del archivo y referencias al paciente y cita.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchivoAdjuntoDTO {

    private Long id;

    @NotNull(message = "El ID del paciente es obligatorio")
    private Long pacienteUsuarioId;

    private Long citaId;

    private String nombreArchivoOriginal;

    private String rutaArchivo;

    private String tipoMime;

    private Long tamanoBytes;

    private String descripcion;

    private LocalDateTime fechaSubida;

    // --- Campos de solo lectura (para mostrar información) ---

    private String pacienteNombre;

    private String tamanoLegible; // Ej: "2.5 MB"
}
