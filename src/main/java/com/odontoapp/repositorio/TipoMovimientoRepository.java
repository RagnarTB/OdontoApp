package com.odontoapp.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.odontoapp.entidad.TipoMovimiento;
import java.util.Optional;

public interface TipoMovimientoRepository extends JpaRepository<TipoMovimiento, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByNombre(String nombre);

    Optional<TipoMovimiento> findByCodigo(String codigo);
    Optional<TipoMovimiento> findByNombre(String nombre);
}
