package com.odontoapp.servicio.impl;

import com.odontoapp.dto.ArchivoAdjuntoDTO;
import com.odontoapp.entidad.ArchivoAdjunto;
import com.odontoapp.entidad.Cita;
import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.ArchivoAdjuntoRepository;
import com.odontoapp.repositorio.CitaRepository;
import com.odontoapp.repositorio.UsuarioRepository;
import com.odontoapp.servicio.ArchivoAdjuntoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de gestión de archivos adjuntos.
 * Maneja el almacenamiento en el sistema de archivos local.
 */
@Service
@Transactional
public class ArchivoAdjuntoServiceImpl implements ArchivoAdjuntoService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final ArchivoAdjuntoRepository archivoAdjuntoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;

    public ArchivoAdjuntoServiceImpl(ArchivoAdjuntoRepository archivoAdjuntoRepository,
                                    UsuarioRepository usuarioRepository,
                                    CitaRepository citaRepository) {
        this.archivoAdjuntoRepository = archivoAdjuntoRepository;
        this.usuarioRepository = usuarioRepository;
        this.citaRepository = citaRepository;
    }

    @Override
    @Transactional
    public ArchivoAdjunto guardarArchivo(MultipartFile archivo, Long pacienteUsuarioId, Long citaId, String descripcion) {
        // 1. Validar archivo
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        // 2. Validar tamaño (por ejemplo, máximo 10 MB)
        long maxSize = 10 * 1024 * 1024; // 10 MB
        if (archivo.getSize() > maxSize) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 10 MB");
        }

        // 3. Validar tipo de archivo (solo imágenes y PDFs)
        String contentType = archivo.getContentType();
        if (contentType == null || !(contentType.startsWith("image/") || contentType.equals("application/pdf"))) {
            throw new IllegalArgumentException("Solo se permiten imágenes y archivos PDF");
        }

        // 4. Buscar el paciente
        Usuario paciente = usuarioRepository.findById(pacienteUsuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente no encontrado con ID: " + pacienteUsuarioId));

        // 5. Buscar la cita si se proporciona citaId
        Cita cita = null;
        if (citaId != null) {
            cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new EntityNotFoundException("Cita no encontrada con ID: " + citaId));
        }

        try {
            // 6. Crear el directorio de subida si no existe
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 7. Generar nombre de archivo único
            String originalFilename = archivo.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String nombreArchivoUnico = UUID.randomUUID().toString() + extension;

            // 8. Resolver la ruta de destino
            Path targetLocation = uploadPath.resolve(nombreArchivoUnico);

            // 9. Copiar el archivo al destino
            Files.copy(archivo.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 10. Crear entidad ArchivoAdjunto
            ArchivoAdjunto archivoAdjunto = new ArchivoAdjunto();
            archivoAdjunto.setPaciente(paciente);
            archivoAdjunto.setCita(cita);
            archivoAdjunto.setNombreArchivoOriginal(originalFilename);
            archivoAdjunto.setNombreArchivoGuardado(nombreArchivoUnico);
            archivoAdjunto.setRutaArchivo(nombreArchivoUnico); // Solo guardamos el nombre del archivo
            archivoAdjunto.setTipoMime(contentType);
            archivoAdjunto.setTamanoBytes(archivo.getSize());
            archivoAdjunto.setDescripcion(descripcion);

            // 11. Guardar en la base de datos
            return archivoAdjuntoRepository.save(archivoAdjunto);

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArchivoAdjunto> buscarPorId(Long id) {
        return archivoAdjuntoRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource cargarArchivoComoRecurso(Long id) {
        // 1. Buscar el archivo adjunto
        ArchivoAdjunto archivoAdjunto = archivoAdjuntoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + id));

        try {
            // 2. Resolver la ruta completa del archivo
            Path filePath = Paths.get(uploadDir).resolve(archivoAdjunto.getRutaArchivo()).normalize();

            // 3. Crear el recurso
            Resource resource = new UrlResource(filePath.toUri());

            // 4. Verificar si el recurso existe y es legible
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("No se pudo leer el archivo: " + archivoAdjunto.getNombreArchivoOriginal());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al cargar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void eliminarArchivo(Long id) {
        // 1. Buscar el archivo adjunto
        ArchivoAdjunto archivoAdjunto = archivoAdjuntoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + id));

        try {
            // 2. Resolver la ruta completa del archivo
            Path filePath = Paths.get(uploadDir).resolve(archivoAdjunto.getRutaArchivo());

            // 3. Eliminar el archivo físico del disco
            Files.deleteIfExists(filePath);

            // 4. Eliminar el registro de la base de datos (soft delete)
            archivoAdjuntoRepository.deleteById(id);

        } catch (IOException e) {
            throw new RuntimeException("Error al eliminar el archivo físico: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchivoAdjuntoDTO> buscarPorPaciente(Long pacienteUsuarioId) {
        List<ArchivoAdjunto> archivos = archivoAdjuntoRepository.findByPacienteIdOrderByFechaCreacionDesc(pacienteUsuarioId);
        return archivos.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArchivoAdjunto> buscarPorCita(Long citaId) {
        return archivoAdjuntoRepository.findByCitaIdOrderByFechaCreacionDesc(citaId);
    }

    /**
     * Convierte una entidad ArchivoAdjunto a DTO.
     *
     * @param archivo Entidad a convertir
     * @return DTO con información del archivo
     */
    private ArchivoAdjuntoDTO toDTO(ArchivoAdjunto archivo) {
        ArchivoAdjuntoDTO dto = new ArchivoAdjuntoDTO();
        dto.setId(archivo.getId());
        dto.setPacienteUsuarioId(archivo.getPaciente().getId());
        dto.setPacienteNombre(archivo.getPaciente().getNombreCompleto());

        if (archivo.getCita() != null) {
            dto.setCitaId(archivo.getCita().getId());
        }

        dto.setNombreArchivoOriginal(archivo.getNombreArchivoOriginal());
        dto.setRutaArchivo(archivo.getRutaArchivo());
        dto.setTipoMime(archivo.getTipoMime());
        dto.setTamanoBytes(archivo.getTamanoBytes());
        dto.setDescripcion(archivo.getDescripcion());
        dto.setFechaSubida(archivo.getFechaCreacion());

        // Calcular tamaño legible
        dto.setTamanoLegible(formatearTamano(archivo.getTamanoBytes()));

        return dto;
    }

    /**
     * Formatea el tamaño de bytes a una cadena legible.
     *
     * @param bytes Tamaño en bytes
     * @return Cadena formateada (ej: "2.5 MB")
     */
    private String formatearTamano(Long bytes) {
        if (bytes == null) {
            return "0 B";
        }

        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
