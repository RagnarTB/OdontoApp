// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\repositorio\TipoDocumentoRepository.java
package com.odontoapp.repositorio;

import com.odontoapp.entidad.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {
    Optional<TipoDocumento> findByCodigo(String codigo);
}