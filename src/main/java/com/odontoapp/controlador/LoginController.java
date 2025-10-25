package com.odontoapp.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String vistaLogin() {
        return "publico/login";
    }

    @GetMapping({ "/", "/dashboard" })
    public String verDashboard() {
        return "dashboard";
    }

    @GetMapping("/paciente/dashboard")
    public String verDashboardPaciente() {
        return "paciente/dashboard";
    }

}