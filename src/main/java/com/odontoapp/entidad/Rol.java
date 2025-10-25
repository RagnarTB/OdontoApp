package com.odontoapp.entidad;

import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = { "usuarios", "permisos" }) // Excluir colecciones
@ToString(callSuper = true, exclude = { "usuarios", "permisos" }) // Excluir colecciones
@Entity
@Table(name = "roles")
@SQLDelete(sql = "UPDATE roles SET eliminado = true WHERE id = ?")
@Where(clause = "eliminado = false")
public class Rol extends EntidadAuditable { // <-- Extiende EntidadAuditable
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @ManyToMany(mappedBy = "roles")
    private Set<Usuario> usuarios;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "roles_permisos", joinColumns = @JoinColumn(name = "rol_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "permiso_id", referencedColumnName = "id"))
    private Set<Permiso> permisos;

    private boolean estaActivo = true;

    // --- NUEVO CAMPO SOFT DELETE ---
    private boolean eliminado = false;

    public boolean isEliminado() {
        return eliminado;
    }
}