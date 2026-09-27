package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record InteracaoRespostaDTO(
        Long id,
        Long chamadoId,
        Long autorId,
        String autorNome,
        String tipo,
        String mensagem,
        LocalDateTime dataCriacao,
        java.util.List<AnexoInteracaoRespostaDTO> anexos
) {
    public InteracaoRespostaDTO(Long id, Long chamadoId, Long autorId, String autorNome,
                                String mensagem, LocalDateTime dataCriacao) {
        this(id, chamadoId, autorId, autorNome, "Comentário", mensagem, dataCriacao, java.util.List.of());
    }
}
