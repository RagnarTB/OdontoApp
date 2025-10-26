package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "motivos_movimiento")
public class MotivoMovimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre; // Compra, Uso en Procedimiento, Vencimiento

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_movimiento_id")
    private TipoMovimiento tipoMovimiento;
}