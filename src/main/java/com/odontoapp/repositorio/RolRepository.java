package com.odontoapp.repositorio;

import com.odontoapp.entidad.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(String nombre);

    // --- MÉTODO PARA BÚSQUEDA Y PAGINACIÓN ---
    @Query("SELECT r FROM Rol r WHERE r.nombre LIKE %:keyword%")
    Page<Rol> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}