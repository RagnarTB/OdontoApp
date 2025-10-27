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
import java.util.List;

@Service
public class ProcedimientoServiceImpl implements ProcedimientoService {

    private final ProcedimientoRepository procedimientoRepository;
    private final CategoriaProcedimientoRepository categoriaRepository;

    public ProcedimientoServiceImpl(ProcedimientoRepository procedimientoRepository,
            CategoriaProcedimientoRepository categoriaRepository) {
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
        procedimientoRepository.deleteById(id);
    }

    @Override
    public void cambiarEstado(Long id) {
        Procedimiento procedimiento = procedimientoRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Servicio no encontrado."));

        // Lógica de negocio opcional: Si se intenta desactivar, validar que no esté en
        // citas futuras.
        // if (procedimiento.isEstaActivo()) {
        // long citasFuturas = citaRepository.countByProcedimientoIdAndFechaAfter(id,
        // LocalDateTime.now());
        // if (citasFuturas > 0) {
        // throw new DataIntegrityViolationException("No se puede desactivar. El
        // servicio está agendado en " + citasFuturas + " cita(s) futura(s).");
        // }
        // }
        procedimiento.setEstaActivo(!procedimiento.isEstaActivo());
        procedimientoRepository.save(procedimiento);
    }

    @Override
    public List<Procedimiento> listarActivos() {
        return procedimientoRepository.findByEstaActivoTrue();
    }
}
