package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record GmudRespostaDTO(
        Long id,
        Long ticketId,
        Long responsavelId,
        String responsavel,
        String modeloUtilizado,
        String ambiente,
        LocalDateTime dataAgendada,
        String riscosImpactos,
        String statusAprovacao,
        String nomeArquivoGerado,
        Long anexoId
) {}
