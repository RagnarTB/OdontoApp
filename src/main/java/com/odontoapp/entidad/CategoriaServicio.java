package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidad para categorías de servicios/procedimientos
 * Ejemplos: "Ortodoncia", "Endodoncia", "Periodoncia", etc.
 */
@Data
@Entity
@Table(name = "categorias_servicio")
public class CategoriaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;
}
