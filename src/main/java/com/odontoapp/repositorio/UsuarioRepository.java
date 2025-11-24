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

        // --- NUEVOS MÉTODOS NATIVOS PARA IGNORAR SOFT DELETE ---

        @Query(value = "SELECT * FROM usuarios WHERE email = :email", nativeQuery = true)
        Optional<Usuario> findByEmailIgnorandoSoftDelete(@Param("email") String email);

        @Query(value = "SELECT * FROM usuarios WHERE id = :id", nativeQuery = true)
        Optional<Usuario> findByIdIgnorandoSoftDelete(@Param("id") Long id);

        @Query(value = "SELECT * FROM usuarios WHERE numero_documento = :numDoc AND tipo_documento_id = :tipoDocId", nativeQuery = true)
        Optional<Usuario> findByNumeroDocumentoAndTipoDocumentoIdIgnorandoSoftDelete(
                        @Param("numDoc") String numDoc, @Param("tipoDocId") Long tipoDocId);

        @Query(value = "SELECT * FROM usuarios WHERE numero_documento = :numeroDocumento", nativeQuery = true)
        Optional<Usuario> findByNumeroDocumentoIgnorandoSoftDelete(@Param("numeroDocumento") String numeroDocumento);

        @Query(value = "SELECT * FROM usuarios WHERE telefono = :telefono", nativeQuery = true)
        Optional<Usuario> findByTelefonoIgnorandoSoftDelete(@Param("telefono") String telefono);

        // Buscar usuario por número de documento y tipo (respetando soft delete)
        Optional<Usuario> findByNumeroDocumentoAndTipoDocumento_Id(String numeroDocumento, Long tipoDocumentoId);

        // Buscar usuarios por nombre de rol
        @Query("SELECT u FROM Usuario u JOIN u.roles r WHERE r.nombre = :rolNombre")
        java.util.List<Usuario> findByRolesNombre(@Param("rolNombre") String rolNombre);

        // Buscar usuarios ACTIVOS por nombre de rol (solo usuarios activos y no
        // eliminados)
        @Query("SELECT u FROM Usuario u JOIN u.roles r WHERE r.nombre = :rolNombre AND u.estaActivo = true AND r.estaActivo = true")
        java.util.List<Usuario> findActiveByRolesNombre(@Param("rolNombre") String rolNombre);

        /**
         * Busca usuarios activos cuya fecha de vigencia ha vencido.
         * Usado por el scheduler para desactivar usuarios automáticamente.
         *
         * @param fecha Fecha de referencia (típicamente la fecha actual)
         * @return Lista de usuarios con vigencia vencida
         */
        java.util.List<Usuario> findByFechaVigenciaBeforeAndEstaActivoTrue(java.time.LocalDate fecha);

        // --- MÉTODO PARA LISTAR USUARIOS ELIMINADOS ---
        @Query(value = "SELECT * FROM usuarios WHERE eliminado = true ORDER BY fecha_modificacion DESC", countQuery = "SELECT COUNT(*) FROM usuarios WHERE eliminado = true", nativeQuery = true)
        Page<Usuario> findEliminados(Pageable pageable);

        /**
         * Cuenta cuántos usuarios activos tienen un rol específico.
         * Útil para validar si un rol puede ser eliminado.
         * 
         * @param rolId El ID del rol
         * @return El número de usuarios activos con ese rol
         */
        @Query("SELECT COUNT(DISTINCT u) FROM Usuario u JOIN u.roles r WHERE r.id = :rolId AND u.estaActivo = true")
        long countUsuariosActivosByRolId(@Param("rolId") Long rolId);

        /**
         * Lista usuarios eliminados que NO son solo pacientes (tienen otros roles
         * además de PACIENTE o no tienen el rol PACIENTE).
         * Esto permite separar la gestión de usuarios de personal de los pacientes.
         * 
         * @param pageable Paginación
         * @return Página de usuarios eliminados que son personal (no solo pacientes)
         */
        @Query(value = "SELECT DISTINCT u.* FROM usuarios u " +
                        "LEFT JOIN usuarios_roles ur ON u.id = ur.usuario_id " +
                        "LEFT JOIN roles r ON ur.rol_id = r.id " +
                        "WHERE u.eliminado = true " +
                        "AND (u.id NOT IN (" +
                        "    SELECT u2.id FROM usuarios u2 " +
                        "    JOIN usuarios_roles ur2 ON u2.id = ur2.usuario_id " +
                        "    JOIN roles r2 ON ur2.rol_id = r2.id " +
                        "    WHERE u2.eliminado = true " +
                        "    GROUP BY u2.id " +
                        "    HAVING COUNT(DISTINCT r2.id) = 1 AND MAX(r2.nombre) = 'PACIENTE'" +
                        ") OR u.id NOT IN (SELECT usuario_id FROM usuarios_roles)) " +
                        "ORDER BY u.fecha_modificacion DESC", countQuery = "SELECT COUNT(DISTINCT u.id) FROM usuarios u "
                                        +
                                        "LEFT JOIN usuarios_roles ur ON u.id = ur.usuario_id " +
                                        "LEFT JOIN roles r ON ur.rol_id = r.id " +
                                        "WHERE u.eliminado = true " +
                                        "AND (u.id NOT IN (" +
                                        "    SELECT u2.id FROM usuarios u2 " +
                                        "    JOIN usuarios_roles ur2 ON u2.id = ur2.usuario_id " +
                                        "    JOIN roles r2 ON ur2.rol_id = r2.id " +
                                        "    WHERE u2.eliminado = true " +
                                        "    GROUP BY u2.id " +
                                        "    HAVING COUNT(DISTINCT r2.id) = 1 AND MAX(r2.nombre) = 'PACIENTE'" +
                                        ") OR u.id NOT IN (SELECT usuario_id FROM usuarios_roles))", nativeQuery = true)
        Page<Usuario> findEliminadosExcluyendoSoloPacientes(Pageable pageable);
}