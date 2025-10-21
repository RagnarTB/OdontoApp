package com.odontoapp.configuracion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermisoRepository permisoRepository; // Añadido

    public DataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder, PermisoRepository permisoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.permisoRepository = permisoRepository; // Añadido
    }

    @Override
    public void run(String... args) throws Exception {
        // --- NUEVA SECCIÓN: CREAR PERMISOS ---
        List<String> modulos = Arrays.asList("USUARIOS", "ROLES", "PACIENTES", "CITAS", "SERVICIOS", "FACTURACION",
                "INVENTARIO", "REPORTES", "CONFIGURACION");
        List<String> acciones = Arrays.asList("VER_LISTA", "VER_DETALLE", "CREAR", "EDITAR", "ELIMINAR");

        for (String modulo : modulos) {
            for (String accion : acciones) {
                permisoRepository.findByModuloAndAccion(modulo, accion).orElseGet(() -> {
                    Permiso permiso = new Permiso();
                    permiso.setModulo(modulo);
                    permiso.setAccion(accion);
                    return permisoRepository.save(permiso);
                });
            }
        }

        // Crear rol de ADMIN si no existe
        Rol adminRol = rolRepository.findByNombre("ADMIN").orElseGet(() -> {
            Rol nuevoRol = new Rol();
            nuevoRol.setNombre("ADMIN");
            // Asignar todos los permisos al rol ADMIN
            nuevoRol.setPermisos(new HashSet<>(permisoRepository.findAll()));
            return rolRepository.save(nuevoRol);
        });

        // Crear usuario administrador si no existe
        if (usuarioRepository.findByEmail("admin@odontoapp.com").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setNombreCompleto("Administrador del Sistema");
            admin.setEmail("admin@odontoapp.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of(adminRol));
            admin.setEstaActivo(true);

            usuarioRepository.save(admin);
            System.out.println(">>> Usuario administrador creado con éxito!");
        }
    }
}