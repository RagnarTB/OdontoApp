package com.odontoapp.repositorio;

import com.odontoapp.entidad.Procedimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcedimientoRepository extends JpaRepository<Procedimiento, Long> {

    @Query("SELECT DISTINCT p FROM Procedimiento p LEFT JOIN FETCH p.categoria WHERE p.nombre LIKE %:keyword% OR p.codigo LIKE %:keyword% OR p.categoria.nombre LIKE %:keyword%")
    Page<Procedimiento> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<Procedimiento> findByCodigo(String codigo);
    long countByCategoriaId(Long categoriaId);

    // Método para cargar procedimientos con sus relaciones (EAGER fetch)
    @Query("SELECT p FROM Procedimiento p LEFT JOIN FETCH p.categoria")
    java.util.List<Procedimiento> findAllWithRelations();

    // Método paginado para cargar procedimientos con sus relaciones
    @Query(value = "SELECT DISTINCT p FROM Procedimiento p LEFT JOIN FETCH p.categoria",
           countQuery = "SELECT COUNT(p) FROM Procedimiento p")
    Page<Procedimiento> findAllWithRelations(Pageable pageable);

    // Método para filtrar por categoría con paginación
    @Query(value = "SELECT DISTINCT p FROM Procedimiento p LEFT JOIN FETCH p.categoria c WHERE c.id = :categoriaId",
           countQuery = "SELECT COUNT(p) FROM Procedimiento p WHERE p.categoria.id = :categoriaId")
    Page<Procedimiento> findByCategoriaId(@Param("categoriaId") Long categoriaId, Pageable pageable);

    // Método para filtrar por categoría y keyword con paginación
    @Query(value = "SELECT DISTINCT p FROM Procedimiento p LEFT JOIN FETCH p.categoria c WHERE c.id = :categoriaId AND (p.nombre LIKE %:keyword% OR p.codigo LIKE %:keyword% OR p.descripcion LIKE %:keyword%)",
           countQuery = "SELECT COUNT(p) FROM Procedimiento p WHERE p.categoria.id = :categoriaId AND (p.nombre LIKE %:keyword% OR p.codigo LIKE %:keyword% OR p.descripcion LIKE %:keyword%)")
    Page<Procedimiento> findByCategoriaIdAndKeyword(@Param("categoriaId") Long categoriaId, @Param("keyword") String keyword, Pageable pageable);

    // --- MÉTODO PARA RESTAURAR PROCEDIMIENTOS SOFT-DELETED ---
    @Query(value = "SELECT * FROM procedimientos WHERE id = :id", nativeQuery = true)
    Optional<Procedimiento> findByIdIgnorandoSoftDelete(@Param("id") Long id);

    // --- MÉTODO PARA LISTAR PROCEDIMIENTOS ELIMINADOS ---
    @Query(value = "SELECT * FROM procedimientos WHERE eliminado = true ORDER BY fecha_modificacion DESC",
           countQuery = "SELECT COUNT(*) FROM procedimientos WHERE eliminado = true",
           nativeQuery = true)
    Page<Procedimiento> findEliminados(Pageable pageable);
}
