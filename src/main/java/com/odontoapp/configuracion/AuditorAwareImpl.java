package com.odontoapp.configuracion;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.of("sistema"); // Valor por defecto si no hay usuario
        }

        Object principal = authentication.getPrincipal();
        // Verifica si el principal es UserDetails (caso común con Spring Security)
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername()); // Devuelve el email
        }
        // Fallback si el principal no es UserDetails
        return Optional.of(principal.toString());
    }
}