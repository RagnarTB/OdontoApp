package com.odontoapp.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "categorias_procedimiento")
@SQLDelete(sql = "UPDATE categorias_procedimiento SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")

public class CategoriaProcedimiento extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    private String icono;
    private int orden;
    private String color;

    @Column(nullable = false)
    private boolean estaActiva = true; // Valor por defecto true para nuevas categorías

    private boolean eliminado = false;
}