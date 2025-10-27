// ruta: com/odontoapp/servicio/CategoriaInsumoService.java
package com.odontoapp.servicio;

import com.odontoapp.entidad.CategoriaInsumo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface CategoriaInsumoService {
    List<CategoriaInsumo> listarTodasOrdenadasPorNombre();

    List<CategoriaInsumo> listarCategoriasActivas();

    Optional<CategoriaInsumo> buscarPorId(Long id);

    void guardar(CategoriaInsumo categoria);

    void eliminar(Long id);

    void cambiarEstado(Long id);
}
