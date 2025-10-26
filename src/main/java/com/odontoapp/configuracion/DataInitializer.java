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

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermisoRepository permisoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository; // NUEVO
    private final CategoriaProcedimientoRepository categoriaProcedimientoRepository; // <-- AÑADIR
    private final ProcedimientoRepository procedimientoRepository;
    private final CategoriaInsumoRepository categoriaInsumoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final InsumoRepository insumoRepository;
    private final TipoMovimientoRepository tipoMovimientoRepository;
    private final MotivoMovimientoRepository motivoMovimientoRepository;// NUEVO

    public DataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder, PermisoRepository permisoRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            CategoriaProcedimientoRepository categoriaProcedimientoRepository,
            ProcedimientoRepository procedimientoRepository, CategoriaInsumoRepository categoriaInsumoRepository,
            UnidadMedidaRepository unidadMedidaRepository,
            InsumoRepository insumoRepository,
            TipoMovimientoRepository tipoMovimientoRepository, // <-- Pide el Repositorio
            MotivoMovimientoRepository motivoMovimientoRepository) { // NUEVO
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.permisoRepository = permisoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository; // NUEVO
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

        // 🔥 --- CREAR TIPOS DE DOCUMENTO BASE ---
        crearTipoDocumento("DNI", "DNI", true);
        crearTipoDocumento("RUC", "RUC", false);
        crearTipoDocumento("Carnet de Extranjería", "C.E.", false);

        // 🔥 --- CREAR CATEGORÍAS DE SERVICIOS ---
        System.out.println(">>> Creando categorías de servicios...");
        CategoriaProcedimiento consulta = crearCategoriaSiNoExiste("Consulta", "Diagnóstico y planificación",
                "fas fa-stethoscope", "#3498db");
        CategoriaProcedimiento preventiva = crearCategoriaSiNoExiste("Preventiva", "Limpiezas y profilaxis",
                "fas fa-shield-alt", "#2ecc71");
        CategoriaProcedimiento restaurativa = crearCategoriaSiNoExiste("Restaurativa", "Curaciones y empastes",
                "fas fa-tooth", "#f1c40f");
        CategoriaProcedimiento endodoncia = crearCategoriaSiNoExiste("Endodoncia", "Tratamiento de conductos",
                "fas fa-syringe", "#e74c3c");
        CategoriaProcedimiento cirugia = crearCategoriaSiNoExiste("Cirugía", "Extracciones y cirugías menores",
                "fas fa-cut", "#9b59b6");
        CategoriaProcedimiento ortodoncia = crearCategoriaSiNoExiste("Ortodoncia", "Brackets y alineadores",
                "fas fa-teeth", "#e67e22");
        CategoriaProcedimiento estetica = crearCategoriaSiNoExiste("Estética", "Blanqueamientos y carillas",
                "fas fa-smile", "#1abc9c");
        CategoriaProcedimiento protesis = crearCategoriaSiNoExiste("Prótesis", "Coronas y puentes",
                "fas fa-puzzle-piece", "#34495e");

        // 🔥 --- CREAR SERVICIOS DE EJEMPLO ---
        System.out.println(">>> Creando servicios de ejemplo...");
        crearProcedimientoSiNoExiste("CON-001", "Consulta General", new BigDecimal("80.00"), 30, consulta);
        crearProcedimientoSiNoExiste("PRE-001", "Limpieza Dental (Profilaxis)", new BigDecimal("120.00"), 45,
                preventiva);
        crearProcedimientoSiNoExiste("RES-001", "Restauración con Resina", new BigDecimal("150.00"), 60, restaurativa);
        crearProcedimientoSiNoExiste("ENDO-001", "Endodoncia Unirradicular", new BigDecimal("400.00"), 90, endodoncia);
        crearProcedimientoSiNoExiste("CIR-001", "Extracción Simple", new BigDecimal("150.00"), 30, cirugia);

        // 🔥 --- CREAR UNIDADES DE MEDIDA ---
        UnidadMedida unidad = crearUnidadSiNoExiste("Unidad", "und");
        UnidadMedida caja = crearUnidadSiNoExiste("Caja", "cja");
        UnidadMedida gramo = crearUnidadSiNoExiste("Gramo", "g");
        UnidadMedida ml = crearUnidadSiNoExiste("Mililitro", "ml");

        // 🔥 --- CREAR CATEGORÍAS DE INSUMOS ---
        CategoriaInsumo anestesicos = crearCategoriaInsumoSiNoExiste("Anestésicos",
                "Anestésicos locales y complementos");
        CategoriaInsumo restauracion = crearCategoriaInsumoSiNoExiste("Material de Restauración",
                "Resinas, amalgamas, cementos");
        CategoriaInsumo proteccion = crearCategoriaInsumoSiNoExiste("Protección Personal",
                "Guantes, mascarillas, gorros");

        // 🔥 --- ¡NUEVO! CREAR INSUMOS DE EJEMPLO ---
        System.out.println(">>> Creando insumos de ejemplo...");
        crearInsumoSiNoExiste("ANES-LIDO-01", "Anestesia Lidocaina 2%", "Laboratorio XYZ", new BigDecimal("10"),
                new BigDecimal("5.50"), anestesicos, unidad);
        crearInsumoSiNoExiste("PROT-GUAN-01", "Guantes de Látex Medianos", "SafeTouch", new BigDecimal("100"),
                new BigDecimal("0.80"), proteccion, caja);
        crearInsumoSiNoExiste("REST-COMP-A2", "Resina Compuesta A2", "DentalFill", new BigDecimal("20"),
                new BigDecimal("45.00"), restauracion, gramo);

        System.out.println(">>> Creando tipos y motivos de movimiento...");
        TipoMovimiento entrada = crearTipoMovimiento("Entrada", "ENTRADA", TipoMovimiento.AfectaStock.SUMA);
        TipoMovimiento salida = crearTipoMovimiento("Salida", "SALIDA", TipoMovimiento.AfectaStock.RESTA);

        tipoMovimientoRepository.saveAll(List.of(entrada, salida)); // Usar saveAll es más eficiente

        crearMotivoSiNoExiste("Compra a proveedor", entrada);
        crearMotivoSiNoExiste("Uso en procedimiento", salida);
        crearMotivoSiNoExiste("Vencimiento o merma", salida);
        crearMotivoSiNoExiste("Ajuste de inventario", salida);

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

    private CategoriaProcedimiento crearCategoriaSiNoExiste(String nombre, String desc, String icono, String color) {
        return categoriaProcedimientoRepository.findByNombre(nombre).orElseGet(() -> {
            CategoriaProcedimiento cat = new CategoriaProcedimiento();
            cat.setNombre(nombre);
            cat.setDescripcion(desc);
            cat.setIcono(icono);
            cat.setColor(color);
            // Puedes añadir 'orden' si lo necesitas
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
            // El stock actual se inicializa en 0 por defecto, lo cual está bien.
            // Se ajustará con los movimientos de inventario.
            return insumoRepository.save(insumo);
        });
    }

    private TipoMovimiento crearTipoMovimiento(String nombre, String codigo, TipoMovimiento.AfectaStock afecta) {
        // Este método crea un objeto TipoMovimiento pero NO lo guarda.
        // Lo guardaremos en el método run() usando saveAll para ser más eficientes.
        TipoMovimiento tm = new TipoMovimiento();
        tm.setNombre(nombre);
        tm.setCodigo(codigo);
        tm.setAfectaStock(afecta);
        return tm;
    }

    private void crearMotivoSiNoExiste(String nombre, TipoMovimiento tipo) {
        // Busca si el motivo ya existe por su nombre.
        // Si no existe (orElseGet), crea uno nuevo, le asigna su tipo y lo guarda.
        motivoMovimientoRepository.findByNombre(nombre).orElseGet(() -> {
            MotivoMovimiento mm = new MotivoMovimiento();
            mm.setNombre(nombre);
            mm.setTipoMovimiento(tipo);
            return motivoMovimientoRepository.save(mm);
        });
    }
}