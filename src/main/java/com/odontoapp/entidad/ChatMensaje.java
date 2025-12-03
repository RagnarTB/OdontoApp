package com.odontoapp.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad para almacenar el historial de conversaciones del chatbot.
 * Cada mensaje del usuario y su respuesta se guardan en la base de datos.
 */
@Entity
@Table(name = "chat_mensajes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensajeUsuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String respuestaBot;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private Boolean eliminado = false;

    @PrePersist
    protected void onCreate() {
        fechaHora = LocalDateTime.now();
    }
}
