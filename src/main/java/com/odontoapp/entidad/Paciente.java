// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\entidad\Paciente.java
package com.odontoapp.entidad;

import java.time.LocalDate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "usuario", "tipoDocumento" }) // Excluir nuevas relaciones
@ToString(callSuper = true, exclude = { "usuario", "tipoDocumento" })
@Entity
@Table(name = "pacientes", uniqueConstraints = { // Constraint único en la dupla
        @UniqueConstraint(columnNames = { "tipo_documento_id", "numero_documento" })
})
@SQLDelete(sql = "UPDATE pacientes SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class Paciente extends EntidadAuditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, name = "numero_documento")
    private String numeroDocumento; // Reemplaza dni

    @ManyToOne(fetch = FetchType.LAZY) // Nueva relación
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

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

    private boolean eliminado = false;

    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", unique = true)
    private Usuario usuario;
}