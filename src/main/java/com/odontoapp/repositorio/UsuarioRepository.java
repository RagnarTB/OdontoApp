package com.odontoapp.repositorio;

import com.odontoapp.entidad.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // --- MÉTODO OPTIMIZADO PARA LOGIN ---
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permisos WHERE u.email = :email")
    Optional<Usuario> findByEmailWithRolesAndPermissions(@Param("email") String email);

    Optional<Usuario> findByEmail(String email);

    // --- MÉTODO PARA BÚSQUEDA Y PAGINACIÓN ---
    @Query("SELECT u FROM Usuario u WHERE u.nombreCompleto LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<Usuario> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}