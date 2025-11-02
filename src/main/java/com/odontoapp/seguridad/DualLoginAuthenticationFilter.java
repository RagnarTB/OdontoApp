package com.odontoapp.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Filtro personalizado para validar que el tipo de login coincida con el rol del usuario.
 *
 * - Login "personal": Solo permite roles ADMIN, ODONTOLOGO, RECEPCIONISTA, etc.
 * - Login "paciente": Solo permite rol PACIENTE
 */
public class DualLoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(DualLoginAuthenticationFilter.class);

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain chain, Authentication authResult)
            throws IOException, ServletException {

        // Obtener el tipo de login del formulario
        String loginType = request.getParameter("loginType");
        log.info("Login exitoso para '{}' usando loginType='{}'", authResult.getName(), loginType);

        // Validar que el loginType esté presente
        if (loginType == null || loginType.trim().isEmpty()) {
            log.warn("loginType no especificado, asumiendo 'personal' por defecto");
            loginType = "personal";
        }

        // Verificar roles del usuario autenticado
        boolean esPaciente = authResult.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("PACIENTE"));

        boolean esPersonal = authResult.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ADMIN") || role.equals("ODONTOLOGO")
                        || role.equals("RECEPCIONISTA") || role.equals("AUXILIAR"));

        // ✅ VALIDACIÓN DE LOGIN DUAL
        if ("paciente".equalsIgnoreCase(loginType)) {
            // Login de pacientes: solo permitir rol PACIENTE
            if (!esPaciente) {
                log.warn("❌ Usuario '{}' intentó ingresar por login de PACIENTES pero NO tiene rol PACIENTE",
                        authResult.getName());

                // Redirigir con error específico
                String errorUrl = request.getContextPath() +
                        "/login?error=true&reason=wrongPortal&loginType=paciente";
                response.sendRedirect(errorUrl);
                return;
            }
            log.info("✅ Usuario PACIENTE '{}' validado correctamente en login de pacientes",
                    authResult.getName());
        } else if ("personal".equalsIgnoreCase(loginType)) {
            // Login de personal: NO permitir rol PACIENTE
            if (esPaciente && !esPersonal) {
                log.warn("❌ Usuario PACIENTE '{}' intentó ingresar por login de PERSONAL",
                        authResult.getName());

                // Redirigir con error específico
                String errorUrl = request.getContextPath() +
                        "/login?error=true&reason=wrongPortal&loginType=personal";
                response.sendRedirect(errorUrl);
                return;
            }
            log.info("✅ Usuario PERSONAL '{}' validado correctamente en login de personal",
                    authResult.getName());
        }

        // Si pasa la validación, continuar con el flujo normal
        super.successfulAuthentication(request, response, chain, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            AuthenticationException failed)
            throws IOException, ServletException {

        // Preservar el loginType en la URL de error
        String loginType = request.getParameter("loginType");
        if (loginType != null && !loginType.trim().isEmpty()) {
            String errorUrl = request.getContextPath() +
                    "/login?error=true&loginType=" + loginType;
            response.sendRedirect(errorUrl);
        } else {
            super.unsuccessfulAuthentication(request, response, failed);
        }
    }
}
