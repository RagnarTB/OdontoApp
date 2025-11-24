// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\servicio\PacienteService.java
package com.odontoapp.servicio;

import com.odontoapp.dto.PacienteDTO;
import com.odontoapp.dto.RegistroPacienteDTO; // Importar nuevo DTO
import com.odontoapp.entidad.Paciente;
import com.odontoapp.entidad.Usuario; // Importar Usuario
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface PacienteService {
    void guardarPaciente(PacienteDTO pacienteDTO); // Para Admin

    // 🔥 NUEVOS MÉTODOS PARA REGISTRO DE PACIENTE SELF-SERVICE
    Usuario crearUsuarioTemporalParaRegistro(String email);

    void completarRegistroPaciente(RegistroPacienteDTO registroDTO, String token, String password);

    Page<Paciente> listarTodosLosPacientes(String keyword, Pageable pageable);

    Optional<Paciente> buscarPorId(Long id);

    void eliminarPaciente(Long id);

    Optional<Paciente> buscarPorDocumento(String numeroDocumento, Long tipoDocumentoId); // Modificado

    String restablecerPaciente(Long id);
}