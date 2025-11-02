package com.odontoapp.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.odontoapp.entidad.Insumo;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    Optional<Insumo> findByCodigo(String codigo);

    @Query("SELECT i FROM Insumo i WHERE i.nombre LIKE %:keyword% OR i.codigo LIKE %:keyword% OR i.marca LIKE %:keyword%")
    Page<Insumo> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT i FROM Insumo i WHERE i.stockActual <= i.stockMinimo AND i.stockMinimo > 0")
    List<Insumo> findInsumosConStockBajo();

    // Método para cargar insumos con sus relaciones (EAGER fetch)
    @Query("SELECT i FROM Insumo i LEFT JOIN FETCH i.categoria LEFT JOIN FETCH i.unidadMedida")
    List<Insumo> findAllWithRelations();

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
}
