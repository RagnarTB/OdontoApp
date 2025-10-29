package com.odontoapp.entidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "cita", "paciente", "estadoPago", "detalles" })
@ToString(callSuper = true, exclude = { "cita", "paciente", "estadoPago", "detalles" })
@Entity
@Table(name = "comprobantes")
@SQLDelete(sql = "UPDATE comprobantes SET eliminado = true, fecha_eliminacion = NOW() WHERE id = ?")
@Where(clause = "eliminado = false")
public class Comprobante extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id", nullable = true)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_usuario_id", nullable = false)
    private Usuario paciente;

    @Column(name = "numero_comprobante", nullable = false, unique = true)
    private String numeroComprobante;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "monto_pendiente", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPendiente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_pago_id", nullable = false)
    private EstadoPago estadoPago;

    @Column(name = "tipo_comprobante", length = 50)
    private String tipoComprobante; // 'CITA', 'BOLETA', 'FACTURA', 'TICKET'

    @Column(name = "descripcion")
    private String descripcion;

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleComprobante> detalles = new ArrayList<>();

    // --- Campos Soft Delete ---
    private boolean eliminado = false;
    private LocalDateTime fechaEliminacion;
}
