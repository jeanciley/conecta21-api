package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record ArtigoRespostaDTO(
        Long id,
        String titulo,
        String conteudo,
        Long autorId,
        String nomeAutor,
        LocalDateTime dataCriacao
) {
}
