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

                                                // Cambiar password obligatorio - todos autenticados
                                                .requestMatchers("/cambiar-password-obligatorio").authenticated()

                                                // Selector de rol - todos autenticados
                                                .requestMatchers("/seleccionar-rol", "/seleccionar-rol/**").authenticated()

                                                // ADMIN: Acceso total
                                                .requestMatchers("/usuarios/**", "/roles/**").hasRole("ADMIN")

                                                // PACIENTE: Portal exclusivo para pacientes
                                                .requestMatchers("/paciente/**").hasRole("PACIENTE")

                                                // Dashboard general: NO permitir a PACIENTE (usan /paciente/dashboard)
                                                .requestMatchers("/", "/home", "/dashboard")
                                                .hasAnyRole("ADMIN", "ODONTOLOGO", "RECEPCIONISTA", "ALMACEN")

                                                // ODONTOLOGO: GESTIÓN CLÍNICA + FACTURACIÓN (sin inventario)
                                                .requestMatchers("/pacientes/**", "/servicios/**", "/facturacion/**",
                                                                "/tratamientos/**", "/odontograma/**", "/api/odontograma/**")
                                                .hasAnyRole("ODONTOLOGO", "ADMIN")

                                                // RECEPCIONISTA: Citas, pacientes y facturación
                                                .requestMatchers("/citas/**", "/agenda/**").hasAnyRole("RECEPCIONISTA", "ADMIN", "ODONTOLOGO")
                                                .requestMatchers("/pacientes/**").hasAnyRole("RECEPCIONISTA", "ADMIN", "ODONTOLOGO")
                                                .requestMatchers("/facturacion/**").hasAnyRole("RECEPCIONISTA", "ADMIN", "ODONTOLOGO")

                                                // ALMACEN: Solo inventario
                                                .requestMatchers("/insumos/**", "/categorias-insumo/**",
                                                                "/unidades-medida/**", "/movimientos-inventario/**")
                                                .hasAnyRole("ALMACEN", "ADMIN")

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
                                                .accessDeniedHandler(customAccessDeniedHandler));
                return http.build();
        }
}