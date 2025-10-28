package com.odontoapp.entidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "cita", "procedimiento", "odontologo", "insumoAjustado" })
@ToString(callSuper = true, exclude = { "cita", "procedimiento", "odontologo", "insumoAjustado" })
@Entity
@Table(name = "tratamientos_realizados")
public class TratamientoRealizado extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedimiento_id", nullable = false)
    private Procedimiento procedimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "odontologo_usuario_id", nullable = false)
    private Usuario odontologo;

    @Column(length = 50)
    private String piezaDental;

    @Lob
    @Column(name = "descripcion_trabajo", length = 2000)
    private String descripcionTrabajo;

    @Column(name = "fecha_realizacion", nullable = false)
    private LocalDateTime fechaRealizacion;

    @Column(name = "cantidad_insumo_ajustada", precision = 10, scale = 2)
    private BigDecimal cantidadInsumoAjustada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_ajustado_id")
    private Insumo insumoAjustado;
}
