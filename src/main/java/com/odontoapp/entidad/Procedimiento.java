package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true, exclude = "categoria")
@ToString(callSuper = true, exclude = "categoria")
@Entity
@Table(name = "procedimientos")
@SQLDelete(sql = "UPDATE procedimientos SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class Procedimiento extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(name = "precio_base", precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "duracion_base_minutos")
    private int duracionBaseMinutos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProcedimiento categoria;

    private boolean eliminado = false;
    
    @Column(nullable = false)
    private boolean estaActivo = true;
}