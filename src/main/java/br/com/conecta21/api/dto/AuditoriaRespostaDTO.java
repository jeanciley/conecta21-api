package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record AuditoriaRespostaDTO(
        Long id,
        String nomeUsuario,
        String acao,
        String detalhes,
        LocalDateTime dataCriacao
) {
}
