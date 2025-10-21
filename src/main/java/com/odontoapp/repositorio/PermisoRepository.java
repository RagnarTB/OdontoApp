package com.odontoapp.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.odontoapp.entidad.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    Optional<Permiso> findByModuloAndAccion(String modulo, String accion);
}