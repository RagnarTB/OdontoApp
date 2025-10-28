package com.odontoapp.entidad;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "paciente", "cita" })
@ToString(callSuper = true, exclude = { "paciente", "cita" })
@Entity
@Table(name = "archivos_adjuntos")
@SQLDelete(sql = "UPDATE archivos_adjuntos SET eliminado = true, fecha_eliminacion = NOW() WHERE id = ?")
@Where(clause = "eliminado = false")
public class ArchivoAdjunto extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_usuario_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = true)
    private Cita cita;

    @Column(name = "nombre_archivo_original", nullable = false)
    private String nombreArchivoOriginal;

    @Column(name = "nombre_archivo_guardado", nullable = false, unique = true)
    private String nombreArchivoGuardado;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "descripcion")
    private String descripcion;

    // --- Campos Soft Delete ---
    private boolean eliminado = false;
    private LocalDateTime fechaEliminacion;
}
