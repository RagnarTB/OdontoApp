package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "pacientes")
@SQLDelete(sql = "UPDATE pacientes SET eliminado = true WHERE id = ?") // <-- Soft Delete
@Where(clause = "eliminado = false") // <-- Siempre filtra los eliminados
public class Paciente {
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

    @Lob // Para textos largos
    private String alergias;

    @Lob
    private String antecedentesMedicos;

    private boolean eliminado = false; // <-- Columna para el Soft Delete
}