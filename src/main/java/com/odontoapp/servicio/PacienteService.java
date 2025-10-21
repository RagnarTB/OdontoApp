package com.odontoapp.servicio;

import com.odontoapp.entidad.Paciente;
import java.util.List;
import java.util.Optional;

public interface PacienteService {
    Paciente guardarPaciente(Paciente paciente);

    List<Paciente> listarTodosLosPacientes();

    Optional<Paciente> buscarPorId(Long id);

    void eliminarPaciente(Long id);

    Optional<Paciente> buscarPorDni(String dni);
}