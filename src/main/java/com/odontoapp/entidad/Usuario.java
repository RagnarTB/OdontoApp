// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\entidad\Usuario.java
package com.odontoapp.entidad;

import java.time.DayOfWeek; // NUEVO import
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList; // NUEVO import
import java.util.HashMap; // NUEVO import
import java.util.List; // NUEVO import
import java.util.Map; // NUEVO import
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable; // NUEVO import
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection; // NUEVO import
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType; // NUEVO import
import jakarta.persistence.FetchType; // NUEVO import
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated; // NUEVO import
import jakarta.persistence.OneToOne; // NUEVO import
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
// ACTUALIZADO: Añadir nuevas colecciones a las exclusiones
@EqualsAndHashCode(callSuper = true, exclude = { "roles", "paciente", "horarioRegular", "excepcionesHorario" })
@ToString(callSuper = true, exclude = { "roles", "paciente", "horarioRegular", "excepcionesHorario" })
@Entity
@Table(name = "usuarios")
@SQLDelete(sql = "UPDATE usuarios SET eliminado = true, fecha_eliminacion = NOW() WHERE id = ?")
@Where(clause = "eliminado = false")
public class Usuario extends EntidadAuditable { // Extiende EntidadAuditable
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Solo campos del Usuario (Admin, Odontologo, Recepcionista, etc.)
    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean estaActivo = true;
    private int intentosFallidos = 0;
    private LocalDateTime fechaBloqueo;

    // Este campo es solo para el flujo de activaciÃ³n
    private String verificationToken;

    // --- Relaciones ---
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "usuarios_roles", joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "rol_id", referencedColumnName = "id"))
    private Set<Rol> roles;

    @OneToOne(mappedBy = "usuario")
    private Paciente paciente;

    // --- Campos Soft Delete ---
    private boolean eliminado = false;
    private LocalDateTime fechaEliminacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id") // Puede ser nullable si no es obligatorio para todos
    private TipoDocumento tipoDocumento;

    @Column(length = 20, name = "numero_documento") // Puede necesitar unique constraint si es clave
    private String numeroDocumento;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    private LocalDate fechaNacimiento;

    @Column(name = "fecha_contratacion")
    private LocalDate fechaContratacion; // Fecha de contrataciÃ³n/creaciÃ³n inicial

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso; // Fecha y hora del Ãºltimo login

    @Column(name = "debe_actualizar_password")
    private boolean debeActualizarPassword = false;

    @Column(name = "password_temporal")
    private String passwordTemporal;

    // --- NUEVOS CAMPOS PARA HORARIO (Odontólogo) ---

    /**
     * Almacena el horario regular semanal.
     * La clave es el día de la semana (LUNES, MARTES, etc.).
     * El valor es un String que representa los intervalos de trabajo, separados por
     * comas.
     * Ejemplo: "09:00-13:00,15:00-19:00"
     */
    @ElementCollection(fetch = FetchType.LAZY) // Carga diferida es mejor para datos potencialmente grandes o no siempre
                                               // necesarios
    @CollectionTable(name = "usuario_horario_regular", joinColumns = @JoinColumn(name = "usuario_id")) // Nombre de la
                                                                                                       // tabla
                                                                                                       // intermedia
    @MapKeyColumn(name = "dia_semana") // Nombre de la columna para la clave (Lunes, Martes...)
    @MapKeyEnumerated(EnumType.STRING) // Guardar el nombre del día ("MONDAY", "TUESDAY", etc.)
    @Column(name = "horas", length = 100) // Nombre de la columna para el valor (String con horas)
    private Map<DayOfWeek, String> horarioRegular = new HashMap<>();

    /**
     * Almacena excepciones al horario regular para fechas específicas.
     * Utiliza la clase @Embeddable HorarioExcepcion.
     */
    @ElementCollection(fetch = FetchType.LAZY) // Carga diferida
    @CollectionTable(name = "usuario_horario_excepcion", joinColumns = @JoinColumn(name = "usuario_id")) // Nombre tabla
                                                                                                         // intermedia
    private List<HorarioExcepcion> excepcionesHorario = new ArrayList<>();

    // --- FIN NUEVOS CAMPOS PARA HORARIO ---

}
