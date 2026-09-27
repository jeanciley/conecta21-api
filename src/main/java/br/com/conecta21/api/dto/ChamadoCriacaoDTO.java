package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.conecta21.api.model.TipoChamado;

public record ChamadoCriacaoDTO(
        @NotBlank
        @Size(max = 150)
        String titulo,

        @NotBlank
        String descricao,

        @NotNull Long categoriaId,
        Long tecnicoId,
        boolean interno,
        TipoChamado tipo
) {
    public ChamadoCriacaoDTO(String titulo, String descricao, Long categoriaId, Long tecnicoId, boolean interno) {
        this(titulo, descricao, categoriaId, tecnicoId, interno, null);
    }

    public ChamadoCriacaoDTO(String titulo, String descricao, br.com.conecta21.api.model.PrioridadeChamado prioridade) {
        this(titulo, descricao, null, null, false, null);
    }
}
