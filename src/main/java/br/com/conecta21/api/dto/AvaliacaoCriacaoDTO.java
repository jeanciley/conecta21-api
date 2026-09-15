package br.com.conecta21.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoCriacaoDTO(
        @NotNull
        @Min(1)
        @Max(5)
        Integer nota,

        @Size(max = 1000)
        String comentario
) {
}
