package com.odontoapp.entidad;

import java.time.LocalDate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "usuario" }) // Excluir relaciones
@ToString(callSuper = true, exclude = { "usuario" }) // Excluir relaciones
@Entity
@Table(name = "pacientes")
@SQLDelete(sql = "UPDATE pacientes SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class Paciente extends EntidadAuditable { // <-- Extiende EntidadAuditable
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 8)
    private String dni;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column(unique = true)
    private String email;

    private String telefono;
    private LocalDate fechaNacimiento;
    private String direccion;

    @Lob
    private String alergias;

    @Lob
    private String antecedentesMedicos;

    private boolean eliminado = false; // Mantenemos esta explícitamente

    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", unique = true)
    private Usuario usuario;
}