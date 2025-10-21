package com.odontoapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReniecResponseDTO {

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

    // Lógica para construir el nombre si "full_name" no viene
    public String getNombreCompleto() {
        if (this.nombreCompleto != null && !this.nombreCompleto.isEmpty()) {
            return this.nombreCompleto;
        }
        if (this.nombres != null && this.apellidoPaterno != null) {
            return (this.apellidoPaterno + " " + this.apellidoMaterno + " " + this.nombres).trim().replaceAll("\\s+",
                    " ");
        }
        return null;
    }
}