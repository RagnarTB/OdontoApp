package com.odontoapp.servicio;

import com.odontoapp.dto.MovimientoDTO;
import com.odontoapp.entidad.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface InventarioService {

    void registrarMovimiento(MovimientoDTO movimientoDTO);

    Page<MovimientoInventario> listarMovimientosPorInsumo(Long insumoId, Pageable pageable);

    /**
     * Descuenta del stock los insumos utilizados al realizar un procedimiento.
     * Si se especifica un insumo ajustado, solo descuenta ese insumo con la cantidad ajustada.
     * Si no, utiliza los insumos por defecto del procedimiento desde ProcedimientoInsumo.
     *
     * @param procedimientoId ID del procedimiento realizado
     * @param cantidadAjustada Cantidad ajustada del insumo (si aplica)
     * @param insumoAjustadoId ID del insumo ajustado (si aplica)
     * @param referenciaCita Referencia a la cita para trazabilidad
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el procedimiento o insumo
     * @throws IllegalStateException si no hay stock suficiente
     */
    void descontarStockPorProcedimientoRealizado(Long procedimientoId, BigDecimal cantidadAjustada,
                                                  Long insumoAjustadoId, String referenciaCita);
}

