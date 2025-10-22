package com.odontoapp.servicio;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.odontoapp.dto.UsuarioDTO;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_INTENTOS_FALLIDOS = 5;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Añade este método privado
    private void validarComplejidadPassword(String password) {
        if (password == null || password.isEmpty()) {
            // No validar si está vacío (puede ser una edición sin cambio de contraseña)
            return;
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        // Añadir más reglas aquí si es necesario (regex, etc.)
    }

    @Override
    public void guardarUsuario(UsuarioDTO usuarioDTO) {
        Usuario usuario;
        boolean esNuevo = usuarioDTO.getId() == null;

        if (!esNuevo) {
            usuario = usuarioRepository.findById(usuarioDTO.getId())
                    .orElseThrow(
                            () -> new IllegalStateException("Usuario no encontrado con ID: " + usuarioDTO.getId()));
        } else {
            usuario = new Usuario();
            // Contraseña obligatoria solo para nuevos
            if (usuarioDTO.getPassword() == null || usuarioDTO.getPassword().isEmpty()) {
                throw new IllegalArgumentException("La contraseña es obligatoria para nuevos usuarios.");
            }
        }

        // Validar complejidad ANTES de codificar y guardar (si se proporcionó
        // contraseña)
        if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty()) {
            validarComplejidadPassword(usuarioDTO.getPassword()); // <-- LLAMADA AL MÉTODO
            usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        }

        usuario.setNombreCompleto(usuarioDTO.getNombreCompleto());
        usuario.setEmail(usuarioDTO.getEmail());

        List<Rol> roles = rolRepository.findAllById(usuarioDTO.getRoles());
        usuario.setRoles(new HashSet<>(roles));

        usuarioRepository.save(usuario);
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

    @Override
    public void eliminarUsuario(Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setRoles(null);
            usuarioRepository.save(usuario);
            usuarioRepository.deleteById(id);
        }
    }

    @Override
    public void cambiarEstadoUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        // Regla de negocio: No se puede desactivar al admin principal
        if ("admin@odontoapp.com".equals(usuario.getEmail())) {
            throw new UnsupportedOperationException("No se puede cambiar el estado del administrador principal.");
        }

        usuario.setEstaActivo(!usuario.isEstaActivo());
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