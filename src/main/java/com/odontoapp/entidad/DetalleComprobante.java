package com.odontoapp.entidad;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Representa una línea de detalle en un comprobante.
 * Puede ser un procedimiento o un insumo vendido.
 */
@Data
@EqualsAndHashCode(callSuper = true, exclude = { "comprobante" })
@ToString(callSuper = true, exclude = { "comprobante" })
@Entity
@Table(name = "detalles_comprobante")
public class DetalleComprobante extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private Comprobante comprobante;

    @Column(name = "tipo_item", nullable = false, length = 50)
    private String tipoItem; // 'PROCEDIMIENTO' o 'INSUMO'

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "descripcion_item", nullable = false, length = 255)
    private String descripcionItem;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "notas", length = 500)
    private String notas;
}
