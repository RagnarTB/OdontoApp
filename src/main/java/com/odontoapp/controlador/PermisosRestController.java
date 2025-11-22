package com.odontoapp.controlador;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador REST para validar permisos desde JavaScript.
 * Permite al frontend verificar si el usuario actual tiene permisos específicos.
 */
@RestController
@RequestMapping("/api/permisos")
public class PermisosRestController {

    /**
     * Verifica si el usuario actual tiene un permiso específico.
     *
     * @param permiso El nombre del permiso a verificar (ej: "CREAR_USUARIOS")
     * @param authentication El objeto de autenticación de Spring Security
     * @return JSON con el resultado: {"tienePermiso": true/false}
     */
    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Boolean>> verificarPermiso(
            @RequestParam("permiso") String permiso,
            Authentication authentication) {

        boolean tienePermiso = false;

        if (authentication != null && authentication.isAuthenticated()) {
            tienePermiso = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(permiso));
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("tienePermiso", tienePermiso);

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica si el usuario actual tiene al menos uno de varios permisos.
     *
     * @param permisos Lista de permisos separados por coma (ej: "CREAR_USUARIOS,EDITAR_USUARIOS")
     * @param authentication El objeto de autenticación de Spring Security
     * @return JSON con el resultado: {"tienePermiso": true/false}
     */
    @GetMapping("/verificar-alguno")
    public ResponseEntity<Map<String, Boolean>> verificarAlgunoDePermisos(
            @RequestParam("permisos") String permisos,
            Authentication authentication) {

        boolean tienePermiso = false;

        if (authentication != null && authentication.isAuthenticated()) {
            String[] permisosArray = permisos.split(",");
            Set<String> permisosSet = Set.of(permisosArray);

            tienePermiso = authentication.getAuthorities().stream()
                    .anyMatch(auth -> permisosSet.contains(auth.getAuthority()));
        }

        Map<String, Boolean> response = new HashMap<>();
        response.put("tienePermiso", tienePermiso);

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene todos los permisos del usuario actual.
     *
     * @param authentication El objeto de autenticación de Spring Security
     * @return JSON con la lista de permisos: {"permisos": ["CREAR_USUARIOS", "EDITAR_USUARIOS", ...]}
     */
    @GetMapping("/mis-permisos")
    public ResponseEntity<Map<String, Object>> obtenerMisPermisos(Authentication authentication) {

        Set<String> permisos = Set.of();

        if (authentication != null && authentication.isAuthenticated()) {
            permisos = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> !auth.startsWith("ROLE_")) // Excluir roles, solo permisos
                    .collect(Collectors.toSet());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("permisos", permisos);

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica múltiples permisos a la vez.
     *
     * @param permisos Lista de permisos separados por coma
     * @param authentication El objeto de autenticación de Spring Security
     * @return JSON con el resultado de cada permiso: {"CREAR_USUARIOS": true, "EDITAR_USUARIOS": false, ...}
     */
    @GetMapping("/verificar-multiples")
    public ResponseEntity<Map<String, Boolean>> verificarMultiplesPermisos(
            @RequestParam("permisos") String permisos,
            Authentication authentication) {

        Map<String, Boolean> response = new HashMap<>();
        String[] permisosArray = permisos.split(",");

        if (authentication != null && authentication.isAuthenticated()) {
            Set<String> permisosUsuario = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            for (String permiso : permisosArray) {
                String permisoTrimmed = permiso.trim();
                response.put(permisoTrimmed, permisosUsuario.contains(permisoTrimmed));
            }
        } else {
            // Si no está autenticado, todos son false
            for (String permiso : permisosArray) {
                response.put(permiso.trim(), false);
            }
        }

        return ResponseEntity.ok(response);
    }
}
