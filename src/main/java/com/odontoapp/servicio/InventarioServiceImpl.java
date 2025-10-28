package com.odontoapp.servicio;

import com.odontoapp.dto.MovimientoDTO;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.MovimientoInventario;
import com.odontoapp.entidad.MotivoMovimiento;
import com.odontoapp.entidad.TipoMovimiento;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MovimientoInventarioRepository;
import com.odontoapp.repositorio.MotivoMovimientoRepository;
import com.odontoapp.repositorio.TipoMovimientoRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class InventarioServiceImpl implements InventarioService {

    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;

    public InventarioServiceImpl(InsumoRepository insumoRepository,
            MovimientoInventarioRepository movimientoRepository,
            TipoMovimientoRepository tipoMovimientoRepository,
            MotivoMovimientoRepository motivoMovimientoRepository) {
        this.insumoRepository = insumoRepository;
        this.movimientoRepository = movimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
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
}
