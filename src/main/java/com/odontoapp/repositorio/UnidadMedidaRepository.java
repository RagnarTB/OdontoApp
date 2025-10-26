package com.odontoapp.repositorio;

import com.odontoapp.entidad.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    Optional<UnidadMedida> findByAbreviatura(String abreviatura);
}
