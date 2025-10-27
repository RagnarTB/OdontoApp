package com.odontoapp.repositorio;

import com.odontoapp.entidad.Insumo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    Optional<Insumo> findByCodigo(String codigo);

    @Query("SELECT i FROM Insumo i WHERE i.nombre LIKE %:keyword% OR i.codigo LIKE %:keyword% OR i.marca LIKE %:keyword%")
    Page<Insumo> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT i FROM Insumo i WHERE i.stockActual <= i.stockMinimo AND i.stockMinimo > 0")
    List<Insumo> findInsumosConStockBajo();

    long countByCategoriaId(Long categoriaId);
}
