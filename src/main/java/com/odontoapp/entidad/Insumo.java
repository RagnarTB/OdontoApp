package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"categoria", "unidadMedida"})
@ToString(callSuper = true, exclude = {"categoria", "unidadMedida"})
@Entity
@Table(name = "insumos")
@SQLDelete(sql = "UPDATE insumos SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class Insumo extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;
    private String marca;
    private String ubicacion;
    private String lote;
    private LocalDate fechaVencimiento;

    @Column(precision = 12, scale = 2)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaInsumo categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidad_medida_id", nullable = false)
    private UnidadMedida unidadMedida;

    private boolean eliminado = false;
}

