package com.odontoapp.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.odontoapp.entidad.TipoMovimiento;

public interface TipoMovimientoRepository extends JpaRepository<TipoMovimiento, Long> {
    boolean existsByCodigo(String codigo);
}
