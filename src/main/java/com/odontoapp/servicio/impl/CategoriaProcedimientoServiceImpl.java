package com.odontoapp.servicio.impl;

import com.odontoapp.entidad.CategoriaProcedimiento;
import com.odontoapp.repositorio.CategoriaProcedimientoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.servicio.CategoriaProcedimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio para gestionar categorías de procedimientos
 */
@Service
@RequiredArgsConstructor
public class CategoriaProcedimientoServiceImpl implements CategoriaProcedimientoService {

    private final CategoriaProcedimientoRepository categoriaRepository;
    private final ProcedimientoRepository procedimientoRepository;

    @Override
    public List<CategoriaProcedimiento> listarTodasOrdenadasPorNombre() {
        return categoriaRepository.findAll(Sort.by("nombre"));
    }

    @Override
    public List<CategoriaProcedimiento> listarCategoriasActivas() {
        return categoriaRepository.findByEstaActivaTrue();
    }

    @Override
    public Optional<CategoriaProcedimiento> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    @Override
    public void guardar(CategoriaProcedimiento categoria) {
        // Validar que no exista otra categoría con el mismo nombre
        categoriaRepository.findByNombre(categoria.getNombre()).ifPresent(existente -> {
            if (!existente.getId().equals(categoria.getId())) {
                throw new DataIntegrityViolationException(
                    "El nombre de categoría '" + categoria.getNombre() + "' ya existe."
                );
            }
        });
        categoriaRepository.save(categoria);
    }

    @Override
    public void eliminar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new IllegalStateException("La categoría no existe.");
        }

        // Validar que no tenga procedimientos asociados
        long conteoProcedimientos = procedimientoRepository.countByCategoriaId(id);
        if (conteoProcedimientos > 0) {
            throw new DataIntegrityViolationException(
                "No se puede eliminar la categoría porque tiene " + conteoProcedimientos +
                " procedimiento(s) asociado(s)."
            );
        }

        categoriaRepository.deleteById(id);
    }

    @Override
    public void cambiarEstado(Long id) {
        CategoriaProcedimiento categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Categoría no encontrada."));

        // Si se está desactivando, validar que no tenga procedimientos activos
        if (categoria.isEstaActiva()) {
            long conteoProcedimientos = procedimientoRepository.countByCategoriaId(id);
            if (conteoProcedimientos > 0) {
                throw new DataIntegrityViolationException(
                    "No se puede desactivar la categoría porque tiene " +
                    conteoProcedimientos + " procedimiento(s) asociado(s)."
                );
            }
        }

        categoria.setEstaActiva(!categoria.isEstaActiva());
        categoriaRepository.save(categoria);
    }
}
