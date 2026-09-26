package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaCriacaoDTO(
        @NotBlank
        @Size(max = 50)
        String nome,
        @jakarta.validation.constraints.NotNull Long prioridadeId
) {
}
