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
    Optional<Paciente> findByDni(String dni);

    Optional<Paciente> findByEmail(String email);

    @Query("SELECT p FROM Paciente p WHERE p.nombreCompleto LIKE %:keyword% OR p.dni LIKE %:keyword% OR p.email LIKE %:keyword%")
    Page<Paciente> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // --- Consultas que IGNORAN el soft delete (para validaciones) ---
    @Query("SELECT p FROM Paciente p WHERE p.dni = :dni")
    Optional<Paciente> findByDniIgnorandoSoftDelete(@Param("dni") String dni);

    @Query("SELECT p FROM Paciente p WHERE p.email = :email")
    Optional<Paciente> findByEmailIgnorandoSoftDelete(@Param("email") String email);
}