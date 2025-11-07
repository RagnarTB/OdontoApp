package com.odontoapp.repositorio;

import com.odontoapp.entidad.OdontogramaHistorial;
import com.odontoapp.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OdontogramaHistorialRepository extends JpaRepository<OdontogramaHistorial, Long> {

    /**
     * Obtener historial completo de un paciente
     */
    List<OdontogramaHistorial> findByPacienteOrderByFechaCambioDesc(Usuario paciente);

    /**
     * Obtener historial de un diente específico
     */
    List<OdontogramaHistorial> findByPacienteAndNumeroDienteOrderByFechaCambioDesc(Usuario paciente, String numeroDiente);

    /**
     * Obtener últimos N cambios de un paciente
     */
    @Query("SELECT h FROM OdontogramaHistorial h WHERE h.paciente = :paciente " +
           "ORDER BY h.fechaCambio DESC")
    List<OdontogramaHistorial> findUltimosCambios(@Param("paciente") Usuario paciente);
}
