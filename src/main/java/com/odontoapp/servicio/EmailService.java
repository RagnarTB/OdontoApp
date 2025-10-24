// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\servicio\EmailService.java
package com.odontoapp.servicio;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Este método envía el link para que el ADMIN creado por el sistema establezca
    // su password
    public void enviarEmailActivacionAdmin(String para, String nombre, String token) {
        String urlActivacion = "http://localhost:8080/activar-cuenta?token=" + token;
        String subject = "Activa tu cuenta en OdontoApp (Personal)";
        String content = "<p>Hola " + nombre + ",</p>"
                + "<p>Tu cuenta de personal ha sido creada en OdontoApp.</p>"
                + "<p>Haz clic en el siguiente enlace para establecer tu contraseña y activar tu acceso:</p>"
                + "<h3><a href=\"" + urlActivacion + "\">Establecer Contraseña</a></h3>"
                + "<p>Si no esperabas este email, por favor contacta al administrador.</p>";
        try {
            enviarEmail(para, subject, content);
        } catch (MessagingException e) {
            System.err.println("Error al enviar email de activación para ADMIN: " + e.getMessage());
        }
    }

    // 🔥 NUEVO MÉTODO para el link de registro de PACIENTE (Flujo Self-Service)
    public void enviarEmailActivacion(String para, String nombre, String token) {
        String urlActivacion = "http://localhost:8080/registro/completar?token=" + token;
        String subject = "Completa tu registro en OdontoApp";
        String content = "<p>Hola " + nombre + ",</p>"
                + "<p>Gracias por iniciar tu registro en OdontoApp.</p>"
                + "<p>Haz clic en el siguiente enlace para completar tus datos y crear tu contraseña:</p>"
                + "<h3><a href=\"" + urlActivacion + "\">Completar Registro</a></h3>"
                + "<p>Si no te registraste, por favor ignora este email.</p>";
        try {
            enviarEmail(para, subject, content);
        } catch (MessagingException e) {
            System.err.println("Error al enviar email de activación para PACIENTE: " + e.getMessage());
        }
    }

    private void enviarEmail(String para, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(para);
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }

    public void enviarPasswordTemporal(String para, String nombre, String passwordTemporal) {
        String subject = "Bienvenido a OdontoApp - Credenciales de Acceso";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #007bff;'>¡Bienvenido a OdontoApp!</h2>"
                + "<p>Hola <strong>" + nombre + "</strong>,</p>"
                + "<p>Se ha creado tu cuenta en el sistema. A continuación tus credenciales de acceso:</p>"
                + "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>"
                + "<p><strong>Email:</strong> " + para + "</p>"
                + "<p><strong>Contraseña Temporal:</strong> <code style='background-color: #fff; padding: 5px; border: 1px solid #ddd; font-size: 16px;'>"
                + passwordTemporal + "</code></p>"
                + "</div>"
                + "<div style='background-color: #fff3cd; padding: 15px; border-left: 4px solid #ffc107; margin: 20px 0;'>"
                + "<p style='margin: 0; color: #856404;'><strong>⚠️ IMPORTANTE:</strong></p>"
                + "<p style='margin: 5px 0 0 0; color: #856404;'>Por seguridad, deberás cambiar esta contraseña en tu primer inicio de sesión.</p>"
                + "</div>"
                + "<p>Puedes iniciar sesión en: <a href='http://localhost:8080/login'>http://localhost:8080/login</a></p>"
                + "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>"
                + "<p style='color: #6c757d; font-size: 12px;'>Este es un mensaje automático, por favor no responder.</p>"
                + "</div>";

        try {
            enviarEmail(para, subject, content);
        } catch (MessagingException e) {
            System.err.println("Error al enviar email con contraseña temporal: " + e.getMessage());
            throw new RuntimeException("Error al enviar email con contraseña temporal: " + e.getMessage());
        }
    }
}