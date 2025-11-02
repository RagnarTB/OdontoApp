package com.odontoapp.repositorio;

import com.odontoapp.entidad.Procedimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcedimientoRepository extends JpaRepository<Procedimiento, Long> {

    @Query("SELECT p FROM Procedimiento p WHERE p.nombre LIKE %:keyword% OR p.codigo LIKE %:keyword% OR p.categoria.nombre LIKE %:keyword%")
    Page<Procedimiento> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<Procedimiento> findByCodigo(String codigo);
    long countByCategoriaId(Long categoriaId);

    // Método para cargar procedimientos con sus relaciones (EAGER fetch)
    @Query("SELECT p FROM Procedimiento p LEFT JOIN FETCH p.categoria")
    java.util.List<Procedimiento> findAllWithRelations();
}
