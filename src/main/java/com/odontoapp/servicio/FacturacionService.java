package com.odontoapp.servicio;

import com.odontoapp.dto.ComprobanteDTO;
import com.odontoapp.dto.DetalleComprobanteDTO;
import com.odontoapp.dto.PagoDTO;
import com.odontoapp.entidad.Comprobante;
import com.odontoapp.entidad.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de facturación y pagos.
 * Maneja comprobantes, pagos y estados de cuenta.
 */
public interface FacturacionService {

    /**
     * Genera un comprobante automáticamente desde una cita completada (estado ASISTIO).
     * Incluye los procedimientos y tratamientos realizados, más detalles adicionales opcionales.
     *
     * @param citaId ID de la cita
     * @param detallesAdicionales Lista de detalles adicionales (ej: insumos vendidos durante la cita)
     * @return El comprobante generado
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra la cita
     * @throws IllegalStateException si la cita no está en estado ASISTIO o ya tiene comprobante
     */
    Comprobante generarComprobanteDesdeCita(Long citaId, List<DetalleComprobanteDTO> detallesAdicionales);

    /**
     * Genera un comprobante de venta directa (sin cita previa).
     * Útil para venta de productos o servicios en punto de venta.
     *
     * @param dto Datos del comprobante a generar
     * @return El comprobante generado
     * @throws IllegalArgumentException si los datos son inválidos
     */
    Comprobante generarComprobanteVentaDirecta(ComprobanteDTO dto);

    /**
     * Registra un pago sobre un comprobante existente.
     * Actualiza el estado del comprobante según el monto pagado.
     * Valida monto, método, y actualiza saldo/estado del comprobante.
     *
     * @param dto Datos del pago a registrar
     * @return El pago registrado
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el comprobante
     * @throws IllegalArgumentException si el monto excede el saldo pendiente
     * @throws IllegalStateException si el comprobante está ANULADO o PAGADO_TOTAL
     */
    Pago registrarPago(PagoDTO dto);

    /**
     * Busca un comprobante por su ID.
     *
     * @param id ID del comprobante
     * @return Optional con el comprobante si existe
     */
    Optional<Comprobante> buscarComprobantePorId(Long id);

    /**
     * Busca un comprobante por su número.
     *
     * @param numeroComprobante Número del comprobante
     * @return Optional con el comprobante si existe
     */
    Optional<Comprobante> buscarComprobantePorNumero(String numeroComprobante);

    /**
     * Busca todos los comprobantes de un paciente.
     *
     * @param pacienteUsuarioId ID del paciente
     * @param pageable Configuración de paginación
     * @return Página de comprobantes del paciente
     */
    Page<Comprobante> buscarComprobantesPorPaciente(Long pacienteUsuarioId, Pageable pageable);

    /**
     * Busca todos los comprobantes con saldo pendiente de un paciente.
     * Útil para mostrar deudas en el portal del paciente.
     *
     * @param pacienteUsuarioId ID del paciente
     * @return Lista de comprobantes pendientes del paciente
     */
    List<Comprobante> buscarComprobantesPendientesPorPaciente(Long pacienteUsuarioId);

    /**
     * Busca todos los pagos realizados sobre un comprobante.
     *
     * @param comprobanteId ID del comprobante
     * @return Lista de pagos del comprobante
     */
    List<Pago> buscarPagosPorComprobante(Long comprobanteId);

    /**
     * Anula un comprobante existente.
     * Cambia el estado a ANULADO y no permite más pagos.
     * Podría requerir lógica adicional (ej: revertir stock si es venta directa).
     *
     * @param comprobanteId ID del comprobante a anular
     * @param motivoAnulacion Motivo de la anulación
     * @param regresarInventario Si se debe regresar el inventario de insumos al stock
     * @return El comprobante anulado
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el comprobante
     * @throws IllegalStateException si el comprobante ya tiene pagos registrados o ya está anulado
     */
    Comprobante anularComprobante(Long comprobanteId, String motivoAnulacion, boolean regresarInventario);

    /**
     * Busca todos los comprobantes con saldo pendiente.
     *
     * @param pageable Configuración de paginación
     * @return Página de comprobantes con saldo pendiente
     */
    Page<Comprobante> buscarComprobantesPendientes(Pageable pageable);
}
