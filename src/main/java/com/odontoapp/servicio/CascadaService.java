package com.odontoapp.servicio;

import org.springframework.dao.DataIntegrityViolationException;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.PacienteRepository;
import com.odontoapp.repositorio.UsuarioRepository; // Usar repositorio para encontrar

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
public class CascadaService {

    private final UsuarioService usuarioService; // Para la lógica de desactivación
    private final PacienteRepository pacienteRepository; // Para el soft delete del paciente
    private final UsuarioRepository usuarioRepository; // Para encontrar el usuario

    public CascadaService(UsuarioService usuarioService,
            PacienteRepository pacienteRepository,
            UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Desactiva un usuario (cambia estaActivo=false) y realiza soft delete
     * de su paciente asociado si existe y no está ya eliminado.
     * NOTA: Esta lógica asume que quieres DESACTIVAR (no hacer soft delete) al
     * usuario
     * y hacer SOFT DELETE al paciente. Ajusta si la lógica debe ser diferente.
     */
    @Transactional
    public void desactivarUsuarioYSoftDeletePaciente(Long usuarioId) {
        // Encontrar usuario (ignorar soft delete para poder encontrarlo si ya está
        // 'eliminado')
        // Necesitaríamos un método findByIdIgnorandoSoftDelete en UsuarioRepository si
        // usamos soft delete para Usuario
        // Por ahora, usamos findById normal, asumiendo que el usuario no está eliminado
        // (soft delete) aún.
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + usuarioId));

        // 1. Desactivar usuario (usando el servicio existente)
        try {
            // Usamos el método que fuerza el estado a false (inactivo)
            usuarioService.cambiarEstadoUsuario(usuarioId, false);
            System.out.println(">>> Usuario " + usuario.getEmail() + " desactivado por cascada.");
        } catch (UnsupportedOperationException | IllegalStateException e) {
            // Podría fallar si es el admin principal o no se encontró (ya manejado arriba)
            System.err.println("Advertencia: No se pudo desactivar usuario en cascada: " + e.getMessage());
            // Considera si quieres detener la operación aquí o continuar
            // throw e; // Para detener
        }

        // 2. Si tiene paciente asociado Y el paciente NO está ya eliminado, hacer soft
        // delete
        if (usuario.getPaciente() != null && !usuario.getPaciente().isEliminado()) {
            try {
                pacienteRepository.deleteById(usuario.getPaciente().getId()); // Activa @SQLDelete de Paciente
                System.out.println(">>> Paciente asociado " + usuario.getPaciente().getId()
                        + " eliminado (soft delete) por cascada.");
            } catch (Exception e) {
                System.err.println("Error al intentar soft delete del paciente asociado en cascada: " + e.getMessage());
                // Loggear pero no necesariamente detener todo, ya que el usuario se desactivó
            }
        }
    }

    /**
     * Realiza soft delete de un usuario y de su paciente asociado si existe.
     * NOTA: Esta es una alternativa si quieres aplicar soft delete a AMBOS.
     */
    @Transactional
    public void softDeleteUsuarioYPaciente(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado con ID: " + usuarioId));

        // 1. Soft delete del Paciente (si existe y no está eliminado)
        if (usuario.getPaciente() != null && !usuario.getPaciente().isEliminado()) {
            try {
                pacienteRepository.deleteById(usuario.getPaciente().getId());
                System.out.println(">>> Paciente asociado " + usuario.getPaciente().getId()
                        + " eliminado (soft delete) por cascada.");
            } catch (Exception e) {
                System.err.println("Error al intentar soft delete del paciente asociado en cascada: " + e.getMessage());
                // Considera si detener la operación si falla el soft delete del paciente
                // throw new RuntimeException("Fallo al eliminar paciente asociado.", e);
            }
        }

        // 2. Soft delete del Usuario (después del paciente)
        try {
            // Validaciones de eliminarUsuario (como no ser admin) ya están en el
            // service/repo
            usuarioRepository.deleteById(usuarioId); // Activa @SQLDelete de Usuario
            System.out.println(">>> Usuario " + usuario.getEmail() + " eliminado (soft delete) por cascada.");
        } catch (UnsupportedOperationException | DataIntegrityViolationException e) {
            System.err.println("Advertencia: No se pudo eliminar (soft delete) usuario en cascada: " + e.getMessage());
            // Decidir si relanzar la excepción
            // throw e;
        } catch (Exception e) {
            System.err.println("Error inesperado al eliminar (soft delete) usuario en cascada: " + e.getMessage());
            // throw new RuntimeException("Fallo al eliminar usuario en cascada.", e);
        }
    }
}