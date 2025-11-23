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

    // --- MÉTODOS PARA CONTAR USUARIOS SIN CARGAR LA COLECCIÓN ---
    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE r.id = :rolId AND u.eliminado = false")
    long countUsuariosActivosByRolId(@Param("rolId") Long rolId);

    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE r.id = :rolId AND u.eliminado = false AND NOT EXISTS (SELECT 1 FROM Usuario u2 JOIN u2.roles r2 WHERE u2.id = u.id AND r2.id <> :rolId AND r2.estaActivo = true)")
    long countUsuariosSinOtrosRolesActivosByRolId(@Param("rolId") Long rolId);

    // --- MÉTODO PARA RESTAURAR ROLES SOFT-DELETED ---
    @Query("SELECT r FROM Rol r WHERE r.id = :id") // Ignora @Where
    Optional<Rol> findByIdIgnorandoSoftDelete(@Param("id") Long id);

    // --- MÉTODO PARA LISTAR ROLES ELIMINADOS ---
    @Query("SELECT r FROM Rol r WHERE r.eliminado = true")
    Page<Rol> findEliminados(Pageable pageable);
}