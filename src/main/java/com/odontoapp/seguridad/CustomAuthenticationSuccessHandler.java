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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component // Registrar como Bean
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

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

        // Puedes hacer esto más robusto si un usuario puede tener MÚLTIPLES roles
        // principales
        for (GrantedAuthority grantedAuthority : authorities) {
            // Asumiendo que el rol PACIENTE se llama "PACIENTE" y no "ROLE_PACIENTE"
            // Ajusta si usas el prefijo ROLE_ en los nombres guardados
            if (grantedAuthority.getAuthority().equals("PACIENTE")) {
                // Aquí defines la URL para el panel de pacientes
                return "/paciente/dashboard"; // Cambia esta URL a la que vayas a usar
            }
            // Puedes añadir más roles aquí (ej. DOCTOR, RECEPCIONISTA)
            // if (grantedAuthority.getAuthority().equals("DOCTOR")) {
            // return "/doctor/agenda";
            // }
        }

        // Si no es paciente (o cualquier otro rol específico), va al dashboard general
        return "/dashboard";
    }
}