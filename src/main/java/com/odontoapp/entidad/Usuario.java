package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // Importar si usas @Data con herencia
import lombok.ToString; // Importar si usas @Data con herencia
import java.time.LocalDateTime;
import java.util.Set;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@EqualsAndHashCode(callSuper = true) // Necesario con @Data y herencia
@ToString(callSuper = true) // Necesario con @Data y herencia
@Entity
@Table(name = "usuarios")
@SQLDelete(sql = "UPDATE usuarios SET eliminado = true, fecha_eliminacion = NOW() WHERE id = ?")
@Where(clause = "eliminado = false")
public class Usuario extends EntidadAuditable { // <-- Extiende EntidadAuditable
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean estaActivo = true;
    private int intentosFallidos = 0;
    private LocalDateTime fechaBloqueo;

    @EqualsAndHashCode.Exclude // Evitar recursión en equals/hashCode
    @ToString.Exclude // Evitar recursión en toString
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "usuarios_roles", joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "rol_id", referencedColumnName = "id"))
    private Set<Rol> roles;

    @EqualsAndHashCode.Exclude // Evitar recursión en equals/hashCode
    @ToString.Exclude // Evitar recursión en toString
    @OneToOne(mappedBy = "usuario")
    private Paciente paciente;

    private String verificationToken;

    // --- NUEVOS CAMPOS SOFT DELETE ---
    private boolean eliminado = false;
    private LocalDateTime fechaEliminacion;
}