package com.odontoapp.servicio;

import com.odontoapp.dto.MovimientoDTO;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.MovimientoInventario;
import com.odontoapp.entidad.MotivoMovimiento;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.entidad.TipoMovimiento;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MovimientoInventarioRepository;
import com.odontoapp.repositorio.MotivoMovimientoRepository;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.TipoMovimientoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class InventarioServiceImpl implements InventarioService {

    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;

    public InventarioServiceImpl(InsumoRepository insumoRepository,
            MovimientoInventarioRepository movimientoRepository,
            TipoMovimientoRepository tipoMovimientoRepository,
            MotivoMovimientoRepository motivoMovimientoRepository,
            ProcedimientoRepository procedimientoRepository,
            ProcedimientoInsumoRepository procedimientoInsumoRepository) {
        this.insumoRepository = insumoRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
    }

    @Override
    @Transactional
    public void registrarMovimiento(MovimientoDTO dto) {
        Insumo insumo = insumoRepository.findById(dto.getInsumoId())
                .orElseThrow(() -> new IllegalStateException("Insumo no encontrado con ID: " + dto.getInsumoId()));

        // Validar que el insumo no esté eliminado
        if (insumo.isEliminado()) {
            throw new IllegalStateException("No se puede registrar un movimiento para un insumo eliminado.");
        }

        TipoMovimiento tipo = tipoMovimientoRepository.findById(dto.getTipoMovimientoId())
                .orElseThrow(() -> new IllegalStateException("Tipo de movimiento no encontrado."));

        MotivoMovimiento motivo = motivoMovimientoRepository.findById(dto.getMotivoMovimientoId())
                .orElseThrow(() -> new IllegalStateException("Motivo de movimiento no encontrado."));

        BigDecimal stockAnterior = insumo.getStockActual();
        BigDecimal cantidad = dto.getCantidad();
        BigDecimal stockNuevo;

        if (tipo.getAfectaStock() == TipoMovimiento.AfectaStock.SUMA) {
            stockNuevo = stockAnterior.add(cantidad);
        } else if (tipo.getAfectaStock() == TipoMovimiento.AfectaStock.RESTA) {
            stockNuevo = stockAnterior.subtract(cantidad);
            if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException(
                        "No hay stock suficiente para registrar la salida. Stock actual: " + stockAnterior);
            }
        } else {
            stockNuevo = stockAnterior; // Para movimientos de tipo AJUSTE que no afectan stock
        }

        // 1. Crear y guardar el registro del movimiento
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setInsumo(insumo);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setMotivoMovimiento(motivo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setNotas(dto.getNotas());
        movimiento.setReferencia(dto.getReferencia());
        movimientoRepository.save(movimiento);

        // 2. Actualizar el stock del insumo
        insumo.setStockActual(stockNuevo);
        insumoRepository.save(insumo);
    }

    @Override
    public Page<MovimientoInventario> listarMovimientosPorInsumo(Long insumoId, Pageable pageable) {
        return movimientoRepository.findByInsumoIdOrderByFechaCreacionDesc(insumoId, pageable);
    }

    @Override
    @Transactional
    public void descontarStockPorProcedimientoRealizado(Long procedimientoId, BigDecimal cantidadAjustada,
                                                         Long insumoAjustadoId, String referenciaCita) {
        // Validar que el procedimiento existe
        Procedimiento procedimiento = procedimientoRepository.findById(procedimientoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Procedimiento no encontrado con ID: " + procedimientoId));

        // Buscar el tipo de movimiento "SALIDA" y motivo "USO_PROCEDIMIENTO"
        TipoMovimiento tipoSalida = tipoMovimientoRepository.findByNombre("SALIDA")
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de movimiento 'SALIDA' no encontrado en el sistema"));

        MotivoMovimiento motivoProcedimiento = motivoMovimientoRepository.findByNombre("USO_PROCEDIMIENTO")
                .orElseThrow(() -> new IllegalStateException(
                        "Motivo 'USO_PROCEDIMIENTO' no encontrado en el sistema"));

        // Caso 1: Se especificó un insumo ajustado (se usa solo ese insumo con cantidad ajustada)
        if (insumoAjustadoId != null && cantidadAjustada != null) {
            Insumo insumo = insumoRepository.findById(insumoAjustadoId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Insumo no encontrado con ID: " + insumoAjustadoId));

            descontarInsumo(insumo, cantidadAjustada, tipoSalida, motivoProcedimiento, referenciaCita);
        }
        // Caso 2: No se especificó insumo ajustado, usar insumos por defecto del procedimiento
        else {
            List<ProcedimientoInsumo> insumosDelProcedimiento =
                    procedimientoInsumoRepository.findByProcedimientoId(procedimientoId);

            if (insumosDelProcedimiento.isEmpty()) {
                // No hay insumos configurados para este procedimiento, no se descuenta nada
                return;
            }

            // Descontar cada insumo según la cantidad por defecto configurada
            for (ProcedimientoInsumo pi : insumosDelProcedimiento) {
                descontarInsumo(pi.getInsumo(), pi.getCantidadDefault(), tipoSalida, motivoProcedimiento,
                        referenciaCita);
            }
        }
    }

    /**
     * Método auxiliar para descontar stock de un insumo y registrar el movimiento.
     */
    private void descontarInsumo(Insumo insumo, BigDecimal cantidad, TipoMovimiento tipoMovimiento,
                                  MotivoMovimiento motivo, String referencia) {
        BigDecimal stockAnterior = insumo.getStockActual();
        BigDecimal stockNuevo = stockAnterior.subtract(cantidad);

        // Validar que hay stock suficiente
        if (stockNuevo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                    "Stock insuficiente para el insumo '" + insumo.getNombre() +
                    "'. Stock actual: " + stockAnterior + ", se requiere: " + cantidad);
        }

        // Crear el movimiento de inventario
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setInsumo(insumo);
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setMotivoMovimiento(motivo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setReferencia(referencia);
        movimiento.setNotas("Descuento automático por realización de procedimiento");
        movimientoRepository.save(movimiento);

        // Actualizar el stock del insumo
        insumo.setStockActual(stockNuevo);
        insumoRepository.save(insumo);
    }
}
