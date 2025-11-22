package com.odontoapp.servicio;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import com.odontoapp.entidad.Rol;
import com.odontoapp.entidad.Usuario;

import java.util.List;

/**
 * Servicio para invalidar sesiones de usuarios cuando cambian sus roles o permisos.
 */
@Service
public class SessionInvalidationService {

    private final SessionRegistry sessionRegistry;

    public SessionInvalidationService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Invalida todas las sesiones activas de usuarios que tienen un rol específico.
     * Se usa cuando se modifican los permisos de un rol.
     *
     * @param rol El rol cuyos usuarios deben ser deslogueados
     * @return Cantidad de sesiones invalidadas
     */
    public int invalidarSesionesPorRol(Rol rol) {
        int sesionesInvalidadas = 0;

        // Obtener todos los principals (usuarios autenticados)
        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();

        for (Object principal : allPrincipals) {
            if (principal instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) principal;

                // Verificar si el usuario tiene el rol modificado
                String roleName = "ROLE_" + rol.getNombre();
                boolean tieneRol = userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(roleName));

                if (tieneRol) {
                    sesionesInvalidadas += invalidarSesionesUsuario(principal);
                }
            }
        }

        return sesionesInvalidadas;
    }

    /**
     * Invalida todas las sesiones activas de un usuario específico.
     * Se usa cuando se modifican los roles de un usuario.
     *
     * @param usuario El usuario cuyas sesiones deben ser invalidadas
     * @return Cantidad de sesiones invalidadas
     */
    public int invalidarSesionesPorUsuario(Usuario usuario) {
        int sesionesInvalidadas = 0;

        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();

        for (Object principal : allPrincipals) {
            if (principal instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) principal;

                // Comparar por email (username)
                if (userDetails.getUsername().equals(usuario.getEmail())) {
                    sesionesInvalidadas += invalidarSesionesUsuario(principal);
                }
            }
        }

        return sesionesInvalidadas;
    }

    /**
     * Invalida todas las sesiones de un principal específico.
     *
     * @param principal El principal cuyas sesiones serán invalidadas
     * @return Cantidad de sesiones invalidadas
     */
    private int invalidarSesionesUsuario(Object principal) {
        List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);

        for (SessionInformation session : sessions) {
            session.expireNow();
        }

        return sessions.size();
    }

    /**
     * Invalida todas las sesiones activas del sistema (usar con precaución).
     *
     * @return Cantidad de sesiones invalidadas
     */
    public int invalidarTodasLasSesiones() {
        int sesionesInvalidadas = 0;
        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();

        for (Object principal : allPrincipals) {
            sesionesInvalidadas += invalidarSesionesUsuario(principal);
        }

        return sesionesInvalidadas;
    }
}
