package com.odontoapp.repositorio;

import com.odontoapp.entidad.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    /**
     * Busca todos los pagos asociados a un comprobante específico.
     * 
     * @param comprobanteId El ID del comprobante
     * @return Lista de pagos ordenados por fecha de pago descendente
     */
    List<Pago> findByComprobanteIdOrderByFechaPagoDesc(Long comprobanteId);

    /**
     * Suma el total de pagos realizados en un rango de fechas
     * 
     * @param inicio Fecha y hora de inicio
     * @param fin    Fecha y hora de fin
     * @return Suma total de pagos en el rango
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.fechaPago BETWEEN :inicio AND :fin")
    BigDecimal sumMontoByFechaPagoBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
