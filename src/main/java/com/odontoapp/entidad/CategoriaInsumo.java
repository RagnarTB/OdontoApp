package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import jakarta.persistence.Column;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "categorias_insumo")
@SQLDelete(sql = "UPDATE categorias_insumo SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class CategoriaInsumo extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String descripcion;

    private boolean eliminado = false;

    @Column(nullable = false)
    private boolean estaActiva = true;
}
