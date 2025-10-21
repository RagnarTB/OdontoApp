package com.odontoapp.entidad;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "permisos")
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ej: PACIENTES, CITAS, USUARIOS
    private String modulo;

    // Ej: VER, CREAR, EDITAR, ELIMINAR
    private String accion;

    public String getNombre() {
        return accion + "_" + modulo;
    }
}