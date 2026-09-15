package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record InteracaoRespostaDTO(
        Long id,
        Long chamadoId,
        Long autorId,
        String autorNome,
        String mensagem,
        LocalDateTime dataCriacao
) {
}
