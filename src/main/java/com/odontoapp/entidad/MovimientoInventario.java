package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "movimientos_inventario")
@SQLDelete(sql = "UPDATE movimientos_inventario SET eliminado = true, fecha_eliminacion = NOW() WHERE id = ?")
@Where(clause = "eliminado = false")
public class MovimientoInventario extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_movimiento_id", nullable = false)
    private TipoMovimiento tipoMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_movimiento_id")
    private MotivoMovimiento motivoMovimiento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stockAnterior;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stockNuevo;

    private String referencia; // Ej: "Cita #123", "Compra #456"
    private String notas;

    // --- Campos Soft Delete ---
    private boolean eliminado = false;
    private LocalDateTime fechaEliminacion;
}
