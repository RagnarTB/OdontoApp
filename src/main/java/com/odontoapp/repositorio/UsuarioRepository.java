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

    // --- MÉTODO PARA BÚSQUEDA Y PAGINACIÓN CON ROLES ---
    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.roles WHERE u.nombreCompleto LIKE %:keyword% OR u.email LIKE %:keyword%")
    Page<Usuario> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // --- MÉTODO PARA LISTADO COMPLETO CON ROLES ---
    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.roles")
    Page<Usuario> findAllWithRoles(Pageable pageable);

    Optional<Usuario> findByVerificationToken(String verificationToken);

    Optional<Usuario> findByPasswordResetToken(String passwordResetToken);

    // --- NUEVO MÉTODO ---
    @Query("SELECT u FROM Usuario u WHERE u.email = :email") // Ignora el @Where global
    Optional<Usuario> findByEmailIgnorandoSoftDelete(@Param("email") String email);

    // --- NUEVO MÉTODO ---
    @Query("SELECT u FROM Usuario u WHERE u.id = :id") // Ignora @Where
    Optional<Usuario> findByIdIgnorandoSoftDelete(@Param("id") Long id);

    @Query("SELECT u FROM Usuario u WHERE u.numeroDocumento = :numDoc AND u.tipoDocumento.id = :tipoDocId")
    Optional<Usuario> findByNumeroDocumentoAndTipoDocumentoIdIgnorandoSoftDelete(
            @Param("numDoc") String numDoc, @Param("tipoDocId") Long tipoDocId);

    // --- NUEVO MÉTODO PARA NÚMERO DE DOCUMENTO ---
    @Query("SELECT u FROM Usuario u WHERE u.numeroDocumento = :numeroDocumento")
    Optional<Usuario> findByNumeroDocumentoIgnorandoSoftDelete(@Param("numeroDocumento") String numeroDocumento);

    // Buscar usuario por número de documento y tipo (respetando soft delete)
    Optional<Usuario> findByNumeroDocumentoAndTipoDocumento_Id(String numeroDocumento, Long tipoDocumentoId);

    // --- NUEVO MÉTODO PARA TELÉFONO ---
    @Query("SELECT u FROM Usuario u WHERE u.telefono = :telefono AND u.telefono IS NOT NULL")
    Optional<Usuario> findByTelefonoIgnorandoSoftDelete(@Param("telefono") String telefono);

    // Buscar usuarios por nombre de rol
    @Query("SELECT u FROM Usuario u JOIN u.roles r WHERE r.nombre = :rolNombre")
    java.util.List<Usuario> findByRolesNombre(@Param("rolNombre") String rolNombre);

    /**
     * Busca usuarios activos cuya fecha de vigencia ha vencido.
     * Usado por el scheduler para desactivar usuarios automáticamente.
     *
     * @param fecha Fecha de referencia (típicamente la fecha actual)
     * @return Lista de usuarios con vigencia vencida
     */
    java.util.List<Usuario> findByFechaVigenciaBeforeAndEstaActivoTrue(java.time.LocalDate fecha);

    // --- MÉTODO PARA LISTAR USUARIOS ELIMINADOS ---
    @Query(value = "SELECT * FROM usuarios WHERE eliminado = true ORDER BY fecha_eliminacion DESC",
           countQuery = "SELECT COUNT(*) FROM usuarios WHERE eliminado = true",
           nativeQuery = true)
    Page<Usuario> findEliminados(Pageable pageable);

    /**
     * Cuenta cuántos usuarios activos tienen un rol específico.
     * Útil para validar si un rol puede ser eliminado.
     * @param rolId El ID del rol
     * @return El número de usuarios activos con ese rol
     */
    @Query("SELECT COUNT(DISTINCT u) FROM Usuario u JOIN u.roles r WHERE r.id = :rolId AND u.estaActivo = true")
    long countUsuariosActivosByRolId(@Param("rolId") Long rolId);
}