// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\configuracion\DataInitializer.java
package com.odontoapp.configuracion;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.odontoapp.entidad.CategoriaProcedimiento;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.Permiso;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.TipoDocumento;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.CategoriaProcedimientoRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.PermisoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.RolRepository;
import com.odontoapp.repositorio.TipoDocumentoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.entidad.CategoriaInsumo;
import com.odontoapp.entidad.UnidadMedida;
import com.odontoapp.repositorio.CategoriaInsumoRepository;
import com.odontoapp.repositorio.UnidadMedidaRepository;
import com.odontoapp.entidad.TipoMovimiento;
import com.odontoapp.entidad.MotivoMovimiento;
import com.odontoapp.repositorio.TipoMovimientoRepository;
import com.odontoapp.repositorio.MotivoMovimientoRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    // ... (Inyecciones de dependencias - sin cambios) ...
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermisoRepository permisoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final CategoriaProcedimientoRepository categoriaProcedimientoRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final CategoriaInsumoRepository categoriaInsumoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final InsumoRepository insumoRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;

    public DataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                           PasswordEncoder passwordEncoder, PermisoRepository permisoRepository,
                           TipoDocumentoRepository tipoDocumentoRepository,
                           CategoriaProcedimientoRepository categoriaProcedimientoRepository,
                           ProcedimientoRepository procedimientoRepository, CategoriaInsumoRepository categoriaInsumoRepository,
                           UnidadMedidaRepository unidadMedidaRepository,
                           InsumoRepository insumoRepository,
                           TipoMovimientoRepository tipoMovimientoRepository,
                           MotivoMovimientoRepository motivoMovimientoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.permisoRepository = permisoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.categoriaProcedimientoRepository = categoriaProcedimientoRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.categoriaInsumoRepository = categoriaInsumoRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.insumoRepository = insumoRepository;
        this.motivoMovimientoRepository = motivoMovimientoRepository;
        this.tipoMovimientoRepository = tipoMovimientoRepository;
    }


    @Override
    public void run(String... args) throws Exception {

        // ... (Creación de Tipos de Documento - sin cambios) ...
        crearTipoDocumento("DNI", "DNI", true);
        crearTipoDocumento("RUC", "RUC", false);
        crearTipoDocumento("Carnet de Extranjería", "C.E.", false);

        // ... (Creación de Categorías y Procedimientos - sin cambios) ...
        System.out.println(">>> Creando categorías de servicios...");
        CategoriaProcedimiento consulta = crearCategoriaSiNoExiste("Consulta", "Diagnóstico y planificación", "fas fa-stethoscope", "#3498db");
        // ... (otras categorías) ...
        System.out.println(">>> Creando servicios de ejemplo...");
        crearProcedimientoSiNoExiste("CON-001", "Consulta General", new BigDecimal("80.00"), 30, consulta);
        // ... (otros procedimientos) ...

        // ... (Creación de Unidades y Categorías de Insumo - sin cambios) ...
        UnidadMedida unidad = crearUnidadSiNoExiste("Unidad", "und");
        // ... (otras unidades) ...
        CategoriaInsumo anestesicos = crearCategoriaInsumoSiNoExiste("Anestésicos", "Anestésicos locales y complementos");
        // ... (otras categorías insumo) ...

        // ... (Creación de Insumos - sin cambios) ...
        System.out.println(">>> Creando insumos de ejemplo...");
        crearInsumoSiNoExiste("ANES-LIDO-01", "Anestesia Lidocaina 2%", "Laboratorio XYZ", new BigDecimal("10"), new BigDecimal("5.50"), anestesicos, unidad);
        // ... (otros insumos) ...

        // --- 👇 CORRECCIÓN AQUÍ 👇 ---
        System.out.println(">>> Creando tipos y motivos de movimiento...");

        TipoMovimiento entrada = null; // Declarar fuera del if para usarlo después
        TipoMovimiento salida = null;

        // Solo crear si la tabla está vacía
        if (tipoMovimientoRepository.count() == 0) {
            System.out.println(">>> Guardando tipos de movimiento iniciales...");
            entrada = crearTipoMovimiento("Entrada", "ENTRADA", TipoMovimiento.AfectaStock.SUMA);
            salida = crearTipoMovimiento("Salida", "SALIDA", TipoMovimiento.AfectaStock.RESTA);
            tipoMovimientoRepository.saveAll(List.of(entrada, salida));
        } else {
            // Si ya existen, solo búscarlos para usarlos en los motivos
            System.out.println(">>> Tipos de movimiento ya existen, cargándolos...");
            entrada = tipoMovimientoRepository.findByCodigo("ENTRADA").orElseThrow(() -> new RuntimeException("Tipo ENTRADA no encontrado"));
            salida = tipoMovimientoRepository.findByCodigo("SALIDA").orElseThrow(() -> new RuntimeException("Tipo SALIDA no encontrado"));
        }

        // Crear motivos (esto está bien, usa orElseGet)
        crearMotivoSiNoExiste("Compra a proveedor", entrada);
        crearMotivoSiNoExiste("Uso en procedimiento", salida);
        crearMotivoSiNoExiste("Vencimiento o merma", salida);
        crearMotivoSiNoExiste("Ajuste de inventario", salida);
        // --- 👆 FIN DE LA CORRECCIÓN 👆 ---

        // ... (Creación de Permisos - sin cambios) ...
        List<String> modulos = Arrays.asList("USUARIOS", "ROLES", "PACIENTES", "CITAS", "SERVICIOS", "FACTURACION", "INVENTARIO", "REPORTES", "CONFIGURACION");
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

        // ... (Creación de Roles y Usuario Admin - sin cambios) ...
        crearRolSiNoExiste("PACIENTE", permisoRepository.findByModuloAndAccion("CITAS", "VER_LISTA").map(Set::of).orElse(Set.of()));
        crearRolSiNoExiste("ODONTOLOGO", new HashSet<>(permisoRepository.findAll()));
        crearRolSiNoExiste("RECEPCIONISTA", new HashSet<>(permisoRepository.findAll()));
        crearRolSiNoExiste("ALMACEN", new HashSet<>(permisoRepository.findAll()));

        Rol adminRol = rolRepository.findByNombre("ADMIN").orElseGet(() -> {
            Rol nuevoRol = new Rol();
            nuevoRol.setNombre("ADMIN");
            nuevoRol.setPermisos(new HashSet<>(permisoRepository.findAll()));
            return rolRepository.save(nuevoRol);
        });

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

    // ... (Métodos helper - sin cambios) ...
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

    private CategoriaProcedimiento crearCategoriaSiNoExiste(String nombre, String desc, String icono, String color) {
        return categoriaProcedimientoRepository.findByNombre(nombre).orElseGet(() -> {
            CategoriaProcedimiento cat = new CategoriaProcedimiento();
            cat.setNombre(nombre);
            cat.setDescripcion(desc);
            cat.setIcono(icono);
            cat.setColor(color);
            return categoriaProcedimientoRepository.save(cat);
        });
    }

    private void crearProcedimientoSiNoExiste(String codigo, String nombre, BigDecimal precio, int duracion,
                                              CategoriaProcedimiento categoria) {
        procedimientoRepository.findByCodigo(codigo).orElseGet(() -> {
            Procedimiento proc = new Procedimiento();
            proc.setCodigo(codigo);
            proc.setNombre(nombre);
            proc.setPrecioBase(precio);
            proc.setDuracionBaseMinutos(duracion);
            proc.setCategoria(categoria);
            return procedimientoRepository.save(proc);
        });
    }

    private UnidadMedida crearUnidadSiNoExiste(String nombre, String abreviatura) {
        return unidadMedidaRepository.findByAbreviatura(abreviatura).orElseGet(() -> {
            UnidadMedida um = new UnidadMedida();
            um.setNombre(nombre);
            um.setAbreviatura(abreviatura);
            return unidadMedidaRepository.save(um);
        });
    }

    private CategoriaInsumo crearCategoriaInsumoSiNoExiste(String nombre, String descripcion) {
        return categoriaInsumoRepository.findByNombre(nombre).orElseGet(() -> {
            CategoriaInsumo ci = new CategoriaInsumo();
            ci.setNombre(nombre);
            ci.setDescripcion(descripcion);
            return categoriaInsumoRepository.save(ci);
        });
    }

    private void crearInsumoSiNoExiste(String codigo, String nombre, String marca, BigDecimal stockMinimo,
                                       BigDecimal precio, CategoriaInsumo categoria, UnidadMedida unidad) {
        insumoRepository.findByCodigo(codigo).orElseGet(() -> {
            Insumo insumo = new Insumo();
            insumo.setCodigo(codigo);
            insumo.setNombre(nombre);
            insumo.setMarca(marca);
            insumo.setStockMinimo(stockMinimo);
            insumo.setPrecioUnitario(precio);
            insumo.setCategoria(categoria);
            insumo.setUnidadMedida(unidad);
            return insumoRepository.save(insumo);
        });
    }

    private TipoMovimiento crearTipoMovimiento(String nombre, String codigo, TipoMovimiento.AfectaStock afecta) {
        TipoMovimiento tm = new TipoMovimiento();
        tm.setNombre(nombre);
        tm.setCodigo(codigo);
        tm.setAfectaStock(afecta);
        return tm;
    }

    private void crearMotivoSiNoExiste(String nombre, TipoMovimiento tipo) {
        motivoMovimientoRepository.findByNombre(nombre).orElseGet(() -> {
            MotivoMovimiento mm = new MotivoMovimiento();
            mm.setNombre(nombre);
            mm.setTipoMovimiento(tipo);
            return motivoMovimientoRepository.save(mm);
        });
    }
}