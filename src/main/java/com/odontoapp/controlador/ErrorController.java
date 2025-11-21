package com.odontoapp.controlador;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador para manejar páginas de error personalizadas
 */
@Controller
@RequestMapping("/error")
public class ErrorController {

    /**
     * Página de error 403 - Acceso Denegado
     */
    @GetMapping("/403")
    public String error403(HttpServletRequest request, Model model) {
        // Obtener el mensaje de error de la sesión si existe
        String errorMessage = (String) request.getSession().getAttribute("errorMessage");
        String errorDetails = (String) request.getSession().getAttribute("errorDetails");

        if (errorMessage == null) {
            errorMessage = "🚫 No tienes permisos para acceder a este recurso.";
        }

        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("errorDetails", errorDetails);
        model.addAttribute("errorCode", "403");
        model.addAttribute("errorTitle", "Acceso Denegado");

        // Limpiar mensajes de sesión después de usarlos
        request.getSession().removeAttribute("errorMessage");
        request.getSession().removeAttribute("errorDetails");

        return "error/403";
    }

    /**
     * Página de error 404 - No Encontrado
     */
    @GetMapping("/404")
    public String error404(Model model) {
        model.addAttribute("errorMessage", "🔍 La página que buscas no existe.");
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Página No Encontrada");
        return "error/404";
    }

    /**
     * Página de error genérico
     */
    @GetMapping("/500")
    public String error500(Model model) {
        model.addAttribute("errorMessage", "⚠️ Ha ocurrido un error interno en el servidor.");
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "Error Interno");
        return "error/500";
    }
}
