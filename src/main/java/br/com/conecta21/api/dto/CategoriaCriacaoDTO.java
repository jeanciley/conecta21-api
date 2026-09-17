package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaCriacaoDTO(
        @NotBlank
        String nome
) {
}
