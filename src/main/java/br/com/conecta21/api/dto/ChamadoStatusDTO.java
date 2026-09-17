package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChamadoStatusDTO(
        @NotBlank
        @Size(max = 30)
        String status
) {
}
