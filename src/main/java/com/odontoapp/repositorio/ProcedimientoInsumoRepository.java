package com.odontoapp.repositorio;

import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcedimientoInsumoRepository extends JpaRepository<ProcedimientoInsumo, Long> {

    /**
     * Obtener todos los insumos de un procedimiento
     */
    List<ProcedimientoInsumo> findByProcedimiento(Procedimiento procedimiento);

    /**
     * Obtener solo insumos obligatorios
     */
    List<ProcedimientoInsumo> findByProcedimientoAndEsObligatorio(Procedimiento procedimiento, boolean esObligatorio);

    /**
     * Verificar si existe relación específica
     */
    Optional<ProcedimientoInsumo> findByProcedimientoAndInsumo(Procedimiento procedimiento, Insumo insumo);

    /**
     * Obtener procedimientos que usan un insumo específico
     */
    List<ProcedimientoInsumo> findByInsumo(Insumo insumo);

    /**
     * Eliminar relación específica
     */
    void deleteByProcedimientoAndInsumo(Procedimiento procedimiento, Insumo insumo);
}
