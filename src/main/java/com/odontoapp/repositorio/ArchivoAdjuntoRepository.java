package com.odontoapp.repositorio;

import com.odontoapp.entidad.ArchivoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchivoAdjuntoRepository extends JpaRepository<ArchivoAdjunto, Long> {

    /**
     * Busca todos los archivos adjuntos de un paciente específico.
     * @param pacienteId El ID del usuario paciente
     * @return Lista de archivos adjuntos ordenados por fecha de creación descendente
     */
    List<ArchivoAdjunto> findByPacienteIdOrderByFechaCreacionDesc(Long pacienteId);

    /**
     * Busca todos los archivos adjuntos asociados a una cita específica.
     * @param citaId El ID de la cita
     * @return Lista de archivos adjuntos ordenados por fecha de creación descendente
     */
    List<ArchivoAdjunto> findByCitaIdOrderByFechaCreacionDesc(Long citaId);
}
