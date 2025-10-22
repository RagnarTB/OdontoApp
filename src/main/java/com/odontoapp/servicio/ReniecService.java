package com.odontoapp.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.odontoapp.dto.ReniecResponseDTO;

@Service
public class ReniecService {

    private final RestTemplate restTemplate;

    // Inyectamos el token desde application.properties (lo añadiremos después)
    @Value("${api.decolecta.token}")
    private String apiToken;

    public ReniecService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ReniecResponseDTO consultarDni(String dni) {
        String url = "https://api.decolecta.com/v1/reniec/dni?numero=" + dni;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ReniecResponseDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    ReniecResponseDTO.class);
            // Pequeña validación extra: asegúrate de que el cuerpo no sea nulo
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                System.err.println("Error: Respuesta exitosa pero cuerpo vacío de API Reniec para DNI: " + dni);
                return null; // O lanzar una excepción personalizada
            }

        } catch (HttpClientErrorException e) {
            // Errores 4xx (Cliente)
            if (e.getStatusCode().value() == 404) {
                System.err.println("API Reniec: DNI no encontrado: " + dni);
            } else if (e.getStatusCode().value() == 401) {
                System.err.println("API Reniec: Error de autorización. Verifica el token.");
            } else {
                System.err.println("Error del cliente al consultar API Reniec (" + e.getStatusCode() + "): "
                        + e.getResponseBodyAsString());
            }
            return null; // Devuelve null o lanza excepción específica
        } catch (HttpServerErrorException e) {
            // Errores 5xx (Servidor de la API)
            System.err.println(
                    "Error del servidor de API Reniec (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return null; // O lanzar excepción
        } catch (ResourceAccessException e) {
            // Errores de conexión (Timeout, DNS, etc.)
            System.err.println("Error de conexión al consultar API Reniec: " + e.getMessage());
            return null; // O lanzar excepción
        } catch (Exception e) {
            // Otros errores inesperados
            System.err.println("Error inesperado al consultar API Reniec: " + e.getMessage());
            // Considera loggear el stack trace completo: e.printStackTrace();
            return null; // O lanzar excepción
        }
    }
}