package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChamadoCriacaoDTO(
        @NotBlank
        @Size(max = 150)
        String titulo,

        @NotBlank
        String descricao
) {
}
