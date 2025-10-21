package com.odontoapp.servicio;

import com.odontoapp.dto.PacienteDTO; // Importar DTO
import com.odontoapp.entidad.Paciente;
import org.springframework.data.domain.Page; // Importar Page
import org.springframework.data.domain.Pageable; // Importar Pageable
import java.util.Optional;

public interface PacienteService {
    void guardarPaciente(PacienteDTO pacienteDTO); // Modificado

    Page<Paciente> listarTodosLosPacientes(String keyword, Pageable pageable); // Modificado

    Optional<Paciente> buscarPorId(Long id);

    void eliminarPaciente(Long id);

    Optional<Paciente> buscarPorDni(String dni);
}