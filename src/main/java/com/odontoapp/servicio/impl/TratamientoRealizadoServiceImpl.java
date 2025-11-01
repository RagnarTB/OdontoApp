package com.odontoapp.servicio.impl;

import com.odontoapp.dto.TratamientoRealizadoDTO;
import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.Insumo;
import com.odontoapp.entidad.Procedimiento;
import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.TratamientoRealizado;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.InsumoRepository;
import com.odontoapp.repositorio.ProcedimientoRepository;
import com.odontoapp.repositorio.TratamientoRealizadoRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.TratamientoRealizadoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de gestión de tratamientos realizados.
 * Maneja el registro, consulta y eliminación de tratamientos durante citas.
 */
@Service
@Transactional
public class TratamientoRealizadoServiceImpl implements TratamientoRealizadoService {

    private final TratamientoRealizadoRepository tratamientoRealizadoRepository;
    private final CitaRepository citaRepository;
    private final ProcedimientoRepository procedimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InsumoRepository insumoRepository;

    public TratamientoRealizadoServiceImpl(TratamientoRealizadoRepository tratamientoRealizadoRepository,
                                          CitaRepository citaRepository,
                                          ProcedimientoRepository procedimientoRepository,
                                          UsuarioRepository usuarioRepository,
                                          InsumoRepository insumoRepository) {
        this.tratamientoRealizadoRepository = tratamientoRealizadoRepository;
        this.citaRepository = citaRepository;
        this.procedimientoRepository = procedimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.insumoRepository = insumoRepository;
    }

    @Override
    @Transactional
    public TratamientoRealizado registrarTratamiento(TratamientoRealizadoDTO dto) {
        // Validar que los IDs obligatorios no sean nulos
        if (dto.getCitaId() == null) {
            throw new IllegalArgumentException("El ID de la cita es obligatorio");
        }
        if (dto.getProcedimientoId() == null) {
            throw new IllegalArgumentException("El ID del procedimiento es obligatorio");
        }
        if (dto.getOdontologoUsuarioId() == null) {
            throw new IllegalArgumentException("El ID del odontólogo es obligatorio");
        }
        if (dto.getFechaRealizacion() == null) {
            throw new IllegalArgumentException("La fecha de realización es obligatoria");
        }

        // Buscar la cita
        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cita no encontrada con ID: " + dto.getCitaId()));

        // Buscar el procedimiento
        Procedimiento procedimiento = procedimientoRepository.findById(dto.getProcedimientoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Procedimiento no encontrado con ID: " + dto.getProcedimientoId()));

        // Buscar el odontólogo
        Usuario odontologo = usuarioRepository.findById(dto.getOdontologoUsuarioId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Odontólogo no encontrado con ID: " + dto.getOdontologoUsuarioId()));

        // Validar que el usuario sea realmente un odontólogo
        boolean esOdontologo = odontologo.getRoles().stream()
                .map(Rol::getNombre)
                .anyMatch(nombre -> nombre.equals("ODONTOLOGO") || nombre.equals("ADMIN"));

        if (!esOdontologo) {
            throw new IllegalStateException(
                    "El usuario con ID " + dto.getOdontologoUsuarioId() + " no tiene el rol de odontólogo");
        }

        // Buscar el insumo ajustado si se especificó
        Insumo insumoAjustado = null;
        if (dto.getInsumoAjustadoId() != null) {
            insumoAjustado = insumoRepository.findById(dto.getInsumoAjustadoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Insumo no encontrado con ID: " + dto.getInsumoAjustadoId()));

            // Validar que si hay insumo ajustado, también haya cantidad
            if (dto.getCantidadInsumoAjustada() == null) {
                throw new IllegalArgumentException(
                        "Si se especifica un insumo ajustado, la cantidad ajustada es obligatoria");
            }
        }

        // Crear la nueva instancia de TratamientoRealizado
        TratamientoRealizado tratamiento = new TratamientoRealizado();
        tratamiento.setCita(cita);
        tratamiento.setProcedimiento(procedimiento);
        tratamiento.setOdontologo(odontologo);
        tratamiento.setPiezaDental(dto.getPiezaDental());
        tratamiento.setDescripcionTrabajo(dto.getDescripcionTrabajo());
        tratamiento.setFechaRealizacion(dto.getFechaRealizacion());
        tratamiento.setCantidadInsumoAjustada(dto.getCantidadInsumoAjustada());
        tratamiento.setInsumoAjustado(insumoAjustado);

        // Guardar y devolver la entidad
        return tratamientoRealizadoRepository.save(tratamiento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TratamientoRealizado> buscarPorCita(Long citaId) {
        if (citaId == null) {
            throw new IllegalArgumentException("El ID de la cita no puede ser nulo");
        }
        return tratamientoRealizadoRepository.findByCitaIdOrderByFechaRealizacionDesc(citaId);
    }

    @Override
    @Transactional(readOnly = true)
    public TratamientoRealizado buscarPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del tratamiento no puede ser nulo");
        }
        return tratamientoRealizadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tratamiento realizado no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void eliminarTratamiento(Long tratamientoId) {
        if (tratamientoId == null) {
            throw new IllegalArgumentException("El ID del tratamiento no puede ser nulo");
        }

        // Buscar el tratamiento para validar que existe
        TratamientoRealizado tratamiento = tratamientoRealizadoRepository.findById(tratamientoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Tratamiento realizado no encontrado con ID: " + tratamientoId));

        // Validaciones adicionales (opcional)
        // Por ejemplo, no permitir eliminar tratamientos de citas facturadas
        // if (tratamiento.getCita().tieneComprobante()) {
        //     throw new IllegalStateException("No se puede eliminar un tratamiento de una cita facturada");
        // }

        // Eliminar el tratamiento
        // Nota: Como TratamientoRealizado no tiene soft delete, esto es eliminación física
        tratamientoRealizadoRepository.deleteById(tratamientoId);
    }
}
