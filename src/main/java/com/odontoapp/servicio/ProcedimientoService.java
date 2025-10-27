package com.odontoapp.servicio;

import com.odontoapp.dto.ProcedimientoDTO;
import com.odontoapp.entidad.Procedimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

public interface ProcedimientoService {
    Page<Procedimiento> listarTodos(String keyword, Pageable pageable);
    Optional<Procedimiento> buscarPorId(Long id);
    void guardar(ProcedimientoDTO procedimientoDTO);
    void eliminar(Long id);
    void cambiarEstado(Long id);
    List<Procedimiento> listarActivos();
}
