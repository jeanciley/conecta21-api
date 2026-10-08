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
        boolean interno,
        TipoChamado tipo
) {
    public ChamadoCriacaoDTO(String titulo, String descricao, Long categoriaId, boolean interno) {
        this(titulo, descricao, categoriaId, interno, null);
    }

    /** Mantido para compatibilidade de código legado; tecnicoId não participa da criação. */
    @Deprecated
    public ChamadoCriacaoDTO(String titulo, String descricao, Long categoriaId, Long tecnicoId, boolean interno) {
        this(titulo, descricao, categoriaId, interno, null);
    }

    public ChamadoCriacaoDTO(String titulo, String descricao, br.com.conecta21.api.model.PrioridadeChamado prioridade) {
        this(titulo, descricao, null, false, null);
    }
}
