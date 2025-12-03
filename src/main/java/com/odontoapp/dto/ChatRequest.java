package com.odontoapp.dto;

import lombok.Data;

/**
 * DTO para recibir mensajes del usuario desde el frontend
 */
@Data
public class ChatRequest {
    private String mensaje;
    private Long conversacionId; // Opcional: para mantener contexto de conversación
}
