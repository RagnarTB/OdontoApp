// ruta: com/odontoapp/servicio/CategoriaInsumoServiceImpl.java
package com.odontoapp.servicio;

import com.odontoapp.entidad.CategoriaInsumo;
import com.odontoapp.repositorio.CategoriaInsumoRepository;
import com.odontoapp.repositorio.InsumoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriaInsumoServiceImpl implements CategoriaInsumoService {

    private final CategoriaInsumoRepository categoriaRepository;
    private final InsumoRepository insumoRepository;

    public CategoriaInsumoServiceImpl(CategoriaInsumoRepository categoriaRepository,
            InsumoRepository insumoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.insumoRepository = insumoRepository;
    }

    @Override
    public List<CategoriaInsumo> listarTodasOrdenadasPorNombre() {
        return categoriaRepository.findAll(Sort.by("nombre"));
    }

    @Override
    public List<CategoriaInsumo> listarCategoriasActivas() {
        return categoriaRepository.findByEstaActivaTrue();
    }

    @Override
    public Optional<CategoriaInsumo> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    @Override
    public void guardar(CategoriaInsumo categoria) {
        categoriaRepository.findByNombre(categoria.getNombre()).ifPresent(existente -> {
            if (!existente.getId().equals(categoria.getId())) {
                throw new DataIntegrityViolationException(
                        "El nombre de categoría '" + categoria.getNombre() + "' ya existe.");
            }
        });
        categoriaRepository.save(categoria);
    }

    @Override
    public void eliminar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new IllegalStateException("La categoría no existe.");
        }
        long conteoInsumos = insumoRepository.countByCategoriaId(id);
        if (conteoInsumos > 0) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar la categoría porque tiene " + conteoInsumos + " insumo(s) asociado(s).");
        }
        categoriaRepository.deleteById(id);
    }

    @Override
    public void cambiarEstado(Long id) {
        CategoriaInsumo categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Categoría no encontrada."));

        if (categoria.isEstaActiva()) {
            long conteoInsumos = insumoRepository.countByCategoriaId(id);
            if (conteoInsumos > 0) {
                throw new DataIntegrityViolationException("No se puede desactivar la categoría porque tiene "
                        + conteoInsumos + " insumo(s) asociado(s).");
            }
        }
        categoria.setEstaActiva(!categoria.isEstaActiva());
        categoriaRepository.save(categoria);
    }
}
