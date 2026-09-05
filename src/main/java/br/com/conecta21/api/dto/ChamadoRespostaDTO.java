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
        LocalDateTime dataFechamento
) {
}
