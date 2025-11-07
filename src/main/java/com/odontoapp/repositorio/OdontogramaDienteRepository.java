package com.odontoapp.repositorio;

import com.odontoapp.entidad.OdontogramaDiente;
import com.odontoapp.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OdontogramaDienteRepository extends JpaRepository<OdontogramaDiente, Long> {

    /**
     * Obtener todo el odontograma de un paciente
     */
    List<OdontogramaDiente> findByPaciente(Usuario paciente);

    /**
     * Obtener estado de un diente específico
     */
    Optional<OdontogramaDiente> findByPacienteAndNumeroDiente(Usuario paciente, String numeroDiente);

    /**
     * Obtener dientes por estado
     */
    List<OdontogramaDiente> findByPacienteAndEstado(Usuario paciente, String estado);

    /**
     * Verificar si existe el diente
     */
    boolean existsByPacienteAndNumeroDiente(Usuario paciente, String numeroDiente);
}
