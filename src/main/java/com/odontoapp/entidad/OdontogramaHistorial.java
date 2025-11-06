package com.odontoapp.entidad;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Historial de todos los cambios realizados en el odontograma
 * Registro de auditoría de cada modificación de estado
 */
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"paciente", "usuario"})
@ToString(exclude = {"paciente", "usuario"})
@Entity
@Table(name = "odontograma_historial")
public class OdontogramaHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_usuario_id", nullable = false)
    private Usuario paciente;

    @Column(name = "numero_diente", nullable = false, length = 2)
    private String numeroDiente;

    @Column(name = "estado_anterior", length = 20)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Lob
    @Column(name = "notas")
    private String notas;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    /**
     * Usuario que realizó el cambio (odontólogo o admin)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_modificador_id", nullable = false)
    private Usuario usuario;

    /**
     * Referencia al tratamiento que causó este cambio (si aplica)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tratamiento_realizado_id")
    private TratamientoRealizado tratamientoRealizado;
}
