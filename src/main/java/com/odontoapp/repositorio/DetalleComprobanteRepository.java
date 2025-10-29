package com.odontoapp.repositorio;

import com.odontoapp.entidad.DetalleComprobante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio para gestionar los detalles de comprobantes.
 * Cada detalle representa una línea en el comprobante (procedimiento o insumo).
 */
public interface DetalleComprobanteRepository extends JpaRepository<DetalleComprobante, Long> {

    /**
     * Busca todos los detalles de un comprobante específico.
     * @param comprobanteId El ID del comprobante
     * @return Lista de detalles del comprobante
     */
    List<DetalleComprobante> findByComprobanteId(Long comprobanteId);

    /**
     * Cuenta cuántos detalles tiene un comprobante.
     * @param comprobanteId El ID del comprobante
     * @return Número de detalles
     */
    long countByComprobanteId(Long comprobanteId);
}
