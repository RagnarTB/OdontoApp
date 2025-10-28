package com.odontoapp.repositorio;

import com.odontoapp.entidad.ProcedimientoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedimientoInsumoRepository extends JpaRepository<ProcedimientoInsumo, Long> {

    /**
     * Busca todos los insumos asociados a un procedimiento específico.
     * @param procedimientoId El ID del procedimiento
     * @return Lista de relaciones procedimiento-insumo
     */
    List<ProcedimientoInsumo> findByProcedimientoId(Long procedimientoId);

    /**
     * Busca una relación específica entre procedimiento e insumo.
     * @param procedimientoId El ID del procedimiento
     * @param insumoId El ID del insumo
     * @return Optional con la relación si existe
     */
    Optional<ProcedimientoInsumo> findByProcedimientoIdAndInsumoId(Long procedimientoId, Long insumoId);
}
