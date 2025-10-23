// Archivo: C:\proyectos\nuevo\odontoapp\src\main\java\com\odontoapp\servicio\ReniecService.java
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

    // 🔥 Modificado para usar un DNI de ejemplo si no hay token (solo en
    // desarrollo)
    @Value("${api.decolecta.token:dummy_token}")
    private String apiToken;

    public ReniecService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    // 🔥 MODIFICADO: Renombrado a consultarDni
    public ReniecResponseDTO consultarDni(String dni) {

        // Simulación si no hay token real o es solo "dummy_token"
        if ("dummy_token".equals(apiToken)) {
            System.out.println(">>> Modo Demo: Usando datos de prueba para DNI.");
            if ("12345678".equals(dni)) {
                ReniecResponseDTO demo = new ReniecResponseDTO();
                demo.setNombres("JUAN CARLOS");
                demo.setApellidoPaterno("PEREZ");
                demo.setApellidoMaterno("LOPEZ");
                demo.setDni(dni);
                return demo;
            }
            return null;
        }

        String url = "https://api.decolecta.com/v1/reniec/dni?numero=" + dni;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<ReniecResponseDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    ReniecResponseDTO.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                System.err.println("Error: Respuesta exitosa pero cuerpo vacío de API Reniec para DNI: " + dni);
                return null;
            }

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                System.err.println("API Reniec: DNI no encontrado: " + dni);
            } else if (e.getStatusCode().value() == 401) {
                System.err.println("API Reniec: Error de autorización. Verifica el token.");
            } else {
                System.err.println("Error del cliente al consultar API Reniec (" + e.getStatusCode() + "): "
                        + e.getResponseBodyAsString());
            }
            return null;
        } catch (HttpServerErrorException e) {
            System.err.println(
                    "Error del servidor de API Reniec (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return null;
        } catch (ResourceAccessException e) {
            System.err.println("Error de conexión al consultar API Reniec: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error inesperado al consultar API Reniec: " + e.getMessage());
            return null;
        }
    }
}