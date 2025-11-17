package com.odontoapp.seguridad;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.odontoapp.entidad.Usuario;
import com.odontoapp.repositorio.UsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Manejador personalizado de fallos de autenticación
 * Proporciona mensajes detallados sobre intentos fallidos y bloqueos
 */
@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final int MAX_INTENTOS_FALLIDOS = 3;
    private static final int TIEMPO_BLOQUEO_MINUTOS = 15;

    private final UsuarioRepository usuarioRepository;

    public CustomAuthenticationFailureHandler(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String email = request.getParameter("username");
        String errorMessage = "";

        // Verificar el tipo de excepción
        if (exception instanceof LockedException) {
            // Cuenta bloqueada - Calcular tiempo restante
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                if (usuario.getFechaBloqueo() != null) {
                    LocalDateTime tiempoDesbloqueo = usuario.getFechaBloqueo().plusMinutes(TIEMPO_BLOQUEO_MINUTOS);
                    Duration duracion = Duration.between(LocalDateTime.now(), tiempoDesbloqueo);

                    if (duracion.isNegative() || duracion.isZero()) {
                        errorMessage = "Tu cuenta estuvo bloqueada temporalmente. Por favor, intenta nuevamente.";
                    } else {
                        long minutosRestantes = duracion.toMinutes();
                        if (minutosRestantes > 0) {
                            errorMessage = String.format("Cuenta bloqueada. Intenta nuevamente en %d minuto%s.",
                                minutosRestantes,
                                minutosRestantes > 1 ? "s" : "");
                        } else {
                            long segundosRestantes = duracion.getSeconds();
                            errorMessage = String.format("Cuenta bloqueada. Intenta nuevamente en %d segundo%s.",
                                segundosRestantes,
                                segundosRestantes > 1 ? "s" : "");
                        }
                    }
                }
            } else {
                errorMessage = "Cuenta bloqueada temporalmente por exceso de intentos fallidos.";
            }
        } else if (exception instanceof DisabledException) {
            // Cuenta desactivada
            errorMessage = "Tu cuenta está inactiva. Contacta al administrador.";
        } else {
            // Credenciales incorrectas - Mostrar intentos restantes
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                int intentosRestantes = MAX_INTENTOS_FALLIDOS - usuario.getIntentosFallidos();

                if (intentosRestantes > 0) {
                    errorMessage = String.format("Email o contraseña incorrectos. Te quedan %d intento%s.",
                        intentosRestantes,
                        intentosRestantes > 1 ? "s" : "");
                } else {
                    errorMessage = "Email o contraseña incorrectos.";
                }
            } else {
                // Usuario no existe
                errorMessage = "Email o contraseña incorrectos.";
            }
        }

        // Guardar el mensaje en la sesión
        request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION_MESSAGE", errorMessage);

        // Continuar con el flujo normal
        super.onAuthenticationFailure(request, response, exception);
    }
}
