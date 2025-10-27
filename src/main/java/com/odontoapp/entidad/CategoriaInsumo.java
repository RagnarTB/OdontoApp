package com.odontoapp.entidad;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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

    // --- NUEVO CAMPO ---
    @Column(nullable = false)
    private boolean estaActiva = true; // Valor por defecto true para nuevas categorías

    @Column(nullable = false) // Asegúrate que la columna exista en la BD
    private boolean eliminado = false;

    // Lombok @Data genera automáticamente isEstaActiva() y setEstaActiva()
}
