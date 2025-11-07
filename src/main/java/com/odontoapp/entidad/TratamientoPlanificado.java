package com.odontoapp.entidad;

import java.time.LocalDate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Tratamientos planificados para citas futuras
 * Se crean cuando el odontólogo detecta algo en la evaluación
 * pero no lo trata en esa misma cita
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"paciente", "procedimiento", "odontologo", "citaAsociada"})
@ToString(callSuper = true, exclude = {"paciente", "procedimiento", "odontologo", "citaAsociada"})
@Entity
@Table(name = "tratamientos_planificados")
@SQLDelete(sql = "UPDATE tratamientos_planificados SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class TratamientoPlanificado extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_usuario_id", nullable = false)
    private Usuario paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedimiento_id", nullable = false)
    private Procedimiento procedimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_usuario_id", nullable = false)
    private Usuario odontologo;

    /**
     * Puede ser un solo diente "16" o múltiples "11,12,21,22"
     * NULL para procedimientos que no requieren diente específico (limpieza)
     */
    @Column(name = "piezas_dentales", length = 100)
    private String piezasDentales;

    @Lob
    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    @Column(name = "fecha_planificada")
    private LocalDate fechaPlanificada;

    /**
     * PLANIFICADO - Aún no se agenda cita
     * EN_CURSO - Ya se agendó cita para este tratamiento
     * COMPLETADO - Ya se realizó (existe TratamientoRealizado asociado)
     * CANCELADO - El paciente decidió no hacerlo
     */
    @Column(nullable = false, length = 20)
    private String estado = "PLANIFICADO";

    @Lob
    @Column(name = "notas")
    private String notas;

    /**
     * Si ya se agendó cita para este tratamiento planificado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_asociada_id")
    private Cita citaAsociada;

    /**
     * Si ya se completó, referencia al tratamiento realizado
     */
    @Column(name = "tratamiento_realizado_id")
    private Long tratamientoRealizadoId;

    private boolean eliminado = false;
}
