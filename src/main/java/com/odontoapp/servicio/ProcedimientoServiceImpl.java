package com.odontoapp.servicio;

import com.odontoapp.dto.ProcedimientoDTO;
import com.odontoapp.entidad.CategoriaProcedimiento;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.ProcedimientoInsumo;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.repositorio.CategoriaProcedimientoRepository;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.ProcedimientoInsumoRepository;
import com.odontoapp.repositorio.InsumoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProcedimientoServiceImpl implements ProcedimientoService {

    private final ProcedimientoRepository procedimientoRepository;
    private final CategoriaProcedimientoRepository categoriaRepository;
    private final CitaRepository citaRepository;
    private final ProcedimientoInsumoRepository procedimientoInsumoRepository;
    private final InsumoRepository insumoRepository;

    public ProcedimientoServiceImpl(ProcedimientoRepository procedimientoRepository,
                                     CategoriaProcedimientoRepository categoriaRepository,
                                     CitaRepository citaRepository,
                                     ProcedimientoInsumoRepository procedimientoInsumoRepository,
                                     InsumoRepository insumoRepository) {
        this.procedimientoRepository = procedimientoRepository;
        this.categoriaRepository = categoriaRepository;
        this.citaRepository = citaRepository;
        this.procedimientoInsumoRepository = procedimientoInsumoRepository;
        this.insumoRepository = insumoRepository;
    }

    @Override
    public Page<Procedimiento> listarTodos(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return procedimientoRepository.findByKeyword(keyword, pageable);
        }
        return procedimientoRepository.findAllWithRelations(pageable);
    }

    @Override
    public Optional<Procedimiento> buscarPorId(Long id) {
        return procedimientoRepository.findById(id);
    }

    @Override
    @Transactional
    public void guardar(ProcedimientoDTO dto) {
        // Validación de código único
        Optional<Procedimiento> existente = procedimientoRepository.findByCodigo(dto.getCodigo());
        if (existente.isPresent() && !existente.get().getId().equals(dto.getId())) {
            throw new DataIntegrityViolationException("El código '" + dto.getCodigo() + "' ya está en uso.");
        }

        Procedimiento procedimiento = (dto.getId() != null)
                ? procedimientoRepository.findById(dto.getId()).orElse(new Procedimiento())
                : new Procedimiento();

        CategoriaProcedimiento categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new IllegalStateException("Categoría no encontrada."));

        // Validar que la categoría no esté eliminada (tiene soft delete)
        if (categoria.isEliminado()) {
            throw new IllegalStateException("No se puede usar una categoría de procedimiento eliminada.");
        }

        procedimiento.setCodigo(dto.getCodigo());
        procedimiento.setNombre(dto.getNombre());
        procedimiento.setDescripcion(dto.getDescripcion());
        procedimiento.setPrecioBase(dto.getPrecioBase());
        procedimiento.setDuracionBaseMinutos(dto.getDuracionBaseMinutos());
        procedimiento.setCategoria(categoria);

        // Guardar el procedimiento primero
        procedimiento = procedimientoRepository.save(procedimiento);

        // Gestionar insumos asociados
        // Si estamos editando, eliminar todas las relaciones existentes primero
        if (procedimiento.getId() != null) {
            List<ProcedimientoInsumo> insumosExistentes = procedimientoInsumoRepository.findByProcedimientoId(procedimiento.getId());
            if (!insumosExistentes.isEmpty()) {
                procedimientoInsumoRepository.deleteAll(insumosExistentes);
                // Forzar el flush para asegurar que los deletes se ejecuten antes de los inserts
                procedimientoInsumoRepository.flush();
            }
        }

        // Crear nuevas relaciones solo si hay insumos en el DTO
        if (dto.getInsumos() != null && !dto.getInsumos().isEmpty()) {
            for (ProcedimientoDTO.InsumoItemDTO insumoItem : dto.getInsumos()) {
                Insumo insumo = insumoRepository.findById(insumoItem.getInsumoId())
                        .orElseThrow(() -> new IllegalStateException("Insumo no encontrado: " + insumoItem.getInsumoId()));

                ProcedimientoInsumo procedimientoInsumo = new ProcedimientoInsumo();
                procedimientoInsumo.setProcedimiento(procedimiento);
                procedimientoInsumo.setInsumo(insumo);
                procedimientoInsumo.setCantidadDefecto(insumoItem.getCantidad());
                procedimientoInsumo.setUnidad(insumoItem.getUnidad());
                procedimientoInsumo.setEsObligatorio(insumoItem.isEsObligatorio());

                procedimientoInsumoRepository.save(procedimientoInsumo);
            }
        }
    }

    @Override
    public void eliminar(Long id) {
        if (!procedimientoRepository.existsById(id)) {
            throw new IllegalStateException("El procedimiento no existe.");
        }

        // Validar que no tenga citas asociadas
        long conteoCitas = citaRepository.countByProcedimientoId(id);
        if (conteoCitas > 0) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar el procedimiento porque tiene " + conteoCitas +
                    " cita(s) asociada(s).");
        }

        procedimientoRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void restablecer(Long id) {
        Procedimiento procedimiento = procedimientoRepository.findByIdIgnorandoSoftDelete(id)
                .orElseThrow(() -> new IllegalStateException("Procedimiento no encontrado con ID: " + id));

        if (!procedimiento.isEliminado()) {
            throw new IllegalStateException("El procedimiento '" + procedimiento.getNombre() + "' no está eliminado.");
        }

        // Restablecer el procedimiento
        procedimiento.setEliminado(false);
        procedimientoRepository.save(procedimiento);

        System.out.println("✅ Procedimiento '" + procedimiento.getNombre() + "' restablecido exitosamente.");
    }
}
