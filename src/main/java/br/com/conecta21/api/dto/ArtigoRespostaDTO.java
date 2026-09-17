package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record ArtigoRespostaDTO(
        Long id,
        String titulos,
        String conteudo,
        Long autorId,
        String nomeAutor,
        String nomeCategoria,
        LocalDateTime dataCriacao
) {
}
