package br.com.conecta21.api.dto;

public record DashboardResponseDTO(
        long chamadosAbertos,
        long chamadosEmAndamento,
        long chamadosResolvidos
) {
}
