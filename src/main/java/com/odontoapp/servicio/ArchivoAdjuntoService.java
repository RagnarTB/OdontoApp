package com.odontoapp.servicio;

import com.odontoapp.dto.ArchivoAdjuntoDTO;
import com.odontoapp.entidad.ArchivoAdjunto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para la gestión de archivos adjuntos (radiografías, documentos, etc.).
 * Maneja el almacenamiento, recuperación y eliminación de archivos.
 */
public interface ArchivoAdjuntoService {

    /**
     * Guarda un archivo en el sistema de archivos y registra sus metadatos.
     *
     * @param archivo Archivo a guardar
     * @param pacienteUsuarioId ID del paciente propietario del archivo
     * @param citaId ID de la cita asociada (opcional)
     * @param descripcion Descripción del archivo
     * @return El registro del archivo guardado
     * @throws IllegalArgumentException si el archivo está vacío o tiene formato inválido
     * @throws java.io.IOException si hay error al guardar el archivo
     */
    ArchivoAdjunto guardarArchivo(MultipartFile archivo, Long pacienteUsuarioId, Long citaId, String descripcion);

    /**
     * Busca un archivo adjunto por su ID.
     *
     * @param id ID del archivo
     * @return Optional con el archivo si existe
     */
    Optional<ArchivoAdjunto> buscarPorId(Long id);

    /**
     * Carga un archivo como recurso para su descarga o visualización.
     *
     * @param id ID del archivo
     * @return Resource con el contenido del archivo
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el archivo
     * @throws java.io.FileNotFoundException si el archivo físico no existe
     */
    Resource cargarArchivoComoRecurso(Long id);

    /**
     * Elimina un archivo adjunto (soft delete) y opcionalmente el archivo físico.
     *
     * @param id ID del archivo a eliminar
     * @throws jakarta.persistence.EntityNotFoundException si no se encuentra el archivo
     */
    void eliminarArchivo(Long id);

    /**
     * Busca todos los archivos adjuntos de un paciente.
     *
     * @param pacienteUsuarioId ID del paciente
     * @return Lista de DTOs con información de los archivos
     */
    List<ArchivoAdjuntoDTO> buscarPorPaciente(Long pacienteUsuarioId);

    /**
     * Busca todos los archivos adjuntos de una cita.
     *
     * @param citaId ID de la cita
     * @return Lista de archivos de la cita
     */
    List<ArchivoAdjunto> buscarPorCita(Long citaId);
}
