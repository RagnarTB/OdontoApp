// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\servicio\EmailService.java
package com.odontoapp.servicio;

import com.odontoapp.entidad.Cita;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Este método envía el link para que el ADMIN creado por el sistema establezca
    // su password
    public void enviarEmailActivacionAdmin(String para, String nombre, String token) {
        String urlActivacion = "http://159.112.151.106:8080/activar-cuenta?token=" + token;
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
        String urlActivacion = "http://159.112.151.106:8080/registro/completar?token=" + token;
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
                + "<p>Puedes iniciar sesión en: <a href='http://159.112.151.106:8080/login'>http://159.112.151.106:8080/login</a></p>"
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

    /**
     * Envía email de recuperación de contraseña
     */
    public void enviarEmailRecuperacionPassword(String para, String nombre, String token) {
        String urlRestablecer = "http://159.112.151.106:8080/recuperar-password/restablecer?token=" + token;
        String subject = "Recuperar Contraseña - OdontoApp";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #007bff;'>Recuperar Contraseña</h2>"
                + "<p>Hola <strong>" + nombre + "</strong>,</p>"
                + "<p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en OdontoApp.</p>"
                + "<p>Haz clic en el siguiente enlace para establecer una nueva contraseña:</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "<a href=\"" + urlRestablecer + "\" "
                + "style='background-color: #007bff; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>"
                + "Restablecer Contraseña"
                + "</a>"
                + "</div>"
                + "<div style='background-color: #fff3cd; padding: 15px; border-left: 4px solid #ffc107; margin: 20px 0;'>"
                + "<p style='margin: 0; color: #856404;'><strong>⚠️ IMPORTANTE:</strong></p>"
                + "<p style='margin: 5px 0 0 0; color: #856404;'>Este enlace expirará en 24 horas por seguridad.</p>"
                + "<p style='margin: 5px 0 0 0; color: #856404;'>Si no solicitaste restablecer tu contraseña, ignora este email y tu contraseña permanecerá sin cambios.</p>"
                + "</div>"
                + "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>"
                + "<p style='color: #6c757d; font-size: 12px;'>Este es un mensaje automático, por favor no responder.</p>"
                + "</div>";

        try {
            enviarEmail(para, subject, content);
        } catch (MessagingException e) {
            System.err.println("Error al enviar email de recuperación de contraseña: " + e.getMessage());
            throw new RuntimeException("Error al enviar email de recuperación: " + e.getMessage());
        }
    }

    // ============ NOTIFICACIONES DE CITAS ============

    /**
     * Envía email de confirmación de cita al paciente
     */
    public void enviarConfirmacionCita(Cita cita) {
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");

        String fechaCita = cita.getFechaHoraInicio().format(formatoFecha);
        String horaCita = cita.getFechaHoraInicio().format(formatoHora);

        String subject = "Confirmación de Cita - OdontoApp";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #28a745;'><i>✓</i> Cita Confirmada</h2>"
                + "<p>Estimado(a) <strong>" + cita.getPaciente().getNombreCompleto() + "</strong>,</p>"
                + "<p>Su cita ha sido confirmada exitosamente. A continuación los detalles:</p>"
                + "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0;'>"
                + "<p><strong>📅 Fecha:</strong> " + fechaCita + "</p>"
                + "<p><strong>🕐 Hora:</strong> " + horaCita + "</p>"
                + "<p><strong>👨‍⚕️ Odontólogo:</strong> " + cita.getOdontologo().getNombreCompleto() + "</p>"
                + "<p><strong>🦷 Procedimiento:</strong> " + (cita.getProcedimiento() != null ? cita.getProcedimiento().getNombre() : "Por definir") + "</p>"
                + "</div>"
                + "<div style='background-color: #d1ecf1; padding: 15px; border-left: 4px solid #17a2b8; margin: 20px 0;'>"
                + "<p style='margin: 0; color: #0c5460;'><strong>ℹ️ Importante:</strong></p>"
                + "<p style='margin: 5px 0 0 0; color: #0c5460;'>Por favor llegue 10 minutos antes de su cita.</p>"
                + "</div>"
                + "<p>¿Necesita reprogramar o cancelar? Contáctenos lo antes posible.</p>"
                + "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>"
                + "<p style='color: #6c757d; font-size: 12px;'>Este es un mensaje automático de OdontoApp.</p>"
                + "</div>";

        try {
            enviarEmail(cita.getPaciente().getEmail(), subject, content);
            System.out.println("✓ Email de confirmación enviado a: " + cita.getPaciente().getEmail());
        } catch (MessagingException e) {
            System.err.println("Error al enviar email de confirmación de cita: " + e.getMessage());
        }
    }

    /**
     * Envía email de cancelación de cita al paciente
     */
    public void enviarCancelacionCita(Cita cita, String motivo) {
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");

        String fechaCita = cita.getFechaHoraInicio().format(formatoFecha);
        String horaCita = cita.getFechaHoraInicio().format(formatoHora);

        String subject = "Cita Cancelada - OdontoApp";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #dc3545;'><i>✗</i> Cita Cancelada</h2>"
                + "<p>Estimado(a) <strong>" + cita.getPaciente().getNombreCompleto() + "</strong>,</p>"
                + "<p>Le informamos que su cita ha sido <strong>cancelada</strong>:</p>"
                + "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0;'>"
                + "<p><strong>📅 Fecha:</strong> " + fechaCita + "</p>"
                + "<p><strong>🕐 Hora:</strong> " + horaCita + "</p>"
                + "<p><strong>👨‍⚕️ Odontólogo:</strong> " + cita.getOdontologo().getNombreCompleto() + "</p>"
                + "<p><strong>📝 Motivo:</strong> " + (motivo != null ? motivo : "No especificado") + "</p>"
                + "</div>"
                + "<p>Si desea agendar una nueva cita, no dude en contactarnos.</p>"
                + "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>"
                + "<p style='color: #6c757d; font-size: 12px;'>Este es un mensaje automático de OdontoApp.</p>"
                + "</div>";

        try {
            enviarEmail(cita.getPaciente().getEmail(), subject, content);
            System.out.println("✓ Email de cancelación enviado a: " + cita.getPaciente().getEmail());
        } catch (MessagingException e) {
            System.err.println("Error al enviar email de cancelación de cita: " + e.getMessage());
        }
    }

    /**
     * Envía email de reprogramación de cita al paciente
     */
    public void enviarReprogramacionCita(Cita citaAntigua, Cita citaNueva) {
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");

        String fechaAntigua = citaAntigua.getFechaHoraInicio().format(formatoFecha);
        String horaAntigua = citaAntigua.getFechaHoraInicio().format(formatoHora);
        String fechaNueva = citaNueva.getFechaHoraInicio().format(formatoFecha);
        String horaNueva = citaNueva.getFechaHoraInicio().format(formatoHora);

        String subject = "Cita Reprogramada - OdontoApp";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #fd7e14;'><i>↻</i> Cita Reprogramada</h2>"
                + "<p>Estimado(a) <strong>" + citaNueva.getPaciente().getNombreCompleto() + "</strong>,</p>"
                + "<p>Su cita ha sido reprogramada. A continuación los detalles:</p>"
                + "<div style='background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0;'>"
                + "<p style='margin: 0; text-decoration: line-through; color: #856404;'><strong>Fecha anterior:</strong> " + fechaAntigua + " a las " + horaAntigua + "</p>"
                + "</div>"
                + "<div style='background-color: #d4edda; padding: 20px; border-radius: 5px; margin: 20px 0; border: 2px solid #28a745;'>"
                + "<p style='margin: 0; color: #155724;'><strong>📅 Nueva Fecha:</strong> " + fechaNueva + "</p>"
                + "<p style='margin: 5px 0 0 0; color: #155724;'><strong>🕐 Nueva Hora:</strong> " + horaNueva + "</p>"
                + "<p style='margin: 5px 0 0 0; color: #155724;'><strong>👨‍⚕️ Odontólogo:</strong> " + citaNueva.getOdontologo().getNombreCompleto() + "</p>"
                + "</div>"
                + "<p>Por favor confirme su asistencia o contáctenos si necesita hacer algún cambio.</p>"
                + "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>"
                + "<p style='color: #6c757d; font-size: 12px;'>Este es un mensaje automático de OdontoApp.</p>"
                + "</div>";

        try {
            enviarEmail(citaNueva.getPaciente().getEmail(), subject, content);
            System.out.println("✓ Email de reprogramación enviado a: " + citaNueva.getPaciente().getEmail());
        } catch (MessagingException e) {
            System.err.println("Error al enviar email de reprogramación de cita: " + e.getMessage());
        }
    }

    /**
     * Envía email recordatorio de cita (24 horas antes)
     */
    public void enviarRecordatorioCita(Cita cita) {
        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");

        String fechaCita = cita.getFechaHoraInicio().format(formatoFecha);
        String horaCita = cita.getFechaHoraInicio().format(formatoHora);

        String subject = "Recordatorio de Cita - Mañana - OdontoApp";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #17a2b8;'><i>🔔</i> Recordatorio de Cita</h2>"
                + "<p>Estimado(a) <strong>" + cita.getPaciente().getNombreCompleto() + "</strong>,</p>"
                + "<p>Le recordamos que tiene una cita <strong>mañana</strong>:</p>"
                + "<div style='background-color: #d1ecf1; padding: 20px; border-radius: 5px; border: 2px solid #17a2b8; margin: 20px 0;'>"
                + "<p><strong>📅 Fecha:</strong> " + fechaCita + "</p>"
                + "<p><strong>🕐 Hora:</strong> " + horaCita + "</p>"
                + "<p><strong>👨‍⚕️ Odontólogo:</strong> " + cita.getOdontologo().getNombreCompleto() + "</p>"
                + "<p><strong>🦷 Procedimiento:</strong> " + (cita.getProcedimiento() != null ? cita.getProcedimiento().getNombre() : "Por definir") + "</p>"
                + "</div>"
                + "<div style='background-color: #fff3cd; padding: 15px; border-left: 4px solid #ffc107; margin: 20px 0;'>"
                + "<p style='margin: 0; color: #856404;'><strong>⚠️ Importante:</strong></p>"
                + "<p style='margin: 5px 0 0 0; color: #856404;'>Por favor llegue 10 minutos antes. Si no puede asistir, contáctenos lo antes posible.</p>"
                + "</div>"
                + "<hr style='margin: 30px 0; border: none; border-top: 1px solid #ddd;'>"
                + "<p style='color: #6c757d; font-size: 12px;'>Este es un mensaje automático de OdontoApp.</p>"
                + "</div>";

        try {
            enviarEmail(cita.getPaciente().getEmail(), subject, content);
            System.out.println("✓ Email recordatorio enviado a: " + cita.getPaciente().getEmail());
        } catch (MessagingException e) {
            System.err.println("Error al enviar email recordatorio de cita: " + e.getMessage());
        }
    }
}