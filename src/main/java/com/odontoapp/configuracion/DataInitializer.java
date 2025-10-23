// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\configuracion\DataInitializer.java
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
import com.odontoapp.entidad.TipoDocumento; // NUEVO
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository; // NUEVO
import com.odontoapp.repositorio.UsuarioRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermisoRepository permisoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository; // NUEVO

    public DataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder, PermisoRepository permisoRepository,
            TipoDocumentoRepository tipoDocumentoRepository) { // NUEVO
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.permisoRepository = permisoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository; // NUEVO
    }

    @Override
    public void run(String... args) throws Exception {

        // 🔥 --- CREAR TIPOS DE DOCUMENTO BASE ---
        crearTipoDocumento("DNI", "DNI", true);
        crearTipoDocumento("RUC", "RUC", false);
        crearTipoDocumento("Carnet de Extranjería", "C.E.", false);

        // --- CREAR PERMISOS BASE ---
        List<String> modulos = Arrays.asList(
                "USUARIOS", "ROLES", "PACIENTES", "CITAS",
                "SERVICIOS", "FACTURACION", "INVENTARIO",
                "REPORTES", "CONFIGURACION");

        List<String> acciones = Arrays.asList(
                "VER_LISTA", "VER_DETALLE", "CREAR", "EDITAR", "ELIMINAR");
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

        // 🔥 --- CREAR ROLES ADICIONALES ---
        crearRolSiNoExiste("PACIENTE",
                permisoRepository.findByModuloAndAccion("CITAS", "VER_LISTA").map(Set::of).orElse(Set.of()));
        crearRolSiNoExiste("ODONTOLOGO", new HashSet<>(permisoRepository.findAll())); // Por ahora todos los permisos
        crearRolSiNoExiste("RECEPCIONISTA", new HashSet<>(permisoRepository.findAll())); // Por ahora todos los permisos
        crearRolSiNoExiste("ALMACEN", new HashSet<>(permisoRepository.findAll())); // Por ahora todos los permisos

        // --- CREAR ROL ADMIN (asegura todos los permisos) ---
        Rol adminRol = rolRepository.findByNombre("ADMIN").orElseGet(() -> {
            Rol nuevoRol = new Rol();
            nuevoRol.setNombre("ADMIN");
            nuevoRol.setPermisos(new HashSet<>(permisoRepository.findAll()));
            return rolRepository.save(nuevoRol);
        });

        // --- CREAR USUARIO ADMIN ---
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

    // Métodos helper para reducir duplicidad
    private TipoDocumento crearTipoDocumento(String nombre, String codigo, boolean esNacional) {
        return tipoDocumentoRepository.findByCodigo(codigo).orElseGet(() -> {
            TipoDocumento nuevo = new TipoDocumento();
            nuevo.setNombre(nombre);
            nuevo.setCodigo(codigo);
            nuevo.setEsNacional(esNacional);
            return tipoDocumentoRepository.save(nuevo);
        });
    }

    private Rol crearRolSiNoExiste(String nombre, Set<Permiso> permisos) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> {
            Rol nuevoRol = new Rol();
            nuevoRol.setNombre(nombre);
            nuevoRol.setPermisos(permisos);
            return rolRepository.save(nuevoRol);
        });
    }
}