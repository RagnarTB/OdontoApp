package com.odontoapp.entidad;

import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Imágenes del paciente (radiografías, fotos clínicas, etc.)
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"paciente"})
@ToString(callSuper = true, exclude = {"paciente"})
@Entity
@Table(name = "paciente_imagenes")
@SQLDelete(sql = "UPDATE paciente_imagenes SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class PacienteImagen extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_usuario_id", nullable = false)
    private Usuario paciente;

    /**
     * RADIOGRAFIA_PANORAMICA
     * RADIOGRAFIA_PERIAPICAL
     * RADIOGRAFIA_BITEWING
     * FOTO_CLINICA
     * TOMOGRAFIA
     * OTRO
     */
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Lob
    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    /**
     * Tamaño del archivo en bytes
     */
    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    /**
     * MIME type (image/jpeg, image/png, application/pdf, etc.)
     */
    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;

    /**
     * Diente asociado si aplica (ej: radiografía periapical de diente 16)
     */
    @Column(name = "diente_asociado", length = 10)
    private String dienteAsociado;

    private boolean eliminado = false;
}
