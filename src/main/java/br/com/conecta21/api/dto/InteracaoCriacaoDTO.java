package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InteracaoCriacaoDTO(
        @NotBlank
        @Size(max = 4000)
        String mensagem,

        @Size(max = 40)
        String tipo
) {
    public InteracaoCriacaoDTO(String mensagem) {
        this(mensagem, null);
    }
}
