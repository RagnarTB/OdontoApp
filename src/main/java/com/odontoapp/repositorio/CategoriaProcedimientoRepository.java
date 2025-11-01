package com.odontoapp.repositorio;

import com.odontoapp.entidad.CategoriaProcedimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface CategoriaProcedimientoRepository extends JpaRepository<CategoriaProcedimiento, Long> {

    Optional<CategoriaProcedimiento> findByNombre(String nombre);

    /**
     * Busca categorías activas
     */
    List<CategoriaProcedimiento> findByEstaActivaTrue();
}