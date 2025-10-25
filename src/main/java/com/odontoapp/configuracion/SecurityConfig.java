package com.odontoapp.configuracion;

import org.springframework.beans.factory.annotation.Autowired; // Importar
import org.springframework.context.annotation.Bean; // Importar
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.odontoapp.seguridad.CustomAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        // Inyecta tu handler personalizado
        @Autowired
        private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/login", "/adminlte/**", "/css/**", "/js/**",
                                                                "/activar-cuenta", "/establecer-password",
                                                                "/resultado-activacion", "/registro/**",
                                                                "/api/reniec")
                                                .permitAll()
                                                .requestMatchers("/cambiar-password-obligatorio").authenticated() // 🔥
                                                                                                                  // NUEVA
                                                                                                                  // LÍNEA
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .successHandler(customAuthenticationSuccessHandler)
                                                .failureUrl("/login?error=true")

                                                .permitAll())
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