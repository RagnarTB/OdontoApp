// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\servicio\EmailService.java
package com.odontoapp.servicio;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
}