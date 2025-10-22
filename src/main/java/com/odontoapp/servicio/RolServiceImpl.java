package com.odontoapp.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.odontoapp.dto.RolDTO;
import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Rol;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.repositorio.RolRepository;

import jakarta.transaction.Transactional;

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

    // En src/main/java/com/odontoapp/servicio/RolServiceImpl.java

    @Override
    @Transactional
    public void eliminarRol(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado"));

        if ("ADMIN".equals(rol.getNombre()) || "PACIENTE".equals(rol.getNombre())) {
            throw new UnsupportedOperationException(
                    "No se puede eliminar el rol '" + rol.getNombre() + "'. Es un rol protegido.");
        }
        // Asegurarse de cargar los usuarios para la validación
        if (!rol.getUsuarios().isEmpty()) { // Comprueba si la colección está vacía
            throw new DataIntegrityViolationException("El rol tiene usuarios asignados y no puede ser eliminado.");
        }

        // Llama al deleteById que activará @SQLDelete
        rolRepository.deleteById(id);
    }

    @Override
    public void cambiarEstadoRol(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado"));

        // --- PROTEGER ROLES CRÍTICOS ---
        if ("ADMIN".equals(rol.getNombre()) || "PACIENTE".equals(rol.getNombre())) {
            throw new UnsupportedOperationException(
                    "No se puede cambiar el estado del rol '" + rol.getNombre() + "'. Es un rol protegido.");
        }

        // VALIDACIÓN MEJORADA ---
        if (rol.isEstaActivo() && rol.getUsuarios() != null && !rol.getUsuarios().isEmpty()) {
            // Verificar si algún usuario quedaría sin roles activos
            long usuariosQueQuedaranSinRoles = rol.getUsuarios().stream()
                    .filter(u -> u.getRoles().size() == 1 && u.getRoles().contains(rol))
                    .count();

            if (usuariosQueQuedaranSinRoles > 0) {
                throw new DataIntegrityViolationException(
                        usuariosQueQuedaranSinRoles + " usuario(s) quedarían sin roles activos. " +
                                "Asígneles otros roles antes de desactivar este.");
            }

            // También puedes mantener tu validación anterior si deseas
            // lanzar un error genérico si el rol tiene usuarios
            // throw new DataIntegrityViolationException("No se puede desactivar un rol que
            // tiene usuarios asignados.");
        }

        // --- CAMBIO DE ESTADO ---
        rol.setEstaActivo(!rol.isEstaActivo());
        rolRepository.save(rol);
    }

}