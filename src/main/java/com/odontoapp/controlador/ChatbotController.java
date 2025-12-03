package com.odontoapp.controlador;

import com.odontoapp.dto.ChatRequest;
import com.odontoapp.dto.ChatResponse;
import com.odontoapp.entidad.ChatMensaje;
import com.odontoapp.servicio.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el chatbot IA.
 * Expone endpoints para enviar mensajes y obtener historial.
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Endpoint para enviar un mensaje al chatbot y recibir respuesta
     * 
     * @param request Objeto con el mensaje del usuario
     * @return Respuesta del chatbot con el texto generado
     */
    @PostMapping("/mensaje")
    public ResponseEntity<ChatResponse> enviarMensaje(@RequestBody ChatRequest request) {

        // Validar que el mensaje no esté vacío
        if (request.getMensaje() == null || request.getMensaje().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ChatResponse.error("El mensaje no puede estar vacío."));
        }

        // Validar longitud del mensaje
        if (request.getMensaje().length() > 500) {
            return ResponseEntity.badRequest()
                    .body(ChatResponse.error("El mensaje es demasiado largo. Máximo 500 caracteres."));
        }

        // Procesar el mensaje
        ChatResponse response = chatbotService.procesarMensaje(request);

        if (response.getExito()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para obtener el historial de conversación del paciente
     * 
     * @return Lista de los últimos 10 mensajes
     */
    @GetMapping("/historial")
    public ResponseEntity<List<ChatMensaje>> obtenerHistorial() {
        List<ChatMensaje> historial = chatbotService.obtenerHistorial();
        return ResponseEntity.ok(historial);
    }
}
