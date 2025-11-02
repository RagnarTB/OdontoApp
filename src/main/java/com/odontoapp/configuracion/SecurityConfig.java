package com.odontoapp.configuracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.odontoapp.seguridad.CustomAuthenticationSuccessHandler;
import com.odontoapp.seguridad.DualLoginAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // ✅ Habilitar seguridad a nivel de método
public class SecurityConfig {

        // Inyecta tu handler personalizado
        @Autowired
        private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        /**
         * Bean del filtro de autenticación dual personalizado
         */
        @Bean
        public DualLoginAuthenticationFilter dualLoginAuthenticationFilter(
                        AuthenticationManager authenticationManager) throws Exception {

                DualLoginAuthenticationFilter filter = new DualLoginAuthenticationFilter();
                filter.setAuthenticationManager(authenticationManager);
                filter.setFilterProcessesUrl("/login");
                filter.setAuthenticationSuccessHandler(customAuthenticationSuccessHandler);

                // Configurar parámetros del formulario
                filter.setUsernameParameter("username");
                filter.setPasswordParameter("password");

                // Configurar el manejo de fallos de autenticación
                filter.setAuthenticationFailureHandler((request, response, exception) -> {
                        String loginType = request.getParameter("loginType");
                        String redirectUrl = "/login?error=true";
                        if (loginType != null && !loginType.trim().isEmpty()) {
                                redirectUrl += "&loginType=" + loginType;
                        }
                        response.sendRedirect(redirectUrl);
                });

                return filter;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        DualLoginAuthenticationFilter dualLoginAuthenticationFilter) throws Exception {
                http
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/login", "/adminlte/**", "/css/**", "/js/**",
                                                                "/activar-cuenta", "/establecer-password",
                                                                "/resultado-activacion", "/registro/**",
                                                                "/api/reniec")
                                                .permitAll()
                                                .requestMatchers("/cambiar-password-obligatorio").authenticated()
                                                .anyRequest().authenticated())
                                // ✅ Configurar manejo de excepciones
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) ->
                                                        response.sendRedirect("/login")))
                                // ✅ Usar nuestro filtro personalizado
                                .addFilterAt(dualLoginAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll());
                return http.build();
        }
}