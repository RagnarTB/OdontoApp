package com.odontoapp.controlador;

import com.odontoapp.entidad.ArchivoAdjunto;
import com.odontoapp.servicio.ArchivoAdjuntoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * Controlador para la gestión de archivos adjuntos.
 * Maneja la subida, descarga y eliminación de archivos (radiografías, documentos).
 */
@Controller
@RequestMapping("/archivos")
public class ArchivoAdjuntoController {

    private final ArchivoAdjuntoService archivoAdjuntoService;

    public ArchivoAdjuntoController(ArchivoAdjuntoService archivoAdjuntoService) {
        this.archivoAdjuntoService = archivoAdjuntoService;
    }

    /**
     * Sube un archivo al servidor y lo asocia a un paciente.
     *
     * @param file Archivo a subir
     * @param pacienteId ID del paciente propietario
     * @param citaId ID de la cita asociada (opcional)
     * @param descripcion Descripción del archivo
     * @param attributes Atributos para mensajes flash
     * @return Redirección a la página del paciente
     */
    @PostMapping("/subir")
    public String subirArchivo(@RequestParam("file") MultipartFile file,
                              @RequestParam Long pacienteId,
                              @RequestParam(required = false) Long citaId,
                              @RequestParam(required = false) String descripcion,
                              RedirectAttributes attributes) {
        try {
            // Validar que se haya seleccionado un archivo
            if (file.isEmpty()) {
                attributes.addFlashAttribute("error", "Por favor, selecciona un archivo para subir.");
                return "redirect:/pacientes/detalle/" + pacienteId;
            }

            // Guardar el archivo
            ArchivoAdjunto archivoGuardado = archivoAdjuntoService.guardarArchivo(file, pacienteId, citaId, descripcion);

            attributes.addFlashAttribute("success",
                    "Archivo '" + archivoGuardado.getNombreArchivoOriginal() + "' subido con éxito.");

        } catch (IllegalArgumentException e) {
            attributes.addFlashAttribute("error",
                    "Error en los datos del archivo: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            attributes.addFlashAttribute("error",
                    "Error: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error inesperado al subir el archivo: " + e.getMessage());
        }

        return "redirect:/pacientes/detalle/" + pacienteId;
    }

    /**
     * Descarga un archivo del servidor.
     *
     * @param id ID del archivo a descargar
     * @param request Petición HTTP
     * @return ResponseEntity con el archivo como recurso
     */
    @GetMapping("/descargar/{id}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Long id,
                                                     HttpServletRequest request) {
        try {
            // Buscar el archivo en la base de datos
            ArchivoAdjunto archivo = archivoAdjuntoService.buscarPorId(id)
                    .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + id));

            // Cargar el archivo como recurso
            Resource resource = archivoAdjuntoService.cargarArchivoComoRecurso(id);

            // Determinar el tipo de contenido
            String contentType = archivo.getTipoMime();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Configurar headers para la descarga
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + archivo.getNombreArchivoOriginal() + "\"")
                    .body(resource);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Visualiza un archivo en el navegador (para imágenes principalmente).
     *
     * @param id ID del archivo a visualizar
     * @return ResponseEntity con el archivo como recurso
     */
    @GetMapping("/ver/{id}")
    public ResponseEntity<Resource> verArchivo(@PathVariable Long id) {
        try {
            // Buscar el archivo en la base de datos
            ArchivoAdjunto archivo = archivoAdjuntoService.buscarPorId(id)
                    .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + id));

            // Cargar el archivo como recurso
            Resource resource = archivoAdjuntoService.cargarArchivoComoRecurso(id);

            // Determinar el tipo de contenido
            String contentType = archivo.getTipoMime();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Configurar headers para visualización en el navegador (inline)
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + archivo.getNombreArchivoOriginal() + "\"")
                    .body(resource);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Elimina un archivo del servidor y de la base de datos.
     *
     * @param id ID del archivo a eliminar
     * @param pacienteId ID del paciente (para redirección)
     * @param attributes Atributos para mensajes flash
     * @return Redirección a la página del paciente
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarArchivo(@PathVariable Long id,
                                  @RequestParam Long pacienteId,
                                  RedirectAttributes attributes) {
        try {
            // Obtener información del archivo antes de eliminarlo
            ArchivoAdjunto archivo = archivoAdjuntoService.buscarPorId(id)
                    .orElseThrow(() -> new EntityNotFoundException("Archivo no encontrado con ID: " + id));

            String nombreArchivo = archivo.getNombreArchivoOriginal();

            // Eliminar el archivo
            archivoAdjuntoService.eliminarArchivo(id);

            attributes.addFlashAttribute("success",
                    "Archivo '" + nombreArchivo + "' eliminado con éxito.");

        } catch (EntityNotFoundException e) {
            attributes.addFlashAttribute("error",
                    "Error: " + e.getMessage());
        } catch (Exception e) {
            attributes.addFlashAttribute("error",
                    "Error inesperado al eliminar el archivo: " + e.getMessage());
        }

        return "redirect:/pacientes/detalle/" + pacienteId;
    }

    /**
     * Endpoint alternativo para eliminar archivo mediante POST (más seguro).
     *
     * @param id ID del archivo a eliminar
     * @param pacienteId ID del paciente (para redirección)
     * @param attributes Atributos para mensajes flash
     * @return Redirección a la página del paciente
     */
    @PostMapping("/eliminar/{id}")
    public String eliminarArchivoPost(@PathVariable Long id,
                                     @RequestParam Long pacienteId,
                                     RedirectAttributes attributes) {
        return eliminarArchivo(id, pacienteId, attributes);
    }
}
