package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime; // <-- Importar
import java.util.Set;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean estaActivo = true;

    // --- CAMPOS NUEVOS PARA SEGURIDAD ---
    private int intentosFallidos = 0;
    private LocalDateTime fechaBloqueo;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "usuarios_roles", joinColumns = @JoinColumn(name = "usuario_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "rol_id", referencedColumnName = "id"))
    private Set<Rol> roles;

    @OneToOne(mappedBy = "usuario")
    private Paciente paciente;

    // --- AÑADIR ESTE CAMPO ---
    private String verificationToken;
}