package com.odontoapp.seguridad;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Manejador personalizado para errores de acceso denegado (403).
 * Proporciona mensajes amigables cuando un usuario intenta acceder
 * a recursos para los que no tiene permisos.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            String username = auth.getName();
            String requestedUrl = request.getRequestURI();

            System.out.println("⚠️ Acceso denegado para usuario: " + username + " al recurso: " + requestedUrl);
        }

        // Detectar si es una petición AJAX o normal
        String ajaxHeader = request.getHeader("X-Requested-With");
        boolean isAjax = "XMLHttpRequest".equals(ajaxHeader);

        if (isAjax) {
            // Para peticiones AJAX, devolver JSON con error
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"No tienes permisos para realizar esta acción.\"}");
        } else {
            // Para peticiones normales, redirigir a página de error personalizada
            request.getSession().setAttribute("errorMessage",
                "🚫 No tienes permisos para acceder a este recurso. Si crees que esto es un error, contacta al administrador.");
            request.getSession().setAttribute("errorDetails",
                "Recurso solicitado: " + request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/error/403");
        }
    }
}
