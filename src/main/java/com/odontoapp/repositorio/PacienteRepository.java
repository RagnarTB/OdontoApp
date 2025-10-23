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
    // 🔥 MODIFICADO: Buscar por Número y Tipo de Documento
    @Query("SELECT p FROM Paciente p WHERE p.numeroDocumento = :numDoc AND p.tipoDocumento.id = :tipoDocId")
    Optional<Paciente> findByNumeroTipoDocumento(@Param("numDoc") String numDoc, @Param("tipoDocId") Long tipoDocId);

    Optional<Paciente> findByEmail(String email);

    // 🔥 MODIFICADO: Búsqueda por palabra clave (ahora incluye numeroDocumento)
    @Query("SELECT p FROM Paciente p WHERE p.nombreCompleto LIKE %:keyword% OR p.numeroDocumento LIKE %:keyword% OR p.email LIKE %:keyword%")
    Page<Paciente> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // --- Consultas que IGNORAN el soft delete (para validaciones) ---
    // 🔥 MODIFICADO: Ignorar Soft Delete usando Número y Tipo de Documento
    @Query("SELECT p FROM Paciente p WHERE p.numeroDocumento = :numDoc AND p.tipoDocumento.id = :tipoDocId")
    Optional<Paciente> findByNumeroTipoDocumentoIgnorandoSoftDelete(@Param("numDoc") String numDoc,
            @Param("tipoDocId") Long tipoDocId);

    @Query("SELECT p FROM Paciente p WHERE p.email = :email")
    Optional<Paciente> findByEmailIgnorandoSoftDelete(@Param("email") String email);

    @Query("SELECT p FROM Paciente p WHERE p.id = :id")
    Optional<Paciente> findByIdIgnorandoSoftDelete(@Param("id") Long id);
}