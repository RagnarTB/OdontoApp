package com.odontoapp.repositorio;

import com.odontoapp.entidad.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoPagoRepository extends JpaRepository<EstadoPago, Long> {

    /**
     * Busca un estado de pago por su nombre.
     * @param nombre El nombre del estado de pago (ej: "Pendiente", "Pagado", "Parcial")
     * @return Optional con el estado de pago si existe
     */
    Optional<EstadoPago> findByNombre(String nombre);
}
