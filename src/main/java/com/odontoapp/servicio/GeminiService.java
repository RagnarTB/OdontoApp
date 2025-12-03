package com.odontoapp.servicio;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para integración con Google Gemini API.
 * Maneja la comunicación con la API de IA de Google.
 */
@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash-latest}")
    private String model;

    private final WebClient webClient;
    private final Gson gson;

    public GeminiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.gson = new Gson();
    }

    /**
     * Genera una respuesta usando Gemini API
     * 
     * @param prompt El prompt completo (instrucciones del sistema + pregunta del
     *               usuario)
     * @return La respuesta generada por Gemini
     */
    public String generarRespuesta(String prompt) {
        try {
            log.info("Iniciando llamada a Gemini API...");

            // Construir el request body según la API de Gemini
            Map<String, Object> requestBody = new HashMap<>();

            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();

            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);

            content.put("parts", parts);
            contents.add(content);

            requestBody.put("contents", contents);

            // Configuración de generación
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.put("generationConfig", generationConfig);

            // Llamar a la API v1beta (requerida por Google AI Studio)
            // La API key debe ir como query parameter, no como header
            String response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("Error de respuesta de Gemini API: Status={}, Body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.just("{\"error\": \"API Error\"}");
                    })
                    .block();

            log.info("Respuesta recibida de Gemini API");

            // Parsear la respuesta
            return extraerTextoRespuesta(response);

        } catch (Exception e) {
            log.error("Error al llamar a Gemini API: {}", e.getMessage(), e);
            return "Lo siento, ocurrió un error al procesar tu mensaje. Por favor, intenta nuevamente.";
        }
    }

    /**
     * Extrae el texto de la respuesta JSON de Gemini
     */
    private String extraerTextoRespuesta(String jsonResponse) {
        try {
            log.debug("Parseando respuesta JSON: {}", jsonResponse);

            JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);

            // Verificar si hay error en la respuesta
            if (jsonObject.has("error")) {
                log.error("Error en respuesta de Gemini: {}", jsonObject.get("error"));
                return "Error al comunicarse con el servicio de IA. Por favor, verifica tu API key.";
            }

            // Verificación extra de seguridad por si la respuesta viene vacía
            if (!jsonObject.has("candidates") || jsonObject.getAsJsonArray("candidates").size() == 0) {
                return "Lo siento, no pude generar una respuesta válida para esa consulta.";
            }

            return jsonObject
                    .getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            log.error("Error al parsear respuesta de Gemini: {}", e.getMessage(), e);
            log.error("Respuesta JSON recibida: {}", jsonResponse);
            return "Error al procesar la respuesta.";
        }
    }
}