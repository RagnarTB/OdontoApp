package com.odontoapp.repositorio;

import com.odontoapp.entidad.PacienteImagen;
import com.odontoapp.entidad.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PacienteImagenRepository extends JpaRepository<PacienteImagen, Long> {

    /**
     * Obtener todas las imágenes de un paciente
     */
    List<PacienteImagen> findByPacienteOrderByFechaSubidaDesc(Usuario paciente);

    /**
     * Obtener imágenes por tipo
     */
    List<PacienteImagen> findByPacienteAndTipoOrderByFechaSubidaDesc(Usuario paciente, String tipo);

    /**
     * Obtener imágenes asociadas a un diente específico
     */
    List<PacienteImagen> findByPacienteAndDienteAsociadoOrderByFechaSubidaDesc(Usuario paciente, String dienteAsociado);
}
