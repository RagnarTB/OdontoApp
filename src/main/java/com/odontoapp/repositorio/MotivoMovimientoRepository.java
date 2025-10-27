package com.odontoapp.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.odontoapp.entidad.MotivoMovimiento;
import java.util.Optional;
import java.util.List;

public interface MotivoMovimientoRepository extends JpaRepository<MotivoMovimiento, Long> {
    Optional<MotivoMovimiento> findByNombre(String nombre);
    List<MotivoMovimiento> findByTipoMovimientoId(Long tipoMovimientoId);
}