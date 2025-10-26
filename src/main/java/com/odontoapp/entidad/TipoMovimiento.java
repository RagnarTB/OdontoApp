package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipos_movimiento")
public class TipoMovimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre; // Entrada, Salida, Ajuste

    @Column(unique = true)
    private String codigo; // ENTRADA, SALIDA, AJUSTE

    @Enumerated(EnumType.STRING)
    private AfectaStock afectaStock; // SUMA, RESTA, NINGUNO

    public enum AfectaStock {
        SUMA, RESTA, NINGUNO
    }
}
