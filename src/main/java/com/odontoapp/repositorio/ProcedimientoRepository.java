package com.odontoapp.repositorio;

import com.odontoapp.entidad.Procedimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ProcedimientoRepository extends JpaRepository<Procedimiento, Long> {

    @Query("SELECT p FROM Procedimiento p WHERE p.nombre LIKE %:keyword% OR p.codigo LIKE %:keyword% OR p.categoria.nombre LIKE %:keyword%")
    Page<Procedimiento> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<Procedimiento> findByCodigo(String codigo);
    long countByCategoriaId(Long categoriaId);
    List<Procedimiento> findByEstaActivoTrue();
}
