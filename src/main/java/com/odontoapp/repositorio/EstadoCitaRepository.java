package com.odontoapp.repositorio;

import com.odontoapp.entidad.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoCitaRepository extends JpaRepository<EstadoCita, Long> {

    /**
     * Busca un estado de cita por su nombre.
     * @param nombre El nombre del estado de cita (ej: "Pendiente", "Confirmada", "Cancelada")
     * @return Optional con el estado de cita si existe
     */
    Optional<EstadoCita> findByNombre(String nombre);
}
