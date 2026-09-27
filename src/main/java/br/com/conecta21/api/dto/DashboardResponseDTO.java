package br.com.conecta21.api.dto;

import java.util.Map;

public record DashboardResponseDTO(
        long chamadosAbertos,
        long chamadosEmAndamento,
        long chamadosResolvidos,
        long chamadosEmAtraso,
        Map<String, Long> chamadosPorCategoria,
        Map<String, Long> chamadosPorStatus,
        long slaCumpridos,
        long slaViolados,
        long slaRespostaCumprido,
        long slaRespostaViolado
) {
}
