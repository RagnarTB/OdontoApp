package com.odontoapp.repositorio;

import com.odontoapp.entidad.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {

    /**
     * Busca un método de pago por su nombre.
     * @param nombre El nombre del método de pago (ej: "Efectivo", "Tarjeta", "Transferencia")
     * @return Optional con el método de pago si existe
     */
    Optional<MetodoPago> findByNombre(String nombre);
}
