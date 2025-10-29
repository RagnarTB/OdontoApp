package com.odontoapp.servicio.impl;

import com.odontoapp.dto.ComprobanteDTO;
import com.odontoapp.dto.DetalleComprobanteDTO;
import com.odontoapp.dto.PagoDTO;
import com.odontoapp.entidad.Comprobante;
import com.odontoapp.entidad.EstadoCita;
import com.odontoapp.entidad.EstadoPago;
import com.odontoapp.entidad.MetodoPago;
import com.odontoapp.entidad.Pago;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.ComprobanteRepository;
import com.odontoapp.repositorio.DetalleComprobanteRepository;
import com.odontoapp.repositorio.EstadoCitaRepository;
import com.odontoapp.repositorio.EstadoPagoRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MetodoPagoRepository;
import com.odontoapp.repositorio.PagoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.FacturacionService;
import com.odontoapp.servicio.InventarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de facturación y pagos.
 * Maneja la generación de comprobantes, registro de pagos y estados de cuenta.
 */
@Service
@Transactional
public class FacturacionServiceImpl implements FacturacionService {

    // --- Constantes para nombres de estados ---
    private static final String ESTADO_PAGO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_PAGO_PAGADO_PARCIAL = "PAGADO_PARCIAL";
    private static final String ESTADO_PAGO_PAGADO_TOTAL = "PAGADO_TOTAL";
    private static final String ESTADO_PAGO_ANULADO = "ANULADO";
    private static final String ESTADO_CITA_ASISTIO = "ASISTIO";

    // --- Repositorios ---
    private final ComprobanteRepository comprobanteRepository;
    private final DetalleComprobanteRepository detalleComprobanteRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final InsumoRepository insumoRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final InventarioService inventarioService;

    public FacturacionServiceImpl(ComprobanteRepository comprobanteRepository,
                                 DetalleComprobanteRepository detalleComprobanteRepository,
                                 PagoRepository pagoRepository,
                                 UsuarioRepository usuarioRepository,
                                 CitaRepository citaRepository,
                                 ProcedimientoRepository procedimientoRepository,
                                 InsumoRepository insumoRepository,
                                 EstadoPagoRepository estadoPagoRepository,
                                 MetodoPagoRepository metodoPagoRepository,
                                 EstadoCitaRepository estadoCitaRepository,
                                 InventarioService inventarioService) {
        this.comprobanteRepository = comprobanteRepository;
        this.detalleComprobanteRepository = detalleComprobanteRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.insumoRepository = insumoRepository;
        this.estadoPagoRepository = estadoPagoRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.estadoCitaRepository = estadoCitaRepository;
        this.inventarioService = inventarioService;
    }

    @Override
    @Transactional
    public Comprobante generarComprobanteDesdeCita(Long citaId, List<DetalleComprobanteDTO> detallesAdicionales) {
        // TODO: Implementar lógica completa
        // 1. Validar que la cita exista y esté en estado ASISTIO
        // 2. Validar que la cita no tenga ya un comprobante
        // 3. Obtener los tratamientos realizados de la cita
        // 4. Generar detalles del comprobante desde los tratamientos
        // 5. Agregar detalles adicionales si se proporcionan
        // 6. Calcular totales
        // 7. Crear y guardar el comprobante con estado PENDIENTE
        throw new UnsupportedOperationException("Método pendiente de implementación");
    }

    @Override
    @Transactional
    public Comprobante generarComprobanteVentaDirecta(ComprobanteDTO dto) {
        // TODO: Implementar lógica completa
        // 1. Validar que el paciente exista
        // 2. Validar que haya detalles en el comprobante
        // 3. Crear el comprobante
        // 4. Generar número de comprobante único
        // 5. Crear y guardar los detalles
        // 6. Registrar salidas de inventario para insumos vendidos
        // 7. Calcular totales y establecer estado PENDIENTE
        throw new UnsupportedOperationException("Método pendiente de implementación");
    }

    @Override
    @Transactional
    public Pago registrarPago(PagoDTO dto) {
        // TODO: Implementar lógica completa
        // 1. Validar que el comprobante exista
        // 2. Validar que el comprobante no esté ANULADO
        // 3. Validar que el comprobante no esté PAGADO_TOTAL
        // 4. Validar que el monto no exceda el saldo pendiente
        // 5. Validar que el método de pago exista
        // 6. Si es pago MIXTO, validar montoEfectivo y montoYape
        // 7. Crear y guardar el pago
        // 8. Actualizar montoPagado y montoPendiente del comprobante
        // 9. Actualizar estado del comprobante según saldo restante
        throw new UnsupportedOperationException("Método pendiente de implementación");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comprobante> buscarComprobantePorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del comprobante no puede ser nulo");
        }
        return comprobanteRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Comprobante> buscarComprobantePorNumero(String numeroComprobante) {
        if (numeroComprobante == null || numeroComprobante.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de comprobante no puede ser nulo o vacío");
        }
        return comprobanteRepository.findByNumeroComprobante(numeroComprobante);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comprobante> buscarComprobantesPorPaciente(Long pacienteUsuarioId, Pageable pageable) {
        if (pacienteUsuarioId == null) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        return comprobanteRepository.findByPacienteIdOrderByFechaEmisionDesc(pacienteUsuarioId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comprobante> buscarComprobantesPendientesPorPaciente(Long pacienteUsuarioId) {
        if (pacienteUsuarioId == null) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        return comprobanteRepository.findByPacienteIdAndMontoPendienteGreaterThan(
                pacienteUsuarioId, BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pago> buscarPagosPorComprobante(Long comprobanteId) {
        if (comprobanteId == null) {
            throw new IllegalArgumentException("El ID del comprobante no puede ser nulo");
        }
        return pagoRepository.findByComprobanteIdOrderByFechaPagoDesc(comprobanteId);
    }

    @Override
    @Transactional
    public Comprobante anularComprobante(Long comprobanteId, String motivoAnulacion) {
        // TODO: Implementar lógica completa
        // 1. Validar que el comprobante exista
        // 2. Validar que no esté ya anulado
        // 3. Validar que no tenga pagos registrados
        // 4. Cambiar estado a ANULADO
        // 5. Registrar motivo de anulación
        // 6. (Opcional) Revertir movimientos de inventario si es venta directa
        throw new UnsupportedOperationException("Método pendiente de implementación");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comprobante> buscarComprobantesPendientes(Pageable pageable) {
        return comprobanteRepository.findByMontoPendienteGreaterThan(BigDecimal.ZERO, pageable);
    }
}
