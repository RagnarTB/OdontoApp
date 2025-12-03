package com.odontoapp.repositorio;

import com.odontoapp.entidad.MotivoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MotivoMovimientoRepository extends JpaRepository<MotivoMovimiento, Long> {
    Optional<MotivoMovimiento> findByNombre(String nombre);

    // Buscar motivos manuales por tipo de movimiento
    @Query("SELECT m FROM MotivoMovimiento m WHERE m.tipoMovimiento.codigo = :codigoTipo AND m.esManual = :esManual")
    List<MotivoMovimiento> findByTipoMovimientoCodigoAndEsManual(@Param("codigoTipo") String codigoTipo,
            @Param("esManual") boolean esManual);

    // Buscar todos los motivos de un tipo (manuales y automáticos)
    @Query("SELECT m FROM MotivoMovimiento m WHERE m.tipoMovimiento.codigo = :codigoTipo")
    List<MotivoMovimiento> findByTipoMovimientoCodigo(@Param("codigoTipo") String codigoTipo);
}