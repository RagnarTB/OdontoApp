package com.odontoapp.repositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.odontoapp.entidad.Insumo;
import org.springframework.stereotype.Repository;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

       Optional<Insumo> findByCodigo(String codigo);

       @Query("SELECT DISTINCT i FROM Insumo i LEFT JOIN FETCH i.categoria LEFT JOIN FETCH i.unidadMedida WHERE i.nombre LIKE %:keyword% OR i.codigo LIKE %:keyword% OR i.marca LIKE %:keyword%")
       Page<Insumo> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

       @Query("SELECT i FROM Insumo i LEFT JOIN FETCH i.categoria LEFT JOIN FETCH i.unidadMedida WHERE i.stockActual <= i.stockMinimo AND i.stockMinimo > 0")
       List<Insumo> findInsumosConStockBajo();

       // Método para cargar insumos con sus relaciones (EAGER fetch)
       @Query("SELECT i FROM Insumo i LEFT JOIN FETCH i.categoria LEFT JOIN FETCH i.unidadMedida")
       List<Insumo> findAllWithRelations();

       // Método paginado para cargar insumos con sus relaciones
       @Query(value = "SELECT DISTINCT i FROM Insumo i LEFT JOIN FETCH i.categoria LEFT JOIN FETCH i.unidadMedida", countQuery = "SELECT COUNT(i) FROM Insumo i")
       Page<Insumo> findAllWithRelations(Pageable pageable);

       // --- NUEVO MÉTODO AÑADIDO ---
       /**
        * Cuenta cuántos insumos pertenecen a una categoría específica.
        * Útil para validar si una categoría puede ser eliminada o desactivada.
        * Spring Data JPA genera la consulta: "SELECT count(i) FROM Insumo i WHERE
        * i.categoria.id = :categoriaId"
        *
        * @param categoriaId El ID de la CategoriaInsumo.
        * @return El número de insumos asociados a esa categoría.
        */
       long countByCategoriaId(Long categoriaId);

       // Método para filtrar por categoría con paginación
       @Query(value = "SELECT DISTINCT i FROM Insumo i LEFT JOIN FETCH i.categoria c LEFT JOIN FETCH i.unidadMedida WHERE c.id = :categoriaId", countQuery = "SELECT COUNT(i) FROM Insumo i WHERE i.categoria.id = :categoriaId")
       Page<Insumo> findByCategoriaId(@Param("categoriaId") Long categoriaId, Pageable pageable);

       // Método para filtrar por categoría y keyword con paginación
       @Query(value = "SELECT DISTINCT i FROM Insumo i LEFT JOIN FETCH i.categoria c LEFT JOIN FETCH i.unidadMedida WHERE c.id = :categoriaId AND (i.nombre LIKE %:keyword% OR i.codigo LIKE %:keyword% OR i.marca LIKE %:keyword%)", countQuery = "SELECT COUNT(i) FROM Insumo i WHERE i.categoria.id = :categoriaId AND (i.nombre LIKE %:keyword% OR i.codigo LIKE %:keyword% OR i.marca LIKE %:keyword%)")
       Page<Insumo> findByCategoriaIdAndKeyword(@Param("categoriaId") Long categoriaId,
                     @Param("keyword") String keyword, Pageable pageable);

       // --- MÉTODOS NUEVOS PARA SOLUCIONAR EL ERROR ---

       // 1. Buscar vencidos antes de una fecha (filtro "VENCIDOS")
       @Query("SELECT i FROM Insumo i LEFT JOIN FETCH i.categoria LEFT JOIN FETCH i.unidadMedida WHERE " +
                     "i.fechaVencimiento < :fecha " +
                     "AND (:categoriaId IS NULL OR i.categoria.id = :categoriaId) " +
                     "AND (:keyword IS NULL OR :keyword = '' OR LOWER(i.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.codigo) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<Insumo> findByFechaVencimientoBeforeWithFilters(
                     @Param("fecha") LocalDate fecha,
                     @Param("keyword") String keyword,
                     @Param("categoriaId") Long categoriaId,
                     Pageable pageable);

       // 2. Buscar vencidos entre dos fechas (filtro "POR_VENCER_X")
       @Query("SELECT i FROM Insumo i LEFT JOIN FETCH i.categoria LEFT JOIN FETCH i.unidadMedida WHERE " +
                     "(i.fechaVencimiento BETWEEN :fechaInicio AND :fechaFin) " +
                     "AND (:categoriaId IS NULL OR i.categoria.id = :categoriaId) " +
                     "AND (:keyword IS NULL OR :keyword = '' OR LOWER(i.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.codigo) LIKE LOWER(CONCAT('%', :keyword, '%')))")
       Page<Insumo> findByFechaVencimientoBetweenWithFilters(
                     @Param("fechaInicio") LocalDate fechaInicio,
                     @Param("fechaFin") LocalDate fechaFin,
                     @Param("keyword") String keyword,
                     @Param("categoriaId") Long categoriaId,
                     Pageable pageable);
}
