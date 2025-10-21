package com.odontoapp.servicio;

import com.odontoapp.dto.ReniecResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
            return response.getBody();
        } catch (Exception e) {
            // Manejo básico de errores. En una app real, esto sería más robusto.
            System.err.println("Error al consultar API de Reniec: " + e.getMessage());
            return null;
        }
    }
}