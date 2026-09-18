package br.com.conecta21.api.dto;

import br.com.conecta21.api.model.PrioridadeChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoCriacaoDTO(
        @NotBlank(message = "O título é obrigatório") String titulo,
        @NotBlank(message = "A descrição é obrigatória") String descricao,
        String prioridade,
        String tipo
) {
}
