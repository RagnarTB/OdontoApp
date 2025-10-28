package com.odontoapp.servicio;

import com.odontoapp.dto.ProcedimientoDTO;
import com.odontoapp.entidad.CategoriaProcedimiento;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.repositorio.CategoriaProcedimientoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProcedimientoServiceImpl implements ProcedimientoService {

    private final ProcedimientoRepository procedimientoRepository;
    private final CategoriaProcedimientoRepository categoriaRepository;

    public ProcedimientoServiceImpl(ProcedimientoRepository procedimientoRepository, CategoriaProcedimientoRepository categoriaRepository) {
        this.procedimientoRepository = procedimientoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public Page<Procedimiento> listarTodos(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return procedimientoRepository.findByKeyword(keyword, pageable);
        }
        return procedimientoRepository.findAll(pageable);
    }

    @Override
    public Optional<Procedimiento> buscarPorId(Long id) {
        return procedimientoRepository.findById(id);
    }

    @Override
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

        procedimientoRepository.save(procedimiento);
    }

    @Override
    public void eliminar(Long id) {
        if (!procedimientoRepository.existsById(id)) {
            throw new IllegalStateException("El procedimiento no existe.");
        }

        // NOTA: Si existe una entidad Cita que referencia a Procedimiento,
        // se debería validar que no haya citas asociadas antes de eliminar.
        // Ejemplo:
        // long conteoCitas = citaRepository.countByProcedimientoId(id);
        // if (conteoCitas > 0) {
        //     throw new DataIntegrityViolationException(
        //             "No se puede eliminar el procedimiento porque tiene citas asociadas.");
        // }

        procedimientoRepository.deleteById(id);
    }
}
