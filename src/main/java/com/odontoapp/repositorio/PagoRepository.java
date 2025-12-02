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
        // --- Consultas para Reportes ---

        @Query("SELECT new com.odontoapp.dto.ReporteDTO(p.metodoPago.nombre, SUM(p.monto)) " +
                        "FROM Pago p " +
                        "WHERE p.fechaPago BETWEEN :fechaInicio AND :fechaFin " +
                        "GROUP BY p.metodoPago.nombre")
        List<com.odontoapp.dto.ReporteDTO> obtenerIngresosPorMetodoPago(
                        @Param("fechaInicio") LocalDateTime fechaInicio,
                        @Param("fechaFin") LocalDateTime fechaFin);

        @Query("SELECT new com.odontoapp.dto.ReporteDTO(FUNCTION('DATE_FORMAT', p.fechaPago, '%Y-%m'), SUM(p.monto)) " +
                        "FROM Pago p " +
                        "WHERE p.fechaPago >= :fechaInicio AND p.fechaPago <= :fechaFin " +
                        "GROUP BY FUNCTION('DATE_FORMAT', p.fechaPago, '%Y-%m') " +
                        "ORDER BY FUNCTION('DATE_FORMAT', p.fechaPago, '%Y-%m') ASC")
        List<com.odontoapp.dto.ReporteDTO> obtenerIngresosPorMes(
                        @Param("fechaInicio") LocalDateTime fechaInicio,
                        @Param("fechaFin") LocalDateTime fechaFin);
}
