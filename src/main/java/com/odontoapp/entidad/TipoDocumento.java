// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\entidad\TipoDocumento.java
package com.odontoapp.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "tipos_documento")
public class TipoDocumento extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre; // Ej: DNI, RUC, Carnet de Extranjería

    @Column(nullable = false, unique = true, length = 10)
    private String codigo; // Ej: DNI, RUC

    @Column(nullable = false)
    private boolean esNacional = true; // Permite saber si se consulta en API Reniec
}