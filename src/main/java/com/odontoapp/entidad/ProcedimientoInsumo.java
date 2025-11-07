package com.odontoapp.entidad;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Relación entre Procedimientos e Insumos
 * Define qué insumos se usan por defecto en cada procedimiento
 * y en qué cantidad
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"procedimiento", "insumo"})
@ToString(callSuper = true, exclude = {"procedimiento", "insumo"})
@Entity
@Table(name = "procedimiento_insumos",
       uniqueConstraints = @UniqueConstraint(columnNames = {"procedimiento_id", "insumo_id"}))
public class ProcedimientoInsumo extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedimiento_id", nullable = false)
    private Procedimiento procedimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    /**
     * Cantidad por defecto que se usa en este procedimiento
     * Ej: 1 carpule de anestesia, 5g de resina
     */
    @Column(name = "cantidad_defecto", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidadDefecto;

    /**
     * Unidad de medida (la misma del insumo)
     * Ej: carpule, g, ml, unidades
     */
    @Column(nullable = false, length = 20)
    private String unidad;

    /**
     * Si es obligatorio o opcional
     * Ej: Anestesia es obligatoria, matriz metálica es opcional
     */
    @Column(name = "es_obligatorio")
    private boolean esObligatorio = true;

    /**
     * Notas sobre el uso de este insumo en el procedimiento
     */
    @Column(name = "notas", length = 500)
    private String notas;
}
