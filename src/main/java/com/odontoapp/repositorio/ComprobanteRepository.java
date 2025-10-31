package com.odontoapp.repositorio;

import com.odontoapp.entidad.Comprobante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    /**
     * Busca un comprobante por su número único.
     * @param numeroComprobante El número del comprobante
     * @return Optional con el comprobante si existe
     */
    Optional<Comprobante> findByNumeroComprobante(String numeroComprobante);

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
     * Busca el último comprobante que comienza con un prefijo de serie,
     * ordenado por el nombre de la serie de forma descendente (para manejar B001-999 vs B001-1000).
     * Usamos SerieNumero como campo de ordenación.
     * @param seriePrefix El prefijo de la serie (ej: "B001")
     * @return Optional con el último comprobante de esa serie si existe
     */
    Optional<Comprobante> findTopBySerieNumeroStartingWithOrderBySerieNumeroDesc(String seriePrefix);
}
