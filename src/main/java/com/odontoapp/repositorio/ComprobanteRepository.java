package com.odontoapp.repositorio;

import com.odontoapp.entidad.Comprobante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.odontoapp.entidad.EstadoPago;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    /**
     * Busca un comprobante por su número único.
     * @param numeroComprobante El número del comprobante
     * @return Optional con el comprobante si existe
     */
    Optional<Comprobante> findByNumeroComprobante(String numeroComprobante);

    /**
     * Busca un comprobante por ID con bloqueo pesimista para evitar deadlocks.
     * Utilizado al registrar pagos para asegurar consistencia transaccional.
     * @param id El ID del comprobante
     * @return Optional con el comprobante bloqueado si existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comprobante c WHERE c.id = :id")
    Optional<Comprobante> findByIdWithLock(@Param("id") Long id);

    /**
     * Busca todos los comprobantes de un paciente específico.
     * @param pacienteId El ID del usuario paciente
     * @param pageable Paginación
     * @return Página de comprobantes del paciente
     */
    Page<Comprobante> findByPacienteId(Long pacienteId, Pageable pageable);

    /**
     * Busca todos los comprobantes de un paciente ordenados por fecha de emisión descendente.
     * @param pacienteId El ID del usuario paciente
     * @param pageable Paginación
     * @return Página de comprobantes del paciente ordenados
     */
    Page<Comprobante> findByPacienteIdOrderByFechaEmisionDesc(Long pacienteId, Pageable pageable);

    /**
     * Busca comprobantes con saldo pendiente de un paciente específico.
     * @param pacienteId El ID del usuario paciente
     * @param monto Monto mínimo de saldo pendiente (usar BigDecimal.ZERO)
     * @return Lista de comprobantes con saldo pendiente
     */
    List<Comprobante> findByPacienteIdAndMontoPendienteGreaterThan(Long pacienteId, BigDecimal monto);

    /**
     * Busca el comprobante asociado a una cita específica.
     * @param citaId El ID de la cita
     * @return Optional con el comprobante si existe
     */
    Optional<Comprobante> findByCitaId(Long citaId);

    /**
     * Busca comprobantes por estado de pago.
     * @param estadoPagoId El ID del estado de pago
     * @param pageable Paginación
     * @return Página de comprobantes con ese estado
     */
    Page<Comprobante> findByEstadoPagoId(Long estadoPagoId, Pageable pageable);

    /**
     * Busca comprobantes pendientes (con saldo pendiente).
     * @param pageable Paginación
     * @return Página de comprobantes con saldo pendiente
     */
    Page<Comprobante> findByMontoPendienteGreaterThan(BigDecimal monto, Pageable pageable);

    /**
     * Busca el último comprobante que comienza con un prefijo de número,
     * ordenado por el número de comprobante de forma descendente (para manejar B001-999 vs B001-1000).
     * @param numeroPrefix El prefijo del número de comprobante (ej: "B001")
     * @return Optional con el último comprobante de esa serie si existe
     */
    Optional<Comprobante> findTopByNumeroComprobanteStartingWithOrderByNumeroComprobanteDesc(String numeroPrefix);

    /**
     * Busca comprobantes por estado de pago y que estén activos (no eliminados)
     * @param estadoPago El estado de pago
     * @return Lista de comprobantes con ese estado
     */
    List<Comprobante> findByEstadoPagoAndEliminadoFalse(EstadoPago estadoPago);

    /**
     * Busca comprobantes pagados en un rango de fechas
     * @param estadoPago El estado de pago
     * @param inicio Fecha de inicio
     * @param fin Fecha de fin
     * @return Lista de comprobantes pagados en el rango
     */
    List<Comprobante> findByEstadoPagoAndFechaEmisionBetweenAndEliminadoFalse(
        EstadoPago estadoPago,
        LocalDateTime inicio,
        LocalDateTime fin
    );

    /**
     * Busca comprobantes de un paciente específico con un estado de pago determinado
     * @param pacienteId El ID del usuario paciente
     * @param estadoPago El estado de pago
     * @return Lista de comprobantes del paciente con ese estado
     */
    List<Comprobante> findByPacienteIdAndEstadoPago(Long pacienteId, EstadoPago estadoPago);
}
