package com.odontoapp.configuracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.odontoapp.seguridad.CustomAuthenticationSuccessHandler;
import com.odontoapp.seguridad.CustomAuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // ✅ Habilitar seguridad a nivel de método
public class SecurityConfig {

        // Inyecta tus handlers personalizados
        @Autowired
        private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

        @Autowired
        private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

        @Autowired
        private com.odontoapp.seguridad.CustomAccessDeniedHandler customAccessDeniedHandler;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public SessionRegistry sessionRegistry() {
                return new SessionRegistryImpl();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authorize -> authorize
                                                // Recursos públicos
                                                .requestMatchers("/login", "/adminlte/**", "/css/**", "/js/**",
                                                                "/activar-cuenta", "/establecer-password",
                                                                "/resultado-activacion", "/registro/**",
                                                                "/recuperar-password/**", "/api/reniec",
                                                                "/error/**")
                                                .permitAll()

                                                // API de permisos - solo para usuarios autenticados
                                                .requestMatchers("/api/permisos/**").authenticated()

                                                // Cambiar password obligatorio - todos autenticados
                                                .requestMatchers("/cambiar-password-obligatorio").authenticated()

                                                // Selector de rol - todos autenticados
                                                .requestMatchers("/seleccionar-rol", "/seleccionar-rol/**").authenticated()

                                                // PACIENTE: Portal exclusivo para pacientes
                                                .requestMatchers("/paciente/**").hasAuthority("PACIENTE")

                                                // Dashboard general: NO permitir a PACIENTE (usan /paciente/dashboard)
                                                .requestMatchers("/", "/home", "/dashboard")
                                                .hasAnyRole("ADMIN", "ODONTOLOGO", "RECEPCIONISTA", "ALMACEN")

                                                // Los siguientes módulos usan @PreAuthorize con permisos granulares
                                                // Solo requieren autenticación, el control de acceso se hace en los controllers
                                                .requestMatchers("/usuarios/**", "/roles/**", "/administracion/**").authenticated()
                                                .requestMatchers("/pacientes/**", "/servicios/**", "/facturacion/**",
                                                                "/tratamientos/**", "/odontograma/**", "/api/odontograma/**").authenticated()
                                                .requestMatchers("/citas/**", "/agenda/**").authenticated()
                                                .requestMatchers("/insumos/**", "/categorias-insumo/**",
                                                                "/unidades-medida/**", "/movimientos-inventario/**").authenticated()

                                                // Cualquier otra petición requiere autenticación
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .usernameParameter("username")
                                                .passwordParameter("password")
                                                .successHandler(customAuthenticationSuccessHandler)
                                                .failureHandler(customAuthenticationFailureHandler)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .exceptionHandling(exception -> exception
                                                .accessDeniedHandler(customAccessDeniedHandler))
                                .sessionManagement(session -> session
                                                .maximumSessions(-1) // Sin límite de sesiones concurrentes
                                                .sessionRegistry(sessionRegistry()));
                return http.build();
        }
}