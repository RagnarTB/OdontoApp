// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\entidad\Usuario.java
package com.odontoapp.entidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "roles", "paciente" }) // Excluir colecciones/relaciones
                                                                        // bidireccionales
@ToString(callSuper = true, exclude = { "roles", "paciente" })
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

    // Este campo es solo para el flujo de activación
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

    private String telefono;

    private LocalDate fechaNacimiento;

    private String direccion;

    @Column(name = "debe_actualizar_password")
    private boolean debeActualizarPassword = false;

    @Column(name = "password_temporal")
    private String passwordTemporal;
}