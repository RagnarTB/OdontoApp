package com.odontoapp.servicio.impl;

import com.odontoapp.dto.MovimientoDTO;
import com.odontoapp.dto.TratamientoRealizadoDTO;
import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.MotivoMovimiento;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.TipoMovimiento;
import com.odontoapp.entidad.TratamientoRealizado;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MotivoMovimientoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;
import com.odontoapp.repositorio.TipoMovimientoRepository;
import com.odontoapp.repositorio.TratamientoRealizadoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.InventarioService;
import com.odontoapp.servicio.TratamientoRealizadoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementación del servicio de gestión de tratamientos realizados.
 * Maneja el registro, consulta y eliminación de tratamientos durante citas.
 */
@Service
@Transactional
public class TratamientoRealizadoServiceImpl implements TratamientoRealizadoService {

    private final TratamientoRealizadoRepository tratamientoRealizadoRepository;
    private final CitaRepository citaRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InsumoRepository insumoRepository;
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
    private final InventarioService inventarioService;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;

    public TratamientoRealizadoServiceImpl(TratamientoRealizadoRepository tratamientoRealizadoRepository,
                                          CitaRepository citaRepository,
                                          ProcedimientoRepository procedimientoRepository,
                                          UsuarioRepository usuarioRepository,
                                          InsumoRepository insumoRepository,
                                          ProcedimientoInsumoRepository procedimientoInsumoRepository,
                                          InventarioService inventarioService,
                                          TipoMovimientoRepository tipoMovimientoRepository,
                                          MotivoMovimientoRepository motivoMovimientoRepository) {
        this.tratamientoRealizadoRepository = tratamientoRealizadoRepository;
        this.citaRepository = citaRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.insumoRepository = insumoRepository;
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
        this.inventarioService = inventarioService;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
    }

    @Override
    @Transactional
    public TratamientoRealizado registrarTratamiento(TratamientoRealizadoDTO dto) {
        // Validar que los IDs obligatorios no sean nulos
        if (dto.getCitaId() == null) {
            throw new IllegalArgumentException("El ID de la cita es obligatorio");
        }
        if (dto.getProcedimientoId() == null) {
            throw new IllegalArgumentException("El ID del procedimiento es obligatorio");
        }
        if (dto.getOdontologoUsuarioId() == null) {
            throw new IllegalArgumentException("El ID del odontólogo es obligatorio");
        }
        if (dto.getFechaRealizacion() == null) {
            throw new IllegalArgumentException("La fecha de realización es obligatoria");
        }

        // Buscar la cita
        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cita no encontrada con ID: " + dto.getCitaId()));

        // Buscar el procedimiento
        Procedimiento procedimiento = procedimientoRepository.findById(dto.getProcedimientoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Procedimiento no encontrado con ID: " + dto.getProcedimientoId()));

        // Buscar el odontólogo
        Usuario odontologo = usuarioRepository.findById(dto.getOdontologoUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Odontólogo no encontrado con ID: " + dto.getOdontologoUsuarioId()));

        // Validar que el usuario sea realmente un odontólogo
        boolean esOdontologo = odontologo.getRoles().stream()
                .map(Rol::getNombre)
                .anyMatch(nombre -> nombre.equals("ODONTOLOGO") || nombre.equals("ADMIN"));

        if (!esOdontologo) {
            throw new IllegalStateException(
                    "El usuario con ID " + dto.getOdontologoUsuarioId() + " no tiene el rol de odontólogo");
        }

        // Buscar el insumo ajustado si se especificó
        Insumo insumoAjustado = null;
        if (dto.getInsumoAjustadoId() != null) {
            insumoAjustado = insumoRepository.findById(dto.getInsumoAjustadoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Insumo no encontrado con ID: " + dto.getInsumoAjustadoId()));

            // Validar que si hay insumo ajustado, también haya cantidad
            if (dto.getCantidadInsumoAjustada() == null || dto.getCantidadInsumoAjustada().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Si se especifica un insumo ajustado, la cantidad debe ser mayor a cero");
            }

            // ✅ VALIDAR STOCK DISPONIBLE
            if (insumoAjustado.getStockActual().compareTo(dto.getCantidadInsumoAjustada()) < 0) {
                throw new IllegalStateException(
                    String.format("Stock insuficiente del insumo '%s'. " +
                        "Disponible: %.2f %s, Requerido: %.2f %s",
                        insumoAjustado.getNombre(),
                        insumoAjustado.getStockActual(),
                        insumoAjustado.getUnidadMedida().getNombre(),
                        dto.getCantidadInsumoAjustada(),
                        insumoAjustado.getUnidadMedida().getNombre()));
            }

            // ✅ DESCONTAR STOCK DEL INSUMO UTILIZADO
            BigDecimal nuevoStock = insumoAjustado.getStockActual().subtract(dto.getCantidadInsumoAjustada());
            insumoAjustado.setStockActual(nuevoStock);
            insumoRepository.save(insumoAjustado);

            // ✅ REGISTRAR MOVIMIENTO EN HISTORIAL DE INVENTARIO
            registrarMovimientoInventario(
                insumoAjustado,
                dto.getCantidadInsumoAjustada(),
                "Uso en tratamiento - Cita #" + cita.getId()
            );

            System.out.println("✓ Stock actualizado: " + insumoAjustado.getNombre() +
                " | Cantidad utilizada: " + dto.getCantidadInsumoAjustada() +
                " | Nuevo stock: " + nuevoStock);
        }

        // ✅ DESCONTAR AUTOMÁTICAMENTE LOS INSUMOS PREDETERMINADOS DEL PROCEDIMIENTO
        // (Excepto si es "Consulta General" - CON-001)
        if (!procedimiento.getCodigo().equals("CON-001")) {
            List<ProcedimientoInsumo> insumosPredeterminados =
                procedimientoInsumoRepository.findByProcedimientoId(procedimiento.getId());

            if (!insumosPredeterminados.isEmpty()) {
                System.out.println("✓ Descontando " + insumosPredeterminados.size() +
                    " insumos predeterminados del procedimiento: " + procedimiento.getNombre());

                for (ProcedimientoInsumo pi : insumosPredeterminados) {
                    Insumo insumo = pi.getInsumo();
                    BigDecimal cantidadRequerida = pi.getCantidadDefecto();

                    // Validar stock disponible
                    if (insumo.getStockActual().compareTo(cantidadRequerida) < 0) {
                        // Si es obligatorio, lanzar excepción
                        if (pi.isEsObligatorio()) {
                            throw new IllegalStateException(
                                String.format("Stock insuficiente del insumo obligatorio '%s'. " +
                                    "Disponible: %.2f %s, Requerido: %.2f %s",
                                    insumo.getNombre(),
                                    insumo.getStockActual(),
                                    insumo.getUnidadMedida().getAbreviatura(),
                                    cantidadRequerida,
                                    insumo.getUnidadMedida().getAbreviatura()));
                        } else {
                            // Si es opcional, solo advertir y continuar
                            System.out.println("⚠ Advertencia: Stock insuficiente del insumo opcional '" +
                                insumo.getNombre() + "'. Se omitirá el descuento.");
                            continue;
                        }
                    }

                    // Descontar stock
                    BigDecimal nuevoStock = insumo.getStockActual().subtract(cantidadRequerida);
                    insumo.setStockActual(nuevoStock);
                    insumoRepository.save(insumo);

                    // ✅ REGISTRAR MOVIMIENTO EN HISTORIAL DE INVENTARIO
                    registrarMovimientoInventario(
                        insumo,
                        cantidadRequerida,
                        "Uso en procedimiento: " + procedimiento.getNombre() + " - Cita #" + cita.getId()
                    );

                    System.out.println("  ✓ " + insumo.getNombre() +
                        " | Cantidad: " + cantidadRequerida + " " + insumo.getUnidadMedida().getAbreviatura() +
                        " | Nuevo stock: " + nuevoStock);
                }
            }
        } else {
            System.out.println("ℹ Consulta General detectada - No se descontarán insumos predeterminados");
        }

        // Crear la nueva instancia de TratamientoRealizado
        TratamientoRealizado tratamiento = new TratamientoRealizado();
        tratamiento.setCita(cita);
        tratamiento.setProcedimiento(procedimiento);
        tratamiento.setOdontologo(odontologo);
        tratamiento.setPiezaDental(dto.getPiezaDental());
        tratamiento.setDescripcionTrabajo(dto.getDescripcionTrabajo());
        tratamiento.setFechaRealizacion(dto.getFechaRealizacion());
        tratamiento.setCantidadInsumoAjustada(dto.getCantidadInsumoAjustada());
        tratamiento.setInsumoAjustado(insumoAjustado);

        // Guardar y devolver la entidad
        return tratamientoRealizadoRepository.save(tratamiento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TratamientoRealizado> buscarPorCita(Long citaId) {
        if (citaId == null) {
            throw new IllegalArgumentException("El ID de la cita no puede ser nulo");
        }
        return tratamientoRealizadoRepository.findByCitaIdOrderByFechaRealizacionDesc(citaId);
    }

    @Override
    @Transactional(readOnly = true)
    public TratamientoRealizado buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del tratamiento no puede ser nulo");
        }
        return tratamientoRealizadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tratamiento realizado no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void eliminarTratamiento(Long tratamientoId) {
        if (tratamientoId == null) {
            throw new IllegalArgumentException("El ID del tratamiento no puede ser nulo");
        }

        // Buscar el tratamiento para validar que existe
        TratamientoRealizado tratamiento = tratamientoRealizadoRepository.findById(tratamientoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tratamiento realizado no encontrado con ID: " + tratamientoId));

        // Validaciones adicionales (opcional)
        // Por ejemplo, no permitir eliminar tratamientos de citas facturadas
        // if (tratamiento.getCita().tieneComprobante()) {
        //     throw new IllegalStateException("No se puede eliminar un tratamiento de una cita facturada");
        // }

        // Eliminar el tratamiento
        // Nota: Como TratamientoRealizado no tiene soft delete, esto es eliminación física
        tratamientoRealizadoRepository.deleteById(tratamientoId);
    }

    /**
     * Método helper para registrar movimientos de inventario cuando se usan insumos en tratamientos.
     * Busca automáticamente el tipo "SALIDA" y el motivo "Uso en procedimiento".
     */
    private void registrarMovimientoInventario(Insumo insumo, BigDecimal cantidad, String referencia) {
        try {
            // Buscar tipo de movimiento "SALIDA"
            TipoMovimiento tipoSalida = tipoMovimientoRepository.findByNombre("SALIDA")
                .orElseThrow(() -> new EntityNotFoundException(
                    "No se encontró el tipo de movimiento 'SALIDA'. Asegúrate de que exista en la base de datos."));

            // Buscar motivo "Uso en procedimiento" (o crear variantes similares)
            MotivoMovimiento motivoUso = motivoMovimientoRepository.findByNombre("Uso en procedimiento")
                .orElseGet(() -> motivoMovimientoRepository.findByNombre("USO_PROCEDIMIENTO")
                    .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el motivo de movimiento 'Uso en procedimiento'. Asegúrate de que exista en la base de datos.")));

            // Crear DTO de movimiento
            MovimientoDTO movimientoDTO = new MovimientoDTO();
            movimientoDTO.setInsumoId(insumo.getId());
            movimientoDTO.setTipoMovimientoId(tipoSalida.getId());
            movimientoDTO.setMotivoMovimientoId(motivoUso.getId());
            movimientoDTO.setCantidad(cantidad);
            movimientoDTO.setReferencia(referencia);
            movimientoDTO.setNotas("Descuento automático por uso en tratamiento dental");

            // Registrar movimiento en el servicio de inventario
            inventarioService.registrarMovimiento(movimientoDTO);

            System.out.println("  ✅ Movimiento de inventario registrado: " + cantidad + " " +
                insumo.getUnidadMedida().getAbreviatura() + " de " + insumo.getNombre());

        } catch (EntityNotFoundException e) {
            // Log pero no fallar el tratamiento si no se puede registrar el movimiento
            System.err.println("⚠️ Advertencia: No se pudo registrar el movimiento de inventario: " + e.getMessage());
            System.err.println("   El descuento de stock se realizó correctamente, pero no se registró en el historial.");
        }
    }
}
