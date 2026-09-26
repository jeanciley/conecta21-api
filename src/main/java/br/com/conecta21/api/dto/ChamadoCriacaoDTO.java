package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoCriacaoDTO(
        @NotBlank
        @Size(max = 150)
        String titulo,

        @NotBlank
        String descricao,

        @NotNull Long categoriaId,
        Long tecnicoId
) {
    public ChamadoCriacaoDTO(String titulo, String descricao, br.com.conecta21.api.model.PrioridadeChamado prioridade) {
        this(titulo, descricao, null, null);
    }
}
