package com.odontoapp.servicio.impl;

import com.odontoapp.dto.ProcedimientoInsumoDTO;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.servicio.ProcedimientoInsumoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de gestión de la relación entre procedimientos e insumos.
 * Administra qué insumos se utilizan en cada procedimiento dental y en qué cantidad.
 */
@Service
@Transactional
public class ProcedimientoInsumoServiceImpl implements ProcedimientoInsumoService {

    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final InsumoRepository insumoRepository;

    public ProcedimientoInsumoServiceImpl(ProcedimientoInsumoRepository procedimientoInsumoRepository,
                                         ProcedimientoRepository procedimientoRepository,
                                         InsumoRepository insumoRepository) {
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.insumoRepository = insumoRepository;
    }

    @Override
    @Transactional
    public ProcedimientoInsumo asignarInsumoAProcedimiento(ProcedimientoInsumoDTO dto) {
        // Validar que los IDs no sean nulos
        if (dto.getProcedimientoId() == null) {
            throw new IllegalArgumentException("El ID del procedimiento no puede ser nulo");
        }
        if (dto.getInsumoId() == null) {
            throw new IllegalArgumentException("El ID del insumo no puede ser nulo");
        }
        if (dto.getCantidadDefault() == null) {
            throw new IllegalArgumentException("La cantidad por defecto no puede ser nula");
        }

        // Validar que la cantidad sea positiva
        if (dto.getCantidadDefault().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad por defecto debe ser mayor a cero");
        }

        // Buscar el procedimiento
        Procedimiento procedimiento = procedimientoRepository.findById(dto.getProcedimientoId())
            .orElseThrow(() -> new EntityNotFoundException(
                "No se encontró el procedimiento con ID: " + dto.getProcedimientoId()));

        // Buscar el insumo
        Insumo insumo = insumoRepository.findById(dto.getInsumoId())
            .orElseThrow(() -> new EntityNotFoundException(
                "No se encontró el insumo con ID: " + dto.getInsumoId()));

        // Verificar si ya existe la relación (duplicado)
        Optional<ProcedimientoInsumo> relacionExistente =
            procedimientoInsumoRepository.findByProcedimientoIdAndInsumoId(
                dto.getProcedimientoId(), dto.getInsumoId());

        if (relacionExistente.isPresent()) {
            throw new IllegalStateException(
                "Este insumo ya está asignado a este procedimiento. " +
                "Use el método de actualización si desea modificar la cantidad.");
        }

        // Crear la nueva relación
        ProcedimientoInsumo nuevaRelacion = new ProcedimientoInsumo();
        nuevaRelacion.setProcedimiento(procedimiento);
        nuevaRelacion.setInsumo(insumo);
        nuevaRelacion.setCantidadDefault(dto.getCantidadDefault());

        // Guardar y retornar
        return procedimientoInsumoRepository.save(nuevaRelacion);
    }

    @Override
    @Transactional
    public ProcedimientoInsumo actualizarCantidadInsumo(Long procedimientoInsumoId, BigDecimal nuevaCantidad) {
        // Validar que la nueva cantidad no sea nula
        if (nuevaCantidad == null) {
            throw new IllegalArgumentException("La nueva cantidad no puede ser nula");
        }

        // Validar que la cantidad sea positiva
        if (nuevaCantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        // Buscar la relación existente
        ProcedimientoInsumo relacion = procedimientoInsumoRepository.findById(procedimientoInsumoId)
            .orElseThrow(() -> new EntityNotFoundException(
                "No se encontró la relación procedimiento-insumo con ID: " + procedimientoInsumoId));

        // Actualizar la cantidad
        relacion.setCantidadDefault(nuevaCantidad);

        // Guardar y retornar
        return procedimientoInsumoRepository.save(relacion);
    }

    @Override
    @Transactional
    public void quitarInsumoDeProcedimiento(Long procedimientoInsumoId) {
        // Verificar que la relación existe antes de eliminar
        if (!procedimientoInsumoRepository.existsById(procedimientoInsumoId)) {
            throw new EntityNotFoundException(
                "No se encontró la relación procedimiento-insumo con ID: " + procedimientoInsumoId);
        }

        // Eliminar la relación
        procedimientoInsumoRepository.deleteById(procedimientoInsumoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedimientoInsumo> buscarInsumosPorProcedimiento(Long procedimientoId) {
        // Validar que el ID no sea nulo
        if (procedimientoId == null) {
            throw new IllegalArgumentException("El ID del procedimiento no puede ser nulo");
        }

        // Buscar todas las relaciones para este procedimiento
        return procedimientoInsumoRepository.findByProcedimientoId(procedimientoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedimientoInsumo> buscarProcedimientosPorInsumo(Long insumoId) {
        // Validar que el ID no sea nulo
        if (insumoId == null) {
            throw new IllegalArgumentException("El ID del insumo no puede ser nulo");
        }

        // Buscar todas las relaciones para este insumo
        return procedimientoInsumoRepository.findByInsumoId(insumoId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedimientoInsumo buscarRelacion(Long procedimientoId, Long insumoId) {
        // Validar que los IDs no sean nulos
        if (procedimientoId == null) {
            throw new IllegalArgumentException("El ID del procedimiento no puede ser nulo");
        }
        if (insumoId == null) {
            throw new IllegalArgumentException("El ID del insumo no puede ser nulo");
        }

        // Buscar la relación específica
        return procedimientoInsumoRepository.findByProcedimientoIdAndInsumoId(procedimientoId, insumoId)
            .orElseThrow(() -> new EntityNotFoundException(
                "No se encontró una relación entre el procedimiento " + procedimientoId +
                " y el insumo " + insumoId));
    }
}
