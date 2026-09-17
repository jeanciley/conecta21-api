package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ArtigoCriacaoDTO(
        @NotBlank
        @Size(max = 150)
        String titulo,

        @NotBlank
        String conteudo,

        @NotNull
        Long categoriaId
) {
}
