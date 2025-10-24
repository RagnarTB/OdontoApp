package com.odontoapp.servicio;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.odontoapp.dto.UsuarioDTO;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private final EmailService emailService;
    private final PacienteRepository pacienteRepository;

    public UsuarioServiceImpl(EmailService emailService, PasswordEncoder passwordEncoder, RolRepository rolRepository,
            UsuarioRepository usuarioRepository, PacienteRepository pacienteRepository) {
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;

    }

    @Override
    @Transactional
    public void guardarUsuario(UsuarioDTO usuarioDTO) {
        Usuario usuario;
        boolean esNuevo = usuarioDTO.getId() == null;
        String emailNuevo = usuarioDTO.getEmail();
        String emailOriginal = null;

        // --- 1. VALIDACIÓN PREVIA DE EMAIL ---
        Optional<Usuario> existenteConEmailOpt = usuarioRepository.findByEmailIgnorandoSoftDelete(emailNuevo);

        if (esNuevo) {
            // Creando: Verificar si el email ya existe (activo o eliminado)
            if (existenteConEmailOpt.isPresent()) {
                Usuario usuarioExistente = existenteConEmailOpt.get();
                if (usuarioExistente.isEliminado()) {
                    throw new DataIntegrityViolationException( // Lanzar excepción específica para restaurar
                            "EMAIL_ELIMINADO:" + usuarioExistente.getId() + ":" + emailNuevo);
                } else {
                    throw new DataIntegrityViolationException( // Lanzar excepción para email activo
                            "El email '" + emailNuevo + "' ya está en uso por otro usuario activo.");
                }
            }
            // Si no existe, preparamos el nuevo usuario
            usuario = new Usuario();

        } else {
            // Editando: Obtener usuario existente y verificar si el email cambió
            usuario = usuarioRepository.findById(usuarioDTO.getId())
                    .orElseThrow(
                            () -> new IllegalStateException("Usuario no encontrado con ID: " + usuarioDTO.getId()));
            emailOriginal = usuario.getEmail();

            if (!emailNuevo.equals(emailOriginal)) {
                // El email cambió, verificar si el NUEVO email ya existe (activo o eliminado)
                if (existenteConEmailOpt.isPresent()) {
                    Usuario usuarioConNuevoEmail = existenteConEmailOpt.get();
                    if (usuarioConNuevoEmail.isEliminado()) {
                        throw new DataIntegrityViolationException( // Lanzar para restaurar
                                "EMAIL_ELIMINADO:" + usuarioConNuevoEmail.getId() + ":" + emailNuevo);
                    } else {
                        throw new DataIntegrityViolationException( // Lanzar para email activo
                                "El email '" + emailNuevo + "' ya está en uso por otro usuario activo.");
                    }
                }
                // Validar si se intenta cambiar el email del admin principal
                if ("admin@odontoapp.com".equals(emailOriginal)) {
                    throw new IllegalArgumentException("No se puede cambiar el email del administrador principal.");
                }
            }
        }

        // --- 2. SI PASÓ LA VALIDACIÓN, PROCEDER A ACTUALIZAR/CREAR ---
        usuario.setNombreCompleto(usuarioDTO.getNombreCompleto());
        usuario.setEmail(emailNuevo);

        boolean enviarEmailTemporal = false;
        // Lógica de contraseña temporal (solo para nuevos)
        if (esNuevo) {
            String passwordTemporal = com.odontoapp.util.PasswordUtil.generarPasswordAleatoria();
            usuario.setPasswordTemporal(passwordTemporal);
            usuario.setPassword(passwordEncoder.encode(passwordTemporal));
            usuario.setDebeActualizarPassword(true);
            usuario.setEstaActivo(true);
            enviarEmailTemporal = true;
        }
        // NOTA: No cambiamos contraseña al editar desde aquí.

        // Roles (validación de quitar rol admin)
        boolean esAdminPrincipal = "admin@odontoapp.com".equals(usuario.getEmail());
        Rol rolAdmin = rolRepository.findByNombre("ADMIN").orElse(null);
        boolean intentaQuitarRolAdmin = (!esNuevo && rolAdmin != null
                && !usuarioDTO.getRoles().contains(rolAdmin.getId()));
        if (esAdminPrincipal && intentaQuitarRolAdmin) {
            throw new IllegalArgumentException("No se puede quitar el rol ADMIN al administrador principal.");
        }
        List<Rol> roles = rolRepository.findAllById(usuarioDTO.getRoles());
        usuario.setRoles(new HashSet<>(roles));

        // --- 3. GUARDAR ---
        Usuario usuarioGuardado;
        try {
            usuarioGuardado = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Si AÚN ASÍ ocurre un error de duplicado (puede pasar por concurrencia),
            // lo relanzamos con un mensaje más genérico pero claro.
            throw new DataIntegrityViolationException("Error al guardar: El email '" + emailNuevo + "' ya existe.", e);
        }

        // --- 4. ENVIAR EMAIL (SOLO SI ES NUEVO) ---
        if (enviarEmailTemporal && usuarioGuardado.getPasswordTemporal() != null) {
            try {
                emailService.enviarPasswordTemporal(
                        usuarioGuardado.getEmail(),
                        usuarioGuardado.getNombreCompleto(),
                        usuarioGuardado.getPasswordTemporal());
            } catch (Exception e) {
                System.err.println("Error al enviar email con contraseña temporal para " + usuarioGuardado.getEmail()
                        + ": " + e.getMessage());
                // Considera añadir un flash attribute de advertencia para el admin
                // No relanzamos la excepción para no deshacer el guardado del usuario
            }
        }
    }

    @Override
    public Page<Usuario> listarTodosLosUsuarios(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return usuarioRepository.findByKeyword(keyword, pageable);
        }
        return usuarioRepository.findAll(pageable);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    // En src/main/java/com/odontoapp/servicio/UsuarioServiceImpl.java

    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        // Buscar usuario (ignorando soft delete para asegurar que lo encontramos)
        // Necesitaríamos un método findByIdIgnorandoSoftDelete en UsuarioRepository si
        // quisiéramos encontrar uno ya eliminado.
        // Por ahora, asumimos que solo eliminamos usuarios no eliminados previamente.
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));

        // Si ya está eliminado, no hacer nada más
        if (usuario.isEliminado()) {
            System.out.println(">>> Usuario " + usuario.getEmail() + " ya estaba eliminado lógicamente.");
            return;
        }

        // Regla: No eliminar admin principal
        if ("admin@odontoapp.com".equals(usuario.getEmail())) {
            throw new UnsupportedOperationException("No se puede eliminar al administrador principal.");
        }

        // 1. Soft delete del Paciente asociado PRIMERO (si existe y no está eliminado)
        if (usuario.getPaciente() != null && !usuario.getPaciente().isEliminado()) {
            Long pacienteId = usuario.getPaciente().getId();
            try {
                pacienteRepository.deleteById(pacienteId); // Activa @SQLDelete de Paciente
                System.out.println(
                        ">>> Paciente asociado " + pacienteId + " eliminado (soft delete) por cascada desde usuario.");
            } catch (Exception e) {
                System.err.println("Error Crítico: No se pudo eliminar (soft delete) el paciente asociado " + pacienteId
                        + ". Cancelando eliminación del usuario. Error: " + e.getMessage());
                // Detener la operación si falla eliminar el paciente dependiente.
                throw new RuntimeException("No se pudo eliminar el paciente asociado. Operación cancelada.", e);
            }
        } else if (usuario.getPaciente() != null && usuario.getPaciente().isEliminado()) {
            System.out.println(">>> Paciente asociado ya estaba eliminado lógicamente.");
        }

        // 2. Soft delete del Usuario DESPUÉS del paciente (si aplica)
        try {
            usuarioRepository.deleteById(id); // Activa @SQLDelete de Usuario
            System.out.println(">>> Usuario " + usuario.getEmail() + " eliminado (soft delete) con éxito.");
        } catch (Exception e) {
            // Si llega aquí, es un error inesperado al marcar el usuario como eliminado.
            System.err.println(
                    "Error Crítico al intentar soft delete del usuario " + usuario.getEmail() + ": " + e.getMessage());
            // Relanzar para que la transacción haga rollback
            throw new RuntimeException("Error al marcar el usuario como eliminado.", e);
        }
    }

    @Override
    public void cambiarEstadoUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));
        // Llama al método sobrecargado con el estado opuesto
        cambiarEstadoUsuario(id, !usuario.isEstaActivo());
    }

    @Override
    public void cambiarEstadoUsuario(Long id, boolean activar)
            throws UnsupportedOperationException, IllegalStateException {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));

        // Regla de negocio: No se puede desactivar al admin principal
        if ("admin@odontoapp.com".equals(usuario.getEmail()) && !activar) {
            throw new UnsupportedOperationException("No se puede desactivar al administrador principal.");
        }

        // Aquí podrías añadir la lógica para evitar auto-desactivación si esta acción
        // la pudiese realizar el propio usuario, pero como es desde admin, no es
        // estrictamente necesario aquí.
        // La validación de auto-cambio ya está en el controlador para la acción de
        // alternar estado.

        usuario.setEstaActivo(activar);
        usuarioRepository.save(usuario);
    }

    @Override
    public void procesarLoginFallido(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            if (usuario.getIntentosFallidos() >= MAX_INTENTOS_FALLIDOS) {
                usuario.setFechaBloqueo(LocalDateTime.now());
            }
            usuarioRepository.save(usuario);
        }
    }

    @Override
    public void resetearIntentosFallidos(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setIntentosFallidos(0);
            usuario.setFechaBloqueo(null);
            usuarioRepository.save(usuario);
        }
    }

    @Override
    @Transactional
    public void restablecerUsuario(Long id) {
        Usuario usuario = usuarioRepository.findByIdIgnorandoSoftDelete(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));

        if (!usuario.isEliminado()) {
            throw new IllegalStateException("El usuario con email " + usuario.getEmail() + " no está eliminado.");
        }

        // Restablecer estado
        usuario.setEliminado(false);
        usuario.setFechaEliminacion(null);
        usuario.setEstaActivo(true); // Reactivarlo
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);

        // Generar nueva contraseña temporal y forzar cambio
        String passwordTemporal = com.odontoapp.util.PasswordUtil.generarPasswordAleatoria();
        usuario.setPasswordTemporal(passwordTemporal);
        usuario.setPassword(passwordEncoder.encode(passwordTemporal));
        usuario.setDebeActualizarPassword(true);

        usuarioRepository.save(usuario);

        // Enviar email con la nueva contraseña temporal
        try {
            emailService.enviarPasswordTemporal(
                    usuario.getEmail(),
                    usuario.getNombreCompleto(),
                    passwordTemporal);
        } catch (Exception e) {
            System.err.println("Error al enviar email con nueva contraseña temporal durante restauración para "
                    + usuario.getEmail() + ": " + e.getMessage());
            // Considera cómo notificar este fallo (ej. mensaje flash adicional)
            throw new RuntimeException(
                    "Usuario restablecido, pero ocurrió un error al enviar el email con la nueva contraseña temporal.",
                    e);
        }
    }

}