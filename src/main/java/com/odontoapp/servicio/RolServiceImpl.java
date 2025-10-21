package com.odontoapp.servicio;

import com.odontoapp.dto.RolDTO;
import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Rol;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.repositorio.RolRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    public RolServiceImpl(RolRepository rolRepository, PermisoRepository permisoRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
    }

    @Override
    public void guardarRol(RolDTO rolDTO) {
        String nombreRol = rolDTO.getNombre().toUpperCase();

        Optional<Rol> existente = rolRepository.findByNombre(nombreRol);
        if (existente.isPresent() && !existente.get().getId().equals(rolDTO.getId())) {
            throw new DataIntegrityViolationException("El rol '" + nombreRol + "' ya existe.");
        }

        Rol rol;
        if (rolDTO.getId() != null) {
            rol = rolRepository.findById(rolDTO.getId())
                    .orElseThrow(() -> new IllegalStateException("Rol no encontrado"));
        } else {
            rol = new Rol();
            rol.setEstaActivo(true);
        }

        rol.setNombre(nombreRol);
        if (rolDTO.getPermisos() != null) {
            List<Permiso> permisos = permisoRepository.findAllById(rolDTO.getPermisos());
            rol.setPermisos(new HashSet<>(permisos));
        }

        rolRepository.save(rol);
    }

    @Override
    public Page<Rol> listarTodosLosRoles(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return rolRepository.findByKeyword(keyword, pageable);
        }
        return rolRepository.findAll(pageable);
    }

    @Override
    public Optional<Rol> buscarRolPorId(Long id) {
        return rolRepository.findById(id);
    }

    @Override
    public void eliminarRol(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado"));

        if ("ADMIN".equals(rol.getNombre())) {
            throw new UnsupportedOperationException("No se puede eliminar el rol ADMIN.");
        }

        if (rol.getUsuarios() != null && !rol.getUsuarios().isEmpty()) {
            throw new DataIntegrityViolationException("El rol tiene usuarios asignados y no puede ser eliminado.");
        }

        rol.setPermisos(null);
        rolRepository.save(rol);
        rolRepository.deleteById(id);
    }

    @Override
    public void cambiarEstadoRol(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado"));

        if ("ADMIN".equals(rol.getNombre())) {
            throw new UnsupportedOperationException("No se puede cambiar el estado del rol ADMIN.");
        }

        // --- NUEVA LÓGICA DE VALIDACIÓN ---
        // Si se está intentando desactivar (el estado actual es activo)
        if (rol.isEstaActivo() && rol.getUsuarios() != null && !rol.getUsuarios().isEmpty()) {
            throw new DataIntegrityViolationException("No se puede desactivar un rol que tiene usuarios asignados.");
        }

        rol.setEstaActivo(!rol.isEstaActivo());
        rolRepository.save(rol);
    }
}