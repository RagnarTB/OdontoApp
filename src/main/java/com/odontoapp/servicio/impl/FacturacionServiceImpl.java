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
import com.odontoapp.entidad.MotivoMovimiento;
import com.odontoapp.entidad.Pago;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.TipoMovimiento;
import com.odontoapp.entidad.Usuario;
import jakarta.persistence.EntityNotFoundException;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.ComprobanteRepository;
import com.odontoapp.repositorio.DetalleComprobanteRepository;
import com.odontoapp.repositorio.EstadoCitaRepository;
import com.odontoapp.repositorio.EstadoPagoRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MetodoPagoRepository;
import com.odontoapp.repositorio.MotivoMovimientoRepository;
import com.odontoapp.repositorio.PagoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.TipoMovimientoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.FacturacionService;
import com.odontoapp.servicio.InventarioService;
import com.odontoapp.dto.MovimientoDTO;
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

    // --- Constantes para numeración de comprobantes ---
    private static final String SERIE_DEFAULT = "B001";

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
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
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
                                 TipoMovimientoRepository tipoMovimientoRepository,
                                 MotivoMovimientoRepository motivoMovimientoRepository,
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
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
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
     * Genera el siguiente número de comprobante con serie secuencial.
     * Formato: "B001-0000001" donde B001 es la serie y 0000001 es el correlativo de 7 dígitos.
     * Usa sincronización para evitar problemas de concurrencia.
     *
     * @return El número de comprobante generado
     */
    private synchronized String generarSiguienteSerieNumero() {
        try {
            // 1. Buscar el último comprobante de la serie
            Optional<Comprobante> ultimoComprobante =
                comprobanteRepository.findTopBySerieNumeroStartingWithOrderBySerieNumeroDesc(SERIE_DEFAULT);

            // 2. Si no existe ningún comprobante, es el primero
            if (ultimoComprobante.isEmpty()) {
                return SERIE_DEFAULT + "-0000001";
            }

            // 3. Obtener el número del último comprobante (ej: "B001-0000124")
            String ultimoNumero = ultimoComprobante.get().getSerieNumero();

            // 4. Extraer la parte numérica (después del último "-")
            String[] partes = ultimoNumero.split("-");
            if (partes.length < 2) {
                // Si el formato no es válido, empezar desde 1
                return SERIE_DEFAULT + "-0000001";
            }

            String parteNumerica = partes[partes.length - 1];

            // 5. Parsear a long e incrementar
            long ultimoCorrelativo = Long.parseLong(parteNumerica);
            long nuevoCorrelativo = ultimoCorrelativo + 1;

            // 6. Formatear el nuevo número con 7 dígitos
            String numeroFormateado = String.format("%07d", nuevoCorrelativo);

            // 7. Retornar el comprobante completo
            return SERIE_DEFAULT + "-" + numeroFormateado;

        } catch (NumberFormatException e) {
            // En caso de error en el formato, empezar desde 1
            return SERIE_DEFAULT + "-0000001";
        }
    }

    @Override
    @Transactional
    public Comprobante generarComprobanteVentaDirecta(ComprobanteDTO dto) {
        // 1. Validación de DTO
        if (dto == null) {
            throw new IllegalArgumentException("El DTO del comprobante no puede ser nulo");
        }
        if (dto.getPacienteUsuarioId() == null) {
            throw new IllegalArgumentException("El ID del paciente es obligatorio");
        }
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("El comprobante debe tener al menos un detalle");
        }

        // 2. Obtener entidades base
        Usuario paciente = usuarioRepository.findById(dto.getPacienteUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Paciente no encontrado con ID: " + dto.getPacienteUsuarioId()));

        EstadoPago estadoPendiente = estadoPagoRepository.findByNombre(ESTADO_PAGO_PENDIENTE)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado de pago PENDIENTE no encontrado en la base de datos"));

        // 3. Generar serie y número
        String serieNumero = generarSiguienteSerieNumero();

        // 4. Crear entidad Comprobante
        Comprobante comprobante = new Comprobante();
        comprobante.setPaciente(paciente);
        comprobante.setFechaEmision(LocalDateTime.now());
        comprobante.setNumeroComprobante(serieNumero);
        comprobante.setEstadoPago(estadoPendiente);
        comprobante.setTipoComprobante("VENTA_DIRECTA");
        comprobante.setMontoTotal(BigDecimal.ZERO);
        comprobante.setMontoPagado(BigDecimal.ZERO);
        comprobante.setMontoPendiente(BigDecimal.ZERO);
        comprobante.setCita(null); // Venta directa no tiene cita asociada
        comprobante.setDescripcion(dto.getObservaciones());

        // 5. Procesar detalles y descontar stock
        BigDecimal total = BigDecimal.ZERO;

        for (DetalleComprobanteDTO detalleDTO : dto.getDetalles()) {
            // Validar campos obligatorios del detalle
            if (detalleDTO.getTipoItem() == null || detalleDTO.getTipoItem().isEmpty()) {
                throw new IllegalArgumentException("El tipo de ítem es obligatorio en cada detalle");
            }
            if (detalleDTO.getItemId() == null) {
                throw new IllegalArgumentException("El ID del ítem es obligatorio en cada detalle");
            }
            if (detalleDTO.getCantidad() == null || detalleDTO.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser positiva");
            }
            if (detalleDTO.getPrecioUnitario() == null || detalleDTO.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El precio unitario no puede ser negativo");
            }

            // Crear detalle
            DetalleComprobante detalle = new DetalleComprobante();
            detalle.setComprobante(comprobante);
            detalle.setTipoItem(detalleDTO.getTipoItem());
            detalle.setItemId(detalleDTO.getItemId());
            detalle.setCantidad(detalleDTO.getCantidad());
            detalle.setPrecioUnitario(detalleDTO.getPrecioUnitario());
            detalle.setSubtotal(detalleDTO.getCantidad().multiply(detalleDTO.getPrecioUnitario()));
            detalle.setNotas(detalleDTO.getNotas());

            // Procesar según tipo de ítem
            if ("INSUMO".equals(detalleDTO.getTipoItem())) {
                // Buscar el insumo
                Insumo insumo = insumoRepository.findById(detalleDTO.getItemId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Insumo no encontrado con ID: " + detalleDTO.getItemId()));

                detalle.setDescripcionItem(
                        detalleDTO.getDescripcionItem() != null && !detalleDTO.getDescripcionItem().isEmpty()
                        ? detalleDTO.getDescripcionItem()
                        : insumo.getNombre());

                // Descontar stock
                TipoMovimiento tipoSalida = tipoMovimientoRepository.findByCodigo("SALIDA")
                        .orElseThrow(() -> new IllegalStateException(
                                "Tipo de movimiento SALIDA no encontrado"));

                MotivoMovimiento motivoVentaDirecta = motivoMovimientoRepository.findByNombre("Venta Directa")
                        .orElseThrow(() -> new IllegalStateException(
                                "Motivo de movimiento 'Venta Directa' no encontrado"));

                // Crear MovimientoDTO
                MovimientoDTO movimientoDTO = new MovimientoDTO();
                movimientoDTO.setInsumoId(insumo.getId());
                movimientoDTO.setTipoMovimientoId(tipoSalida.getId());
                movimientoDTO.setMotivoMovimientoId(motivoVentaDirecta.getId());
                movimientoDTO.setCantidad(detalleDTO.getCantidad());
                movimientoDTO.setReferencia("Venta POS: " + serieNumero);

                // Registrar movimiento (esto lanzará excepción si no hay stock suficiente)
                inventarioService.registrarMovimiento(movimientoDTO);

            } else if ("PROCEDIMIENTO".equals(detalleDTO.getTipoItem())) {
                // Buscar el procedimiento
                Procedimiento procedimiento = procedimientoRepository.findById(detalleDTO.getItemId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Procedimiento no encontrado con ID: " + detalleDTO.getItemId()));

                detalle.setDescripcionItem(
                        detalleDTO.getDescripcionItem() != null && !detalleDTO.getDescripcionItem().isEmpty()
                        ? detalleDTO.getDescripcionItem()
                        : procedimiento.getNombre());
            } else {
                throw new IllegalArgumentException(
                        "Tipo de ítem no soportado: " + detalleDTO.getTipoItem() +
                        ". Solo se permiten 'INSUMO' o 'PROCEDIMIENTO'");
            }

            // Añadir detalle al comprobante
            comprobante.getDetalles().add(detalle);

            // Acumular total
            total = total.add(detalle.getSubtotal());
        }

        // 6. Actualizar totales del comprobante
        comprobante.setMontoTotal(total);
        comprobante.setMontoPendiente(total);

        // 7. Guardar y devolver
        Comprobante comprobanteGuardado = comprobanteRepository.save(comprobante);
        return comprobanteGuardado;
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
        // 1. Validación de parámetros
        if (comprobanteId == null) {
            throw new IllegalArgumentException("El ID del comprobante es obligatorio");
        }

        // 2. Obtener entidades
        Comprobante comprobante = comprobanteRepository.findById(comprobanteId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Comprobante no encontrado con ID: " + comprobanteId));

        EstadoPago estadoAnulado = estadoPagoRepository.findByNombre(ESTADO_PAGO_ANULADO)
                .orElseThrow(() -> new IllegalStateException(
                        "Estado de pago ANULADO no encontrado en la base de datos"));

        // 3. Validar estado actual
        if (ESTADO_PAGO_ANULADO.equals(comprobante.getEstadoPago().getNombre())) {
            throw new IllegalStateException("El comprobante ya se encuentra anulado");
        }

        // 4. Validar que no tenga pagos
        List<Pago> pagos = pagoRepository.findByComprobanteIdOrderByFechaPagoDesc(comprobanteId);
        if (pagos != null && !pagos.isEmpty()) {
            throw new IllegalStateException(
                    "No se puede anular un comprobante que ya tiene pagos registrados. " +
                    "Debe revertir los pagos primero. Pagos encontrados: " + pagos.size());
        }

        // 5. Revertir stock si es VENTA_DIRECTA
        if ("VENTA_DIRECTA".equals(comprobante.getTipoComprobante())) {
            // Buscar tipo y motivo para la reversión
            TipoMovimiento tipoEntrada = tipoMovimientoRepository.findByCodigo("ENTRADA")
                    .orElseThrow(() -> new IllegalStateException(
                            "Tipo de movimiento ENTRADA no encontrado"));

            MotivoMovimiento motivoAnulacionVenta = motivoMovimientoRepository.findByNombre("Anulación de Venta")
                    .orElseThrow(() -> new IllegalStateException(
                            "Motivo de movimiento 'Anulación de Venta' no encontrado"));

            // Iterar sobre los detalles para revertir insumos
            for (DetalleComprobante detalle : comprobante.getDetalles()) {
                if ("INSUMO".equals(detalle.getTipoItem())) {
                    // Crear MovimientoDTO para devolver el stock
                    MovimientoDTO movimientoDTO = new MovimientoDTO();
                    movimientoDTO.setInsumoId(detalle.getItemId());
                    movimientoDTO.setTipoMovimientoId(tipoEntrada.getId());
                    movimientoDTO.setMotivoMovimientoId(motivoAnulacionVenta.getId());
                    movimientoDTO.setCantidad(detalle.getCantidad());
                    movimientoDTO.setReferencia("Anulación de " + comprobante.getNumeroComprobante());

                    // Registrar movimiento de entrada (devolver stock)
                    inventarioService.registrarMovimiento(movimientoDTO);
                }
            }
        }

        // 6. Actualizar comprobante
        comprobante.setEstadoPago(estadoAnulado);
        comprobante.setMontoPendiente(BigDecimal.ZERO);

        // Actualizar observaciones con el motivo de anulación
        String observacionesActuales = comprobante.getDescripcion() != null
                ? comprobante.getDescripcion()
                : "";
        String nuevaObservacion = observacionesActuales.isEmpty()
                ? "ANULADO: " + motivoAnulacion
                : observacionesActuales + " | ANULADO: " + motivoAnulacion;
        comprobante.setDescripcion(nuevaObservacion);

        // 7. Guardar y devolver
        Comprobante comprobanteGuardado = comprobanteRepository.save(comprobante);
        return comprobanteGuardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comprobante> buscarComprobantesPendientes(Pageable pageable) {
        return comprobanteRepository.findByMontoPendienteGreaterThan(BigDecimal.ZERO, pageable);
    }
}
