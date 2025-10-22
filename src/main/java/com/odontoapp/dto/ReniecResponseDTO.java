package com.odontoapp.dto;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty; // Importar PostConstruct si usas Spring >= 6, o javax.annotation.PostConstruct si usas Spring < 6 o Jakarta EE

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Ignorar campos no mapeados que puedan venir
public class ReniecResponseDTO {

    // Mantenemos los campos originales mapeados
    @JsonProperty("full_name")
    private String nombreCompleto;

    @JsonProperty("first_name")
    private String nombres;

    @JsonProperty("first_last_name")
    private String apellidoPaterno;

    @JsonProperty("second_last_name")
    private String apellidoMaterno;

    @JsonProperty("document_number")
    private String dni;

    /**
     * Este método NO es el getter principal, se usa internamente
     * para construir el nombre si full_name falta.
     * Usamos StringUtils para manejar nulos/vacíos de forma segura.
     */
    private String construirNombreCompleto() {
        if (StringUtils.hasText(this.nombres) && StringUtils.hasText(this.apellidoPaterno)) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.apellidoPaterno.trim());
            if (StringUtils.hasText(this.apellidoMaterno)) {
                sb.append(" ").append(this.apellidoMaterno.trim());
            }
            sb.append(" ").append(this.nombres.trim());
            // Reemplazar múltiples espacios con uno solo
            return sb.toString().replaceAll("\\s+", " ");
        }
        return null; // No se puede construir
    }

    /**
     * Sobrescribimos el getter generado por Lombok para aplicar la lógica.
     * Si nombreCompleto (de full_name) existe, lo devuelve.
     * Si no, intenta construirlo y lo devuelve.
     * Si no se puede construir, devuelve null.
     * IMPORTANTE: También asigna el valor construido al campo nombreCompleto
     * para que la serialización funcione correctamente.
     */
    public String getNombreCompleto() {
        if (!StringUtils.hasText(this.nombreCompleto)) {
            // Intenta construirlo solo si el campo está vacío
            String construido = construirNombreCompleto();
            if (construido != null) {
                this.nombreCompleto = construido; // Asigna al campo para la serialización
            }
        }
        return this.nombreCompleto; // Devuelve el valor del campo (original o construido)
    }

    // Opcional: Puedes añadir un método post-deserialización si el getter no
    // funciona
    // @PostConstruct // O @javax.annotation.PostConstruct si aplica
    // public void inicializarNombreCompleto() {
    // getNombreCompleto(); // Llama al getter para forzar la lógica de construcción
    // }

    // Puedes añadir getters individuales si los necesitas en algún otro lugar
    // public String getNombres() { return nombres; }
    // public String getApellidoPaterno() { return apellidoPaterno; }
    // public String getApellidoMaterno() { return apellidoMaterno; }
    // public String getDni() { return dni; }

}