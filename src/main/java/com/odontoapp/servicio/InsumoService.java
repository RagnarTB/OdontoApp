package com.odontoapp.servicio;

import com.odontoapp.dto.InsumoDTO;
import com.odontoapp.entidad.Insumo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface InsumoService {
    Page<Insumo> listarTodos(String keyword, Long categoriaId, Pageable pageable);
    Optional<Insumo> buscarPorId(Long id);
    Insumo guardar(InsumoDTO insumoDTO);
    void eliminar(Long id);
    List<Insumo> listarConStockBajo(); // <-- Nuevo método para las alertas
}