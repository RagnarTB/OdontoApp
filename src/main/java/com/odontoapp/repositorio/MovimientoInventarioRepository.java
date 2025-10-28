package com.odontoapp.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.odontoapp.entidad.MovimientoInventario;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    Page<MovimientoInventario> findByInsumoIdOrderByFechaCreacionDesc(Long insumoId, Pageable pageable);

    /**
     * Cuenta cuántos movimientos de inventario pertenecen a un insumo específico.
     * Útil para validar si un insumo puede ser eliminado.
     * @param insumoId El ID del Insumo.
     * @return El número de movimientos asociados a ese insumo.
     */
    long countByInsumoId(Long insumoId);
}
