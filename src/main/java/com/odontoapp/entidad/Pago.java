package com.odontoapp.entidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "comprobante", "metodoPago" })
@ToString(callSuper = true, exclude = { "comprobante", "metodoPago" })
@Entity
@Table(name = "pagos")
public class Pago extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private Comprobante comprobante;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "referencia_yape")
    private String referenciaYape;

    @Column(name = "monto_efectivo", precision = 12, scale = 2)
    private BigDecimal montoEfectivo;

    @Column(name = "monto_yape", precision = 12, scale = 2)
    private BigDecimal montoYape;

    @Column
    private String notas;
}
