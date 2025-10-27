package com.odontoapp.servicio;

import com.odontoapp.dto.InsumoDTO;
import com.odontoapp.entidad.CategoriaInsumo;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.UnidadMedida;
import com.odontoapp.repositorio.CategoriaInsumoRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.UnidadMedidaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import jakarta.transaction.Transactional;

@Service
public class InsumoServiceImpl implements InsumoService {

    private final InsumoRepository insumoRepository;
    private final CategoriaInsumoRepository categoriaInsumoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    public InsumoServiceImpl(InsumoRepository insumoRepository, CategoriaInsumoRepository categoriaInsumoRepository,
            UnidadMedidaRepository unidadMedidaRepository) {
        this.insumoRepository = insumoRepository;
        this.categoriaInsumoRepository = categoriaInsumoRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
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
    @Transactional
    public Insumo guardar(InsumoDTO dto) {
        if (dto.getId() != null && dto.getCodigo() != null && !dto.getCodigo().isBlank()) {
            insumoRepository.findByCodigo(dto.getCodigo()).ifPresent(existente -> {
                if (!existente.getId().equals(dto.getId())) {
                    throw new DataIntegrityViolationException("El código '" + dto.getCodigo() + "' ya está en uso.");
                }
            });
        }

        boolean esNuevo = dto.getId() == null;

        Insumo insumo = esNuevo ? new Insumo()
                : insumoRepository.findById(dto.getId())
                        .orElseThrow(() -> new IllegalStateException("Insumo no encontrado con ID: " + dto.getId()));

        CategoriaInsumo categoria = categoriaInsumoRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new IllegalStateException("Categoría no encontrada."));
        UnidadMedida unidad = unidadMedidaRepository.findById(dto.getUnidadMedidaId())
                .orElseThrow(() -> new IllegalStateException("Unidad de medida no encontrada."));

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

        if (esNuevo) {
            insumo.setCodigo("TEMP-" + System.currentTimeMillis());
            Insumo insumoGuardadoTemporal = insumoRepository.saveAndFlush(insumo);

            String codigoGenerado = "INS-" + String.format("%05d", insumoGuardadoTemporal.getId());
            insumoGuardadoTemporal.setCodigo(codigoGenerado);

            return insumoRepository.save(insumoGuardadoTemporal);
        } else {
            return insumoRepository.save(insumo);
        }
    }

    @Override
    public void eliminar(Long id) {
        if (!insumoRepository.existsById(id)) {
            throw new IllegalStateException("El insumo no existe.");
        }
        insumoRepository.deleteById(id);
    }
}