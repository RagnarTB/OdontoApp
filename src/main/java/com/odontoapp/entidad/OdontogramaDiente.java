package com.odontoapp.entidad;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Estado actual de cada diente del paciente en el odontograma
 * Sistema de numeración FDI (11-18, 21-28, 31-38, 41-48)
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"paciente"})
@ToString(callSuper = true, exclude = {"paciente"})
@Entity
@Table(name = "odontograma_dientes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"paciente_usuario_id", "numero_diente"}))
public class OdontogramaDiente extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_usuario_id", nullable = false)
    private Usuario paciente;

    /**
     * Número del diente según sistema FDI
     * Adultos: "11" a "48" (32 dientes)
     * Niños: "51" a "85" (20 dientes de leche)
     */
    @Column(name = "numero_diente", nullable = false, length = 2)
    private String numeroDiente;

    /**
     * Estado actual del diente:
     * SANO - Verde
     * CARIES - Rojo
     * RESTAURADO - Azul (tiene curación/obturación)
     * ENDODONCIA - Morado (tratamiento de conducto)
     * CORONA - Amarillo
     * EXTRACCION - Gris (ya no existe)
     * IMPLANTE - Cyan
     * AUSENTE - Negro (nunca salió o congénito)
     * FRACTURADO - Naranja
     */
    @Column(nullable = false, length = 20)
    private String estado = "SANO";

    /**
     * Notas sobre el diente (material usado, fecha, etc.)
     */
    @Lob
    @Column(name = "notas")
    private String notas;

    /**
     * Superficies afectadas si aplica
     * Ej: "Oclusal,Vestibular" para caries en esas superficies
     */
    @Column(name = "superficies_afectadas", length = 200)
    private String superficiesAfectadas;

    @Column(name = "fecha_ultima_modificacion")
    private LocalDateTime fechaUltimaModificacion;
}
