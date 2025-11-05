package com.odontoapp.configuracion;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler que verifica diariamente los usuarios con vigencia vencida
 * y los desactiva automáticamente.
 *
 * Programado para ejecutarse todos los días a las 00:05 AM.
 */
@Component
public class UsuarioVencidoScheduler {

    private static final Logger log = LoggerFactory.getLogger(UsuarioVencidoScheduler.class);

    private final UsuarioRepository usuarioRepository;

    public UsuarioVencidoScheduler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Verifica y desactiva usuarios con vigencia vencida.
     * Se ejecuta todos los días a las 00:05 AM.
     *
     * Cron expression: segundo minuto hora día mes día-de-semana
     * "0 5 0 * * *" = A las 00:05:00 todos los días
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void verificarUsuariosVencidos() {
        log.info("========== INICIANDO VERIFICACIÓN DE USUARIOS VENCIDOS ==========");

        try {
            LocalDate hoy = LocalDate.now();

            // Buscar usuarios activos con fecha de vigencia vencida
            List<Usuario> usuariosVencidos = usuarioRepository.findByFechaVigenciaBeforeAndEstaActivoTrue(hoy);

            if (usuariosVencidos.isEmpty()) {
                log.info("No se encontraron usuarios con vigencia vencida.");
                return;
            }

            log.info("Encontrados {} usuario(s) con vigencia vencida", usuariosVencidos.size());

            int contador = 0;
            for (Usuario usuario : usuariosVencidos) {
                try {
                    // No desactivar usuarios ADMIN (por si tienen fecha de vigencia accidentalmente)
                    boolean esAdmin = usuario.getRoles().stream()
                            .anyMatch(rol -> "ADMIN".equals(rol.getNombre()));

                    if (esAdmin) {
                        log.warn("Usuario ADMIN {} tiene vigencia vencida pero no será desactivado",
                                usuario.getEmail());
                        continue;
                    }

                    // Desactivar usuario
                    usuario.setEstaActivo(false);
                    usuarioRepository.save(usuario);
                    contador++;

                    log.info("✓ Usuario desactivado: {} (vigencia: {})",
                            usuario.getEmail(),
                            usuario.getFechaVigencia());

                } catch (Exception e) {
                    log.error("Error al desactivar usuario {}: {}",
                            usuario.getEmail(),
                            e.getMessage(),
                            e);
                }
            }

            log.info("========== VERIFICACIÓN COMPLETADA: {} usuario(s) desactivado(s) ==========", contador);

        } catch (Exception e) {
            log.error("Error crítico en el scheduler de usuarios vencidos", e);
        }
    }

    /**
     * Método manual para ejecutar la verificación (útil para testing).
     * Puede ser llamado desde un endpoint de administración si se necesita.
     */
    @Transactional
    public void ejecutarManualmente() {
        log.info("Ejecución manual del scheduler de usuarios vencidos");
        verificarUsuariosVencidos();
    }
}
