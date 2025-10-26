package com.odontoapp.servicio;

import com.odontoapp.dto.MovimientoDTO;
import com.odontoapp.entidad.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InventarioService {
    
    void registrarMovimiento(MovimientoDTO movimientoDTO);

    Page<MovimientoInventario> listarMovimientosPorInsumo(Long insumoId, Pageable pageable);
}

