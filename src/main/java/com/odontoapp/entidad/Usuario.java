// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\entidad\Usuario.java
package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.Set;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

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
}