package com.odontoapp.repositorio;

import com.odontoapp.entidad.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /**
     * Busca todos los pagos asociados a un comprobante específico.
     * @param comprobanteId El ID del comprobante
     * @return Lista de pagos ordenados por fecha de pago descendente
     */
    List<Pago> findByComprobanteIdOrderByFechaPagoDesc(Long comprobanteId);
}
