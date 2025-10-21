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

    public void enviarEmailActivacion(String para, String nombre, String token) {
        String urlActivacion = "http://localhost:8080/activar-cuenta?token=" + token;
        String subject = "Activa tu cuenta en OdontoApp";
        String content = "<p>Hola " + nombre + ",</p>"
                + "<p>Gracias por registrarte en OdontoApp.</p>"
                + "<p>Haz clic en el siguiente enlace para activar tu cuenta y establecer tu contraseña:</p>"
                + "<h3><a href=\"" + urlActivacion + "\">Activar Cuenta</a></h3>"
                + "<p>Si no te registraste, por favor ignora este email.</p>";

        try {
            enviarEmail(para, subject, content);
        } catch (MessagingException e) {
            // Manejo de errores
            System.err.println("Error al enviar email de activación: " + e.getMessage());
        }
    }

    private void enviarEmail(String para, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(para);
        helper.setSubject(subject);
        helper.setText(content, true); // true para indicar que es HTML
        mailSender.send(message);
    }
}