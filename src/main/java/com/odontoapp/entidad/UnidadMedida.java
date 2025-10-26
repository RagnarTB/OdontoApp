package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "unidades_medida")
@SQLDelete(sql = "UPDATE unidades_medida SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class UnidadMedida extends EntidadAuditable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre; // Ej: Unidad, Caja, Gramo

    @Column(nullable = false, unique = true)
    private String abreviatura; // Ej: und, cja, g

    private boolean eliminado = false;
}
