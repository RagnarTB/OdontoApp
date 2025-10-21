package com.odontoapp.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String vistaLogin() {
        return "publico/login";
    }

    // Controlador para el dashboard (CORREGIDO)
    @GetMapping({ "/", "/dashboard" })
    public String verDashboard() {
        // Simplemente devuelve el nombre del archivo de la plantilla del contenido
        return "dashboard";
    }
}