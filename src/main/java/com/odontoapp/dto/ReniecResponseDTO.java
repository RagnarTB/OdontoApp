package com.odontoapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReniecResponseDTO {
    @JsonProperty("full_name")
    private String nombreCompleto;
}