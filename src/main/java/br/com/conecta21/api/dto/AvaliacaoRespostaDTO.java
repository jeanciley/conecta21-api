package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record AvaliacaoRespostaDTO(
        Long id,
        Long chamadoId,
        Long solicitanteId,
        Integer nota,
        String comentario,
        LocalDateTime dataCriacao
) {
}
