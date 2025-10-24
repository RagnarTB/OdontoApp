package com.odontoapp.seguridad;

import java.io.IOException;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private RequestCache requestCache = new HttpSessionRequestCache();
    private final UsuarioRepository usuarioRepository;

    // 🔥 Constructor para inyectar el repositorio
    public CustomAuthenticationSuccessHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // 🔥 NUEVO: Verificar si el usuario debe cambiar su contraseña
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(username).orElse(null);

        if (usuario != null && usuario.isDebeActualizarPassword()) {
            // Redirigir al cambio de contraseña obligatorio
            response.sendRedirect(request.getContextPath() + "/cambiar-password-obligatorio");
            return;
        }

        // Primero, verifica si había una solicitud guardada (acceso a página protegida
        // antes de login)
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            response.sendRedirect(savedRequest.getRedirectUrl());
            return;
        }

        // Si no hay solicitud guardada, redirigir según el rol
        String targetUrl = determineTargetUrl(authentication);
        response.sendRedirect(targetUrl);
    }

    protected String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority grantedAuthority : authorities) {
            if (grantedAuthority.getAuthority().equals("PACIENTE")) {
                return "/paciente/dashboard";
            }
        }

        // Si no es paciente, va al dashboard general
        return "/dashboard";
    }
}