package com.odontoapp.repositorio;

import com.odontoapp.entidad.TratamientoRealizado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TratamientoRealizadoRepository extends JpaRepository<TratamientoRealizado, Long> {

    /**
     * Busca todos los tratamientos realizados asociados a una cita específica.
     * @param citaId El ID de la cita
     * @return Lista de tratamientos realizados ordenados por fecha de realización descendente
     */
    List<TratamientoRealizado> findByCitaIdOrderByFechaRealizacionDesc(Long citaId);
}
