package com.odontoapp.servicio;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.odontoapp.dto.UsuarioDTO;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
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

    public UsuarioServiceImpl(EmailService emailService, PasswordEncoder passwordEncoder, RolRepository rolRepository,
            UsuarioRepository usuarioRepository) {
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // Añade este método privado
    private void validarComplejidadPassword(String password) {
        if (password == null || password.isEmpty()) {
            return; // No validar si está vacío
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        // Añadir más reglas aquí si es necesario (regex, etc.)
    }

    @Override
    @Transactional // Asegura que toda la operación sea atómica
    public void guardarUsuario(UsuarioDTO usuarioDTO) {
        Usuario usuario;
        boolean esNuevo = usuarioDTO.getId() == null;
        String emailNuevo = usuarioDTO.getEmail();
        String emailOriginal = null;
        boolean emailCambiado = false;

        // --- 🔍 VALIDACIÓN DE EMAIL Y RECUPERACIÓN ---
        Optional<Usuario> existenteConEmail = usuarioRepository.findByEmailIgnorandoSoftDelete(emailNuevo);

        if (!esNuevo) {
            usuario = usuarioRepository.findById(usuarioDTO.getId())
                    .orElseThrow(
                            () -> new IllegalStateException("Usuario no encontrado con ID: " + usuarioDTO.getId()));

            emailOriginal = usuario.getEmail();
            if (!emailNuevo.equals(emailOriginal)) {
                emailCambiado = true;
            }
        } else {
            usuario = new Usuario();
        }

        // 1. Validar duplicidad
        if (existenteConEmail.isPresent()) {
            if (esNuevo || !existenteConEmail.get().getId().equals(usuarioDTO.getId())) {
                throw new DataIntegrityViolationException(
                        "El email '" + emailNuevo + "' ya se encuentra registrado en el sistema " +
                                "(puede estar inactivo o eliminado).");
            }
        }

        // 2. Reglas de ADMIN PRINCIPAL
        if (emailOriginal != null && "admin@odontoapp.com".equals(emailOriginal) && emailCambiado) {
            throw new IllegalArgumentException("No se puede cambiar el email del administrador principal.");
        }

        // 3. DATOS GENERALES
        usuario.setNombreCompleto(usuarioDTO.getNombreCompleto());
        usuario.setEmail(emailNuevo);

        // 4. LÓGICA DE CONTRASEÑA
        if (esNuevo) {
            if (usuarioDTO.getPassword() == null || usuarioDTO.getPassword().isEmpty()) {
                throw new IllegalArgumentException("La contraseña es obligatoria para nuevos usuarios.");
            }
            validarComplejidadPassword(usuarioDTO.getPassword());
            usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
            usuario.setEstaActivo(true); // Usuarios creados por admin nacen activos, pero el Admin los inactiva si usa
                                         // el flujo del paciente
        }

        // 5. 🔥 CORRECCIÓN: INACTIVAR Y ENVIAR TOKEN si el email cambió
        if (emailCambiado) {
            usuario.setEstaActivo(false);
            usuario.setVerificationToken(UUID.randomUUID().toString());

            Usuario usuarioGuardado = usuarioRepository.save(usuario);

            // Reutilizamos el flujo de activación de Admin, asumiendo que es personal
            emailService.enviarEmailActivacionAdmin(
                    usuarioGuardado.getEmail(),
                    usuarioGuardado.getNombreCompleto(),
                    usuarioGuardado.getVerificationToken());
        }

        // 6. VALIDACIÓN DE ROL y ASIGNACIÓN (sin cambios)
        boolean esAdminPrincipal = "admin@odontoapp.com".equals(usuario.getEmail());
        Rol rolAdmin = rolRepository.findByNombre("ADMIN").orElse(null);
        boolean intentaQuitarRolAdmin = (rolAdmin != null && !usuarioDTO.getRoles().contains(rolAdmin.getId()));
        if (esAdminPrincipal && intentaQuitarRolAdmin) {
            throw new IllegalArgumentException("No se puede quitar el rol ADMIN al administrador principal.");
        }
        List<Rol> roles = rolRepository.findAllById(usuarioDTO.getRoles());
        usuario.setRoles(new HashSet<>(roles));

        // Guardar (si no se guardó ya por el cambio de email)
        if (!emailCambiado) {
            usuarioRepository.save(usuario);
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
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + id));

        if ("admin@odontoapp.com".equals(usuario.getEmail())) {
            throw new UnsupportedOperationException("No se puede eliminar al administrador principal.");
        }

        // Validar si el usuario está asociado a un Paciente ACTIVO
        if (usuario.getPaciente() != null && !usuario.getPaciente().isEliminado()) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar un usuario asociado a un paciente activo. Elimine primero el paciente.");
        }

        // Llama al deleteById que activará @SQLDelete
        usuarioRepository.deleteById(id);
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

}