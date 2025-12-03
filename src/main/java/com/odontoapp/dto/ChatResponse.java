package com.odontoapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar respuestas del chatbot al frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String respuesta;
    private Long conversacionId;
    private Boolean exito;
    private String error;

    // Constructor para respuestas exitosas
    public ChatResponse(String respuesta, Long conversacionId) {
        this.respuesta = respuesta;
        this.conversacionId = conversacionId;
        this.exito = true;
    }

    // Método estático para crear respuestas de error
    public static ChatResponse error(String mensajeError) {
        ChatResponse response = new ChatResponse();
        response.setExito(false);
        response.setError(mensajeError);
        return response;
    }
}
