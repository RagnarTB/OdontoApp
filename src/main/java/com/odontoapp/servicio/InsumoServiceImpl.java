package com.odontoapp.servicio;

import com.odontoapp.dto.InsumoDTO;
import com.odontoapp.entidad.CategoriaInsumo;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.UnidadMedida;
import com.odontoapp.repositorio.CategoriaInsumoRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.MovimientoInventarioRepository;
import com.odontoapp.repositorio.UnidadMedidaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class InsumoServiceImpl implements InsumoService {

    private final InsumoRepository insumoRepository;
    private final CategoriaInsumoRepository categoriaInsumoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public InsumoServiceImpl(InsumoRepository insumoRepository, CategoriaInsumoRepository categoriaInsumoRepository,
            UnidadMedidaRepository unidadMedidaRepository, MovimientoInventarioRepository movimientoInventarioRepository) {
        this.insumoRepository = insumoRepository;
        this.categoriaInsumoRepository = categoriaInsumoRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    @Override
    public Page<Insumo> listarTodos(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return insumoRepository.findByKeyword(keyword, pageable);
        }
        return insumoRepository.findAll(pageable);
    }
    
    @Override
    public List<Insumo> listarConStockBajo() {
        return insumoRepository.findInsumosConStockBajo(); // <-- Llamada al nuevo método del repositorio
    }

    @Override
    public Optional<Insumo> buscarPorId(Long id) {
        return insumoRepository.findById(id);
    }

    @Override
    public Insumo guardar(InsumoDTO dto) {
        Optional<Insumo> existente = insumoRepository.findByCodigo(dto.getCodigo());
        if (existente.isPresent() && !existente.get().getId().equals(dto.getId())) {
            throw new DataIntegrityViolationException("El código '" + dto.getCodigo() + "' ya está en uso.");
        }

        Insumo insumo = (dto.getId() != null)
                ? insumoRepository.findById(dto.getId()).orElse(new Insumo())
                : new Insumo();

        CategoriaInsumo categoria = categoriaInsumoRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new IllegalStateException("Categoría no encontrada."));

        // Validar que la categoría esté activa
        if (!categoria.isEstaActiva()) {
            throw new IllegalStateException("No se puede usar una categoría inactiva.");
        }

        UnidadMedida unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalStateException("Unidad de medida no encontrada."));

        // Validar que la unidad de medida no esté eliminada (tiene soft delete)
        if (unidad.isEliminado()) {
            throw new IllegalStateException("No se puede usar una unidad de medida eliminada.");
        }

        insumo.setCodigo(dto.getCodigo());
        insumo.setNombre(dto.getNombre());
        insumo.setDescripcion(dto.getDescripcion());
        insumo.setMarca(dto.getMarca());
        insumo.setUbicacion(dto.getUbicacion());
        insumo.setLote(dto.getLote());
        insumo.setFechaVencimiento(dto.getFechaVencimiento());
        insumo.setStockMinimo(dto.getStockMinimo());
        insumo.setPrecioUnitario(dto.getPrecioUnitario());
        insumo.setCategoria(categoria);
        insumo.setUnidadMedida(unidad);

        return insumoRepository.save(insumo);
    }

    @Override
    public void eliminar(Long id) {
        if (!insumoRepository.existsById(id)) {
            throw new IllegalStateException("El insumo no existe.");
        }

        // Validar que no tenga movimientos de inventario asociados
        long conteoMovimientos = movimientoInventarioRepository.countByInsumoId(id);
        if (conteoMovimientos > 0) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar el insumo porque tiene " + conteoMovimientos +
                    " movimiento(s) de inventario asociado(s). Considere desactivarlo en lugar de eliminarlo.");
        }

        insumoRepository.deleteById(id);
    }
}