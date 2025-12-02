package com.odontoapp.servicio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.odontoapp.dto.UsuarioDTO;
import com.odontoapp.entidad.Usuario;

public interface UsuarioService {
    void guardarUsuario(UsuarioDTO usuarioDTO);

    Page<Usuario> listarTodosLosUsuarios(String keyword, Pageable pageable);

    Optional<Usuario> buscarPorId(Long id);

    void eliminarUsuario(Long id);

    void cambiarEstadoUsuario(Long id);

    void procesarLoginFallido(String email);

    void resetearIntentosFallidos(String email);

    void cambiarEstadoUsuario(Long id, boolean activar) throws UnsupportedOperationException, IllegalStateException;

    void restablecerUsuario(Long id);

    void promoverPacienteAPersonal(Long pacienteId, List<Long> rolesIds,
            LocalDate fechaContratacion, LocalDate fechaVigencia);

    List<Usuario> listarPorRol(String rol);
}