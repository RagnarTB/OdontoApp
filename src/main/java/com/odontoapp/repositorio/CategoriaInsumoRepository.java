package com.odontoapp.repositorio;

import com.odontoapp.entidad.CategoriaInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoriaInsumoRepository extends JpaRepository<CategoriaInsumo, Long> {
    Optional<CategoriaInsumo> findByNombre(String nombre);
}
