package com.odontoapp.servicio.impl;

import com.odontoapp.dto.ComprobanteDTO;
import com.odontoapp.dto.DetalleComprobanteDTO;
import com.odontoapp.dto.PagoDTO;
import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.Comprobante;
import com.odontoapp.entidad.DetalleComprobante;
import com.odontoapp.entidad.EstadoCita;
import com.odontoapp.entidad.EstadoPago;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.MetodoPago;
import com.odontoapp.entidad.Pago;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.Usuario;
import jakarta.persistence.EntityNotFoundException;
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
import java.time.LocalDateTime;
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
        // 1. Buscar la cita
        if (citaId == null) {
            throw new IllegalArgumentException("El ID de la cita no puede ser nulo");
        }

        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cita no encontrada con ID: " + citaId));

        // 2. Validar estado de la cita
        if (!ESTADO_CITA_ASISTIO.equals(cita.getEstadoCita().getNombre())) {
            throw new IllegalStateException(
                    "La cita no está marcada como 'ASISTIO'. No se puede generar comprobante. " +
                    "Estado actual: " + cita.getEstadoCita().getNombre());
        }

        // 3. Verificar que no exista ya un comprobante para esta cita
        Optional<Comprobante> comprobanteExistente = comprobanteRepository.findByCitaId(citaId);
        if (comprobanteExistente.isPresent()) {
            throw new IllegalStateException(
                    "Ya existe un comprobante generado para esta cita con ID: " +
                    comprobanteExistente.get().getId());
        }

        // 4. Obtener el paciente de la cita
        Usuario paciente = cita.getPaciente();
        if (paciente == null) {
            throw new IllegalStateException("La cita no tiene un paciente asignado");
        }

        // 5. Buscar el estado de pago PENDIENTE
        EstadoPago estadoPendiente = estadoPagoRepository.findByNombre(ESTADO_PAGO_PENDIENTE)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado de pago PENDIENTE no encontrado en la base de datos"));

        // 6. Generar serie y número de comprobante
        String serieNumero = generarSiguienteSerieNumero();

        // 7. Crear la entidad Comprobante
        Comprobante comprobante = new Comprobante();
        comprobante.setCita(cita);
        comprobante.setPaciente(paciente);
        comprobante.setFechaEmision(LocalDateTime.now());
        comprobante.setNumeroComprobante(serieNumero);
        comprobante.setEstadoPago(estadoPendiente);
        comprobante.setTipoComprobante("CITA");
        comprobante.setMontoTotal(BigDecimal.ZERO);
        comprobante.setMontoPagado(BigDecimal.ZERO);
        comprobante.setMontoPendiente(BigDecimal.ZERO);

        // 8. Crear detalle para el procedimiento de la cita
        if (cita.getProcedimiento() != null) {
            Procedimiento procedimiento = cita.getProcedimiento();

            DetalleComprobante detalleProcedimiento = new DetalleComprobante();
            detalleProcedimiento.setComprobante(comprobante);
            detalleProcedimiento.setTipoItem("PROCEDIMIENTO");
            detalleProcedimiento.setItemId(procedimiento.getId());
            detalleProcedimiento.setDescripcionItem(procedimiento.getNombre());
            detalleProcedimiento.setCantidad(BigDecimal.ONE);
            detalleProcedimiento.setPrecioUnitario(procedimiento.getPrecioBase());
            detalleProcedimiento.setSubtotal(procedimiento.getPrecioBase());

            comprobante.getDetalles().add(detalleProcedimiento);
        }

        // 9. Añadir detalles adicionales (insumos vendidos durante la cita)
        if (detallesAdicionales != null && !detallesAdicionales.isEmpty()) {
            for (DetalleComprobanteDTO detalleDTO : detallesAdicionales) {
                // Validar que sea un insumo
                if (!"INSUMO".equals(detalleDTO.getTipoItem())) {
                    throw new IllegalArgumentException(
                            "Solo se permiten detalles adicionales de tipo INSUMO. " +
                            "Tipo recibido: " + detalleDTO.getTipoItem());
                }

                // Validar campos obligatorios
                if (detalleDTO.getItemId() == null) {
                    throw new IllegalArgumentException(
                            "El ID del insumo es obligatorio en los detalles adicionales");
                }

                // Buscar el insumo
                Insumo insumo = insumoRepository.findById(detalleDTO.getItemId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Insumo no encontrado con ID: " + detalleDTO.getItemId()));

                // Crear el detalle
                DetalleComprobante detalleInsumo = new DetalleComprobante();
                detalleInsumo.setComprobante(comprobante);
                detalleInsumo.setTipoItem("INSUMO");
                detalleInsumo.setItemId(insumo.getId());
                detalleInsumo.setDescripcionItem(
                        detalleDTO.getDescripcionItem() != null && !detalleDTO.getDescripcionItem().isEmpty()
                        ? detalleDTO.getDescripcionItem()
                        : insumo.getNombre());
                detalleInsumo.setCantidad(detalleDTO.getCantidad());
                detalleInsumo.setPrecioUnitario(
                        detalleDTO.getPrecioUnitario() != null
                        ? detalleDTO.getPrecioUnitario()
                        : insumo.getPrecioUnitario());
                detalleInsumo.setSubtotal(
                        detalleInsumo.getCantidad().multiply(detalleInsumo.getPrecioUnitario()));
                detalleInsumo.setNotas(detalleDTO.getNotas());

                comprobante.getDetalles().add(detalleInsumo);

                // TODO: Llamar a InventarioService.descontarStockPorVentaDirecta(...)
                // para descontar el stock de estos insumos adicionales vendidos
            }
        }

        // 10. Calcular totales
        BigDecimal total = comprobante.getDetalles().stream()
                .map(DetalleComprobante::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        comprobante.setMontoTotal(total);
        comprobante.setMontoPendiente(total);
        comprobante.setDescripcion("Comprobante generado desde cita ID: " + citaId);

        // 11. Guardar el comprobante (con CascadeType.ALL, los detalles se guardan automáticamente)
        Comprobante comprobanteGuardado = comprobanteRepository.save(comprobante);

        return comprobanteGuardado;
    }

    /**
     * Genera el siguiente número de comprobante con serie.
     * Por ahora retorna un valor temporal.
     *
     * @return El número de comprobante generado
     */
    private String generarSiguienteSerieNumero() {
        // TODO: Implementar lógica de secuencia real
        // Podría usar una tabla de secuencias o un contador en base de datos
        // Formato sugerido: "B001-000124" donde B001 es la serie y 000124 es el correlativo
        return "B001-TEMP-" + System.currentTimeMillis();
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
        // 1. Validación de DTO
        if (dto.getComprobanteId() == null) {
            throw new IllegalArgumentException("El ID del comprobante es obligatorio");
        }
        if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser positivo");
        }
        if (dto.getMetodoPagoId() == null) {
            throw new IllegalArgumentException("El ID del método de pago es obligatorio");
        }

        // 2. Obtener entidades
        Comprobante comprobante = comprobanteRepository.findById(dto.getComprobanteId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Comprobante no encontrado con ID: " + dto.getComprobanteId()));

        MetodoPago metodoPago = metodoPagoRepository.findById(dto.getMetodoPagoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Método de pago no encontrado con ID: " + dto.getMetodoPagoId()));

        EstadoPago estadoPagadoTotal = estadoPagoRepository.findByNombre(ESTADO_PAGO_PAGADO_TOTAL)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado de pago PAGADO_TOTAL no encontrado en la base de datos"));

        EstadoPago estadoPagadoParcial = estadoPagoRepository.findByNombre(ESTADO_PAGO_PAGADO_PARCIAL)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado de pago PAGADO_PARCIAL no encontrado en la base de datos"));

        EstadoPago estadoAnulado = estadoPagoRepository.findByNombre(ESTADO_PAGO_ANULADO)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado de pago ANULADO no encontrado en la base de datos"));

        // 3. Validar estado del comprobante
        String estadoActual = comprobante.getEstadoPago().getNombre();
        if (ESTADO_PAGO_ANULADO.equals(estadoActual) || ESTADO_PAGO_PAGADO_TOTAL.equals(estadoActual)) {
            throw new IllegalStateException(
                    "No se pueden registrar pagos para un comprobante ANULADO o que ya está PAGADO TOTALMENTE. " +
                    "Estado actual: " + estadoActual);
        }

        // 4. Validar monto del pago
        if (dto.getMonto().compareTo(comprobante.getMontoPendiente()) > 0) {
            throw new IllegalArgumentException(
                    "El monto del pago (S/ " + dto.getMonto() + ") no puede ser mayor que el saldo pendiente (S/ " +
                    comprobante.getMontoPendiente() + ")");
        }

        // 5. Crear entidad Pago
        Pago pago = new Pago();
        pago.setComprobante(comprobante);
        pago.setMetodoPago(metodoPago);
        pago.setFechaPago(dto.getFechaPago() != null ? dto.getFechaPago() : LocalDateTime.now());
        pago.setMonto(dto.getMonto());
        pago.setReferenciaYape(dto.getReferenciaYape());
        pago.setMontoEfectivo(dto.getMontoEfectivo());
        pago.setMontoYape(dto.getMontoYape());
        pago.setNotas(dto.getNotas());

        // 6. Guardar Pago
        Pago pagoGuardado = pagoRepository.save(pago);

        // 7. Actualizar Comprobante
        BigDecimal nuevoMontoPagado = comprobante.getMontoPagado().add(dto.getMonto());
        BigDecimal nuevoSaldoPendiente = comprobante.getMontoPendiente().subtract(dto.getMonto());

        comprobante.setMontoPagado(nuevoMontoPagado);
        comprobante.setMontoPendiente(nuevoSaldoPendiente);

        // 8. Actualizar estado de pago del comprobante
        if (nuevoSaldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            comprobante.setEstadoPago(estadoPagadoTotal);
        } else {
            comprobante.setEstadoPago(estadoPagadoParcial);
        }

        // 9. Guardar Comprobante actualizado
        comprobanteRepository.save(comprobante);

        // 10. Devolver Pago
        return pagoGuardado;
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
