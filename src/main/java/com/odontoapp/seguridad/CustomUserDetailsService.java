package com.odontoapp.seguridad;

import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.odontoapp.servicio.UsuarioService; // Asegúrate de importar tu servicio
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // Inyectamos el servicio en lugar del repositorio directamente
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    private static final int TIEMPO_BLOQUEO_MINUTOS = 15;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        if (usuario.getFechaBloqueo() != null) {
            if (LocalDateTime.now().isBefore(usuario.getFechaBloqueo().plusMinutes(TIEMPO_BLOQUEO_MINUTOS))) {
                throw new LockedException("La cuenta está bloqueada temporalmente.");
            } else {
                usuarioService.resetearIntentosFallidos(email);
                usuario.setFechaBloqueo(null); // Actualizamos el estado localmente para la sesión actual
            }
        }

        if (!usuario.isEstaActivo()) {
            throw new DisabledException("El usuario está inactivo.");
        }

        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                getAuthorities(usuario.getRoles()) // Usamos el nuevo método
        );
    }

    // --- ESTE ES EL MÉTODO CLAVE ---
    private Collection<? extends GrantedAuthority> getAuthorities(Set<Rol> roles) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (Rol rol : roles) {
            // Añadimos el nombre del rol (ej. "ROLE_ADMIN")
            authorities.add(new SimpleGrantedAuthority(rol.getNombre()));
            // Añadimos todos los permisos asociados a ese rol
            if (rol.getPermisos() != null) {
                for (Permiso permiso : rol.getPermisos()) {
                    authorities.add(new SimpleGrantedAuthority(permiso.getAccion() + "_" + permiso.getModulo()));
                }
            }
        }
        return authorities;
    }
}