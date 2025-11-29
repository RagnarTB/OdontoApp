// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\repositorio\PacienteRepository.java
package com.odontoapp.repositorio;

import com.odontoapp.entidad.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    // --- Consultas que SÍ respetan el soft delete (@Where) ---
    @Query("SELECT p FROM Paciente p WHERE p.numeroDocumento = :numDoc AND p.tipoDocumento.id = :tipoDocId")
    Optional<Paciente> findByNumeroTipoDocumento(@Param("numDoc") String numDoc, @Param("tipoDocId") Long tipoDocId);

    Optional<Paciente> findByEmail(String email);

    @Query("SELECT p FROM Paciente p WHERE p.nombreCompleto LIKE %:keyword% OR p.numeroDocumento LIKE %:keyword% OR p.email LIKE %:keyword%")
    Page<Paciente> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // --- Consultas que IGNORAN el soft delete (para validaciones) ---
    @Query("SELECT p FROM Paciente p WHERE p.numeroDocumento = :numDoc AND p.tipoDocumento.id = :tipoDocId")
    Optional<Paciente> findByNumeroTipoDocumentoIgnorandoSoftDelete(@Param("numDoc") String numDoc,
            @Param("tipoDocId") Long tipoDocId);

    @Query("SELECT p FROM Paciente p WHERE p.email = :email")
    Optional<Paciente> findByEmailIgnorandoSoftDelete(@Param("email") String email);

    @Query("SELECT p FROM Paciente p WHERE p.telefono = :telefono")
    Optional<Paciente> findByTelefonoIgnorandoSoftDelete(@Param("telefono") String telefono);

    @Query(value = "SELECT * FROM pacientes WHERE id = :id", nativeQuery = true)
    Optional<Paciente> findByIdIgnorandoSoftDelete(@Param("id") Long id);

    /**
     * Cuenta los pacientes creados en un rango de fechas (para estadísticas)
     */
    Long countByFechaCreacionBetween(java.time.LocalDateTime inicio, java.time.LocalDateTime fin);

    /**
     * Cuenta el total de pacientes activos
     */
    Long countByEliminadoFalse();

    /**
     * Busca un paciente por su usuario asociado
     */
    @Query("SELECT p FROM Paciente p WHERE p.usuario = :usuario")
    Optional<Paciente> findByUsuario(@Param("usuario") com.odontoapp.entidad.Usuario usuario);

    // --- MÉTODO PARA LISTAR PACIENTES ELIMINADOS ---
    // Solo mostrar pacientes que tienen ÚNICAMENTE el rol PACIENTE
    @Query(value = "SELECT p.* FROM pacientes p " +
            "INNER JOIN usuarios u ON p.usuario_id = u.id " +
            "WHERE p.eliminado = true " +
            "AND (u.id NOT IN (SELECT ur.usuario_id FROM usuarios_roles ur " +
            "                  INNER JOIN roles r ON ur.rol_id = r.id " +
            "                  WHERE r.nombre != 'PACIENTE') " +
            "     OR u.id IN (SELECT ur2.usuario_id FROM usuarios_roles ur2 " +
            "                 GROUP BY ur2.usuario_id " +
            "                 HAVING COUNT(DISTINCT ur2.rol_id) = 1 " +
            "                 AND MAX((SELECT nombre FROM roles WHERE id = ur2.rol_id)) = 'PACIENTE')) " +
            "ORDER BY p.fecha_modificacion DESC", countQuery = "SELECT COUNT(*) FROM pacientes p " +
                    "INNER JOIN usuarios u ON p.usuario_id = u.id " +
                    "WHERE p.eliminado = true " +
                    "AND (u.id NOT IN (SELECT ur.usuario_id FROM usuarios_roles ur " +
                    "                  INNER JOIN roles r ON ur.rol_id = r.id " +
                    "                  WHERE r.nombre != 'PACIENTE') " +
                    "     OR u.id IN (SELECT ur2.usuario_id FROM usuarios_roles ur2 " +
                    "                 GROUP BY ur2.usuario_id " +
                    "                 HAVING COUNT(DISTINCT ur2.rol_id) = 1 " +
                    "                 AND MAX((SELECT nombre FROM roles WHERE id = ur2.rol_id)) = 'PACIENTE'))", nativeQuery = true)
    Page<Paciente> findEliminados(Pageable pageable);
}