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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "paciente", "odontologo", "procedimiento", "estadoCita", "citaReprogramada" })
@ToString(callSuper = true, exclude = { "paciente", "odontologo", "procedimiento", "estadoCita", "citaReprogramada" })
@Entity
@Table(name = "citas")
@SQLDelete(sql = "UPDATE citas SET eliminado = true, fecha_eliminacion = NOW() WHERE id = ?")
@Where(clause = "eliminado = false")
public class Cita extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_usuario_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_usuario_id", nullable = false)
    private Usuario odontologo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedimiento_id", nullable = true)
    private Procedimiento procedimiento;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(name = "duracion_estimada_minutos", nullable = false)
    private Integer duracionEstimadaMinutos;

    @Lob
    @Column(name = "notas_internas")
    private String notasInternas;

    @Lob
    @Column(name = "motivo_consulta")
    private String motivoConsulta;

    @Lob
    @Column(name = "notas")
    private String notas;

    @Lob
    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_cita_id", nullable = false)
    private EstadoCita estadoCita;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_reprogramada_id", nullable = true)
    private Cita citaReprogramada;

    // --- Campos Soft Delete ---
    private boolean eliminado = false;
    private LocalDateTime fechaEliminacion;
}
