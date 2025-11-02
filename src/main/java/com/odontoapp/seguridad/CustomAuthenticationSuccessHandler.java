package com.odontoapp.seguridad;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // Para verificar la URL

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    private RequestCache requestCache = new HttpSessionRequestCache();
    private final UsuarioRepository usuarioRepository;

    // Define la ruta de error por defecto de Spring Boot
    private static final String DEFAULT_ERROR_PATH = "/error";

    public CustomAuthenticationSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        log.info("Éxito de autenticación para usuario: {}", authentication.getName());

        String username = authentication.getName();
        Usuario usuario = null;

        try {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(username);
            if (usuarioOpt.isPresent()) {
                usuario = usuarioOpt.get(); // Assign to the 'usuario' variable
                usuario.setUltimoAcceso(LocalDateTime.now()); // Set current time
                usuarioRepository.save(usuario); // Save the change
                log.info("Último acceso actualizado para {}", username);
            } else {
                log.warn("Usuario '{}' autenticado pero no encontrado en BD para actualizar último acceso.", username);
            }
        } catch (Exception e) {
            log.error("¡ERROR al buscar o actualizar último acceso para '{}'!", username, e);
            // Decide if you want to proceed even if update fails, or handle differently
        }

        try {
            usuario = usuarioRepository.findByEmail(username).orElse(null);
            if (usuario == null) {
                log.warn("Usuario '{}' autenticado pero no encontrado en la base de datos.", username);
            } else {
                log.info("Usuario encontrado: {}, Debe cambiar Pwd: {}", usuario.getEmail(),
                        usuario.isDebeActualizarPassword());
            }
        } catch (Exception e) {
            log.error("¡ERROR al buscar el usuario '{}' en la base de datos después del login!", username, e);
        }

        // ✅ VALIDACIÓN DE LOGIN DUAL
        String loginType = request.getParameter("loginType");
        log.info("Login type recibido: '{}'", loginType);

        if (loginType != null && !loginType.trim().isEmpty()) {
            boolean esPaciente = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("PACIENTE"));

            boolean esPersonal = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role -> role.equals("ADMIN") || role.equals("ODONTOLOGO")
                            || role.equals("RECEPCIONISTA") || role.equals("AUXILIAR") || role.equals("ALMACEN"));

            // Validar login de pacientes: solo permitir rol PACIENTE
            if ("paciente".equalsIgnoreCase(loginType)) {
                if (!esPaciente) {
                    log.warn("❌ Usuario '{}' intentó ingresar por login de PACIENTES pero NO tiene rol PACIENTE",
                            username);
                    String errorUrl = request.getContextPath()
                            + "/login?error=true&reason=wrongPortal&loginType=paciente";
                    response.sendRedirect(errorUrl);
                    return;
                }
                log.info("✅ Usuario PACIENTE '{}' validado correctamente en login de pacientes", username);
            }

            // Validar login de personal: NO permitir solo rol PACIENTE
            if ("personal".equalsIgnoreCase(loginType)) {
                if (esPaciente && !esPersonal) {
                    log.warn("❌ Usuario PACIENTE '{}' intentó ingresar por login de PERSONAL", username);
                    String errorUrl = request.getContextPath()
                            + "/login?error=true&reason=wrongPortal&loginType=personal";
                    response.sendRedirect(errorUrl);
                    return;
                }
                log.info("✅ Usuario PERSONAL '{}' validado correctamente en login de personal", username);
            }
        }

        // Lógica Cambio de Contraseña (con try-catch)
        if (usuario != null && usuario.isDebeActualizarPassword()) {
            String targetUrl = request.getContextPath() + "/cambiar-password-obligatorio";
            log.info("Usuario debe cambiar contraseña. Intentando redirigir a: {}", targetUrl);
            try {
                if (!response.isCommitted()) {
                    response.sendRedirect(targetUrl);
                    log.info("Redirección a cambio de contraseña enviada.");
                } else {
                    log.warn("La respuesta ya estaba 'committed'. No se pudo redirigir a {}", targetUrl);
                }
            } catch (IllegalStateException | IOException e) {
                log.error("¡ERROR al intentar redirigir a cambio de contraseña!", e);
            }
            return;
        }

        // Lógica SavedRequest (con try-catch y verificación de /error)
        SavedRequest savedRequest = null;
        try {
            savedRequest = requestCache.getRequest(request, response);
        } catch (Exception e) {
            log.error("¡ERROR al obtener SavedRequest de la caché!", e);
        }

        // --- NUEVA VERIFICACIÓN: Ignorar si SavedRequest apunta a /error ---
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            // Verifica si la URL obtenida es relativa o absoluta y si empieza con /error
            String requestURI = extractRequestPath(targetUrl); // Extrae la parte de la ruta

            if (StringUtils.hasText(requestURI) && requestURI.startsWith(DEFAULT_ERROR_PATH)) {
                log.warn("SavedRequest encontrado apuntando a '{}'. Ignorando y usando redirección por rol.",
                        targetUrl);
                // No hacemos nada aquí, dejamos que continúe a la lógica de determineTargetUrl
            } else {
                // Si NO es la página de error, redirigir a SavedRequest
                log.info("SavedRequest válido encontrado. Intentando redirigir a: {}", targetUrl);
                try {
                    if (!response.isCommitted()) {
                        response.sendRedirect(targetUrl);
                        log.info("Redirección a SavedRequest enviada.");
                    } else {
                        log.warn("La respuesta ya estaba 'committed'. No se pudo redirigir a {}", targetUrl);
                    }
                } catch (IllegalStateException | IOException e) {
                    log.error("¡ERROR al intentar redirigir a SavedRequest!", e);
                }
                return; // Salir si redirigimos a SavedRequest válido
            }
        } else {
            log.info("No se encontró SavedRequest.");
        }
        // --- FIN VERIFICACIÓN /error ---

        // Lógica Final Redirección por Rol (con try-catch)
        // Se ejecutará si no hay SavedRequest o si el SavedRequest apuntaba a /error
        String targetUrl = determineTargetUrl(authentication);
        log.info("Procediendo con redirección final por rol a: {}", targetUrl);
        try {
            if (!response.isCommitted()) {
                response.sendRedirect(targetUrl);
                log.info("Redirección final enviada.");
            } else {
                log.warn("La respuesta ya estaba 'committed'. No se pudo redirigir a {}", targetUrl);
            }
        } catch (IllegalStateException | IOException e) {
            log.error("¡ERROR al intentar la redirección final por rol!", e);
        }
    }

    protected String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority grantedAuthority : authorities) {
            log.debug("Evaluando rol: {}", grantedAuthority.getAuthority());
            if (grantedAuthority.getAuthority().equals("PACIENTE")) {
                log.info("Rol PACIENTE detectado. URL objetivo: /paciente/dashboard");
                return "/paciente/dashboard";
            }
        }

        log.info("Ningún rol específico coincidió. URL objetivo por defecto: /dashboard");
        return "/dashboard";
    }

    // --- NUEVO MÉTODO HELPER para extraer la ruta de la URL ---
    private String extractRequestPath(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URI(url).toURL();
            return parsedUrl.getPath();
        } catch (Exception e) {
            // Si no es una URL absoluta válida, podría ser relativa
            if (url != null && url.startsWith("/")) {
                int queryIndex = url.indexOf('?');
                return (queryIndex == -1) ? url : url.substring(0, queryIndex);
            }
            log.warn("No se pudo extraer la ruta de la URL guardada: {}", url, e);
            return null; // O devuelve la URL original si prefieres otro manejo
        }
    }
    // --- FIN MÉTODO HELPER ---

}