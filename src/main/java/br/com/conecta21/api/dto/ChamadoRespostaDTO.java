package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record ChamadoRespostaDTO(
        Long id,
        Long empresaId,
        String titulo,
        String descricao,
        String status,
        Long solicitanteId,
        Long tecnicoId,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        String prioridade,
        LocalDateTime dataLimiteResolucao,
        String solicitanteNome,
        String tecnicoNome,
        Long categoriaId,
        String categoriaNome,
        LocalDateTime dataLimiteResposta
) {
    public ChamadoRespostaDTO(Long id, Long empresaId, String titulo, String descricao,
                              String status, Long solicitanteId, Long tecnicoId,
                              LocalDateTime dataAbertura, LocalDateTime dataFechamento) {
        this(id, empresaId, titulo, descricao, status, solicitanteId, tecnicoId,
                dataAbertura, dataFechamento, null, null, null, null, null, null, null);
    }
}
