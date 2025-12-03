// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\configuracion\DataInitializer.java
package com.odontoapp.configuracion;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.entidad.Paciente;
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
import com.odontoapp.entidad.EstadoCita;
import com.odontoapp.entidad.EstadoPago;
import com.odontoapp.entidad.MetodoPago;
import com.odontoapp.repositorio.EstadoCitaRepository;
import com.odontoapp.repositorio.EstadoPagoRepository;
import com.odontoapp.repositorio.MetodoPagoRepository;
import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;

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
        private final EstadoCitaRepository estadoCitaRepository;
        private final EstadoPagoRepository estadoPagoRepository;
        private final MetodoPagoRepository metodoPagoRepository;
        private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
        private final PacienteRepository pacienteRepository;

        public DataInitializer(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                        PasswordEncoder passwordEncoder, PermisoRepository permisoRepository,
                        TipoDocumentoRepository tipoDocumentoRepository,
                        CategoriaProcedimientoRepository categoriaProcedimientoRepository,
                        ProcedimientoRepository procedimientoRepository,
                        CategoriaInsumoRepository categoriaInsumoRepository,
                        UnidadMedidaRepository unidadMedidaRepository,
                        InsumoRepository insumoRepository,
                        TipoMovimientoRepository tipoMovimientoRepository,
                        MotivoMovimientoRepository motivoMovimientoRepository,
                        EstadoCitaRepository estadoCitaRepository,
                        EstadoPagoRepository estadoPagoRepository,
                        MetodoPagoRepository metodoPagoRepository,
                        ProcedimientoInsumoRepository procedimientoInsumoRepository,
                        PacienteRepository pacienteRepository) {
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
                this.estadoCitaRepository = estadoCitaRepository;
                this.estadoPagoRepository = estadoPagoRepository;
                this.metodoPagoRepository = metodoPagoRepository;
                this.procedimientoInsumoRepository = procedimientoInsumoRepository;
                this.pacienteRepository = pacienteRepository;
        }

        @Override
        public void run(String... args) throws Exception {

                // ... (Creación de Tipos de Documento - sin cambios) ...
                crearTipoDocumento("DNI", "DNI", true);
                crearTipoDocumento("RUC", "RUC", false);
                crearTipoDocumento("Carnet de Extranjería", "C.E.", false);

                // === CREACIÓN DE CATEGORÍAS DE SERVICIOS DENTALES ===
                System.out.println(">>> Creando categorías de servicios dentales...");

                CategoriaProcedimiento consulta = crearCategoriaSiNoExiste(
                                "Consulta",
                                "Diagnóstico, evaluación y planificación de tratamiento",
                                "fas fa-user-md",
                                "#34495e");

                CategoriaProcedimiento ortodoncia = crearCategoriaSiNoExiste(
                                "Ortodoncia",
                                "Corrección de malposición dental y problemas de mordida",
                                "fas fa-teeth",
                                "#3498db");

                CategoriaProcedimiento endodoncia = crearCategoriaSiNoExiste(
                                "Endodoncia",
                                "Tratamiento de conductos y pulpa dental",
                                "fas fa-tooth",
                                "#e74c3c");

                CategoriaProcedimiento periodoncia = crearCategoriaSiNoExiste(
                                "Periodoncia",
                                "Tratamiento de encías y tejidos de soporte",
                                "fas fa-teeth-open",
                                "#2ecc71");

                CategoriaProcedimiento implantes = crearCategoriaSiNoExiste(
                                "Implantes",
                                "Implantes dentales y prótesis fijas",
                                "fas fa-tooth",
                                "#9b59b6");

                CategoriaProcedimiento limpieza = crearCategoriaSiNoExiste(
                                "Profilaxis",
                                "Limpieza dental y prevención",
                                "fas fa-broom",
                                "#1abc9c");

                CategoriaProcedimiento estetica = crearCategoriaSiNoExiste(
                                "Estética Dental",
                                "Blanqueamiento y tratamientos estéticos",
                                "fas fa-star",
                                "#f1c40f");

                CategoriaProcedimiento cirugia = crearCategoriaSiNoExiste(
                                "Cirugía Oral",
                                "Extracciones y procedimientos quirúrgicos",
                                "fas fa-scalpel",
                                "#e67e22");

                System.out.println(">>> Creando servicios de ejemplo para cada categoría...");

                // Consultas
                crearProcedimientoSiNoExiste("CON-001", "Consulta General", new BigDecimal("80.00"), 30, consulta);
                crearProcedimientoSiNoExiste("CON-002", "Consulta de Emergencia", new BigDecimal("100.00"), 20,
                                consulta);

                crearProcedimientoSiNoExiste("CON-003", "Consulta de Evaluación", new BigDecimal("0.00"), 20, consulta);
                crearProcedimientoSiNoExiste("CON-004", "Control de Tratamiento", new BigDecimal("0.00"), 15, consulta);
                crearProcedimientoSiNoExiste("CON-005", "Emergencia Dolor Fuerte", new BigDecimal("0.00"), 30,
                                consulta);
                // Ortodoncia
                crearProcedimientoSiNoExiste("ORT-001", "Instalación de Brackets Metálicos", new BigDecimal("1500.00"),
                                90,
                                ortodoncia);
                crearProcedimientoSiNoExiste("ORT-002", "Control de Ortodoncia", new BigDecimal("120.00"), 30,
                                ortodoncia);

                // Endodoncia
                crearProcedimientoSiNoExiste("END-001", "Tratamiento de Conducto Molar", new BigDecimal("450.00"), 90,
                                endodoncia);
                crearProcedimientoSiNoExiste("END-002", "Tratamiento de Conducto Premolar", new BigDecimal("350.00"),
                                60,
                                endodoncia);

                // Periodoncia
                crearProcedimientoSiNoExiste("PER-001", "Raspado y Alisado Radicular", new BigDecimal("200.00"), 45,
                                periodoncia);
                crearProcedimientoSiNoExiste("PER-002", "Cirugía Periodontal", new BigDecimal("600.00"), 90,
                                periodoncia);

                // Implantes
                crearProcedimientoSiNoExiste("IMP-001", "Colocación de Implante Dental", new BigDecimal("2500.00"), 120,
                                implantes);
                crearProcedimientoSiNoExiste("IMP-002", "Corona sobre Implante", new BigDecimal("1200.00"), 60,
                                implantes);

                // Limpieza/Profilaxis
                crearProcedimientoSiNoExiste("PRO-001", "Limpieza Dental Completa", new BigDecimal("150.00"), 45,
                                limpieza);
                crearProcedimientoSiNoExiste("PRO-002", "Fluorización", new BigDecimal("80.00"), 20, limpieza);

                // Estética
                crearProcedimientoSiNoExiste("EST-001", "Blanqueamiento Dental Láser", new BigDecimal("800.00"), 60,
                                estetica);
                crearProcedimientoSiNoExiste("EST-002", "Carilla de Porcelana", new BigDecimal("1000.00"), 45,
                                estetica);

                // Cirugía
                crearProcedimientoSiNoExiste("CIR-001", "Extracción Simple", new BigDecimal("120.00"), 30, cirugia);
                crearProcedimientoSiNoExiste("CIR-002", "Extracción de Muela del Juicio", new BigDecimal("300.00"), 60,
                                cirugia);

                // === CREACIÓN DE UNIDADES DE MEDIDA ===
                System.out.println(">>> Creando unidades de medida...");
                UnidadMedida unidad = crearUnidadSiNoExiste("Unidad", "und");
                UnidadMedida mililitro = crearUnidadSiNoExiste("Mililitro", "ml");
                UnidadMedida gramo = crearUnidadSiNoExiste("Gramo", "g");
                UnidadMedida caja = crearUnidadSiNoExiste("Caja", "cja");
                UnidadMedida paquete = crearUnidadSiNoExiste("Paquete", "paq");
                UnidadMedida frasco = crearUnidadSiNoExiste("Frasco", "fco");
                UnidadMedida carpule = crearUnidadSiNoExiste("Carpule", "carp");
                UnidadMedida sobre = crearUnidadSiNoExiste("Sobre", "sob");
                UnidadMedida rollo = crearUnidadSiNoExiste("Rollo", "rll");

                // === CREACIÓN DE CATEGORÍAS DE INSUMO ===
                System.out.println(">>> Creando categorías de insumo...");
                CategoriaInsumo anestesicos = crearCategoriaInsumoSiNoExiste("Anestésicos",
                                "Anestésicos locales y complementos");
                CategoriaInsumo materialesRestauracion = crearCategoriaInsumoSiNoExiste("Materiales de Restauración",
                                "Resinas, amalgamas y materiales de obturación");
                CategoriaInsumo materialesEndodoncia = crearCategoriaInsumoSiNoExiste("Materiales de Endodoncia",
                                "Limas, gutapercha y selladores");
                CategoriaInsumo materialesImpresion = crearCategoriaInsumoSiNoExiste("Materiales de Impresión",
                                "Alginatos, siliconas y cubetas");
                CategoriaInsumo descartables = crearCategoriaInsumoSiNoExiste("Descartables",
                                "Guantes, gasas, algodón y jeringas");
                CategoriaInsumo instrumental = crearCategoriaInsumoSiNoExiste("Instrumental",
                                "Fresas, agujas y puntas");
                CategoriaInsumo desinfeccion = crearCategoriaInsumoSiNoExiste("Desinfección y Esterilización",
                                "Productos de limpieza y desinfección");
                CategoriaInsumo profilaxis = crearCategoriaInsumoSiNoExiste("Profilaxis",
                                "Pastas, fluoruros y materiales de limpieza");
                CategoriaInsumo insumosOrtodoncia = crearCategoriaInsumoSiNoExiste("Ortodoncia",
                                "Brackets, arcos y ligaduras");
                CategoriaInsumo insumosCirugia = crearCategoriaInsumoSiNoExiste("Cirugía",
                                "Suturas, bisturís y materiales quirúrgicos");

                // === CREACIÓN DE INSUMOS ===
                System.out.println(">>> Creando insumos...");

                // Anestésicos
                crearInsumoSiNoExiste("ANES-LIDO-01", "Anestesia Lidocaina 2%", "Septodont", new BigDecimal("20"),
                                new BigDecimal("5.50"), anestesicos, carpule);
                crearInsumoSiNoExiste("ANES-ARTI-01", "Articaina 4% con Epinefrina", "Septodont", new BigDecimal("15"),
                                new BigDecimal("6.80"), anestesicos, carpule);
                crearInsumoSiNoExiste("ANES-MEPI-01", "Mepivacaina 3%", "Scandinibsa", new BigDecimal("15"),
                                new BigDecimal("5.90"), anestesicos, carpule);

                // Materiales de Restauración
                crearInsumoSiNoExiste("REST-RESI-01", "Resina Compuesta A2", "3M Filtek", new BigDecimal("5"),
                                new BigDecimal("85.00"), materialesRestauracion, unidad);
                crearInsumoSiNoExiste("REST-RESI-02", "Resina Compuesta A3", "3M Filtek", new BigDecimal("5"),
                                new BigDecimal("85.00"), materialesRestauracion, unidad);
                crearInsumoSiNoExiste("REST-RESI-03", "Resina Fluida", "3M Filtek Flow", new BigDecimal("5"),
                                new BigDecimal("72.00"), materialesRestauracion, unidad);
                crearInsumoSiNoExiste("REST-AMAL-01", "Amalgama Dental", "SDI", new BigDecimal("10"),
                                new BigDecimal("45.00"),
                                materialesRestauracion, gramo);
                crearInsumoSiNoExiste("REST-GRAB-01", "Ácido Grabador 37%", "3M Scotchbond", new BigDecimal("8"),
                                new BigDecimal("28.00"), materialesRestauracion, frasco);
                crearInsumoSiNoExiste("REST-ADHE-01", "Adhesivo Dental", "3M Adper", new BigDecimal("5"),
                                new BigDecimal("95.00"), materialesRestauracion, frasco);
                crearInsumoSiNoExiste("REST-IONO-01", "Ionómero de Vidrio", "GC Fuji", new BigDecimal("8"),
                                new BigDecimal("62.00"), materialesRestauracion, unidad);
                crearInsumoSiNoExiste("REST-BASE-01", "Base Cavitaria", "Dentsply", new BigDecimal("10"),
                                new BigDecimal("38.00"), materialesRestauracion, frasco);

                // Materiales de Endodoncia
                crearInsumoSiNoExiste("ENDO-LIMA-01", "Limas K-File #15-40", "Dentsply Maillefer", new BigDecimal("10"),
                                new BigDecimal("35.00"), materialesEndodoncia, caja);
                crearInsumoSiNoExiste("ENDO-GUTA-01", "Conos de Gutapercha", "Dentsply", new BigDecimal("15"),
                                new BigDecimal("28.00"), materialesEndodoncia, caja);
                crearInsumoSiNoExiste("ENDO-SELL-01", "Sellador de Conductos", "Pulpdent", new BigDecimal("5"),
                                new BigDecimal("45.00"), materialesEndodoncia, frasco);
                crearInsumoSiNoExiste("ENDO-HIPO-01", "Hipoclorito de Sodio 5.25%", "Clorox", new BigDecimal("20"),
                                new BigDecimal("8.50"), materialesEndodoncia, frasco);
                crearInsumoSiNoExiste("ENDO-EDTA-01", "EDTA 17%", "Biodinâmica", new BigDecimal("10"),
                                new BigDecimal("15.00"),
                                materialesEndodoncia, frasco);
                crearInsumoSiNoExiste("ENDO-CEME-01", "Cemento Temporal", "Cavit", new BigDecimal("8"),
                                new BigDecimal("32.00"),
                                materialesEndodoncia, frasco);

                // Materiales de Impresión
                crearInsumoSiNoExiste("IMPR-ALGI-01", "Alginato Cromático", "Jeltrate", new BigDecimal("10"),
                                new BigDecimal("42.00"), materialesImpresion, sobre);
                crearInsumoSiNoExiste("IMPR-SILI-01", "Silicona de Adición", "3M ESPE", new BigDecimal("5"),
                                new BigDecimal("120.00"), materialesImpresion, unidad);
                crearInsumoSiNoExiste("IMPR-CUBE-01", "Cubetas de Impresión", "Maquira", new BigDecimal("15"),
                                new BigDecimal("2.50"), materialesImpresion, unidad);
                crearInsumoSiNoExiste("IMPR-YESO-01", "Yeso Dental Tipo III", "Zhermack", new BigDecimal("20"),
                                new BigDecimal("18.00"), materialesImpresion, sobre);

                // Descartables
                crearInsumoSiNoExiste("DESC-GUAN-01", "Guantes de Látex Talla M", "Sempermed", new BigDecimal("50"),
                                new BigDecimal("25.00"), descartables, caja);
                crearInsumoSiNoExiste("DESC-GUAN-02", "Guantes de Látex Talla S", "Sempermed", new BigDecimal("50"),
                                new BigDecimal("25.00"), descartables, caja);
                crearInsumoSiNoExiste("DESC-GASA-01", "Gasas Esterilizadas 7.5x7.5cm", "Gasa Med",
                                new BigDecimal("100"),
                                new BigDecimal("12.00"), descartables, paquete);
                crearInsumoSiNoExiste("DESC-ALGO-01", "Algodón en Rollo", "Apolo", new BigDecimal("10"),
                                new BigDecimal("8.50"),
                                descartables, rollo);
                crearInsumoSiNoExiste("DESC-JERI-01", "Jeringas Descartables 5ml", "BD", new BigDecimal("50"),
                                new BigDecimal("15.00"), descartables, caja);
                crearInsumoSiNoExiste("DESC-BABC-01", "Baberos Descartables", "Dentalcryl", new BigDecimal("100"),
                                new BigDecimal("22.00"), descartables, paquete);
                crearInsumoSiNoExiste("DESC-VASC-01", "Vasos Descartables", "Vasconia", new BigDecimal("100"),
                                new BigDecimal("8.00"), descartables, paquete);
                crearInsumoSiNoExiste("DESC-EYEC-01", "Eyectores de Saliva", "Ultradent", new BigDecimal("100"),
                                new BigDecimal("18.00"), descartables, paquete);
                crearInsumoSiNoExiste("DESC-ROLL-01", "Rollos de Algodón", "Roeko", new BigDecimal("50"),
                                new BigDecimal("12.00"), descartables, paquete);

                // Instrumental
                crearInsumoSiNoExiste("INST-AGUJ-01", "Agujas Dentales 27G Cortas", "Terumo", new BigDecimal("50"),
                                new BigDecimal("18.00"), instrumental, caja);
                crearInsumoSiNoExiste("INST-AGUJ-02", "Agujas Dentales 27G Largas", "Terumo", new BigDecimal("50"),
                                new BigDecimal("18.00"), instrumental, caja);
                crearInsumoSiNoExiste("INST-FRES-01", "Fresas Carbide Redondas", "Maillefer", new BigDecimal("10"),
                                new BigDecimal("35.00"), instrumental, caja);
                crearInsumoSiNoExiste("INST-FRES-02", "Fresas Diamantadas", "KG Sorensen", new BigDecimal("10"),
                                new BigDecimal("42.00"), instrumental, caja);
                crearInsumoSiNoExiste("INST-DISK-01", "Discos de Pulido", "Sof-Lex 3M", new BigDecimal("8"),
                                new BigDecimal("55.00"), instrumental, caja);
                crearInsumoSiNoExiste("INST-PUNT-01", "Puntas de Papel", "Dentsply", new BigDecimal("15"),
                                new BigDecimal("22.00"), instrumental, caja);

                // Desinfección
                crearInsumoSiNoExiste("DESI-GLUT-01", "Glutaraldehído 2%", "Cidex", new BigDecimal("5"),
                                new BigDecimal("85.00"), desinfeccion, frasco);
                crearInsumoSiNoExiste("DESI-ALCO-01", "Alcohol 70%", "Quimtia", new BigDecimal("10"),
                                new BigDecimal("12.00"),
                                desinfeccion, frasco);
                crearInsumoSiNoExiste("DESI-JABO-01", "Jabón Líquido Antibacterial", "Protex", new BigDecimal("15"),
                                new BigDecimal("18.00"), desinfeccion, frasco);
                crearInsumoSiNoExiste("DESI-CLOR-01", "Lejía 5%", "Clorox", new BigDecimal("20"),
                                new BigDecimal("5.00"),
                                desinfeccion, frasco);

                // Profilaxis
                crearInsumoSiNoExiste("PROF-PAST-01", "Pasta Profiláctica", "Maquira", new BigDecimal("10"),
                                new BigDecimal("28.00"), profilaxis, frasco);
                crearInsumoSiNoExiste("PROF-FLUO-01", "Fluoruro Gel 1.23%", "Sultan", new BigDecimal("8"),
                                new BigDecimal("42.00"), profilaxis, frasco);
                crearInsumoSiNoExiste("PROF-CEPI-01", "Cepillos Profilaxis", "Microdont", new BigDecimal("20"),
                                new BigDecimal("18.00"), profilaxis, caja);
                crearInsumoSiNoExiste("PROF-HILO-01", "Hilo Dental", "Oral-B", new BigDecimal("50"),
                                new BigDecimal("3.50"),
                                profilaxis, unidad);

                // Ortodoncia
                crearInsumoSiNoExiste("ORTO-BRAC-01", "Brackets Metálicos Kit", "American Orthodontics",
                                new BigDecimal("5"),
                                new BigDecimal("450.00"), insumosOrtodoncia, unidad);
                crearInsumoSiNoExiste("ORTO-ARCO-01", "Arcos de Níquel-Titanio", "3M Unitek", new BigDecimal("10"),
                                new BigDecimal("35.00"), insumosOrtodoncia, unidad);
                crearInsumoSiNoExiste("ORTO-LIGA-01", "Ligaduras Elásticas", "Morelli", new BigDecimal("20"),
                                new BigDecimal("12.00"), insumosOrtodoncia, paquete);
                crearInsumoSiNoExiste("ORTO-CEME-01", "Cemento para Ortodoncia", "Transbond XT", new BigDecimal("5"),
                                new BigDecimal("180.00"), insumosOrtodoncia, unidad);

                // Cirugía
                crearInsumoSiNoExiste("CIRU-SUTU-01", "Sutura Seda 3-0", "Ethicon", new BigDecimal("20"),
                                new BigDecimal("8.50"), insumosCirugia, unidad);
                crearInsumoSiNoExiste("CIRU-SUTU-02", "Sutura Reabsorbible 4-0", "Vicryl", new BigDecimal("15"),
                                new BigDecimal("12.00"), insumosCirugia, unidad);
                crearInsumoSiNoExiste("CIRU-BIST-01", "Hojas de Bisturí #15", "Feather", new BigDecimal("30"),
                                new BigDecimal("1.20"), insumosCirugia, unidad);
                crearInsumoSiNoExiste("CIRU-GASA-01", "Gasas Hemostáticas", "Gelfoam", new BigDecimal("10"),
                                new BigDecimal("22.00"), insumosCirugia, caja);
                crearInsumoSiNoExiste("CIRU-ESPO-01", "Esponja de Colágeno", "CollaCote", new BigDecimal("8"),
                                new BigDecimal("35.00"), insumosCirugia, unidad);

                // === CREACIÓN DE RELACIONES PROCEDIMIENTO-INSUMO ===
                System.out.println(">>> Creando relaciones procedimiento-insumo...");

                // CON-001: Consulta General
                crearProcedimientoInsumoSiNoExiste("CON-001", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("CON-001", "DESC-BABC-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("CON-001", "DESC-VASC-01", new BigDecimal("1"), "unidad", true);

                // CON-002: Consulta de Emergencia
                crearProcedimientoInsumoSiNoExiste("CON-002", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("CON-002", "DESC-BABC-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("CON-002", "ANES-LIDO-01", new BigDecimal("1"), "carpule", false);
                crearProcedimientoInsumoSiNoExiste("CON-002", "INST-AGUJ-01", new BigDecimal("1"), "unidad", false);
                crearProcedimientoInsumoSiNoExiste("CON-002", "DESC-GASA-01", new BigDecimal("3"), "unidades", true);

                // ORT-001: Instalación de Brackets Metálicos
                crearProcedimientoInsumoSiNoExiste("ORT-001", "ORTO-BRAC-01", new BigDecimal("1"), "kit", true);
                crearProcedimientoInsumoSiNoExiste("ORT-001", "ORTO-CEME-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("ORT-001", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("ORT-001", "REST-GRAB-01", new BigDecimal("2"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("ORT-001", "DESC-ALGO-01", new BigDecimal("5"), "gramos", true);
                crearProcedimientoInsumoSiNoExiste("ORT-001", "DESC-ROLL-01", new BigDecimal("4"), "unidades", true);

                // ORT-002: Control de Ortodoncia
                crearProcedimientoInsumoSiNoExiste("ORT-002", "ORTO-LIGA-01", new BigDecimal("1"), "paquete", true);
                crearProcedimientoInsumoSiNoExiste("ORT-002", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("ORT-002", "ORTO-ARCO-01", new BigDecimal("1"), "unidad", false);
                crearProcedimientoInsumoSiNoExiste("ORT-002", "DESC-BABC-01", new BigDecimal("1"), "unidad", true);

                // END-001: Tratamiento de Conducto Molar
                crearProcedimientoInsumoSiNoExiste("END-001", "ANES-LIDO-01", new BigDecimal("2"), "carpules", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "INST-AGUJ-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "ENDO-LIMA-01", new BigDecimal("1"), "set", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "ENDO-GUTA-01", new BigDecimal("4"), "conos", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "ENDO-SELL-01", new BigDecimal("1"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "ENDO-HIPO-01", new BigDecimal("10"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "ENDO-EDTA-01", new BigDecimal("5"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "ENDO-CEME-01", new BigDecimal("1"), "aplicación", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "DESC-GASA-01", new BigDecimal("5"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("END-001", "DESC-ROLL-01", new BigDecimal("2"), "unidades", true);

                // END-002: Tratamiento de Conducto Premolar
                crearProcedimientoInsumoSiNoExiste("END-002", "ANES-LIDO-01", new BigDecimal("1"), "carpule", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "INST-AGUJ-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "ENDO-LIMA-01", new BigDecimal("1"), "set", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "ENDO-GUTA-01", new BigDecimal("2"), "conos", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "ENDO-SELL-01", new BigDecimal("0.5"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "ENDO-HIPO-01", new BigDecimal("8"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "ENDO-EDTA-01", new BigDecimal("3"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "ENDO-CEME-01", new BigDecimal("1"), "aplicación", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("END-002", "DESC-GASA-01", new BigDecimal("4"), "unidades", true);

                // PER-001: Raspado y Alisado Radicular
                crearProcedimientoInsumoSiNoExiste("PER-001", "ANES-LIDO-01", new BigDecimal("2"), "carpules", true);
                crearProcedimientoInsumoSiNoExiste("PER-001", "INST-AGUJ-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PER-001", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PER-001", "DESC-GASA-01", new BigDecimal("6"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PER-001", "DESI-CLOR-01", new BigDecimal("5"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("PER-001", "PROF-FLUO-01", new BigDecimal("2"), "ml", false);

                // PER-002: Cirugía Periodontal
                crearProcedimientoInsumoSiNoExiste("PER-002", "ANES-ARTI-01", new BigDecimal("3"), "carpules", true);
                crearProcedimientoInsumoSiNoExiste("PER-002", "INST-AGUJ-02", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PER-002", "CIRU-BIST-01", new BigDecimal("2"), "hojas", true);
                crearProcedimientoInsumoSiNoExiste("PER-002", "CIRU-SUTU-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PER-002", "DESC-GUAN-01", new BigDecimal("4"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PER-002", "DESC-GASA-01", new BigDecimal("10"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PER-002", "CIRU-GASA-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PER-002", "DESI-CLOR-01", new BigDecimal("10"), "ml", true);

                // IMP-001: Colocación de Implante Dental
                crearProcedimientoInsumoSiNoExiste("IMP-001", "ANES-ARTI-01", new BigDecimal("4"), "carpules", true);
                crearProcedimientoInsumoSiNoExiste("IMP-001", "INST-AGUJ-02", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("IMP-001", "CIRU-BIST-01", new BigDecimal("3"), "hojas", true);
                crearProcedimientoInsumoSiNoExiste("IMP-001", "CIRU-SUTU-02", new BigDecimal("3"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("IMP-001", "DESC-GUAN-01", new BigDecimal("4"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("IMP-001", "DESC-GASA-01", new BigDecimal("15"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("IMP-001", "CIRU-ESPO-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("IMP-001", "DESI-CLOR-01", new BigDecimal("20"), "ml", true);

                // IMP-002: Corona sobre Implante
                crearProcedimientoInsumoSiNoExiste("IMP-002", "ANES-LIDO-01", new BigDecimal("1"), "carpule", true);
                crearProcedimientoInsumoSiNoExiste("IMP-002", "INST-AGUJ-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("IMP-002", "IMPR-SILI-01", new BigDecimal("1"), "set", true);
                crearProcedimientoInsumoSiNoExiste("IMP-002", "IMPR-CUBE-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("IMP-002", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("IMP-002", "DESC-ROLL-01", new BigDecimal("2"), "unidades", true);

                // También agregar insumos comunes a procedimientos de restauración (PRO, EST,
                // CIR)
                // PRO-001: Limpieza Dental Completa
                crearProcedimientoInsumoSiNoExiste("PRO-001", "PROF-PAST-01", new BigDecimal("5"), "gramos", true);
                crearProcedimientoInsumoSiNoExiste("PRO-001", "PROF-CEPI-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("PRO-001", "PROF-FLUO-01", new BigDecimal("2"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("PRO-001", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PRO-001", "DESC-GASA-01", new BigDecimal("3"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PRO-001", "DESC-VASC-01", new BigDecimal("2"), "unidades", true);

                // PRO-002: Fluorización
                crearProcedimientoInsumoSiNoExiste("PRO-002", "PROF-FLUO-01", new BigDecimal("3"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("PRO-002", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("PRO-002", "DESC-ALGO-01", new BigDecimal("2"), "gramos", true);

                // EST-001: Blanqueamiento Dental Láser
                crearProcedimientoInsumoSiNoExiste("EST-001", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("EST-001", "DESC-BABC-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("EST-001", "DESC-EYEC-01", new BigDecimal("2"), "unidades", true);

                // EST-002: Carilla de Porcelana
                crearProcedimientoInsumoSiNoExiste("EST-002", "ANES-LIDO-01", new BigDecimal("1"), "carpule", true);
                crearProcedimientoInsumoSiNoExiste("EST-002", "INST-AGUJ-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("EST-002", "IMPR-SILI-01", new BigDecimal("1"), "set", true);
                crearProcedimientoInsumoSiNoExiste("EST-002", "REST-ADHE-01", new BigDecimal("1"), "ml", true);
                crearProcedimientoInsumoSiNoExiste("EST-002", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);

                // CIR-001: Extracción Simple
                crearProcedimientoInsumoSiNoExiste("CIR-001", "ANES-LIDO-01", new BigDecimal("2"), "carpules", true);
                crearProcedimientoInsumoSiNoExiste("CIR-001", "INST-AGUJ-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("CIR-001", "DESC-GUAN-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("CIR-001", "DESC-GASA-01", new BigDecimal("5"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("CIR-001", "CIRU-GASA-01", new BigDecimal("1"), "unidad", false);

                // CIR-002: Extracción de Muela del Juicio
                crearProcedimientoInsumoSiNoExiste("CIR-002", "ANES-ARTI-01", new BigDecimal("3"), "carpules", true);
                crearProcedimientoInsumoSiNoExiste("CIR-002", "INST-AGUJ-02", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("CIR-002", "CIRU-BIST-01", new BigDecimal("2"), "hojas", true);
                crearProcedimientoInsumoSiNoExiste("CIR-002", "CIRU-SUTU-01", new BigDecimal("1"), "unidad", true);
                crearProcedimientoInsumoSiNoExiste("CIR-002", "DESC-GUAN-01", new BigDecimal("4"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("CIR-002", "DESC-GASA-01", new BigDecimal("10"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("CIR-002", "CIRU-GASA-01", new BigDecimal("2"), "unidades", true);
                crearProcedimientoInsumoSiNoExiste("CIR-002", "CIRU-ESPO-01", new BigDecimal("1"), "unidad", false);

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
                        entrada = tipoMovimientoRepository.findByCodigo("ENTRADA")
                                        .orElseThrow(() -> new RuntimeException("Tipo ENTRADA no encontrado"));
                        salida = tipoMovimientoRepository.findByCodigo("SALIDA")
                                        .orElseThrow(() -> new RuntimeException("Tipo SALIDA no encontrado"));
                }

                // Crear motivos - REORGANIZADOS según sean manuales o automáticos
                System.out.println(">>> Creando motivos de movimiento...");

                // ENTRADA - Solo motivos manuales
                crearMotivoSiNoExiste("Compra a proveedor", entrada, true);

                // SALIDA - Solo motivos manuales
                crearMotivoSiNoExiste("Vencimiento o merma", salida, true);
                crearMotivoSiNoExiste("Ajuste de inventario", salida, true);

                // Motivos automáticos (NO se muestran en formularios manuales)
                crearMotivoSiNoExiste("Uso en procedimiento", salida, false);
                crearMotivoSiNoExiste("Venta Directa", salida, false);
                crearMotivoSiNoExiste("Anulación de Venta", entrada, false);

                System.out.println("✅ Motivos creados correctamente");
                // --- 👆 FIN DE LA CORRECCIÓN 👆 ---

                // --- CREACIÓN DE ESTADOS DE CITA ---
                System.out.println(">>> Creando estados de cita...");
                crearEstadoCitaSiNoExiste("PENDIENTE", "Cita agendada, esperando confirmación", "#FFC107");
                crearEstadoCitaSiNoExiste("CONFIRMADA", "Cita confirmada por el paciente", "#2196F3");
                crearEstadoCitaSiNoExiste("CANCELADA_PACIENTE", "Cancelada por el paciente", "#FF5722");
                crearEstadoCitaSiNoExiste("CANCELADA_CLINICA", "Cancelada por la clínica", "#F44336");
                crearEstadoCitaSiNoExiste("ASISTIO", "Paciente asistió a la cita", "#4CAF50");
                crearEstadoCitaSiNoExiste("NO_ASISTIO", "Paciente no asistió a la cita", "#9E9E9E");
                crearEstadoCitaSiNoExiste("REPROGRAMADA", "Cita reprogramada", "#FF9800");

                // --- CREACIÓN DE ESTADOS DE PAGO ---
                System.out.println(">>> Creando estados de pago...");
                crearEstadoPagoSiNoExiste("PENDIENTE", "Pago pendiente, sin abonos");
                crearEstadoPagoSiNoExiste("PAGADO_PARCIAL", "Pago parcial realizado");
                crearEstadoPagoSiNoExiste("PAGADO_TOTAL", "Pago completado en su totalidad");
                crearEstadoPagoSiNoExiste("ANULADO", "Comprobante anulado");

                // --- CREACIÓN DE MÉTODOS DE PAGO ---
                System.out.println(">>> Creando métodos de pago...");
                crearMetodoPagoSiNoExiste("EFECTIVO", "Pago en efectivo");
                crearMetodoPagoSiNoExiste("YAPE", "Pago mediante Yape");
                crearMetodoPagoSiNoExiste("MIXTO", "Pago combinado (Efectivo + Yape)");

                // === CREACIÓN DE PERMISOS GRANULARES ===
                System.out.println(">>> Creando permisos granulares del sistema...");

                // Módulos actuales del sistema (sin REPORTES ni CONFIGURACION que no existen)
                // Módulos actuales del sistema (sin REPORTES ni CONFIGURACION que no existen)
                List<String> modulos = Arrays.asList(
                                "USUARIOS",
                                "ROLES",
                                "PACIENTES",
                                "CITAS",
                                "SERVICIOS",
                                "FACTURACION",
                                "INVENTARIO",
                                "TRATAMIENTOS",
                                "ODONTOGRAMA");
                // Módulo especial para el panel de paciente (permisos personalizados)
                List<String> accionesPanelPaciente = Arrays.asList(
                                "VER_MIS_CITAS",
                                "AGENDAR_CITA",
                                "CANCELAR_MIS_CITAS",
                                "VER_MI_PERFIL",
                                "EDITAR_MI_PERFIL",
                                "VER_MIS_TRATAMIENTOS",
                                "VER_MI_ODONTOGRAMA",
                                "VER_MIS_COMPROBANTES");
                // Crear permisos del módulo PANEL_PACIENTE
                for (String accion : accionesPanelPaciente) {
                        permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", accion).orElseGet(() -> {
                                Permiso permiso = new Permiso();
                                permiso.setModulo("PANEL_PACIENTE");
                                permiso.setAccion(accion);
                                System.out.println("  -> Creando permiso: " + accion + "_PANEL_PACIENTE");
                                return permisoRepository.save(permiso);
                        });
                }

                List<String> acciones = Arrays.asList("VER_LISTA", "VER_DETALLE", "CREAR", "EDITAR", "ELIMINAR",
                                "RESTAURAR");

                for (String modulo : modulos) {
                        for (String accion : acciones) {
                                permisoRepository.findByModuloAndAccion(modulo, accion).orElseGet(() -> {
                                        Permiso permiso = new Permiso();
                                        permiso.setModulo(modulo);
                                        permiso.setAccion(accion);
                                        System.out.println("  -> Creando permiso: " + accion + "_" + modulo);
                                        return permisoRepository.save(permiso);
                                });
                        }
                }

                // === PERMISOS ESPECIALES DE ADMINISTRACIÓN ===
                permisoRepository.findByModuloAndAccion("ADMINISTRACION", "VER_REGISTROS_ELIMINADOS").orElseGet(() -> {
                        Permiso permiso = new Permiso();
                        permiso.setModulo("ADMINISTRACION");
                        permiso.setAccion("VER_REGISTROS_ELIMINADOS");
                        System.out.println("  -> Creando permiso: VER_REGISTROS_ELIMINADOS");
                        return permisoRepository.save(permiso);
                });

                // PACIENTE: Solo permisos del PANEL_PACIENTE (sin acceso al admin panel)
                Set<Permiso> permisosPaciente = new HashSet<>();
                // Permisos del módulo PANEL_PACIENTE
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MIS_CITAS")
                                .ifPresent(permisosPaciente::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "AGENDAR_CITA")
                                .ifPresent(permisosPaciente::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "CANCELAR_MIS_CITAS")
                                .ifPresent(permisosPaciente::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MI_PERFIL")
                                .ifPresent(permisosPaciente::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "EDITAR_MI_PERFIL")
                                .ifPresent(permisosPaciente::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MIS_TRATAMIENTOS")
                                .ifPresent(permisosPaciente::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MI_ODONTOGRAMA")
                                .ifPresent(permisosPaciente::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MIS_COMPROBANTES")
                                .ifPresent(permisosPaciente::add);
                crearRolSiNoExiste("PACIENTE", permisosPaciente);

                // ODONTOLOGO: GESTIÓN CLÍNICA (Pacientes, Citas, Servicios) + FACTURACIÓN +
                // Tratamientos + Odontograma
                Set<Permiso> permisosOdontologo = new HashSet<>();
                List<String> modulosOdontologo = Arrays.asList(
                                "PACIENTES",
                                "CITAS",
                                "SERVICIOS",
                                "FACTURACION",
                                "TRATAMIENTOS",
                                "ODONTOGRAMA");
                for (String modulo : modulosOdontologo) {
                        for (String accion : acciones) {
                                permisoRepository.findByModuloAndAccion(modulo, accion)
                                                .ifPresent(permisosOdontologo::add);
                        }
                }
                crearRolSiNoExiste("ODONTOLOGO", permisosOdontologo);

                // RECEPCIONISTA: Solo CITAS, PACIENTES y FACTURACION
                Set<Permiso> permisosRecepcionista = new HashSet<>();
                List<String> modulosRecepcionista = Arrays.asList("CITAS", "PACIENTES", "FACTURACION");
                for (String modulo : modulosRecepcionista) {
                        for (String accion : acciones) {
                                permisoRepository.findByModuloAndAccion(modulo, accion)
                                                .ifPresent(permisosRecepcionista::add);
                        }
                }
                crearRolSiNoExiste("RECEPCIONISTA", permisosRecepcionista);

                // ALMACEN: Solo INVENTARIO
                Set<Permiso> permisosAlmacen = new HashSet<>();
                for (String accion : acciones) {
                        permisoRepository.findByModuloAndAccion("INVENTARIO", accion).ifPresent(permisosAlmacen::add);
                }
                crearRolSiNoExiste("ALMACEN", permisosAlmacen);

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
                crearPacientesParaUsuariosConRolPaciente();
                limpiarPermisosRolPaciente();

        }

        /**
         * Crea registros de paciente para usuarios con rol PACIENTE que no tengan
         * registro
         */
        private void crearPacientesParaUsuariosConRolPaciente() {
                System.out.println(">>> Verificando usuarios con rol PACIENTE sin registro de paciente...");

                // Buscar rol PACIENTE
                Rol rolPaciente = rolRepository.findByNombre("PACIENTE").orElse(null);
                if (rolPaciente == null) {
                        System.out.println("   - Rol PACIENTE no existe aún, saltando verificación");
                        return;
                }

                // Buscar todos los usuarios con rol PACIENTE
                List<Usuario> usuariosConRolPaciente = usuarioRepository.findAll().stream()
                                .filter(u -> u.getRoles().stream()
                                                .anyMatch(r -> r.getNombre().equals("PACIENTE")))
                                .collect(java.util.stream.Collectors.toList());

                System.out.println("   - Usuarios con rol PACIENTE: " + usuariosConRolPaciente.size());

                int pacientesCreados = 0;

                for (Usuario usuario : usuariosConRolPaciente) {
                        // Verificar si ya tiene registro de paciente
                        boolean tienePaciente = pacienteRepository.findByUsuario(usuario).isPresent();

                        if (!tienePaciente) {
                                // Crear registro de paciente
                                Paciente nuevoPaciente = new Paciente();
                                nuevoPaciente.setUsuario(usuario);
                                nuevoPaciente.setNombreCompleto(usuario.getNombreCompleto());
                                nuevoPaciente.setNumeroDocumento(usuario.getNumeroDocumento());
                                nuevoPaciente.setTipoDocumento(usuario.getTipoDocumento());
                                nuevoPaciente.setEmail(usuario.getEmail());
                                nuevoPaciente.setTelefono(usuario.getTelefono());
                                nuevoPaciente.setDireccion(usuario.getDireccion());
                                nuevoPaciente.setFechaNacimiento(usuario.getFechaNacimiento());
                                nuevoPaciente.setAlergias(null);
                                nuevoPaciente.setAntecedentesMedicos(null);
                                nuevoPaciente.setTratamientosActuales(null);
                                nuevoPaciente.setEliminado(false);
                                nuevoPaciente.setCreadoPor("SISTEMA");

                                pacienteRepository.save(nuevoPaciente);
                                pacientesCreados++;

                                System.out.println(
                                                "   ✅ Paciente creado para usuario: " + usuario.getEmail() + " (ID: "
                                                                + usuario.getId() + ")");
                        }
                }

                if (pacientesCreados > 0) {
                        System.out.println(
                                        ">>> " + pacientesCreados + " registro(s) de paciente creado(s) exitosamente");
                } else {
                        System.out.println(">>> Todos los usuarios con rol PACIENTE ya tienen registro de paciente");
                }
        }

        /**
         * Limpia y corrige los permisos del rol PACIENTE
         * Elimina permisos incorrectos y deja solo los necesarios para el panel de
         * paciente
         */
        private void limpiarPermisosRolPaciente() {
                System.out.println(">>> Limpiando permisos del rol PACIENTE...");

                Rol rolPaciente = rolRepository.findByNombre("PACIENTE").orElse(null);
                if (rolPaciente == null) {
                        System.out.println("   - Rol PACIENTE no existe, saltando limpieza");
                        return;
                }

                // Obtener permisos actuales
                int permisosAnteriores = rolPaciente.getPermisos().size();
                System.out.println("   - Permisos actuales: " + permisosAnteriores);

                // Limpiar todos los permisos
                rolPaciente.getPermisos().clear();

                // Agregar solo los permisos correctos del módulo PANEL_PACIENTE
                Set<Permiso> permisosCorrectos = new HashSet<>();
                // Permisos del módulo PANEL_PACIENTE
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MIS_CITAS")
                                .ifPresent(permisosCorrectos::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "AGENDAR_CITA")
                                .ifPresent(permisosCorrectos::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "CANCELAR_MIS_CITAS")
                                .ifPresent(permisosCorrectos::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MI_PERFIL")
                                .ifPresent(permisosCorrectos::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "EDITAR_MI_PERFIL")
                                .ifPresent(permisosCorrectos::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MIS_TRATAMIENTOS")
                                .ifPresent(permisosCorrectos::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MI_ODONTOGRAMA")
                                .ifPresent(permisosCorrectos::add);
                permisoRepository.findByModuloAndAccion("PANEL_PACIENTE", "VER_MIS_COMPROBANTES")
                                .ifPresent(permisosCorrectos::add);

                rolPaciente.setPermisos(permisosCorrectos);
                rolRepository.save(rolPaciente);

                System.out.println("   ✅ Permisos actualizados: " + permisosCorrectos.size());
                System.out.println("   - Permisos eliminados: " + (permisosAnteriores - permisosCorrectos.size()));
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

        private CategoriaProcedimiento crearCategoriaSiNoExiste(String nombre, String desc, String icono,
                        String color) {
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
                        // Establecer stock actual inicial (10x el stock mínimo para tener suficiente
                        // inventario)
                        insumo.setStockActual(stockMinimo.multiply(BigDecimal.valueOf(10)));
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

        private void crearMotivoSiNoExiste(String nombre, TipoMovimiento tipo, boolean esManual) {
                motivoMovimientoRepository.findByNombre(nombre).orElseGet(() -> {
                        MotivoMovimiento mm = new MotivoMovimiento();
                        mm.setNombre(nombre);
                        mm.setTipoMovimiento(tipo);
                        mm.setEsManual(esManual);
                        return motivoMovimientoRepository.save(mm);
                });
        }

        private void crearEstadoCitaSiNoExiste(String nombre, String descripcion, String colorUi) {
                estadoCitaRepository.findByNombre(nombre).orElseGet(() -> {
                        EstadoCita estado = new EstadoCita();
                        estado.setNombre(nombre);
                        estado.setDescripcion(descripcion);
                        estado.setColorUi(colorUi);
                        return estadoCitaRepository.save(estado);
                });
        }

        private void crearEstadoPagoSiNoExiste(String nombre, String descripcion) {
                estadoPagoRepository.findByNombre(nombre).orElseGet(() -> {
                        EstadoPago estado = new EstadoPago();
                        estado.setNombre(nombre);
                        estado.setDescripcion(descripcion);
                        return estadoPagoRepository.save(estado);
                });
        }

        private void crearMetodoPagoSiNoExiste(String nombre, String descripcion) {
                metodoPagoRepository.findByNombre(nombre).orElseGet(() -> {
                        MetodoPago metodo = new MetodoPago();
                        metodo.setNombre(nombre);
                        metodo.setDescripcion(descripcion);
                        return metodoPagoRepository.save(metodo);
                });
        }

        private void crearProcedimientoInsumoSiNoExiste(String codigoProcedimiento, String codigoInsumo,
                        BigDecimal cantidad, String unidad, boolean esObligatorio) {
                Procedimiento proc = procedimientoRepository.findByCodigo(codigoProcedimiento).orElse(null);
                Insumo insumo = insumoRepository.findByCodigo(codigoInsumo).orElse(null);

                if (proc != null && insumo != null) {
                        procedimientoInsumoRepository.findByProcedimientoAndInsumo(proc, insumo).orElseGet(() -> {
                                ProcedimientoInsumo pi = new ProcedimientoInsumo();
                                pi.setProcedimiento(proc);
                                pi.setInsumo(insumo);
                                pi.setCantidadDefecto(cantidad);
                                pi.setUnidad(unidad);
                                pi.setEsObligatorio(esObligatorio);
                                return procedimientoInsumoRepository.save(pi);
                        });
                }
        }
}