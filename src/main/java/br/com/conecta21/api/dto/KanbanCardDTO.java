package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record KanbanCardDTO(
        Long id,
        String titulo,
        String prioridade,
        String status,
        String tipo,
        Long solicitanteId,
        Long tecnicoId,
        LocalDateTime dataAbertura,
        LocalDateTime dataLimiteResolucao
) {
    public KanbanCardDTO(Long id, String titulo, String prioridade, String status,
                         Long solicitanteId, Long tecnicoId,
                         LocalDateTime dataAbertura, LocalDateTime dataLimiteResolucao) {
        this(id, titulo, prioridade, status, null, solicitanteId, tecnicoId, dataAbertura, dataLimiteResolucao);
    }
}
