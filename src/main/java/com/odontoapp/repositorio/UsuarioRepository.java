package com.odontoapp.repositorio;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.odontoapp.entidad.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // --- MÉTODO OPTIMIZADO PARA LOGIN ---
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permisos WHERE u.email = :email")
    Optional<Usuario> findByEmailWithRolesAndPermissions(@Param("email") String email);

    Optional<Usuario> findByEmail(String email);

    // --- MÉTODO PARA BÚSQUEDA Y PAGINACIÓN ---
    @Query("SELECT u FROM Usuario u WHERE u.nombreCompleto LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<Usuario> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    Optional<Usuario> findByVerificationToken(String verificationToken);

    // --- NUEVO MÉTODO ---
    @Query("SELECT u FROM Usuario u WHERE u.email = :email") // Ignora el @Where global
    Optional<Usuario> findByEmailIgnorandoSoftDelete(@Param("email") String email);

    // --- NUEVO MÉTODO ---
    @Query("SELECT u FROM Usuario u WHERE u.id = :id") // Ignora @Where
    Optional<Usuario> findByIdIgnorandoSoftDelete(@Param("id") Long id);

    @Query("SELECT u FROM Usuario u WHERE u.numeroDocumento = :numDoc AND u.tipoDocumento.id = :tipoDocId")
    Optional<Usuario> findByNumeroDocumentoAndTipoDocumentoIdIgnorandoSoftDelete(
            @Param("numDoc") String numDoc, @Param("tipoDocId") Long tipoDocId);
}