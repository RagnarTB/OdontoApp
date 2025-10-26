package com.odontoapp.repositorio;

import com.odontoapp.entidad.CategoriaProcedimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoriaProcedimientoRepository extends JpaRepository<CategoriaProcedimiento, Long> {
    Optional<CategoriaProcedimiento> findByNombre(String nombre);
}