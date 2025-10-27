package com.odontoapp.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository; // Necesario para el nuevo método

import com.odontoapp.entidad.CategoriaInsumo;

public interface CategoriaInsumoRepository extends JpaRepository<CategoriaInsumo, Long> {

    Optional<CategoriaInsumo> findByNombre(String nombre);

    // --- NUEVO MÉTODO ---
    /**
     * Encuentra todas las categorías de insumo que están activas.
     * Spring Data JPA genera la consulta: "SELECT c FROM CategoriaInsumo c WHERE
     * c.estaActiva = true"
     * 
     * @return Una lista de categorías activas.
     */
    List<CategoriaInsumo> findByEstaActivaTrue();

}
