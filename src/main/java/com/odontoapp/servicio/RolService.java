package com.odontoapp.servicio;

import com.odontoapp.dto.RolDTO;
import com.odontoapp.entidad.Rol;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RolService {
    void guardarRol(RolDTO rolDTO);

    Page<Rol> listarTodosLosRoles(String keyword, Pageable pageable);

    Optional<Rol> buscarRolPorId(Long id);

    void eliminarRol(Long id);

    void cambiarEstadoRol(Long id);
}