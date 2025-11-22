package com.odontoapp.servicio;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException; // Asegúrate de importar Set
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.odontoapp.dto.RolDTO;
import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Rol;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.repositorio.RolRepository; // Importar Usuario

import jakarta.transaction.Transactional;

@Service
public class RolServiceImpl implements RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final SessionInvalidationService sessionInvalidationService;

    // Nombres de roles protegidos (constantes para evitar errores tipográficos)
    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_PACIENTE = "PACIENTE";
    private static final String ROL_ODONTOLOGO = "ODONTOLOGO"; // Añadido para protección

    public RolServiceImpl(RolRepository rolRepository, PermisoRepository permisoRepository,
                          SessionInvalidationService sessionInvalidationService) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.sessionInvalidationService = sessionInvalidationService;
    }

    @Override
    @Transactional // Es buena práctica añadir Transactional a métodos que modifican datos
    public void guardarRol(RolDTO rolDTO) {
        String nombreRolUpper = rolDTO.getNombre().toUpperCase(); // Normalizar a mayúsculas

        // --- VALIDACIÓN DE ROLES DEL SISTEMA ---
        if (rolDTO.getId() != null) { // Solo aplica si estamos editando
            Rol rolExistente = rolRepository.findById(rolDTO.getId())
                    .orElseThrow(
                            () -> new IllegalStateException("Rol no encontrado para editar con ID: " + rolDTO.getId()));

            // Impedir editar roles del sistema (Administrador, Paciente, Odontólogo)
            if (rolExistente.isEsRolSistema()) {
                throw new UnsupportedOperationException(
                        "El rol del sistema '" + rolExistente.getNombre() + "' no puede ser modificado.");
            }
        } else {
            // Impedir crear NUEVOS roles con nombres protegidos
            if (ROL_ADMIN.equals(nombreRolUpper) || ROL_PACIENTE.equals(nombreRolUpper)
                    || ROL_ODONTOLOGO.equals(nombreRolUpper)) {
                throw new UnsupportedOperationException(
                        "No se puede crear un rol con el nombre protegido '" + nombreRolUpper + "'.");
            }
        }

        // --- VALIDACIÓN DE NOMBRE ÚNICO (CORREGIDA) ---
        Optional<Rol> existentePorNombre = rolRepository.findByNombre(nombreRolUpper);
        // Si existe un rol con ese nombre Y (estamos creando uno nuevo O estamos
        // editando uno diferente al encontrado)
        if (existentePorNombre.isPresent()
                && (rolDTO.getId() == null || !existentePorNombre.get().getId().equals(rolDTO.getId()))) {
            throw new DataIntegrityViolationException("El nombre de rol '" + nombreRolUpper + "' ya está en uso.");
        }

        // --- LÓGICA DE GUARDADO ---
        Rol rol;
        if (rolDTO.getId() != null) {
            // Re-obtener el rol para asegurar que trabajamos con la entidad gestionada
            rol = rolRepository.findById(rolDTO.getId())
                    .orElseThrow(() -> new IllegalStateException("Rol no encontrado con ID: " + rolDTO.getId()));
        } else {
            rol = new Rol();
            rol.setEstaActivo(true); // Activo por defecto al crear
        }

        rol.setNombre(nombreRolUpper); // Guardar en mayúsculas

        // Asignar permisos (asegurándose de limpiar los anteriores si se edita)
        if (rol.getPermisos() == null) {
            rol.setPermisos(new HashSet<>());
        } else {
            rol.getPermisos().clear(); // Limpiar permisos existentes antes de añadir los nuevos
        }

        if (rolDTO.getPermisos() != null && !rolDTO.getPermisos().isEmpty()) {
            List<Permiso> permisosSeleccionados = permisoRepository.findAllById(rolDTO.getPermisos());
            rol.getPermisos().addAll(permisosSeleccionados); // Añadir los nuevos permisos
        }

        rolRepository.save(rol);

        // ⚠️ INVALIDAR SESIONES: Si se modificaron los permisos de un rol existente,
        // forzar logout a todos los usuarios con ese rol para que obtengan los nuevos permisos
        if (rolDTO.getId() != null) {
            int sesionesInvalidadas = sessionInvalidationService.invalidarSesionesPorRol(rol);
            System.out.println("✅ Permisos del rol '" + rol.getNombre() + "' actualizados. " +
                    sesionesInvalidadas + " sesión(es) invalidada(s).");
        }
    }

    @Override
    public Page<Rol> listarTodosLosRoles(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            // Convertir keyword a mayúsculas para la búsqueda si los nombres se guardan en
            // mayúsculas
            return rolRepository.findByKeyword(keyword.toUpperCase(), pageable);
        }
        return rolRepository.findAll(pageable);
    }

    @Override
    public Optional<Rol> buscarRolPorId(Long id) {
        return rolRepository.findById(id);
    }

    @Override
    @Transactional
    public void eliminarRol(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado"));

        // Proteger roles del sistema (Admin, Paciente, Odontólogo)
        if (rol.isEsRolSistema()) {
            throw new UnsupportedOperationException(
                    "No se puede eliminar el rol del sistema '" + rol.getNombre() + "'.");
        }

        // Validar si tiene usuarios asociados usando query optimizada
        long usuariosActivos = rolRepository.countUsuariosActivosByRolId(id);
        if (usuariosActivos > 0) {
            throw new DataIntegrityViolationException(
                    "El rol tiene " + usuariosActivos
                            + " usuario(s) activo(s) asignado(s) y no puede ser eliminado.");
        }

        // Llama al deleteById que activarÃ¡ @SQLDelete
        rolRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void cambiarEstadoRol(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado"));

        // Proteger roles del sistema de cambio de estado
        if (rol.isEsRolSistema()) {
            throw new UnsupportedOperationException(
                    "No se puede cambiar el estado del rol del sistema '" + rol.getNombre() + "'.");
        }

        // VALIDACIÓN MEJORADA: Evitar desactivar si deja usuarios sin roles activos
        if (rol.isEstaActivo()) { // Solo validar al intentar desactivar
            // Usar query optimizada para contar usuarios que quedarían sin roles activos
            long usuariosSinOtrosRolesActivos = rolRepository.countUsuariosSinOtrosRolesActivosByRolId(id);

            if (usuariosSinOtrosRolesActivos > 0) {
                throw new DataIntegrityViolationException(
                        usuariosSinOtrosRolesActivos + " usuario(s) quedarían sin roles activos. " +
                                "Asígnales otros roles activos antes de desactivar este.");
            }
        }

        // --- CAMBIO DE ESTADO ---
        boolean nuevoEstado = !rol.isEstaActivo();
        rol.setEstaActivo(nuevoEstado);
        rolRepository.save(rol);

        // ⚠️ INVALIDAR SESIONES: Forzar logout a usuarios con este rol
        // para que reflejen el cambio de estado
        int sesionesInvalidadas = sessionInvalidationService.invalidarSesionesPorRol(rol);
        System.out.println("✅ Estado del rol '" + rol.getNombre() + "' cambiado a " +
                (nuevoEstado ? "ACTIVO" : "INACTIVO") + ". " +
                sesionesInvalidadas + " sesión(es) invalidada(s).");
    }

}
